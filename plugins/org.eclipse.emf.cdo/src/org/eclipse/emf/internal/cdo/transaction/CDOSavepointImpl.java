/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Simon McDuff - initial API and implementation
 *    Eike Stepper - maintenance
 *    Simon McDuff - http://bugs.eclipse.org/204890
 */
package org.eclipse.emf.internal.cdo.transaction;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDOReferenceAdjuster;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.delta.CDOFeatureDelta;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDeltaUtil;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.internal.common.revision.delta.CDORevisionDeltaImpl;
import org.eclipse.emf.cdo.internal.common.revision.delta.InternalCDOFeatureDelta;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionDelta;

import org.eclipse.net4j.util.collection.MultiMap;

import org.eclipse.emf.spi.cdo.InternalCDOTransaction;
import org.eclipse.emf.spi.cdo.InternalCDOTransaction.InternalCDOCommitContext;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Simon McDuff
 * @since 2.0
 */
public class CDOSavepointImpl extends AbstractSavepoint
{
  private Map<CDOID, CDOResource> newResources = new HashMap<CDOID, CDOResource>();

  private Map<CDOID, CDOObject> newObjects = new HashMap<CDOID, CDOObject>();

  private Map<CDOID, CDORevision> baseNewObjects = new HashMap<CDOID, CDORevision>();

  private Map<CDOID, CDOObject> dirtyObjects = new HashMap<CDOID, CDOObject>();

  private Map<CDOID, CDOObject> detachedObjects = new HashMap<CDOID, CDOObject>()
  {
    private static final long serialVersionUID = 1L;

    @Override
    public CDOObject put(CDOID key, CDOObject object)
    {
      sharedDetachedObjects.add(key);
      dirtyObjects.remove(key);
      baseNewObjects.remove(key);
      newObjects.remove(key);
      newResources.remove(key);
      revisionDeltas.remove(key);
      return super.put(key, object);
    }
  };

  private ConcurrentMap<CDOID, CDORevisionDelta> revisionDeltas = new ConcurrentHashMap<CDOID, CDORevisionDelta>();

  /**
   * Contains all persistent CDOIDs that were removed. The same instance is shared between all save points that belong
   * to the same transaction.
   */
  private Set<CDOID> sharedDetachedObjects;

  private boolean isDirty;

  public CDOSavepointImpl(InternalCDOTransaction transaction, CDOSavepointImpl lastSavepoint)
  {
    super(transaction, lastSavepoint);
    isDirty = transaction.isDirty();
    if (getPreviousSavepoint() == null)
    {
      sharedDetachedObjects = new HashSet<CDOID>();
    }
    else
    {
      sharedDetachedObjects = lastSavepoint.getSharedDetachedObjects();
    }
  }

  public void clear()
  {
    newResources.clear();
    newObjects.clear();
    dirtyObjects.clear();
    revisionDeltas.clear();
    baseNewObjects.clear();
    detachedObjects.clear();
  }

  public boolean isDirty()
  {
    return isDirty;
  }

  public Map<CDOID, CDOResource> getNewResources()
  {
    return newResources;
  }

  public Map<CDOID, CDOObject> getNewObjects()
  {
    return newObjects;
  }

  public Map<CDOID, CDOObject> getDetachedObjects()
  {
    return detachedObjects;
  }

  public Map<CDOID, CDOObject> getDirtyObjects()
  {
    return dirtyObjects;
  }

  public Set<CDOID> getSharedDetachedObjects()
  {
    return sharedDetachedObjects;
  }

  public ConcurrentMap<CDOID, CDORevisionDelta> getRevisionDeltas()
  {
    return revisionDeltas;
  }

  public Map<CDOID, CDORevision> getBaseNewObjects()
  {
    return baseNewObjects;
  }

  /**
   * Return the list of new objects from this point.
   */
  public Map<CDOID, CDOObject> getAllDirtyObjects()
  {
    if (getPreviousSavepoint() == null)
    {
      return getDirtyObjects();
    }

    MultiMap.ListBased<CDOID, CDOObject> dirtyObjects = new MultiMap.ListBased<CDOID, CDOObject>();
    for (CDOSavepointImpl savepoint = this; savepoint != null; savepoint = savepoint.getPreviousSavepoint())
    {
      dirtyObjects.getDelegates().add(savepoint.getDirtyObjects());
    }

    return dirtyObjects;
  }

  /**
   * Return the list of new objects from this point without objects that are removed.
   */
  public Map<CDOID, CDOObject> getAllNewObjects()
  {
    if (getPreviousSavepoint() == null)
    {
      return Collections.unmodifiableMap(getNewObjects());
    }

    if (getSharedDetachedObjects().size() == 0)
    {
      MultiMap.ListBased<CDOID, CDOObject> newObjects = new MultiMap.ListBased<CDOID, CDOObject>();
      for (CDOSavepointImpl savepoint = this; savepoint != null; savepoint = savepoint.getPreviousSavepoint())
      {
        newObjects.getDelegates().add(savepoint.getNewObjects());
      }

      return newObjects;
    }

    Map<CDOID, CDOObject> newObjects = new HashMap<CDOID, CDOObject>();
    for (CDOSavepointImpl savepoint = this; savepoint != null; savepoint = savepoint.getPreviousSavepoint())
    {
      for (Entry<CDOID, CDOObject> entry : savepoint.getNewObjects().entrySet())
      {
        if (!getSharedDetachedObjects().contains(entry.getKey()))
        {
          newObjects.put(entry.getKey(), entry.getValue());
        }
      }
    }

    return newObjects;
  }

  /**
   * Return the list of new resources from this point without objects that are removed.
   */
  public Map<CDOID, CDOResource> getAllNewResources()
  {
    if (getPreviousSavepoint() == null)
    {
      return Collections.unmodifiableMap(getNewResources());
    }

    if (getSharedDetachedObjects().size() == 0)
    {
      MultiMap.ListBased<CDOID, CDOResource> newResources = new MultiMap.ListBased<CDOID, CDOResource>();
      for (CDOSavepointImpl savepoint = this; savepoint != null; savepoint = savepoint.getPreviousSavepoint())
      {
        newResources.getDelegates().add(savepoint.getNewResources());
      }

      return newResources;
    }

    Map<CDOID, CDOResource> newResources = new HashMap<CDOID, CDOResource>();
    for (CDOSavepointImpl savepoint = this; savepoint != null; savepoint = savepoint.getPreviousSavepoint())
    {
      for (Entry<CDOID, CDOResource> entry : savepoint.getNewResources().entrySet())
      {
        if (!getSharedDetachedObjects().contains(entry.getKey()))
        {
          newResources.put(entry.getKey(), entry.getValue());
        }
      }
    }

    return newResources;
  }

  /**
   * @since 2.0
   */
  public Map<CDOID, CDORevision> getAllBaseNewObjects()
  {
    if (getPreviousSavepoint() == null)
    {
      return Collections.unmodifiableMap(getBaseNewObjects());
    }

    MultiMap.ListBased<CDOID, CDORevision> newObjects = new MultiMap.ListBased<CDOID, CDORevision>();
    for (CDOSavepointImpl savepoint = this; savepoint != null; savepoint = savepoint.getPreviousSavepoint())
    {
      newObjects.getDelegates().add(savepoint.getBaseNewObjects());
    }

    return newObjects;
  }

  /**
   * Return the list of all deltas without objects that are removed.
   */
  public Map<CDOID, CDORevisionDelta> getAllRevisionDeltas()
  {
    if (getPreviousSavepoint() == null)
    {
      return Collections.unmodifiableMap(getRevisionDeltas());
    }

    // We need to combined the result for all delta in different Savepoint
    Map<CDOID, CDORevisionDelta> revisionDeltas = new HashMap<CDOID, CDORevisionDelta>();
    for (CDOSavepointImpl savepoint = (CDOSavepointImpl)getFirstSavePoint(); savepoint != null; savepoint = savepoint
        .getNextSavepoint())
    {
      for (Entry<CDOID, CDORevisionDelta> entry : savepoint.getRevisionDeltas().entrySet())
      {
        // Skipping temporary
        if (entry.getKey().isTemporary() || getSharedDetachedObjects().contains(entry.getKey()))
        {
          continue;
        }

        CDORevisionDeltaImpl revisionDelta = (CDORevisionDeltaImpl)revisionDeltas.get(entry.getKey());
        if (revisionDelta == null)
        {
          revisionDeltas.put(entry.getKey(), CDORevisionDeltaUtil.copy(entry.getValue()));
        }
        else
        {
          for (CDOFeatureDelta delta : entry.getValue().getFeatureDeltas())
          {
            revisionDelta.addFeatureDelta(((InternalCDOFeatureDelta)delta).copy());
          }
        }
      }
    }

    return Collections.unmodifiableMap(revisionDeltas);
  }

  public Map<CDOID, CDOObject> getAllDetachedObjects()
  {
    if (getPreviousSavepoint() == null)
    {
      return Collections.unmodifiableMap(getDetachedObjects());
    }

    Map<CDOID, CDOObject> detachedObjects = new HashMap<CDOID, CDOObject>();
    for (CDOSavepointImpl savepoint = this; savepoint != null; savepoint = savepoint.getPreviousSavepoint())
    {
      for (Entry<CDOID, CDOObject> entry : savepoint.getDetachedObjects().entrySet())
      {
        if (!entry.getKey().isTemporary())
        {
          detachedObjects.put(entry.getKey(), entry.getValue());
        }
      }
    }

    return detachedObjects;
  }

  public void recalculateSharedDetachedObjects()
  {
    sharedDetachedObjects.clear();
    for (CDOSavepointImpl savepoint = this; savepoint != null; savepoint = savepoint.getPreviousSavepoint())
    {
      for (CDOID id : savepoint.getDetachedObjects().keySet())
      {
        sharedDetachedObjects.add(id);
      }
    }
  }

  @Override
  public CDOSavepointImpl getPreviousSavepoint()
  {
    return (CDOSavepointImpl)super.getPreviousSavepoint();
  }

  public void setPreviousSavepoint(CDOSavepointImpl previousSavepoint)
  {
    super.setPreviousSavepoint(previousSavepoint);
  }

  @Override
  public CDOSavepointImpl getNextSavepoint()
  {
    return (CDOSavepointImpl)super.getNextSavepoint();
  }

  public void setNextSavepoint(CDOSavepointImpl nextSavepoint)
  {
    super.setNextSavepoint(nextSavepoint);
  }

  @Override
  public void rollback()
  {
    getUserTransaction().rollback(this);
  }

  public static void applyReferenceAdjuster(InternalCDOCommitContext context, CDOReferenceAdjuster referenceAdjuster)
  {
    adjustObjects(referenceAdjuster, context.getNewResources().values());
    adjustObjects(referenceAdjuster, context.getNewObjects().values());
    adjustObjects(referenceAdjuster, context.getDirtyObjects().values());
    adjustRevisionDeltas(referenceAdjuster, context.getRevisionDeltas().values());
  }

  public static void adjustObjects(CDOReferenceAdjuster referenceAdjuster, Collection<? extends CDOObject> objects)
  {
    for (CDOObject object : objects)
    {
      InternalCDORevision revision = (InternalCDORevision)object.cdoRevision();
      revision.applyReferenceAdjuster(referenceAdjuster);
    }
  }

  public static void adjustRevisionDeltas(CDOReferenceAdjuster referenceAdjuster,
      Collection<? extends CDORevisionDelta> deltas)
  {
    for (CDORevisionDelta delta : deltas)
    {
      InternalCDORevisionDelta revisionDelta = (InternalCDORevisionDelta)delta;
      revisionDelta.applyReferenceAdjuster(referenceAdjuster);
    }
  }
}
