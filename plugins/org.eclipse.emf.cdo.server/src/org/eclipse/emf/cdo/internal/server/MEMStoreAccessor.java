/***************************************************************************
 * Copyright (c) 2004 - 2008 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Simon McDuff - initial API and implementation
 *    Eike Stepper - maintenance
 **************************************************************************/
package org.eclipse.emf.cdo.internal.server;

import org.eclipse.emf.cdo.internal.protocol.revision.InternalCDORevision;
import org.eclipse.emf.cdo.protocol.id.CDOID;
import org.eclipse.emf.cdo.protocol.model.CDOClassRef;
import org.eclipse.emf.cdo.protocol.model.CDOFeature;
import org.eclipse.emf.cdo.protocol.model.CDOPackage;
import org.eclipse.emf.cdo.protocol.model.CDOPackageInfo;
import org.eclipse.emf.cdo.protocol.revision.CDORevision;
import org.eclipse.emf.cdo.protocol.revision.CDORevisionUtil;
import org.eclipse.emf.cdo.protocol.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.server.ISession;
import org.eclipse.emf.cdo.server.IStoreChunkReader;
import org.eclipse.emf.cdo.server.IStoreReader;
import org.eclipse.emf.cdo.server.IStoreWriter;
import org.eclipse.emf.cdo.server.IView;

import org.eclipse.net4j.util.io.CloseableIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Simon McDuff
 */
public class MEMStoreAccessor extends StoreAccessor implements IStoreReader, IStoreWriter
{
  List<CDORevision> newRevisions = new ArrayList<CDORevision>();

  public MEMStoreAccessor(MEMStore store, ISession session)
  {
    super(store, session);
  }

  public MEMStoreAccessor(MEMStore store, IView view)
  {
    super(store, view);
  }

  @Override
  public MEMStore getStore()
  {
    return (MEMStore)super.getStore();
  }

  public IStoreChunkReader createChunkReader(CDORevision revision, CDOFeature feature)
  {
    return new MEMStoreChunkReader(this, revision, feature);
  }

  public Collection<CDOPackageInfo> readPackageInfos()
  {
    return Collections.emptySet();
  }

  public void readPackage(CDOPackage cdoPackage)
  {
    throw new UnsupportedOperationException();
  }

  public CloseableIterator<CDOID> readObjectIDs(boolean withTypes)
  {
    throw new UnsupportedOperationException();
  }

  public CDOClassRef readObjectType(CDOID id)
  {
    throw new UnsupportedOperationException();
  }

  public CDORevision readRevision(CDOID id, int referenceChunk)
  {
    InternalCDORevision storeRevision = (InternalCDORevision)getStore().getRevision(id);
    // IRevisionManager revisionManager = getStore().getRepository().getRevisionManager();
    // InternalCDORevision newRevision = new InternalCDORevision(revisionManager, storeRevision.getCDOClass(),
    // storeRevision
    // .getID());
    // newRevision.setResourceID(storeRevision.getResourceID());
    //
    // for (CDOFeature feature : storeRevision.getCDOClass().getAllFeatures())
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
    throw new UnsupportedOperationException();
  }

  public CDORevision readRevisionByVersion(CDOID id, int referenceChunk, int version)
  {
    throw new UnsupportedOperationException();
  }

  public CDOID readResourceID(String path)
  {
    throw new UnsupportedOperationException();
  }

  public String readResourcePath(CDOID id)
  {
    throw new UnsupportedOperationException();
  }

  public void beginCommit(CommitContext context)
  {
    // Do nothing
  }

  public CDOID createNewResourceID(CommitContext context, int i, CDORevision newResource)
  {
    return getStore().getNextCDOID();
  }

  public CDOID createNewObjectID(CommitContext context, int i, CDORevision newObject)
  {
    return getStore().getNextCDOID();
  }

  public void finishCommit(CommitContext context, CDORevision[] newResources, CDORevision[] newObjects,
      CDORevision[] dirtyObjects)
  {
    writeRevisions(newResources);
    writeRevisions(newObjects);
    writeRevisions(dirtyObjects);
    commit();
  }

  public void finishCommit(CommitContext context, CDORevision[] newResources, CDORevision[] newObjects,
      CDORevisionDelta[] dirtyObjectDeltas)
  {
    writeRevisions(newResources);
    writeRevisions(newObjects);
    writeRevisionDeltas(dirtyObjectDeltas);
    commit();
  }

  public void cancelCommit(CommitContext context)
  {
  }

  protected void writeRevisions(CDORevision[] revisions)
  {
    for (CDORevision revision : revisions)
    {
      newRevisions.add(revision);
    }
  }

  protected void writeRevisionDeltas(CDORevisionDelta[] revisionDeltas)
  {
    for (CDORevisionDelta revisionDelta : revisionDeltas)
    {
      CDORevision revision = getStore().getRevision(revisionDelta.getID());
      CDORevision newRevision = CDORevisionUtil.copy(revision);
      revisionDelta.apply(newRevision);
      newRevisions.add(newRevision);
    }
  }

  protected void commit()
  {
    MEMStore store = getStore();
    for (CDORevision revision : newRevisions)
    {
      store.addRevision(revision);
    }
  }

  @Override
  protected void doRelease()
  {
    newRevisions.clear();
  }
}
