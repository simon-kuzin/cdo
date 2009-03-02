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
 *    Simon McDuff - http://bugs.eclipse.org/213402
 */
package org.eclipse.emf.cdo.internal.server.mem;

import org.eclipse.emf.cdo.common.CDOQueryInfo;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.model.CDOClassifierRef;
import org.eclipse.emf.cdo.common.model.CDOPackageInfo;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.server.IQueryContext;
import org.eclipse.emf.cdo.server.ISession;
import org.eclipse.emf.cdo.server.ITransaction;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.server.StoreAccessor;

import org.eclipse.net4j.util.WrappedException;
import org.eclipse.net4j.util.collection.CloseableIterator;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Simon McDuff
 */
public class MEMStoreAccessor extends StoreAccessor
{
  private List<CDORevision> newRevisions = new ArrayList<CDORevision>();

  public MEMStoreAccessor(MEMStore store, ISession session)
  {
    super(store, session);
  }

  /**
   * @since 2.0
   */
  public MEMStoreAccessor(MEMStore store, ITransaction transaction)
  {
    super(store, transaction);
  }

  @Override
  public MEMStore getStore()
  {
    return (MEMStore)super.getStore();
  }

  /**
   * @since 2.0
   */
  public MEMStoreChunkReader createChunkReader(CDORevision revision, EStructuralFeature feature)
  {
    return new MEMStoreChunkReader(this, revision, feature);
  }

  public Collection<CDOPackageInfo> readPackageInfos()
  {
    return Collections.emptySet();
  }

  public void readPackage(EPackage cdoPackage)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * @since 2.0
   */
  public void readPackageEcore(EPackage cdoPackage)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * @since 2.0
   */
  public CloseableIterator<CDOID> readObjectIDs()
  {
    throw new UnsupportedOperationException();
  }

  public CDOClassifierRef readObjectType(CDOID id)
  {
    InternalCDORevision storeRevision = (InternalCDORevision)getStore().getRevision(id);
    return new CDOClassifierRef(storeRevision.getEClass());
  }

  public CDORevision readRevision(CDOID id, int referenceChunk)
  {
    InternalCDORevision storeRevision = (InternalCDORevision)getStore().getRevision(id);
    // IRevisionManager revisionManager = getStore().getRepository().getRevisionManager();
    // InternalCDORevision newRevision = new InternalCDORevision(revisionManager, storeRevision.getEClass(),
    // storeRevision
    // .getID());
    // newRevision.setResourceID(storeRevision.getResourceID());
    //
    // for (EStructuralFeature feature : storeRevision.TODO.getAllPersistentFeatures(getEClass()))
    // {
    // if (feature.isMany())
    // {
    // newRevision.setListSize(feature, storeRevision.getList(feature).size());
    // MoveableList<Object> list = newRevision.getList(feature);
    // int size = referenceChunk == CDORevision.UNCHUNKED ? list.size() : referenceChunk;
    // for (int i = 0; i < size; i++)
    // {
    // list.set(i, storeRevision.get(feature, i));
    // }
    // }
    // }
    //
    // return newRevision;
    return storeRevision;
  }

  public CDORevision readRevisionByTime(CDOID id, int referenceChunk, long timeStamp)
  {
    return getStore().getRevisionByTime(id, timeStamp);
  }

  public CDORevision readRevisionByVersion(CDOID id, int referenceChunk, int version)
  {
    return getStore().getRevisionByVersion(id, version);
  }

  /**
   * @since 2.0
   */
  public void commit(OMMonitor monitor)
  {
    // Do nothing
  }

  @Override
  public void write(CommitContext context, OMMonitor monitor)
  {
    MEMStore store = getStore();
    synchronized (store)
    {
      super.write(context, monitor);
    }
  }

  @Override
  protected void rollback(CommitContext context)
  {
    MEMStore store = getStore();
    synchronized (store)
    {
      for (CDORevision revision : newRevisions)
      {
        store.rollbackRevision(revision);
      }
    }
  }

  @Override
  protected void writePackageUnits(CDOPackageUnit[] packageUnits, OMMonitor monitor)
  {
    // Do nothing
  }

  @Override
  protected void writeRevisions(CDORevision[] revisions, OMMonitor monitor)
  {
    for (CDORevision revision : revisions)
    {
      writeRevision(revision);
    }
  }

  protected void writeRevision(CDORevision revision)
  {
    newRevisions.add(revision);
    getStore().addRevision(revision);
  }

  /**
   * @since 2.0
   */
  @Override
  protected void writeRevisionDeltas(CDORevisionDelta[] revisionDeltas, long created, OMMonitor monitor)
  {
    for (CDORevisionDelta revisionDelta : revisionDeltas)
    {
      writeRevisionDelta(revisionDelta, created);
    }
  }

  /**
   * @since 2.0
   */
  protected void writeRevisionDelta(CDORevisionDelta revisionDelta, long created)
  {
    CDORevision revision = getStore().getRevision(revisionDelta.getID());
    CDORevision newRevision = revision.copy();
    revisionDelta.apply(newRevision);
    ((InternalCDORevision)newRevision).setCreated(created);
    writeRevision(newRevision);
  }

  @Override
  protected void detachObjects(CDOID[] detachedObjects, long revised, OMMonitor monitor)
  {
    for (CDOID id : detachedObjects)
    {
      detachObject(id);
    }
  }

  /**
   * @since 2.0
   */
  protected void detachObject(CDOID id)
  {
    getStore().removeID(id);
  }

  /**
   * @since 2.0
   */
  public void queryResources(QueryResourcesContext context)
  {
    getStore().queryResources(context);
  }

  /**
   * @since 2.0
   */
  public void executeQuery(CDOQueryInfo info, IQueryContext queryContext)
  {
    if (!info.getQueryLanguage().equals("TEST"))
    {
      throw new RuntimeException("Unsupported language " + info.getQueryLanguage());
    }

    List<Object> filters = new ArrayList<Object>();
    Object context = info.getParameters().get("context");
    Long sleep = (Long)info.getParameters().get("sleep");
    if (context != null)
    {
      if (context instanceof EClass)
      {
        final EClass cdoClass = (EClass)context;
        filters.add(new Object()
        {
          @Override
          public boolean equals(Object obj)
          {
            CDORevision revision = (CDORevision)obj;
            return revision.getEClass().equals(cdoClass);
          }
        });
      }
    }

    for (CDORevision revision : getStore().getCurrentRevisions())
    {
      if (sleep != null)
      {
        try
        {
          Thread.sleep(sleep);
        }
        catch (InterruptedException ex)
        {
          throw WrappedException.wrap(ex);
        }
      }

      boolean valid = true;

      for (Object filter : filters)
      {
        if (!filter.equals(revision))
        {
          valid = false;
          break;
        }
      }
      if (valid)
      {
        if (!queryContext.addResult(revision))
        {
          // No more results allowed
          break;
        }
      }
    }
  }

  /**
   * @since 2.0
   */
  public void refreshRevisions()
  {
  }

  @Override
  protected void doActivate() throws Exception
  {
    // Do nothing
  }

  @Override
  protected void doDeactivate() throws Exception
  {
    newRevisions.clear();
  }

  @Override
  protected void doPassivate() throws Exception
  {
    // Pooling of store accessors not supported
  }

  @Override
  protected void doUnpassivate() throws Exception
  {
    // Pooling of store accessors not supported
  }
}
