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

import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageDescriptor;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageInfo;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageRegistry;

import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EPackage;

/**
 * @author Eike Stepper
 */
public class CDOPackageDescriptorImpl implements InternalCDOPackageDescriptor
{
  private InternalCDOPackageRegistry packageRegistry;

  private InternalCDOPackageInfo packageInfo;

  public CDOPackageDescriptorImpl()
  {
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
    return packageInfo.getEPackage(true);
  }

  public EFactory getEFactory()
  {
    return getEPackage().getEFactoryInstance();
  }
}
