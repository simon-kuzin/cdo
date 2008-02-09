/***************************************************************************
 * Copyright (c) 2004 - 2008 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo.internal.server;

import org.eclipse.emf.cdo.internal.protocol.model.CDOPackageImpl;
import org.eclipse.emf.cdo.internal.protocol.revision.InternalCDORevision;
import org.eclipse.emf.cdo.internal.protocol.revision.delta.InternalCDORevisionDelta;
import org.eclipse.emf.cdo.internal.server.bundle.OM;
import org.eclipse.emf.cdo.protocol.id.CDOID;
import org.eclipse.emf.cdo.protocol.id.CDOIDMetaRange;
import org.eclipse.emf.cdo.protocol.id.CDOIDObjectFactory;
import org.eclipse.emf.cdo.protocol.id.CDOIDTemp;
import org.eclipse.emf.cdo.protocol.model.CDOPackage;
import org.eclipse.emf.cdo.protocol.model.CDOPackageManager;
import org.eclipse.emf.cdo.protocol.model.core.CDOCorePackage;
import org.eclipse.emf.cdo.protocol.model.resource.CDOResourcePackage;
import org.eclipse.emf.cdo.protocol.revision.CDORevision;
import org.eclipse.emf.cdo.protocol.revision.CDORevisionResolver;
import org.eclipse.emf.cdo.protocol.revision.CDORevisionUtil;
import org.eclipse.emf.cdo.protocol.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.server.IPackageManager;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.IStoreWriter;
import org.eclipse.emf.cdo.server.ITransaction;
import org.eclipse.emf.cdo.server.StoreUtil;

import org.eclipse.net4j.internal.util.om.trace.ContextTracer;
import org.eclipse.net4j.util.ObjectUtil;
import org.eclipse.net4j.util.event.IListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Eike Stepper
 */
public class Transaction extends View implements ITransaction, IStoreWriter.CommitContext
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_TRANSACTION, Transaction.class);

  private IRepository repository;

  private IPackageManager repositoryPackageManager;

  private TransactionPackageManager packageManager;

  private IStoreWriter storeWriter;

  private long timeStamp;

  private CDOPackage[] newPackages;

  private CDORevision[] newResources;

  private CDORevision[] newObjects;

  private CDORevisionDelta[] dirtyObjectDeltas;

  private CDORevision[] dirtyObjects;

  private List<CDOIDMetaRange> metaIDRanges = new ArrayList<CDOIDMetaRange>();

  private ConcurrentMap<CDOIDTemp, CDOID> idMappings = new ConcurrentHashMap<CDOIDTemp, CDOID>();

  private String rollbackMessage;

  public Transaction(Session session, int viewID)
  {
    super(session, viewID, Type.TRANSACTION);
    repository = session.getSessionManager().getRepository();
    repositoryPackageManager = repository.getPackageManager();
    packageManager = new TransactionPackageManager();

  }

  public int getTransactionID()
  {
    return getViewID();
  }

  public long getTimeStamp()
  {
    return timeStamp;
  }

  public TransactionPackageManager getPackageManager()
  {
    return packageManager;
  }

  public CDOPackage[] getNewPackages()
  {
    return newPackages;
  }

  public int getNumberOfNewResources()
  {
    return newResources == null ? 0 : newResources.length;
  }

  public int getNumberOfNewObjects()
  {
    return newObjects == null ? 0 : newObjects.length;
  }

  public int getNumberOfDirtyObjects()
  {
    return dirtyObjects == null ? 0 : dirtyObjects.length;
  }

  public List<CDOIDMetaRange> getMetaIDRanges()
  {
    return Collections.unmodifiableList(metaIDRanges);
  }

  public Map<CDOIDTemp, CDOID> getIdMappings()
  {
    return Collections.unmodifiableMap(idMappings);
  }

  public String getRollbackMessage()
  {
    return rollbackMessage;
  }

  public void commit(CDOPackage[] newPackages, CDORevision[] newResources, CDORevision[] newObjects,
      CDORevisionDelta[] dirtyObjectDeltas)
  {
    timeStamp = System.currentTimeMillis();
    this.newPackages = newPackages;
    this.newResources = newResources;
    this.newObjects = newObjects;
    this.dirtyObjectDeltas = dirtyObjectDeltas;
    dirtyObjects = new CDORevision[dirtyObjectDeltas.length];

    storeWriter = repository.getStore().getWriter(this);
    StoreUtil.setReader(storeWriter);

    try
    {
      adjustMetaRanges();
      beginCommit();
      populateIDMappings();
      adjust();
      finishCommit();
      updateInfraStructure();
    }
    catch (RuntimeException ex)
    {
      OM.LOG.error(ex);
      rollbackMessage = ex.getMessage();
      cancelCommit();
    }
    finally
    {
      if (storeWriter != null)
      {
        StoreUtil.setReader(null);
        storeWriter.release();
        storeWriter = null;
      }
    }
  }

  public void postCommit(boolean success)
  {
    try
    {
      int modifications = dirtyObjectDeltas.length;
      if (success && modifications > 0)
      {
        List<CDOID> dirtyIDs = new ArrayList<CDOID>(modifications);
        for (int i = 0; i < modifications; i++)
        {
          dirtyIDs.add(dirtyObjectDeltas[i].getID());
        }

        SessionManager sessionManager = (SessionManager)repository.getSessionManager();
        sessionManager.notifyInvalidation(timeStamp, dirtyIDs, getSession());
      }
    }
    finally
    {
      timeStamp = 0L;
      packageManager.clear();
      metaIDRanges.clear();
      idMappings.clear();
      rollbackMessage = null;
      newPackages = null;
      newResources = null;
      newObjects = null;
      dirtyObjectDeltas = null;
      dirtyObjects = null;
    }
  }

  private void adjustMetaRanges()
  {
    for (CDOPackage newPackage : newPackages)
    {
      adjustMetaRange(newPackage);
    }
  }

  private void adjustMetaRange(CDOPackage newPackage)
  {
    CDOIDMetaRange oldRange = newPackage.getMetaIDRange();
    if (!oldRange.isTemporary())
    {
      throw new IllegalStateException("!oldRange.isTemporary()");
    }

    CDOIDMetaRange newRange = repository.getMetaIDRange(oldRange.size());
    ((CDOPackageImpl)newPackage).setMetaIDRange(newRange);
    for (int l = 0; l < oldRange.size(); l++)
    {
      CDOIDTemp oldID = (CDOIDTemp)oldRange.get(l);
      CDOID newID = newRange.get(l);
      if (TRACER.isEnabled())
      {
        TRACER.format("Mapping meta ID: {0} --> {1}", oldID, newID);
      }

      idMappings.put(oldID, newID);
    }

    metaIDRanges.add(newRange);
  }

  private void beginCommit()
  {
    storeWriter.beginCommit(this);
  }

  private void populateIDMappings()
  {
    for (int i = 0; i < newResources.length; i++)
    {
      CDORevision newResource = newResources[i];
      CDOID newID = storeWriter.createNewResourceID(this, i, newResource);
      addIDMapping((CDOIDTemp)newResource.getID(), newID);
    }

    for (int i = 0; i < newObjects.length; i++)
    {
      CDORevision newObject = newObjects[i];
      CDOID newID = storeWriter.createNewObjectID(this, i, newObject);
      addIDMapping((CDOIDTemp)newObject.getID(), newID);
    }
  }

  private void addIDMapping(CDOIDTemp oldID, CDOID newID)
  {
    if (newID == null)
    {
      throw new IllegalArgumentException("newID == null");
    }

    CDOID previousMapping = idMappings.putIfAbsent(oldID, newID);
    if (previousMapping != null)
    {
      throw new IllegalStateException("previousMapping != null");
    }
  }

  private void adjust()
  {
    for (CDORevision newResource : newResources)
    {
      adjustRevision((InternalCDORevision)newResource);
    }

    for (CDORevision newObject : newObjects)
    {
      adjustRevision((InternalCDORevision)newObject);
    }

    for (CDORevisionDelta dirtyObjectDelta : dirtyObjectDeltas)
    {
      ((InternalCDORevisionDelta)dirtyObjectDelta).adjustReferences(idMappings);
    }
  }

  private void adjustRevision(InternalCDORevision revision)
  {
    revision.setID(idMappings.get(revision.getID()));
    revision.setCreated(timeStamp);
    revision.adjustReferences(idMappings);
  }

  private void computeDirtyObjects(boolean failOnNull)
  {
    for (int i = 0; i < dirtyObjectDeltas.length; i++)
    {
      dirtyObjects[i] = computeDirtyObject(dirtyObjectDeltas[i]);
      if (dirtyObjects[i] == null && failOnNull)
      {
        throw new IllegalStateException("Can not retrieve origin revision for " + dirtyObjectDeltas[i]);
      }
    }
  }

  private CDORevision computeDirtyObject(CDORevisionDelta dirtyObjectDelta)
  {
    CDOID id = dirtyObjectDelta.getID();
    int version = dirtyObjectDelta.getOriginVersion();

    CDORevisionResolver revisionResolver = repository.getRevisionManager();
    CDORevision originObject = revisionResolver.getRevisionByVersion(id, CDORevision.UNCHUNKED, version, false);
    if (originObject != null)
    {
      CDORevision dirtyObject = CDORevisionUtil.copy(originObject);
      dirtyObjectDelta.apply(dirtyObject);
      ((InternalCDORevision)dirtyObject).setCreated(timeStamp);
      return dirtyObject;
    }

    return null;
  }

  private void finishCommit()
  {
    if (repository.isSupportingRevisionDeltas())
    {
      computeDirtyObjects(false);
      storeWriter.finishCommit(this, newResources, newObjects, dirtyObjectDeltas);
    }
    else
    {
      computeDirtyObjects(true);
      storeWriter.finishCommit(this, newResources, newObjects, dirtyObjects);
    }
  }

  private void cancelCommit()
  {
    if (storeWriter != null)
    {
      try
      {
        storeWriter.cancelCommit(this);
      }
      catch (RuntimeException ex)
      {
        OM.LOG.warn("Problem while rolling back  the transaction", ex);
      }
    }
  }

  private void updateInfraStructure()
  {
    try
    {
      addNewPackages();
      addRevisions(newResources);
      addRevisions(newObjects);
      addRevisions(dirtyObjects);
    }
    catch (RuntimeException ex)
    {
      // TODO Rethink this case
      OM.LOG.error("FATAL: Memory infrastructure corrupted after successful commit operation of the store");
    }
  }

  private void addNewPackages()
  {
    PackageManager packageManager = (PackageManager)repository.getPackageManager();
    for (int i = 0; i < newPackages.length; i++)
    {
      CDOPackage cdoPackage = newPackages[i];
      packageManager.addPackage(cdoPackage);
    }
  }

  private void addRevisions(CDORevision[] revisions)
  {
    RevisionManager revisionManager = (RevisionManager)repository.getRevisionManager();
    for (CDORevision revision : revisions)
    {
      if (revision != null)
      {
        revisionManager.addRevision((InternalCDORevision)revision);
      }
    }
  }

  /**
   * @author Eike Stepper
   */
  public final class TransactionPackageManager implements CDOPackageManager
  {

    private List<CDOPackage> newPackages = new ArrayList<CDOPackage>();

    public TransactionPackageManager()
    {
    }

    public void addPackage(CDOPackage cdoPackage)
    {
      newPackages.add(cdoPackage);
    }

    public void clear()
    {
      newPackages.clear();
    }

    public CDOIDObjectFactory getCDOIDObjectFactory()
    {
      return repositoryPackageManager.getCDOIDObjectFactory();
    }

    public CDOPackage lookupPackage(String uri)
    {
      for (CDOPackage cdoPackage : newPackages)
      {
        if (ObjectUtil.equals(cdoPackage.getPackageURI(), uri))
        {
          return cdoPackage;
        }
      }

      return repositoryPackageManager.lookupPackage(uri);
    }

    public CDOCorePackage getCDOCorePackage()
    {
      return repositoryPackageManager.getCDOCorePackage();
    }

    public CDOResourcePackage getCDOResourcePackage()
    {
      return repositoryPackageManager.getCDOResourcePackage();
    }

    public int getPackageCount()
    {
      throw new UnsupportedOperationException();
    }

    public CDOPackage[] getPackages()
    {
      throw new UnsupportedOperationException();
    }

    public CDOPackage[] getElements()
    {
      throw new UnsupportedOperationException();
    }

    public boolean isEmpty()
    {
      throw new UnsupportedOperationException();
    }

    public void addListener(IListener listener)
    {
      throw new UnsupportedOperationException();
    }

    public void removeListener(IListener listener)
    {
      throw new UnsupportedOperationException();
    }
  }
}
