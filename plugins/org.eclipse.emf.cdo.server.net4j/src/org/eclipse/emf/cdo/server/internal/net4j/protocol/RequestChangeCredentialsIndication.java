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
package org.eclipse.emf.cdo.server.internal.net4j.protocol;

import org.eclipse.emf.cdo.common.protocol.CDODataInput;
import org.eclipse.emf.cdo.common.protocol.CDODataOutput;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;
import org.eclipse.emf.cdo.common.util.NotAuthenticatedException;
import org.eclipse.emf.cdo.server.internal.net4j.bundle.OM;
import org.eclipse.emf.cdo.spi.server.InternalSessionManager;

import org.eclipse.net4j.util.om.monitor.OMMonitor;
import org.eclipse.net4j.util.om.monitor.OMMonitor.Async;
import org.eclipse.net4j.util.om.trace.ContextTracer;

/**
 * Handles the request from a client to initiate the change-credentials protocol.
 */
public class RequestChangeCredentialsIndication extends CDOServerIndicationWithMonitoring
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_PROTOCOL,
      RequestChangeCredentialsIndication.class);

  public RequestChangeCredentialsIndication(CDOServerProtocol protocol)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_REQUEST_CHANGE_CREDENTIALS);
  }

  @Override
  protected void indicating(CDODataInput in, OMMonitor monitor) throws Exception
  {
    // nothing to read from the client

    if (TRACER.isEnabled())
    {
      TRACER.trace("Initiating change of user credentials"); //$NON-NLS-1$
    }
  }

  @Override
  protected void responding(CDODataOutput out, OMMonitor monitor) throws Exception
  {
    monitor.begin();
    Async async = monitor.forkAsync();

    try
    {
      try
      {
        InternalSessionManager sessionManager = getRepository().getSessionManager();
        sessionManager.changeUserCredentials(getProtocol());

        if (TRACER.isEnabled())
        {
          TRACER.format("Credentials change processed."); //$NON-NLS-1$
        }
        out.writeBoolean(true);
      }
      catch (NotAuthenticatedException ex)
      {
        // user has cancelled the authentication
        out.writeBoolean(false);
        return;
      }
    }
    finally
    {
      async.stop();
      monitor.done();
    }
  }
}
