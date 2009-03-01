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
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;

import org.eclipse.emf.ecore.EPackage;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public class LoadPackagesRequest extends CDOClientRequest<EPackage[]>
{
  private CDOPackageUnit packageUnit;

  public LoadPackagesRequest(CDOClientProtocol protocol, CDOPackageUnit packageUnit)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_LOAD_PACKAGES);
    this.packageUnit = packageUnit;
  }

  @Override
  protected void requesting(CDODataOutput out) throws IOException
  {
    out.writeEPackageURI(packageUnit.getID());
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
