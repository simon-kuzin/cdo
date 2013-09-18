/*
 * Copyright (c) 2009-2012 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.spi.cdo;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIdentifiable;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionDelta;
import org.eclipse.emf.cdo.transaction.CDOSavepoint;
import org.eclipse.emf.cdo.transaction.CDOTransaction;

import org.eclipse.emf.internal.cdo.view.CDOStateMachine2;

import java.util.Map;
import java.util.Set;

/**
 * A {@link CDOSavepoint} has to capture enough data to support the following use cases:
 * <ul> 
 * <li> <b>Detection</b> of undo operations (netted out deltas) in comparison to the transaction's baseline. 
 * <li> <b>Commit</b> of the entire {@link CDOTransaction}. 
 * <li> <b>Rollback</b> to the initial state of this savepoint.
 * <li> <b>Support</b> of all other {@link CDOStateMachine2} transitions.
 * </ul>
 *   
 * @author Eike Stepper
 * @since 3.0
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface InternalCDOSavepoint extends CDOSavepoint, InternalCDOUserSavepoint
{
  public InternalCDOTransaction getTransaction();

  public InternalCDOSavepoint getFirstSavePoint();

  public InternalCDOSavepoint getPreviousSavepoint();

  public InternalCDOSavepoint getNextSavepoint();

  public ChangeInfo addChangeInfo(ChangeInfo changeInfo);

  public ChangeInfo removeChangeInfo(CDOObject object);

  public ChangeInfo removeChangeInfo(CDOID id);

  public ChangeInfo getChangeInfo(CDOID id);

  public ChangeInfo getDetachedInfo(CDOObject object);

  public Map<CDOID, ChangeInfo> getChangeInfos();

  public Map<InternalCDOObject, ChangeInfo> getDetachedInfos();

  /**
   * @since 4.1
   */
  public boolean isNewObject(CDOID id);

  public boolean isDetachedObject(CDOID id);

  public void clear();

  public boolean isInMemory();

  public void setInMemory(boolean inMemory);

  @Deprecated
  public Set<CDOID> getSharedDetachedObjects();

  @Deprecated
  public void recalculateSharedDetachedObjects();

  /**
   * Adds the given object to this savepoint and returns <code>true</code> if it was TRANSIENT, 
   * <code>false</code> otherwise (DETACHED).
   * 
   * @deprecated As of 4.3 no longer supported.
   */
  @Deprecated
  public void attachObject(InternalCDOObject object);

  /**
   * Removes the given object from this savepoint and returns <code>true</code> if it was NEW, 
   * <code>false</code> otherwise (CLEAN | DIRTY).
   * 
   * @deprecated As of 4.3 no longer supported.
   */
  @Deprecated
  public void removeObject(InternalCDOObject object);

  /**
   * @author Eike Stepper
   */
  public interface ChangeInfo extends CDOIdentifiable
  {
    public ChangeType getType();

    /**
     * Returns the object or <code>null</code>.
     */
    public InternalCDOObject getObject();

    /**
     * Returns the revision delta or <code>null</code>.
     */
    public InternalCDORevisionDelta getRevisionDelta();

    /**
     * Returns the clean revision or <code>null</code>. The clean revision is used to identify undoing operations.
     */
    public InternalCDORevision getCleanRevision();

    /**
     * Returns the base revision or <code>null</code>. The base revision is used to rollback to savepoints.
     */
    public InternalCDORevision getBaseRevision();

    public ChangeInfo setSavepoint();

    /**
     * @author Eike Stepper
     */
    public enum ChangeType
    {
      NEW, DIRTY, CONFLICT, UNDONE, DETACHED
    }
  }
}
