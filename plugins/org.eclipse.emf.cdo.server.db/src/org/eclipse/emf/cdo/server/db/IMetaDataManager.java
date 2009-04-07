/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Stefan Winkler - major refactoring
 */
package org.eclipse.emf.cdo.server.db;

import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;

import org.eclipse.net4j.db.DBType;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EPackage;

import java.sql.Connection;
import java.util.Collection;

/**
 * @author Eike Stepper
 * @author Stefan Winkler
 * @since 2.0
 */
public interface IMetaDataManager
{
  /**
   * @since 2.0
   */
  public long getMetaID(EModelElement modelElement);

  /**
   * @since 2.0
   */
  public EModelElement getMetaInstance(long id);

  /**
   * @since 2.0
   */
  public EPackage[] loadPackageUnit(Connection connection, InternalCDOPackageUnit packageUnit);

  /**
   * @since 2.0
   */
  public Collection<InternalCDOPackageUnit> readPackageUnits(Connection connection);

  /**
   * @since 2.0
   */
  public void writePackageUnits(Connection connection, InternalCDOPackageUnit[] packageUnits, OMMonitor monitor);

  /**
   * @since 2.0
   */
  public DBType getDBType(EClassifier eType);

}
