/***************************************************************************
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Simon McDuff - initial API and implementation
 *    Simon McDuff - bug 230832
 **************************************************************************/
package org.eclipse.emf.cdo.internal.net4j.protocol;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.protocol.CDOProtocol.RefreshSessionHandler;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;

import java.util.Map;

/**
 * @author Simon McDuff
 * @since 2.0
 */
public class SetPassiveUpdateRequest extends RefreshSessionRequest
{
  public SetPassiveUpdateRequest(CDOClientProtocol protocol,
      Map<CDOBranch, Map<CDOID, CDORevisionKey>> viewedRevisions, int initialChunkSize, boolean enablePassiveUpdates,
      RefreshSessionHandler handler)
  {
    super(protocol, viewedRevisions, initialChunkSize, enablePassiveUpdates, handler);
    // TODO: implement SetPassiveUpdateRequest.enclosing_method(enclosing_method_arguments)
    throw new UnsupportedOperationException();
  }

  // private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_PROTOCOL, SetPassiveUpdateRequest.class);
  //
  // private boolean passiveUpdateEnabled;
  //
  // public SetPassiveUpdateRequest(CDOClientProtocol protocol, Map<CDOID, CDOIDAndVersion> idAndVersions,
  // int referenceChunk, boolean passiveUpdateEnabled)
  // {
  // super(protocol, CDOProtocolConstants.SIGNAL_PASSIVE_UPDATE, idAndVersions, referenceChunk);
  // this.passiveUpdateEnabled = passiveUpdateEnabled;
  // }
  //
  // @Override
  // protected void requesting(CDODataOutput out) throws IOException
  // {
  // if (TRACER.isEnabled())
  // {
  //      TRACER.trace("Turning " + (passiveUpdateEnabled ? "on" : "off") + " passive update"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
  // }
  //
  // super.requesting(out);
  // out.writeBoolean(passiveUpdateEnabled);
  // }
}
