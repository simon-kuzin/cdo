/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.server.internal.db;

import org.eclipse.net4j.db.ddl.IDBSchema;

import org.eclipse.emf.ecore.EPackage;

/**
 * @author Eike Stepper
 */
public final class PackageServerInfo extends ServerInfo
{
  private IDBSchema schema;

  private PackageServerInfo(int id)
  {
    super(id);
  }

  public static PackageServerInfo setDBID(EPackage ePackage, int id)
  {
    PackageServerInfo serverInfo = new PackageServerInfo(id);
    ((InternalEPackage)ePackage).setServerInfo(serverInfo);
    return serverInfo;
  }

  public static IDBSchema getSchema(EPackage ePackage)
  {
    return ((PackageServerInfo)ePackage.getServerInfo()).schema;
  }

  public static void setSchema(EPackage ePackage, IDBSchema schema)
  {
    ((PackageServerInfo)ePackage.getServerInfo()).schema = schema;
  }
}
