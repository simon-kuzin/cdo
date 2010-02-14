/***************************************************************************
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Simon McDuff - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo.internal.net4j.protocol;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import java.util.Map;

/**
 * @author Simon McDuff
 */
public class LockObjectsRequest extends RefreshSessionRequest
{
  public LockObjectsRequest(CDOClientProtocol protocol, long lastUpdateTime,
      Map<CDOBranch, Map<CDOID, InternalCDORevision>> viewedRevisions, int initialChunkSize,
      boolean enablePassiveUpdates)
  {
    super(protocol, lastUpdateTime, viewedRevisions, initialChunkSize, enablePassiveUpdates);
    throw new UnsupportedOperationException();
  }

  // private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_PROTOCOL, LockObjectsRequest.class);
  //
  // private CDOView view;
  //
  // private LockType lockType;
  //
  // private long timeout;
  //
  // public LockObjectsRequest(CDOClientProtocol protocol, CDOView view, Map<CDOID, CDOIDAndVersion> idAndVersions,
  // int referenceChunk, long timeout, LockType lockType)
  // {
  // super(protocol, CDOProtocolConstants.SIGNAL_LOCK_OBJECTS, idAndVersions, referenceChunk);
  // this.view = view;
  //
  // this.timeout = timeout;
  // this.lockType = lockType;
  // }
  //
  // @Override
  // protected void requesting(CDODataOutput out) throws IOException
  // {
  // super.requesting(out);
  // out.writeInt(view.getViewID());
  // out.writeCDOLockType(lockType);
  // out.writeLong(timeout);
  //
  // if (TRACER.isEnabled())
  // {
  //      TRACER.format("Locking of type {0} requested for view {1} with timeout {2}", //$NON-NLS-1$
  //          lockType == LockType.READ ? "read" : "write", view.getViewID(), timeout); //$NON-NLS-1$ //$NON-NLS-2$
  // }
  // }
}
