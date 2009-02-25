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

import org.eclipse.emf.cdo.common.model.CDOPackageAdapter;
import org.eclipse.emf.cdo.common.model.CDOPackageInfo;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EPackage;

/**
 * @author Eike Stepper
 */
public class CDOPackageAdapterImpl extends AdapterImpl implements CDOPackageAdapter
{
  private CDOPackageRegistry packageRegistry;

  private CDOPackageInfo packageInfo;

  public CDOPackageAdapterImpl()
  {
  }

  public EPackage getPackage()
  {
    return (EPackage)getTarget();
  }

  public CDOPackageRegistry getPackageRegistry()
  {
    return packageRegistry;
  }

  public void setPackageRegistry(CDOPackageRegistry packageRegistry)
  {
    this.packageRegistry = packageRegistry;
  }

  public CDOPackageInfo getPackageInfo()
  {
    return packageInfo;
  }

  public void setPackageInfo(CDOPackageInfo packageInfo)
  {
    this.packageInfo = packageInfo;
  }
}
