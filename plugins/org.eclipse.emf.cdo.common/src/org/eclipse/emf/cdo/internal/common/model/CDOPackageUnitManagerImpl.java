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

  private Map<String, CDOPackageUnit> packageUnits = new HashMap<String, CDOPackageUnit>();

  public CDOPackageUnitManagerImpl()
  {
  }

  public CDOPackageUnit getPackageUnit(String id)
  {
    synchronized (packageUnits)
    {
      return packageUnits.get(id);
    }
  }

  public CDOPackageUnit[] getPackageUnits()
  {
    synchronized (packageUnits)
    {
      return packageUnits.values().toArray(new CDOPackageUnit[packageUnits.size()]);
    }
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

  public void addPackageUnit(CDOPackageUnit packageUnit)
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
}
