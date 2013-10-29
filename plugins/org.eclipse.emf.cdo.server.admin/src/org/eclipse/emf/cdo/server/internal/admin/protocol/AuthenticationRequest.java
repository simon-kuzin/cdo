/*
 * Copyright (c) 2009, 2011, 2012 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Christian W. Damus (CEA LIST) - bug 418454: adapted from CDO Server
 */
package org.eclipse.emf.cdo.server.internal.admin.protocol;

import org.eclipse.emf.cdo.spi.common.admin.CDOAdminProtocolConstants;

import org.eclipse.net4j.signal.security.AbstractAuthenticationRequest;
import org.eclipse.net4j.util.security.DiffieHellman.Server.Challenge;

/**
 * @author Eike Stepper
 */
public class AuthenticationRequest extends AbstractAuthenticationRequest
{
  public AuthenticationRequest(CDOAdminServerProtocol protocol, Challenge challenge)
  {
    super(protocol, CDOAdminProtocolConstants.SIGNAL_AUTHENTICATION, challenge);
  }
}
