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
package org.eclipse.emf.cdo.spi.common.model;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDMetaRange;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;

import org.eclipse.net4j.util.lifecycle.ILifecycle;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.InternalEObject;

/**
 * @author Eike Stepper
 */
public interface InternalCDOPackageRegistry extends CDOPackageRegistry, ILifecycle.Introspection
{
  public MetaInstanceMapper getMetaInstanceMapper();

  public void setReplacingDescriptors(boolean replacingDescriptors);

  public PackageProcessor getPackageProcessor();

  public void setPackageProcessor(PackageProcessor packageProcessor);

  public PackageLoader getPackageLoader();

  public void setPackageLoader(PackageLoader packageLoader);

  public Object basicPut(String nsURI, Object value);

  public void putPackageUnit(InternalCDOPackageUnit packageUnit);

  public InternalCDOPackageInfo getPackageInfo(Object keyOrValue);

  /**
   * Returns all but the system package infos that are registered in this package registry.
   */
  public InternalCDOPackageInfo[] getPackageInfos();

  /**
   * Returns all but the system package units that are registered in this package registry.
   */
  public InternalCDOPackageUnit[] getPackageUnits();

  public EPackage[] getEPackages();

  /**
   * @author Eike Stepper
   */
  public interface PackageProcessor
  {
    public Object processPackage(Object value);
  }

  /**
   * @author Eike Stepper
   */
  public interface PackageLoader
  {
    public EPackage[] loadPackages(CDOPackageUnit packageUnit);
  }

  /**
   * @author Eike Stepper
   */
  public interface MetaInstanceMapper
  {
    public InternalEObject lookupMetaInstance(CDOID id);

    public CDOID lookupMetaInstanceID(InternalEObject metaInstance);

    public void mapMetaInstances(EPackage ePackage, CDOIDMetaRange metaIDRange);

    public CDOIDMetaRange mapMetaInstances(EPackage ePackage);

    public void remapMetaInstance(CDOID oldID, CDOID newID);

    public void clear();
  }
}
