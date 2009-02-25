package org.eclipse.emf.cdo.spi.common.model;

import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.model.CDOPackageUnitManager;

import org.eclipse.net4j.util.lifecycle.ILifecycle;

/**
 * @author Eike Stepper
 */
public interface InternalCDOPackageUnitManager extends CDOPackageUnitManager, ILifecycle.Introspection
{
  public void addPackageUnit(CDOPackageUnit packageUnit);
}
