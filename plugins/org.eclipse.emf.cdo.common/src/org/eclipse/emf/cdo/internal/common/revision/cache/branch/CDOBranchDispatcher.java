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
package org.eclipse.emf.cdo.internal.common.revision.cache.branch;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.cache.CDORevisionCache;
import org.eclipse.emf.cdo.common.revision.cache.CDORevisionCacheFactory;

import org.eclipse.net4j.util.lifecycle.Lifecycle;

import org.eclipse.emf.ecore.EClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public class CDOBranchDispatcher extends Lifecycle implements CDORevisionCache
{
  private Map<CDOBranch, CDORevisionCache> caches = new HashMap<CDOBranch, CDORevisionCache>();

  private CDORevisionCacheFactory factory;

  public CDOBranchDispatcher()
  {
  }

  public CDORevisionCacheFactory getFactory()
  {
    return factory;
  }

  public void setFactory(CDORevisionCacheFactory factory)
  {
    checkInactive();
    this.factory = factory;
  }

  public EClass getObjectType(CDOID id)
  {
    for (CDORevisionCache cache : getCaches())
    {
      EClass type = cache.getObjectType(id);
      if (type != null)
      {
        return type;
      }
    }

    return null;
  }

  public CDORevision getRevision(CDOID id, CDOBranchPoint branchPoint)
  {
    CDORevisionCache cache = getCache(branchPoint.getBranch());
    if (cache == null)
    {
      return null;
    }

    return cache.getRevision(id, branchPoint);
  }

  public CDORevision getRevisionByVersion(CDOID id, CDOBranch branch, int version)
  {
    CDORevisionCache cache = getCache(branch);
    if (cache == null)
    {
      return null;
    }

    return cache.getRevisionByVersion(id, branch, version);
  }

  public CDORevision removeRevision(CDOID id, CDOBranch branch, int version)
  {
    CDORevisionCache cache = getCache(branch);
    if (cache == null)
    {
      return null;
    }

    return cache.removeRevision(id, branch, version);
  }

  public boolean addRevision(CDORevision revision)
  {
    CDOBranch branch = revision.getBranch();
    CDORevisionCache cache;
    synchronized (caches)
    {
      cache = caches.get(branch);
      if (cache == null)
      {
        cache = factory.createRevisionCache(revision);
        caches.put(branch, cache);
      }
    }

    return cache.addRevision(revision);
  }

  public List<CDORevision> getRevisions()
  {
    List<CDORevision> result = new ArrayList<CDORevision>();
    for (CDORevisionCache cache : getCaches())
    {
      result.addAll(cache.getRevisions());
    }

    return result;
  }

  public void clear()
  {
    for (CDORevisionCache cache : getCaches())
    {
      cache.clear();
    }
  }

  @Override
  protected void doBeforeActivate() throws Exception
  {
    super.doBeforeActivate();
    checkState(factory, "factory");
  }

  private CDORevisionCache getCache(CDOBranch branch)
  {
    synchronized (caches)
    {
      return caches.get(branch);
    }
  }

  private CDORevisionCache[] getCaches()
  {
    synchronized (caches)
    {
      return caches.values().toArray(new CDORevisionCache[caches.size()]);
    }
  }
}
