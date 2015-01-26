/*
 * Copyright (c) 2010-2012 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.explorer.repositories.local;

/**
 * @author Eike Stepper
 */
public class LocalCDORepository extends CDORepositoryImpl
{
  private static final long serialVersionUID = 1L;

  private String connectorType;

  private String connectorDescription;

  public LocalCDORepository()
  {
  }

  public LocalCDORepository(String repositoryName, String connectorType, String connectorDescription)
  {
    super(repositoryName);
    this.connectorType = connectorType;
    this.connectorDescription = connectorDescription;
  }

  public final String getConnectorType()
  {
    return connectorType;
  }

  public final String getConnectorDescription()
  {
    return connectorDescription;
  }

  /**
   * @author Eike Stepper
   */
  public static final class Factory extends CDORepositoryFactory
  {
    public static final String TYPE = "remote";

    public Factory()
    {
      super(TYPE);
    }

    @Override
    public CDORepository create(String description) throws ProductCreationException
    {
      return null;
    }
  }
}
