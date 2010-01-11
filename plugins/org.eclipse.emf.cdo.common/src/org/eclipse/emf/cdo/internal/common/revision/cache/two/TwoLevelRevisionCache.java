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
package org.eclipse.emf.cdo.internal.common.revision.cache.two;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.cache.CDORevisionCache;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.net4j.util.CheckUtil;
import org.eclipse.net4j.util.event.IEvent;
import org.eclipse.net4j.util.event.IListener;
import org.eclipse.net4j.util.lifecycle.Lifecycle;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EClass;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eike Stepper
 */
public class TwoLevelRevisionCache extends Lifecycle implements CDORevisionCache, IListener
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_REVISION, TwoLevelRevisionCache.class);

  private CDORevisionCache level1;

  private CDORevisionCache level2;

  public TwoLevelRevisionCache()
  {
  }

  public CDORevisionCache getLevel1()
  {
    return level1;
  }

  public void setLevel1(CDORevisionCache level1)
  {
    this.level1 = level1;
  }

  public CDORevisionCache getLevel2()
  {
    return level2;
  }

  public void setLevel2(CDORevisionCache level2)
  {
    this.level2 = level2;
  }

  public EClass getObjectType(CDOID id)
  {
    EClass objectType = level1.getObjectType(id);
    if (objectType == null)
    {
      objectType = level2.getObjectType(id);
    }

    return objectType;
  }

  public CDORevision getRevision(CDOID id, CDOBranchPoint branchPoint)
  {
    CDORevision revision = level1.getRevision(id, branchPoint);
    if (revision == null)
    {
      revision = level2.getRevision(id, branchPoint);
    }

    return revision;
  }

  public CDORevision getRevisionByVersion(CDOID id, CDOBranch branch, int version)
  {
    CDORevision revision = level1.getRevisionByVersion(id, branch, version);
    if (revision == null)
    {
      revision = level2.getRevisionByVersion(id, branch, version);
    }

    return revision;
  }

  public List<CDORevision> getRevisions()
  {
    List<CDORevision> revisions = new ArrayList<CDORevision>();
    revisions.addAll(level1.getRevisions());
    revisions.addAll(level2.getRevisions());
    return revisions;
  }

  public boolean addRevision(CDORevision revision)
  {
    CheckUtil.checkArg(revision, "revision");
    boolean added = level1.addRevision(revision);

    // Bugzilla 292372: If a new current revision was added to level1, we must check whether
    // level2 contains a stale current revision, and revise that revision if possible
    if (added && revision.isCurrent())
    {
      CDOID id = revision.getID();
      CDORevision revisionInLevel2 = level2.getRevision(id, revision);
      if (revisionInLevel2 != null && revisionInLevel2.isCurrent())
      {
        // We can only revise if the revisions are consecutive
        if (revision.getVersion() == revisionInLevel2.getVersion() + 1)
        {
          ((InternalCDORevision)revisionInLevel2).setRevised(revision.getTimeStamp() - 1);
        }
        else
        {
          level2.removeRevision(id, revision.getBranch(), revisionInLevel2.getVersion());
        }
      }
    }

    return added;
  }

  public CDORevision removeRevision(CDOID id, CDOBranch branch, int version)
  {
    CDORevision revision1 = level1.removeRevision(id, branch, version);
    CDORevision revision2 = level2.removeRevision(id, branch, version);
    return revision1 != null ? revision1 : revision2;
  }

  public void clear()
  {
    level1.clear();
    level2.clear();
  }

  public void notifyEvent(IEvent event)
  {
    if (event instanceof EvictionEvent)
    {
      EvictionEvent e = (EvictionEvent)event;
      CDORevision revision = e.getRevision();
      if (revision != null)
      {
        CDORevisionCache cache = e.getSource();
        if (cache == level1)
        {
          evictedFromLevel1(revision);
        }
        else if (cache == level2)
        {
          evictedFromLevel2(revision);
        }
      }
    }
  }

  protected void evictedFromLevel1(CDORevision revision)
  {
    level2.addRevision(revision);
    if (TRACER.isEnabled())
    {
      TRACER.format("Recached revision {0}", revision); //$NON-NLS-1$
    }
  }

  protected void evictedFromLevel2(CDORevision revision)
  {
  }

  @Override
  protected void doBeforeActivate() throws Exception
  {
    super.doBeforeActivate();
    checkState(level1, "level1"); //$NON-NLS-1$
    checkState(level2, "level2"); //$NON-NLS-1$
  }

  @Override
  protected void doActivate() throws Exception
  {
    super.doActivate();
    level1.addListener(this);
    level2.addListener(this);
    LifecycleUtil.activate(level1);
    LifecycleUtil.activate(level2);
  }

  @Override
  protected void doDeactivate() throws Exception
  {
    LifecycleUtil.deactivate(level2);
    LifecycleUtil.deactivate(level1);
    level2.removeListener(this);
    level1.removeListener(this);
    super.doDeactivate();
  }
}
