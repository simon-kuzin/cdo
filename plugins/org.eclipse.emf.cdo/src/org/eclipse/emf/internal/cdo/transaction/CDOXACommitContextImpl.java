/***************************************************************************
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Simon McDuff - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.internal.cdo.transaction;

import org.eclipse.emf.cdo.CDOIDDangling;
import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.revision.CDOReferenceAdjuster;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.internal.common.id.CDOIDTempObjectExternalImpl;
import org.eclipse.emf.cdo.spi.common.revision.CDOIDMapper;
import org.eclipse.emf.cdo.util.CDOUtil;

import org.eclipse.emf.internal.cdo.transaction.CDOXATransactionImpl.CDOXAState;
import org.eclipse.emf.internal.cdo.util.FSMUtil;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.spi.cdo.InternalCDOObject;
import org.eclipse.emf.spi.cdo.InternalCDOTransaction;
import org.eclipse.emf.spi.cdo.CDOSessionProtocol.CommitTransactionResult;
import org.eclipse.emf.spi.cdo.InternalCDOTransaction.InternalCDOCommitContext;
import org.eclipse.emf.spi.cdo.InternalCDOXATransaction.InternalCDOXACommitContext;

import org.eclipse.core.runtime.IProgressMonitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Simon McDuff
 * @since 2.0
 */
public class CDOXACommitContextImpl implements InternalCDOXACommitContext
{
  private CDOXATransactionImpl transactionManager;

  private IProgressMonitor progressMonitor;

  private CDOXAState state;

  private CommitTransactionResult result;

  private InternalCDOCommitContext delegateCommitContext;

  private Map<CDOIDTempObjectExternalImpl, InternalCDOTransaction> requestedIDs = new HashMap<CDOIDTempObjectExternalImpl, InternalCDOTransaction>();

  private Map<InternalCDOObject, CDOIDTempObjectExternalImpl> objectToID = new HashMap<InternalCDOObject, CDOIDTempObjectExternalImpl>();

  private CDOIDMapper idMapper;

  public CDOXACommitContextImpl(CDOXATransactionImpl manager, InternalCDOCommitContext commitContext)
  {
    transactionManager = manager;
    delegateCommitContext = commitContext;
  }

  public CDOXATransactionImpl getTransactionManager()
  {
    return transactionManager;
  }

  public void setProgressMonitor(IProgressMonitor progressMonitor)
  {
    this.progressMonitor = progressMonitor;
  }

  public CDOXAState getState()
  {
    return state;
  }

  public void setState(CDOXAState state)
  {
    this.state = state;
  }

  public CommitTransactionResult getResult()
  {
    return result;
  }

  public void setResult(CommitTransactionResult result)
  {
    this.result = result;
  }

  public InternalCDOTransaction getTransaction()
  {
    return delegateCommitContext.getTransaction();
  }

  public Map<CDOIDTempObjectExternalImpl, InternalCDOTransaction> getRequestedIDs()
  {
    return requestedIDs;
  }

  public Map<CDOID, CDOObject> getDirtyObjects()
  {
    return delegateCommitContext.getDirtyObjects();
  }

  public Map<CDOID, CDOObject> getNewObjects()
  {
    return delegateCommitContext.getNewObjects();
  }

  public List<CDOPackageUnit> getNewPackageUnits()
  {
    return delegateCommitContext.getNewPackageUnits();
  }

  public Map<CDOID, CDOResource> getNewResources()
  {
    return delegateCommitContext.getNewResources();
  }

  public Map<CDOID, CDOObject> getDetachedObjects()
  {
    return delegateCommitContext.getDetachedObjects();
  }

  public Map<CDOID, CDORevisionDelta> getRevisionDeltas()
  {
    return delegateCommitContext.getRevisionDeltas();
  }

  public Object call() throws Exception
  {
    state.handle(this, progressMonitor);
    return true;
  }

  public CDOIDMapper getCDOIDMapper()
  {
    return idMapper;
  }

  public void preCommit()
  {
    delegateCommitContext.preCommit();
  }

  public void postCommit(CommitTransactionResult result)
  {
    if (result != null)
    {
      final CDOReferenceAdjuster defaultReferenceAdjuster = result.getReferenceAdjuster();
      result.setReferenceAdjuster(new CDOReferenceAdjuster()
      {
        public CDOID adjustReference(CDOID id)
        {
          CDOIDTempObjectExternalImpl externalID = objectToID.get(id);
          if (externalID != null)
          {
            id = externalID;
          }

          return defaultReferenceAdjuster.adjustReference(id);
        }
      });
    }

    delegateCommitContext.postCommit(result);
  }

  public void commitFail()
  {
    getCDOIDMapper().reverseIDMappings();
    CDOSavepointImpl.applyReferenceAdjuster(this, getCDOIDMapper());
  }

  public CDOReferenceAdjuster createAdjuster(CDOIDMapper idMapper)
  {
    this.idMapper = idMapper;
    final CDOReferenceAdjuster delegateAjuster = delegateCommitContext.createAdjuster(idMapper);
    CDOReferenceAdjuster adjuster = new CDOReferenceAdjuster()
    {
      public CDOID adjustReference(CDOID id)
      {
        if (id instanceof CDOIDDangling)
        {
          CDOIDDangling danglingID = (CDOIDDangling)id;
          EObject target = danglingID.getTarget();
          InternalCDOObject cdoObject = (InternalCDOObject)CDOUtil.getCDOObject(target);

          if (!FSMUtil.isTransient(cdoObject) && cdoObject.cdoView() != delegateCommitContext.getTransaction())
          {
            // Only register objects from others CDO repository that are persisted
            if (target.eResource() != null)
            {
              InternalCDOTransaction transaction = (InternalCDOTransaction)cdoObject.cdoView();
              CDOIDTempObjectExternalImpl idExternalTemp = getTransactionManager().getCDOIDExternalTemp(target);
              getTransactionManager().add(transaction, idExternalTemp);
              requestedIDs.put(idExternalTemp, transaction);
              objectToID.put(cdoObject, idExternalTemp);
              getCDOIDMapper().putIDMapping(danglingID, idExternalTemp);
              id = idExternalTemp;
            }
          }
        }

        return delegateAjuster.adjustReference(id);
      }
    };

    return adjuster;
  }

  public void adjustReferences(CDOReferenceAdjuster adjuster)
  {
    CDOSavepointImpl.applyReferenceAdjuster(this, adjuster);
  }
};
