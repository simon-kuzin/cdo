/*
 * Copyright (c) 2013 Eike Stepper (Berlin, Germany), CEA LIST, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Christian W. Damus (CEA LIST) - initial API and implementation
 */
package org.eclipse.emf.cdo.internal.net4j.protocol;

import org.eclipse.emf.cdo.common.protocol.CDODataInput;
import org.eclipse.emf.cdo.common.protocol.CDODataOutput;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;
import org.eclipse.emf.cdo.internal.net4j.bundle.OM;

import org.eclipse.net4j.util.om.monitor.OMMonitor;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import java.io.IOException;

/**
 * Request from the client to the server to initiate (from the server) the change-credentials protocol. 
 */
public class RequestChangeCredentialsRequest extends CDOClientRequestWithMonitoring<Boolean>
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_PROTOCOL,
      RequestChangeCredentialsRequest.class);

  public RequestChangeCredentialsRequest(CDOClientProtocol protocol)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_REQUEST_CHANGE_CREDENTIALS);
  }

  @Override
  protected void requesting(CDODataOutput out, OMMonitor monitor) throws IOException
  {
    // nothing to communicate
    if (TRACER.isEnabled())
    {
      TRACER.trace("Requesting change of user credentials"); //$NON-NLS-1$
    }
  }

  @Override
  protected Boolean confirming(CDODataInput in, OMMonitor monitor) throws IOException
  {
    return in.readBoolean();
  }
}
