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
package org.eclipse.emf.cdo.internal.common.model;

import org.eclipse.emf.cdo.common.model.CDOModelUtil;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageAdapter;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageInfo;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageRegistry;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EPackage;

/**
 * @author Eike Stepper
 */
public class CDOPackageAdapterImpl extends AdapterImpl implements InternalCDOPackageAdapter
{
  private InternalCDOPackageRegistry packageRegistry;

  private InternalCDOPackageInfo packageInfo;

  public CDOPackageAdapterImpl()
  {
  }

  @Override
  public boolean isAdapterForType(Object type)
  {
    return EPackage.class.isInstance(type);
  }

  public InternalCDOPackageRegistry getPackageRegistry()
  {
    return packageRegistry;
  }

  public void setPackageRegistry(InternalCDOPackageRegistry packageRegistry)
  {
    this.packageRegistry = packageRegistry;
  }

  public InternalCDOPackageInfo getPackageInfo()
  {
    return packageInfo;
  }

  public void setPackageInfo(InternalCDOPackageInfo packageInfo)
  {
    this.packageInfo = packageInfo;
  }

  public EPackage getEPackage()
  {
    return (EPackage)getTarget();
  }

  public boolean isCorePackage()
  {
    return CDOModelUtil.isCorePackage(getEPackage());
  }

  public boolean isResourcePackage()
  {
    return CDOModelUtil.isResourcePackage(getEPackage());
  }

  public boolean isSystemPackage()
  {
    return CDOModelUtil.isSystemPackage(getEPackage());
  }
}
