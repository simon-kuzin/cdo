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
import org.eclipse.emf.cdo.spi.common.revision.StubCDORevision;

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

/**
 * @author Eike Stepper
 */
public class CDORevisionManagerImpl extends Lifecycle implements InternalCDORevisionManager
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_REVISION, CDORevisionManagerImpl.class);

  private static final InternalCDORevision DOES_NOT_EXIST_IN_BRANCH = new StubCDORevision();

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
      InternalCDORevision revision = (InternalCDORevision)cache.getRevisionByVersion(id, branchVersion);
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

  public List<CDORevision> getRevisions(Collection<CDOID> ids, CDOBranchPoint branchPoint, int referenceChunk,
      int prefetchDepth, boolean loadOnDemand)
  {
    List<CDOID> missingIDs = loadOnDemand ? new ArrayList<CDOID>(0) : null;
    List<CDORevision> revisions = new ArrayList<CDORevision>(ids.size());
    for (CDOID id : ids)
    {
      InternalCDORevision revision = getRevision(id, branchPoint, referenceChunk, prefetchDepth, false);
      revisions.add(revision);
      if (revision == null && missingIDs != null)
      {
        missingIDs.add(id);
      }
    }

    if (missingIDs != null && !missingIDs.isEmpty())
    {
      acquireAtomicRequestLock(loadAndAddLock);

      try
      {
        List<InternalCDORevision> missingRevisions = revisionLoader.loadRevisions(missingIDs, branchPoint,
            referenceChunk, prefetchDepth);
        handleMissingRevisions(revisions, missingRevisions);
      }
      finally
      {
        releaseAtomicRequestLock(loadAndAddLock);
      }
    }

    return revisions;
  }

  public InternalCDORevision getRevision(CDOID id, CDOBranchPoint branchPoint, int referenceChunk, int prefetchDepth,
      boolean loadOnDemand)
  {
    acquireAtomicRequestLock(loadAndAddLock);

    try
    {
      boolean prefetch = prefetchDepth != CDORevision.DEPTH_NONE;
      InternalCDORevision revision = prefetch ? null : (InternalCDORevision)getCachedRevision(id, branchPoint);
      if (revision == null || prefetchDepth != CDORevision.DEPTH_NONE)
      {
        if (loadOnDemand)
        {
          if (TRACER.isEnabled())
          {
            TRACER.format("Loading revision {0} from {1}", id, branchPoint); //$NON-NLS-1$
          }

          revision = revisionLoader
              .loadRevisions(Collections.singleton(id), branchPoint, referenceChunk, prefetchDepth).get(0);
          addCachedRevisionIfNotNull(revision);
        }
      }
      else
      {
        InternalCDORevision verified = verifyRevision(revision, referenceChunk);
        if (revision != verified)
        {
          addCachedRevisionIfNotNull(verified);
          revision = verified;
        }
      }

      return revision;
    }
    finally
    {
      releaseAtomicRequestLock(loadAndAddLock);
    }
  }

  public InternalCDORevision getRevisionByVersion(CDOID id, CDOBranchVersion branchVersion, int referenceChunk,
      boolean loadOnDemand)
  {
    acquireAtomicRequestLock(loadAndAddLock);

    try
    {
      InternalCDORevision revision = (InternalCDORevision)cache.getRevisionByVersion(id, branchVersion);
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

  protected InternalCDORevision verifyRevision(InternalCDORevision revision, int referenceChunk)
  {
    return revision;
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
      cache = CDORevisionCacheUtil.createDefaultCache();
    }
  }

  @Override
  protected void doActivate() throws Exception
  {
    super.doActivate();

    if (supportingBranches && !cache.isSupportingBranches())
    {
      cache = CDORevisionCacheUtil.createBranchDispatcher(cache);
    }

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

  private void handleMissingRevisions(List<CDORevision> revisions, List<InternalCDORevision> missingRevisions)
  {
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

  private CDORevision getCachedRevision(CDOID id, CDOBranchPoint branchPoint)
  {
    return cache.getRevision(id, branchPoint);
  }

  private void addCachedRevisionIfNotNull(InternalCDORevision revision)
  {
    if (revision != null)
    {
      cache.addRevision(revision);
    }
  }
}
