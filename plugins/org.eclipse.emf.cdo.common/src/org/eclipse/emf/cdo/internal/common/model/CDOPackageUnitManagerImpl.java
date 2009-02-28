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

import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageRegistry;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnitManager;

import org.eclipse.net4j.util.container.Container;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public class CDOPackageUnitManagerImpl extends Container<CDOPackageUnit> implements InternalCDOPackageUnitManager
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, CDOPackageUnitManagerImpl.class);

  private InternalCDOPackageRegistry packageRegistry;

  private Map<String, InternalCDOPackageUnit> packageUnits = new HashMap<String, InternalCDOPackageUnit>();

  public CDOPackageUnitManagerImpl()
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

  public InternalCDOPackageUnit getPackageUnit(String id)
  {
    synchronized (packageUnits)
    {
      return packageUnits.get(id);
    }
  }

  public InternalCDOPackageUnit[] getPackageUnits()
  {
    synchronized (packageUnits)
    {
      return packageUnits.values().toArray(new InternalCDOPackageUnit[packageUnits.size()]);
    }
  }

  public void addPackageUnit(InternalCDOPackageUnit packageUnit)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Adding {0}", packageUnit);
    }

    synchronized (packageUnits)
    {
      packageUnits.put(packageUnit.getID(), packageUnit);
    }

    fireElementAddedEvent(packageUnit);
  }

  public CDOPackageUnit[] getElements()
  {
    return getPackageUnits();
  }

  @Override
  public boolean isEmpty()
  {
    synchronized (packageUnits)
    {
      return packageUnits.isEmpty();
    }
  }
}
