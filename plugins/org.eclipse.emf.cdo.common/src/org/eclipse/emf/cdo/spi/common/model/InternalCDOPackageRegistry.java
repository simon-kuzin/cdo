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
import org.eclipse.emf.cdo.common.model.CDOPackageLoader;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.InternalEObject;

/**
 * @author Eike Stepper
 */
public interface InternalCDOPackageRegistry extends CDOPackageRegistry
{
  public MetaInstanceMapper getMetaInstanceMapper();

  public void setReplacingDescriptors(boolean replacingDescriptors);

  public void setPackageLoader(CDOPackageLoader packageLoader);

  public Object basicPut(String nsURI, Object value);

  public void putPackageUnit(InternalCDOPackageUnit packageUnit);

  public InternalCDOPackageInfo getPackageInfo(Object value);

  public InternalCDOPackageUnit[] getPackageUnits();

  /**
   * @author Eike Stepper
   */
  public interface MetaInstanceMapper
  {
    public InternalEObject lookupMetaInstance(CDOID id);

    public CDOID lookupMetaInstanceID(InternalEObject metaInstance);

    public CDOIDMetaRange mapMetaInstances(EPackage ePackage);

    public void remapMetaInstance(CDOID oldID, CDOID newID);
  }
}
