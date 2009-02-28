package org.eclipse.emf.cdo.spi.common.model;

import org.eclipse.emf.cdo.common.model.CDOPackageUnitManager;

import org.eclipse.net4j.util.lifecycle.ILifecycle;

/**
 * @author Eike Stepper
 */
public interface InternalCDOPackageUnitManager extends CDOPackageUnitManager, ILifecycle.Introspection
{
  public InternalCDOPackageRegistry getPackageRegistry();

  public void setPackageRegistry(InternalCDOPackageRegistry packageRegistry);

  public InternalCDOPackageUnit[] getPackageUnits();

  public InternalCDOPackageUnit getPackageUnit(String id);

  public void addPackageUnit(InternalCDOPackageUnit packageUnit);
}
