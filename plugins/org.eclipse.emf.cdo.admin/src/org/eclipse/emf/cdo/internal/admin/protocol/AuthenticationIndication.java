/*
 * Copyright (c) 2009-2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Christian W. Damus (CEA LIST) - bug 418454: adapted from CDO Server
 */
package org.eclipse.emf.cdo.internal.admin.protocol;

import org.eclipse.emf.cdo.spi.common.admin.CDOAdminProtocolConstants;

import org.eclipse.net4j.signal.security.AbstractAuthenticationIndication;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.eclipse.net4j.util.security.CredentialsProviderFactory;
import org.eclipse.net4j.util.security.IPasswordCredentialsProvider;

/**
 * @author Eike Stepper
 */
public class AuthenticationIndication extends AbstractAuthenticationIndication
{
  public AuthenticationIndication(CDOAdminClientProtocol protocol)
  {
    super(protocol, CDOAdminProtocolConstants.SIGNAL_AUTHENTICATION);
  }

  @Override
  public CDOAdminClientProtocol getProtocol()
  {
    return (CDOAdminClientProtocol)super.getProtocol();
  }

  @Override
  protected IPasswordCredentialsProvider getCredentialsProvider()
  {
    try
    {
      IManagedContainer container = getProtocol().getInfraStructure().getContainer();
      return (IPasswordCredentialsProvider)container.getElement(CredentialsProviderFactory.PRODUCT_GROUP,
          "interactive", null); //$NON-NLS-1$
    }
    catch (Exception ex)
    {
      return null;
    }
  }
}
