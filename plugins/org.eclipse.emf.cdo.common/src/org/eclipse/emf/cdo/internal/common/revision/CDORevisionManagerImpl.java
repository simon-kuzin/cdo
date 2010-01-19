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
import org.eclipse.emf.cdo.common.model.CDOClassInfo;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.CDORevisionFactory;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;
import org.eclipse.emf.cdo.common.revision.CDORevisionUtil;
import org.eclipse.emf.cdo.common.revision.cache.CDORevisionCache;
import org.eclipse.emf.cdo.common.revision.cache.CDORevisionCacheUtil;
import org.eclipse.emf.cdo.common.revision.cache.InternalCDORevisionCache;
import org.eclipse.emf.cdo.common.revision.cache.InternalCDORevisionCache.ReplaceCallback;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionManager;
import org.eclipse.emf.cdo.spi.common.revision.StubCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionManager.RevisionLoader.MissingRevisionInfo;

import org.eclipse.net4j.util.CheckUtil;
import org.eclipse.net4j.util.ReflectUtil.ExcludeFromDump;
import org.eclipse.net4j.util.lifecycle.Lifecycle;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EClass;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Eike Stepper
 */
public class CDORevisionManagerImpl extends Lifecycle implements InternalCDORevisionManager, ReplaceCallback
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_REVISION, CDORevisionManagerImpl.class);

  private boolean supportingBranches;

  private RevisionLoader revisionLoader;

  private RevisionLocker revisionLocker;

  private CDORevisionFactory factory;

  private InternalCDORevisionCache cache;

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

  public InternalCDORevisionCache getCache()
  {
    return cache;
  }

  public void setCache(CDORevisionCache cache)
  {
    checkInactive();
    this.cache = (InternalCDORevisionCache)cache;
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
      InternalCDORevision revision = getCachedRevisionByVersion(id, branchVersion);
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
    List<MissingRevisionInfo> infos = null;
    for (CDOID id : ids)
    {
      MissingRevisionInfo info = null;
      InternalCDORevision revision = getCachedRevision(id, branchPoint);
      if (supportingBranches)
      {
        if (revision == null)
        {
          if (loadOnDemand)
          {
            InternalCDORevision available = getCachedRevisionRecursively(id, branchPoint);
            if (available != null)
            {
              info = new MissingRevisionInfo.PossiblyAvailable(id, available);
            }
            else
            {
              info = new MissingRevisionInfo.Missing(id);
            }
          }
        }
        else if (revision.getClass() == RevisionPointer.class)
        {
          CDORevisionKey target = ((RevisionPointer)revision).getTarget();
          revision = getCachedRevisionByVersion(target.getID(), target);
          if (revision == null && loadOnDemand)
          {
            info = new MissingRevisionInfo.ExactlyKnown(id, target);
          }
        }
      }
      else
      {
        if (revision == null && loadOnDemand)
        {
          info = new MissingRevisionInfo.Missing(id);
        }
      }

      revisions.add(revision);
      if (info != null)
      {
        CheckUtil.checkState(revision == null, "revision");
        if (infos == null)
        {
          infos = new ArrayList<MissingRevisionInfo>(1);
        }

        infos.add(info);
      }
    }

    if (infos != null)
    {
      acquireAtomicRequestLock(loadAndAddLock);

      try
      {
        List<InternalCDORevision> missingRevisions = //
        revisionLoader.loadRevisions(infos, branchPoint, referenceChunk, prefetchDepth);

        CDOBranch branch = branchPoint.getBranch();

        Iterator<MissingRevisionInfo> itInfo = infos.iterator();
        Iterator<InternalCDORevision> it = missingRevisions.iterator();
        for (int i = 0; i < revisions.size(); i++)
        {
          CDORevision revision = revisions.get(i);
          if (revision == null)
          {
            MissingRevisionInfo info = itInfo.next();
            InternalCDORevision missingRevision = it.next();
            revisions.set(i, missingRevision);
            addRevision(missingRevision);

            if (supportingBranches && //
                info.getType() != MissingRevisionInfo.Type.EXACTLY_KNOWN && //
                !branch.equals(missingRevision.getBranch()))
            {
              addRevision(new RevisionPointer(branch, missingRevision));
            }
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

  public InternalCDORevision getRevisionByVersion(CDOID id, CDOBranchVersion branchVersion, int referenceChunk,
      boolean loadOnDemand)
  {
    acquireAtomicRequestLock(loadAndAddLock);

    try
    {
      InternalCDORevision revision = getCachedRevisionByVersion(id, branchVersion);
      if (revision == null)
      {
        if (loadOnDemand)
        {
          if (TRACER.isEnabled())
          {
            TRACER.format("Loading revision {0} from {1}", id, branchVersion); //$NON-NLS-1$
          }

          revision = revisionLoader.loadRevisionByVersion(id, branchVersion, referenceChunk);
          addRevision(revision);
        }
      }

      return revision;
    }
    finally
    {
      releaseAtomicRequestLock(loadAndAddLock);
    }
  }

  public boolean addRevision(CDORevision revision)
  {
    if (revision != null)
    {
      return cache.addRevision(revision, this);
    }

    return false;
  }

  public boolean canReplace(InternalCDORevision foundRevision)
  {
    if (supportingBranches)
    {
      return foundRevision.getClass() == RevisionPointer.class;
    }

    return false;
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
      cache = (InternalCDORevisionCache)CDORevisionCacheUtil.createDefaultCache(supportingBranches);
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
    return (InternalCDORevision)cache.getRevision(id, branchPoint);
  }

  private InternalCDORevision getCachedRevisionRecursively(CDOID id, CDOBranchPoint branchPoint)
  {
    CDOBranch branch = branchPoint.getBranch();
    if (!branch.isMainBranch())
    {
      CDOBranchPoint base = branch.getBase();
      InternalCDORevision revision = getCachedRevision(id, base);
      if (revision != null)
      {
        return revision;
      }

      // Recurse
      return getCachedRevisionRecursively(id, base);
    }

    // Reached main branch
    return null;
  }

  private InternalCDORevision getCachedRevisionByVersion(CDOID id, CDOBranchVersion branchVersion)
  {
    InternalCDORevision revision = (InternalCDORevision)cache.getRevisionByVersion(id, branchVersion);
    if (revision != null && revision.getClass() == RevisionPointer.class)
    {
      return null;
    }

    return revision;
  }

  /**
   * @author Eike Stepper
   */
  private static final class RevisionPointer extends StubCDORevision
  {
    private CDOBranch branch;

    private CDORevisionKey target;

    public RevisionPointer(CDOBranch branch, CDORevisionKey target)
    {
      this.branch = branch;
      this.target = CDORevisionUtil.createRevisionKey(target);
    }

    public CDORevisionKey getTarget()
    {
      return target;
    }

    @Override
    public CDOClassInfo getClassInfo()
    {
      return null;
    }

    @Override
    public CDOID getID()
    {
      return target.getID();
    }

    @Override
    public CDOBranch getBranch()
    {
      return branch;
    }

    @Override
    public int getVersion()
    {
      return 1;
    }

    @Override
    public long getTimeStamp()
    {
      return branch.getBase().getTimeStamp();
    }

    @Override
    public long getRevised()
    {
      return UNSPECIFIED_DATE;
    }

    @Override
    public boolean isTransactional()
    {
      return false;
    }

    @Override
    public boolean isHistorical()
    {
      return false;
    }

    @Override
    public boolean isValid(long timeStamp)
    {
      return timeStamp >= getTimeStamp();
    }

    @Override
    public String toString()
    {
      return MessageFormat.format("RevisionPointer[branch={0}, target={1}]", branch.getID(), target);
    }
  }
}
