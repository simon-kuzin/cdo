/*
 * Copyright (c) 2009-2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Simon McDuff - initial API and implementation
 *    Eike Stepper - maintenance
 *    Simon McDuff - bug 204890
 */
package org.eclipse.emf.internal.cdo.transaction;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.commit.CDOChangeSetData;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.revision.CDOIDAndVersion;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.internal.common.commit.CDOChangeSetDataImpl;

import org.eclipse.net4j.util.lifecycle.LifecycleUtil;

import org.eclipse.emf.spi.cdo.CDOTransactionStrategy;
import org.eclipse.emf.spi.cdo.InternalCDOObject;
import org.eclipse.emf.spi.cdo.InternalCDOSavepoint;
import org.eclipse.emf.spi.cdo.InternalCDOSavepoint.ChangeInfo.ChangeType;
import org.eclipse.emf.spi.cdo.InternalCDOTransaction;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Simon McDuff
 * @author Eike Stepper
 * @since 2.0
 */
public class CDOSavepointImpl extends CDOUserSavepointImpl implements InternalCDOSavepoint
{
  private final Map<CDOID, ChangeInfo> changeInfos = CDOIDUtil.createMap();

  private final Map<InternalCDOObject, ChangeInfo> detachedInfos = new WeakHashMap<InternalCDOObject, ChangeInfo>();

  private boolean inMemory;

  private File storage;

  public CDOSavepointImpl(InternalCDOTransaction transaction, InternalCDOSavepoint lastSavepoint)
  {
    super(transaction, lastSavepoint);
    if (lastSavepoint != null)
    {
      for (ChangeInfo lastChangeInfo : lastSavepoint.getChangeInfos().values())
      {
        ChangeInfo changeInfo = lastChangeInfo.setSavepoint();
        changeInfos.put(changeInfo.getID(), changeInfo);
      }
    }
  }

  @Override
  public InternalCDOTransaction getTransaction()
  {
    return (InternalCDOTransaction)super.getTransaction();
  }

  @Override
  public InternalCDOSavepoint getFirstSavePoint()
  {
    synchronized (super.getTransaction())
    {
      return (InternalCDOSavepoint)super.getFirstSavePoint();
    }
  }

  @Override
  public InternalCDOSavepoint getPreviousSavepoint()
  {
    synchronized (super.getTransaction())
    {
      return (InternalCDOSavepoint)super.getPreviousSavepoint();
    }
  }

  @Override
  public InternalCDOSavepoint getNextSavepoint()
  {
    synchronized (super.getTransaction())
    {
      return (InternalCDOSavepoint)super.getNextSavepoint();
    }
  }

  public ChangeInfo addChangeInfo(ChangeInfo changeInfo)
  {
    if (changeInfo.getType() == ChangeType.DETACHED)
    {
      detachedInfos.put(changeInfo.getObject(), changeInfo);
    }

    return changeInfos.put(changeInfo.getID(), changeInfo);
  }

  public ChangeInfo removeChangeInfo(CDOID id)
  {
    ChangeInfo changeInfo = changeInfos.remove(id);
    if (changeInfo != null && changeInfo.getType() == ChangeType.DETACHED)
    {
      detachedInfos.remove(changeInfo.getObject());
    }

    return changeInfo;
  }

  public ChangeInfo removeChangeInfo(CDOObject object)
  {
    ChangeInfo changeInfo = detachedInfos.remove(object);
    if (changeInfo != null)
    {
      changeInfos.remove(changeInfo.getID());
    }

    return changeInfo;
  }

  public ChangeInfo getChangeInfo(CDOID id)
  {
    return changeInfos.get(id);
  }

  public ChangeInfo getDetachedInfo(CDOObject object)
  {
    return detachedInfos.get(object);
  }

  public Map<CDOID, ChangeInfo> getChangeInfos()
  {
    return Collections.unmodifiableMap(changeInfos);
  }

  public Map<InternalCDOObject, ChangeInfo> getDetachedInfos()
  {
    return Collections.unmodifiableMap(detachedInfos);
  }

  public void clear()
  {
    synchronized (super.getTransaction())
    {
      changeInfos.clear();
      detachedInfos.clear();
    }
  }

  public boolean isInMemory()
  {
    return storage == null;
  }

  public void setInMemory(boolean inMemory)
  {
    this.inMemory = inMemory;
    if (isInMemory())
    {
      if (!inMemory)
      {
        unload();
      }
    }
    else
    {
      if (!inMemory)
      {
        load();
      }
    }
  }

  private void load()
  {
  }

  private void unload()
  {
  }

  public boolean isEmpty()
  {
    return changeInfos.isEmpty();
  }

  public Map<CDOID, CDOObject> getObjects(ChangeInfo.ChangeType type, Map<CDOID, ChangeInfo> baseline)
  {
    Map<CDOID, CDOObject> result = CDOIDUtil.createMap();
    for (ChangeInfo changeInfo : changeInfos.values())
    {
      if (changeInfo.getType() == type)
      {
        InternalCDOObject object = changeInfo.getObject();
        if (object != null)
        {
          CDOID id = changeInfo.getID();
          if (baseline != null)
          {
            ChangeInfo baselineInfo = baseline.get(id);
            if (baselineInfo != null && baselineInfo.getType() == type)
            {
              // Ignore the object if it has the same change kind in the baseline
              continue;
            }
          }

          result.put(id, object);
        }
      }
    }

    return Collections.unmodifiableMap(result);
  }

  public Map<CDOID, CDOObject> getNewObjects()
  {
    InternalCDOSavepoint previousSavepoint = getPreviousSavepoint();
    Map<CDOID, ChangeInfo> baseline = previousSavepoint == null ? null : previousSavepoint.getChangeInfos();
    return getObjects(ChangeType.NEW, baseline);
  }

  public Map<CDOID, CDOObject> getDirtyObjects()
  {
    InternalCDOSavepoint previousSavepoint = getPreviousSavepoint();
    Map<CDOID, ChangeInfo> baseline = previousSavepoint == null ? null : previousSavepoint.getChangeInfos();
    return getObjects(ChangeType.DIRTY, baseline);
  }

  public Map<CDOID, CDOObject> getDetachedObjects()
  {
    InternalCDOSavepoint previousSavepoint = getPreviousSavepoint();
    Map<CDOID, ChangeInfo> baseline = previousSavepoint == null ? null : previousSavepoint.getChangeInfos();
    return getObjects(ChangeType.DETACHED, baseline);
  }

  public Map<CDOID, CDORevisionDelta> getRevisionDeltas2()
  {
    int xxx;
    throw new UnsupportedOperationException();
  }

  public CDOChangeSetData getChangeSetData()
  {
    int xxx;
    throw new UnsupportedOperationException();
  }

  public CDOChangeSetData getAllChangeSetData()
  {
    synchronized (super.getTransaction())
    {
      List<CDOIDAndVersion> revisions = new ArrayList<CDOIDAndVersion>();
      List<CDORevisionKey> deltas = new ArrayList<CDORevisionKey>();
      List<CDOIDAndVersion> detached = new ArrayList<CDOIDAndVersion>();

      for (ChangeInfo changeInfo : changeInfos.values())
      {
        switch (changeInfo.getType())
        {
        case NEW:
          revisions.add(changeInfo.getObject().cdoRevision());
          break;

        case DIRTY:
          deltas.add(changeInfo.getRevisionDelta());
          break;

        case DETACHED:
          detached.add(changeInfo.getCleanRevision());
        }
      }

      return new CDOChangeSetDataImpl(revisions, deltas, detached);
    }
  }

  /**
   * Return the list of all deltas without objects that are removed.
   */
  public Map<CDOID, CDORevisionDelta> getAllRevisionDeltas()
  {
    Map<CDOID, CDORevisionDelta> result = CDOIDUtil.createMap();
    for (ChangeInfo changeInfo : changeInfos.values())
    {
      CDORevisionDelta revisionDelta = changeInfo.getRevisionDelta();
      if (revisionDelta != null)
      {
        result.put(changeInfo.getID(), revisionDelta);
      }
    }

    return Collections.unmodifiableMap(result);
  }

  /**
   * Return the list of new objects from this point.
   */
  public Map<CDOID, CDOObject> getAllDirtyObjects()
  {
    return getObjects(ChangeType.DIRTY, null);
  }

  /**
   * Return the list of new objects from this point without objects that are removed.
   */
  public Map<CDOID, CDOObject> getAllNewObjects()
  {
    return getObjects(ChangeType.NEW, null);
  }

  public Map<CDOID, CDOObject> getAllDetachedObjects()
  {
    return getObjects(ChangeType.DETACHED, null);
  }

  public boolean isNewObject(CDOID id)
  {
    if (id.isTemporary())
    {
      return true;
    }

    ChangeInfo changeInfo = changeInfos.get(id);
    if (changeInfo != null && changeInfo.getType() == ChangeType.NEW)
    {
      return true;
    }

    return false;
  }

  public boolean isDetachedObject(CDOID id)
  {
    ChangeInfo changeInfo = changeInfos.get(id);
    return changeInfo != null && changeInfo.getType() == ChangeType.DETACHED;
  }

  public void rollback()
  {
    InternalCDOTransaction transaction = getTransaction();
    synchronized (transaction)
    {
      LifecycleUtil.checkActive(transaction);

      CDOTransactionStrategy transactionStrategy = transaction.getTransactionStrategy();
      transactionStrategy.rollback(transaction, this);
    }
  }

  @Deprecated
  public ConcurrentMap<CDOID, CDORevisionDelta> getRevisionDeltas()
  {
    final Map<CDOID, CDORevisionDelta> revisionDeltas = getRevisionDeltas2();
    return new ConcurrentMap<CDOID, CDORevisionDelta>()
    {
      public int size()
      {
        return revisionDeltas.size();
      }

      public boolean isEmpty()
      {
        return revisionDeltas.isEmpty();
      }

      public boolean containsKey(Object key)
      {
        return revisionDeltas.containsKey(key);
      }

      public boolean containsValue(Object value)
      {
        return revisionDeltas.containsValue(value);
      }

      public CDORevisionDelta get(Object key)
      {
        return revisionDeltas.get(key);
      }

      public CDORevisionDelta put(CDOID key, CDORevisionDelta value)
      {
        return revisionDeltas.put(key, value);
      }

      public CDORevisionDelta remove(Object key)
      {
        return revisionDeltas.remove(key);
      }

      public void putAll(Map<? extends CDOID, ? extends CDORevisionDelta> m)
      {
        revisionDeltas.putAll(m);
      }

      public void clear()
      {
        revisionDeltas.clear();
      }

      public Set<CDOID> keySet()
      {
        return revisionDeltas.keySet();
      }

      public Collection<CDORevisionDelta> values()
      {
        return revisionDeltas.values();
      }

      public Set<java.util.Map.Entry<CDOID, CDORevisionDelta>> entrySet()
      {
        return revisionDeltas.entrySet();
      }

      @Override
      public boolean equals(Object o)
      {
        return revisionDeltas.equals(o);
      }

      @Override
      public int hashCode()
      {
        return revisionDeltas.hashCode();
      }

      public CDORevisionDelta putIfAbsent(CDOID key, CDORevisionDelta value)
      {
        return null;
      }

      public boolean remove(Object key, Object value)
      {
        return false;
      }

      public boolean replace(CDOID key, CDORevisionDelta oldValue, CDORevisionDelta newValue)
      {
        return false;
      }

      public CDORevisionDelta replace(CDOID key, CDORevisionDelta value)
      {
        return null;
      }
    };
  }

  @Deprecated
  public boolean wasDirty()
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public Map<CDOID, CDORevision> getBaseNewObjects()
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public Map<CDOID, CDORevision> getAllBaseNewObjects()
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public Map<CDOID, CDOObject> getReattachedObjects()
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public Set<CDOID> getSharedDetachedObjects()
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void recalculateSharedDetachedObjects()
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void attachObject(InternalCDOObject object)
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void removeObject(InternalCDOObject object)
  {
    throw new UnsupportedOperationException();
  }
}
