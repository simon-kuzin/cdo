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
package org.eclipse.emf.spi.cdo;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.model.CDOFeature;
import org.eclipse.emf.cdo.common.model.CDOPackage;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.transaction.CDOTimeStampContext;
import org.eclipse.emf.cdo.view.CDOView;

import org.eclipse.emf.internal.cdo.net4j.protocol.CommitTransactionResult;
import org.eclipse.emf.internal.cdo.net4j.protocol.OpenSessionResult;
import org.eclipse.emf.internal.cdo.net4j.protocol.RepositoryTimeResult;
import org.eclipse.emf.internal.cdo.query.CDOAbstractQueryIteratorImpl;
import org.eclipse.emf.internal.cdo.transaction.CDOXATransactionCommitContext;

import org.eclipse.net4j.util.concurrent.RWLockManager.LockType;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Eike Stepper
 */
public interface CDOSessionProtocol
{
  public OpenSessionResult openSession(String repositoryName, boolean passiveUpdateEnabled);

  public void loadLibraries(Set<String> missingLibraries, File cacheFolder);

  public void setPassiveUpdate(Map<CDOID, CDORevision> allRevisions, int initialChunkSize, boolean passiveUpdateEnabled);

  public RepositoryTimeResult getRepositoryTime();

  public String loadPackage(CDOPackage cdoPackage, boolean onlyEcore);

  public Object loadChunk(InternalCDORevision revision, CDOFeature feature, int accessIndex, int fetchIndex,
      int fromIndex, int toIndex);

  public List<InternalCDORevision> loadRevisions(Collection<CDOID> ids, int referenceChunk);

  public List<InternalCDORevision> loadRevisionsByTime(Collection<CDOID> ids, int referenceChunk, long timeStamp);

  public InternalCDORevision loadRevisionByVersion(CDOID id, int referenceChunk, int version);

  public List<InternalCDORevision> verifyRevision(List<InternalCDORevision> revisions);

  public Collection<CDOTimeStampContext> syncRevisions(Map<CDOID, CDORevision> allRevisions, int initialChunkSize);

  public void openView(int viewId, byte protocolViewType, long timeStamp);

  public void closeView(int viewId);

  public boolean[] setAudit(int viewId, long timeStamp, List<InternalCDOObject> invalidObjects);

  public void changeSubscription(int viewId, List<CDOID> cdoIDs, boolean subscribeMode, boolean clear);

  public List<Object> query(int viewID, CDOAbstractQueryIteratorImpl<?> queryResult);

  public boolean cancelQuery(int queryId);

  public void lockObjects(CDOView view, Collection<? extends CDOObject> objects, long timeout, LockType lockType)
      throws InterruptedException;

  public void unlockObjects(CDOView view, Collection<? extends CDOObject> objects, LockType lockType);

  public boolean isObjectLocked(CDOView view, CDOObject object, LockType lockType);

  public CommitTransactionResult commitTransaction(InternalCDOCommitContext commitContext, OMMonitor monitor);

  public CommitTransactionResult commitTransactionPhase1(CDOXATransactionCommitContext xaContext, OMMonitor monitor);

  public CommitTransactionResult commitTransactionPhase2(CDOXATransactionCommitContext xaContext, OMMonitor monitor);

  public CommitTransactionResult commitTransactionPhase3(CDOXATransactionCommitContext xaContext, OMMonitor monitor);

  public CommitTransactionResult commitTransactionCancel(CDOXATransactionCommitContext xaContext, OMMonitor monitor);
}
