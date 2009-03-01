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

import org.eclipse.emf.cdo.common.model.CDOPackageLoader;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;

/**
 * @author Eike Stepper
 */
public interface InternalCDOPackageRegistry extends CDOPackageRegistry, CDOMetaInstanceMapper
{
  public void setPackageLoader(CDOPackageLoader packageLoader);

  public void putPackageUnit(InternalCDOPackageUnit packageUnit);

  public InternalCDOPackageInfo getPackageInfo(Object value);

  public InternalCDOPackageUnit[] getPackageUnits();
}
