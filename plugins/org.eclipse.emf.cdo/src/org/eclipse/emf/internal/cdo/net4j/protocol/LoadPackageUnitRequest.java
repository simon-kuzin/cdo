/***************************************************************************
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.internal.cdo.net4j.protocol;

import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;

import org.eclipse.emf.ecore.EPackage;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public class LoadPackageUnitRequest extends CDOClientRequest<EPackage[]>
{
  private InternalCDOPackageUnit packageUnit;

  public LoadPackageUnitRequest(CDOClientProtocol protocol, InternalCDOPackageUnit packageUnit)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_LOAD_PACKAGE_UNIT);
    this.packageUnit = packageUnit;
  }

  @Override
  protected void requesting(CDODataOutput out) throws IOException
  {
    out.writeCDOPackageURI(packageUnit.getID());
  }

  @Override
  protected EPackage[] confirming(CDODataInput in) throws IOException
  {
    int size = in.readInt();
    EPackage[] ePackages = new EPackage[size];
    for (int i = 0; i < ePackages.length; i++)
    {
      in.readEPackage(cdoPackage);
    }

    return ePackages;
  }
}
