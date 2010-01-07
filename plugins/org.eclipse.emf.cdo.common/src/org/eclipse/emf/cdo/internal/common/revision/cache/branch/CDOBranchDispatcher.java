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
  private Map<Integer, CDORevisionCache> caches = new HashMap<Integer, CDORevisionCache>();

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

  public CDORevision getRevision(CDOID id, int branchID, long timeStamp)
  {
    CDORevisionCache cache = getCache(branchID);
    if (cache == null)
    {
      return null;
    }

    return cache.getRevision(id, branchID, timeStamp);
  }

  public CDORevision getRevisionByVersion(CDOID id, int branchID, int version)
  {
    CDORevisionCache cache = getCache(branchID);
    if (cache == null)
    {
      return null;
    }

    return cache.getRevisionByVersion(id, branchID, version);
  }

  public CDORevision removeRevision(CDOID id, int branchID, int version)
  {
    CDORevisionCache cache = getCache(branchID);
    if (cache == null)
    {
      return null;
    }

    return cache.removeRevision(id, branchID, version);
  }

  public boolean addRevision(CDORevision revision)
  {
    int branchID = revision.getBranchID();
    CDORevisionCache cache;
    synchronized (caches)
    {
      cache = caches.get(branchID);
      if (cache == null)
      {
        cache = factory.createRevisionCache(revision);
        caches.put(branchID, cache);
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

  private CDORevisionCache getCache(int branchID)
  {
    synchronized (caches)
    {
      return caches.get(branchID);
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
