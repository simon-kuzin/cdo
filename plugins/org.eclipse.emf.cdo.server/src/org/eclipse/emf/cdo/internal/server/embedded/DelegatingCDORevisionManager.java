/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.internal.server.embedded;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.CDORevisionFactory;
import org.eclipse.emf.cdo.common.revision.cache.CDORevisionCache;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionManager;

import org.eclipse.net4j.util.lifecycle.Lifecycle;

import org.eclipse.emf.ecore.EClass;

import java.util.Collection;
import java.util.List;

/**
 * @author Eike Stepper
 */
public abstract class DelegatingCDORevisionManager extends Lifecycle implements InternalCDORevisionManager
{
  public DelegatingCDORevisionManager()
  {
  }

  public CDORevisionCache getCache()
  {
    return getDelegate().getCache();
  }

  public void setCache(CDORevisionCache cache)
  {
    getDelegate().setCache(cache);
  }

  public void setFactory(CDORevisionFactory factory)
  {
    getDelegate().setFactory(factory);
  }

  public CDORevisionFactory getFactory()
  {
    return getDelegate().getFactory();
  }

  public RevisionLoader getRevisionLoader()
  {
    return getDelegate().getRevisionLoader();
  }

  public void setRevisionLoader(RevisionLoader revisionLoader)
  {
    getDelegate().setRevisionLoader(revisionLoader);
  }

  public RevisionLocker getRevisionLocker()
  {
    return getDelegate().getRevisionLocker();
  }

  public void setRevisionLocker(RevisionLocker revisionLocker)
  {
    getDelegate().setRevisionLocker(revisionLocker);
  }

  public boolean containsRevision(CDOID id, int branchID, long timeStamp)
  {
    return getDelegate().containsRevision(id, branchID, timeStamp);
  }

  public boolean containsRevisionByVersion(CDOID id, int branchID, int version)
  {
    return getDelegate().containsRevisionByVersion(id, branchID, version);
  }

  public EClass getObjectType(CDOID id)
  {
    return getDelegate().getObjectType(id);
  }

  public CDORevision getRevision(CDOID id, int branchID, long timeStamp, int referenceChunk, int prefetchDepth,
      boolean loadOnDemand)
  {
    return getDelegate().getRevision(id, branchID, timeStamp, referenceChunk, prefetchDepth, loadOnDemand);
  }

  public CDORevision getRevisionByVersion(CDOID id, int branchID, int version, int referenceChunk, boolean loadOnDemand)
  {
    return getDelegate().getRevisionByVersion(id, branchID, version, referenceChunk, loadOnDemand);
  }

  public List<CDORevision> getRevisions(Collection<CDOID> ids, int branchID, long timeStamp, int referenceChunk,
      int prefetchDepth, boolean loadOnDemand)
  {
    return getDelegate().getRevisions(ids, branchID, timeStamp, referenceChunk, prefetchDepth, loadOnDemand);
  }

  public void reviseLatest(CDOID id, int branchID)
  {
    getDelegate().reviseLatest(id, branchID);
  }

  public void reviseVersion(CDOID id, int branchID, int version, long timeStamp)
  {
    getDelegate().reviseVersion(id, branchID, version, timeStamp);
  }

  protected abstract InternalCDORevisionManager getDelegate();
}
