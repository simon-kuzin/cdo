/*
 * Copyright (c) 2009-2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Christian W. Damus (CEA LIST) - bug 418454
 */
package org.eclipse.emf.cdo.internal.net4j.protocol;

import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;

import org.eclipse.net4j.signal.SignalProtocol;
import org.eclipse.net4j.signal.security.AbstractAuthenticationIndication;
import org.eclipse.net4j.util.security.IPasswordCredentialsProvider;

import org.eclipse.emf.spi.cdo.InternalCDOSession;

/**
 * @author Eike Stepper
 */
public class AuthenticationIndication extends AbstractAuthenticationIndication
{

  public AuthenticationIndication(SignalProtocol<?> protocol)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_AUTHENTICATION);
  }

  @Override
  public CDOClientProtocol getProtocol()
  {
    return (CDOClientProtocol)super.getProtocol();
  }

  protected InternalCDOSession getSession()
  {
    return (InternalCDOSession)getProtocol().getSession();
  }

  @Override
  protected IPasswordCredentialsProvider getCredentialsProvider()
  {
    return getSession().getCredentialsProvider();
  }
}
