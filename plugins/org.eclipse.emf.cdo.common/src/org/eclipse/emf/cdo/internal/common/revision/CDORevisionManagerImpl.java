/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Simon McDuff - bug 201266
 *    Simon McDuff - bug 230832
 */
package org.eclipse.emf.cdo.internal.common.revision;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.branch.CDOBranchVersion;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.CDORevisionFactory;
import org.eclipse.emf.cdo.common.revision.cache.CDORevisionCache;
import org.eclipse.emf.cdo.common.revision.cache.CDORevisionCacheUtil;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionManager;

import org.eclipse.net4j.util.ReflectUtil.ExcludeFromDump;
import org.eclipse.net4j.util.lifecycle.Lifecycle;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Eike Stepper
 */
public class CDORevisionManagerImpl extends Lifecycle implements InternalCDORevisionManager
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_REVISION, CDORevisionManagerImpl.class);

  private boolean supportingBranches;

  private RevisionLoader revisionLoader;

  private RevisionLocker revisionLocker;

  private CDORevisionFactory factory;

  private CDORevisionCache cache;

  @ExcludeFromDump
  private Object loadAndAddLock = new Object();

  @ExcludeFromDump
  private Object revisedLock = new Object();

  public CDORevisionManagerImpl()
  {
  }

  public boolean isSupportingBranches()
  {
    return supportingBranches;
  }

  public void setSupportingBranches(boolean on)
  {
    checkInactive();
    supportingBranches = on;
  }

  public RevisionLoader getRevisionLoader()
  {
    return revisionLoader;
  }

  public void setRevisionLoader(RevisionLoader revisionLoader)
  {
    checkInactive();
    this.revisionLoader = revisionLoader;
  }

  public RevisionLocker getRevisionLocker()
  {
    return revisionLocker;
  }

  public void setRevisionLocker(RevisionLocker revisionLocker)
  {
    checkInactive();
    this.revisionLocker = revisionLocker;
  }

  public CDORevisionFactory getFactory()
  {
    return factory;
  }

  public void setFactory(CDORevisionFactory factory)
  {
    checkInactive();
    this.factory = factory;
  }

  public CDORevisionCache getCache()
  {
    return cache;
  }

  public void setCache(CDORevisionCache cache)
  {
    checkInactive();
    this.cache = cache;
  }

  public EClass getObjectType(CDOID id)
  {
    return cache.getObjectType(id);
  }

  public boolean containsRevision(CDOID id, CDOBranchPoint branchPoint)
  {
    if (supportingBranches)
    {
      return getRevision(id, branchPoint, CDORevision.UNCHUNKED, CDORevision.DEPTH_NONE, false) != null;
    }

    return getCachedRevision(id, branchPoint) != null;
  }

  public boolean containsRevisionByVersion(CDOID id, CDOBranchVersion branchVersion)
  {
    return cache.getRevisionByVersion(id, branchVersion) != null;
  }

  public void reviseLatest(CDOID id, CDOBranch branch)
  {
    acquireAtomicRequestLock(revisedLock);

    try
    {
      InternalCDORevision revision = (InternalCDORevision)cache.getRevision(id, branch.getHead());
      if (revision != null)
      {
        cache.removeRevision(id, branch.getVersion(revision.getVersion()));
      }
    }
    finally
    {
      releaseAtomicRequestLock(revisedLock);
    }
  }

  public void reviseVersion(CDOID id, CDOBranchVersion branchVersion, long timeStamp)
  {
    acquireAtomicRequestLock(revisedLock);

    try
    {
      InternalCDORevision revision = getCachedRevision(id, branchVersion);
      if (revision != null)
      {
        if (timeStamp == CDORevision.UNSPECIFIED_DATE)
        {
          cache.removeRevision(id, branchVersion);
        }
        else
        {
          revision.setRevised(timeStamp - 1);
        }
      }
    }
    finally
    {
      releaseAtomicRequestLock(revisedLock);
    }
  }

  public InternalCDORevision getRevision(CDOID id, CDOBranchPoint branchPoint, int referenceChunk, int prefetchDepth,
      boolean loadOnDemand)
  {
    Set<CDOID> ids = Collections.singleton(id);
    List<CDORevision> revisions = getRevisions(ids, branchPoint, referenceChunk, prefetchDepth, loadOnDemand);
    return (InternalCDORevision)revisions.get(0);
  }

  public List<CDORevision> getRevisions(Collection<CDOID> ids, CDOBranchPoint branchPoint, int referenceChunk,
      int prefetchDepth, boolean loadOnDemand)
  {
    List<CDORevision> revisions = new ArrayList<CDORevision>(ids.size());
    List<CDOID> missingIDs = getMissingIDs(ids, branchPoint, loadOnDemand, revisions);
    if (missingIDs != null)
    {
      acquireAtomicRequestLock(loadAndAddLock);

      try
      {
        List<InternalCDORevision> missingRevisions = //
        revisionLoader.loadRevisions(missingIDs, branchPoint, referenceChunk, prefetchDepth);

        Iterator<InternalCDORevision> it = missingRevisions.iterator();
        for (int i = 0; i < revisions.size(); i++)
        {
          CDORevision revision = revisions.get(i);
          if (revision == null)
          {
            InternalCDORevision missingRevision = it.next();
            revisions.set(i, missingRevision);
            addCachedRevisionIfNotNull(missingRevision);
          }
        }
      }
      finally
      {
        releaseAtomicRequestLock(loadAndAddLock);
      }
    }

    return revisions;
  }

  private List<CDOID> getMissingIDs(Collection<CDOID> ids, CDOBranchPoint branchPoint, boolean loadOnDemand,
      List<CDORevision> revisions)
  {
    List<CDOID> missingIDs = null;
    for (CDOID id : ids)
    {
      InternalCDORevision revision = getCachedRevision(id, branchPoint);
      revisions.add(revision);
      if (revision == null && loadOnDemand)
      {
        if (missingIDs == null)
        {
          missingIDs = new ArrayList<CDOID>(1);
        }

        missingIDs.add(id);
      }
    }

    return missingIDs;
  }

  public InternalCDORevision getRevisionByVersion(CDOID id, CDOBranchVersion branchVersion, int referenceChunk,
      boolean loadOnDemand)
  {
    acquireAtomicRequestLock(loadAndAddLock);

    try
    {
      InternalCDORevision revision = getCachedRevision(id, branchVersion);
      if (revision == null)
      {
        if (loadOnDemand)
        {
          if (TRACER.isEnabled())
          {
            TRACER.format("Loading revision {0} from {1}", id, branchVersion); //$NON-NLS-1$
          }

          revision = revisionLoader.loadRevisionByVersion(id, branchVersion, referenceChunk);
          addCachedRevisionIfNotNull(revision);
        }
      }

      return revision;
    }
    finally
    {
      releaseAtomicRequestLock(loadAndAddLock);
    }
  }

  @Override
  protected void doBeforeActivate() throws Exception
  {
    super.doBeforeActivate();
    if (factory == null)
    {
      factory = CDORevisionFactory.DEFAULT;
    }

    if (cache == null)
    {
      cache = CDORevisionCacheUtil.createDefaultCache(supportingBranches);
    }

    if (supportingBranches && !cache.isSupportingBranches())
    {
      throw new IllegalStateException("Revision cache does not support branches");
    }
  }

  @Override
  protected void doActivate() throws Exception
  {
    super.doActivate();
    LifecycleUtil.activate(cache);
  }

  @Override
  protected void doDeactivate() throws Exception
  {
    LifecycleUtil.deactivate(cache);
    super.doDeactivate();
  }

  private void acquireAtomicRequestLock(Object key)
  {
    if (revisionLocker != null)
    {
      revisionLocker.acquireAtomicRequestLock(key);
    }
  }

  private void releaseAtomicRequestLock(Object key)
  {
    if (revisionLocker != null)
    {
      revisionLocker.releaseAtomicRequestLock(key);
    }
  }

  private InternalCDORevision getCachedRevision(CDOID id, CDOBranchPoint branchPoint)
  {
    InternalCDORevision revision = (InternalCDORevision)cache.getRevision(id, branchPoint);
    if (supportingBranches)
    {
    }

    return revision;
  }

  private InternalCDORevision getCachedRevision(CDOID id, CDOBranchVersion branchVersion)
  {
    return (InternalCDORevision)cache.getRevisionByVersion(id, branchVersion);
  }

  private void addCachedRevisionIfNotNull(CDORevision revision)
  {
    if (revision != null)
    {
      cache.addRevision(revision);
    }
  }
}
