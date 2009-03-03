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
package org.eclipse.emf.cdo.internal.server.protocol;

import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;
import org.eclipse.emf.cdo.internal.server.bundle.OM;

import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EPackage;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public class LoadPackageUnitIndication extends CDOReadIndication
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_PROTOCOL, LoadPackageUnitIndication.class);

  private EPackage cdoPackage;

  public LoadPackageUnitIndication(CDOServerProtocol protocol)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_LOAD_PACKAGE_UNIT);
  }

  @Override
  protected void indicating(CDODataInput in) throws IOException
  {
    String packageUnitID = in.readCDOPackageURI();
    if (TRACER.isEnabled())
    {
      TRACER.format("Read packageUnitID: {0}", packageUnitID);
    }

    cdoPackage = getRepository().getPackageRegistry().getEPackage(packageUnitID);
    if (cdoPackage == null)
    {
      throw new IllegalStateException("CDO package not found: " + packageUnitID);
    }
  }

  @Override
  protected void responding(CDODataOutput out) throws IOException
  {
    if (onlyEcore)
    {
      String ecore = cdoPackage.getEcore();
      if (TRACER.isEnabled())
      {
        TRACER.format("Writing ecore:\n{0}", ecore);
      }

      out.writeString(ecore);
    }
    else
    {
      if (TRACER.isEnabled())
      {
        TRACER.format("Writing package: {0}", cdoPackage);
      }

      out.writeEPackage(cdoPackage);
    }
  }

  public static void sendPackageUnits(CDODataOutput out, CDOPackageUnit[] packageUnits) throws IOException
  {
    int size = packageUnits.length;
    out.writeInt(size);
    if (TRACER.isEnabled())
    {
      TRACER.format("Writing {0} package units", size);
    }

    for (CDOPackageUnit packageUnit : packageUnits)
    {
      out.writeCDOPackageUnit(packageUnit, false);
      out.writeBoolean(packageUnit.getState() == CDOPackageUnit.State.NEW);
      out.writeBoolean(packageUnit.isDynamic());
    }
  }
}
