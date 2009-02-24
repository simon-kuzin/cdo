package org.eclipse.emf.cdo.common.model.internal;

import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.model.CDOPackageInfo;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.model.CDOPackageUnitManager;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public interface InternalCDOPackageUnit extends CDOPackageUnit
{
  public void setPackageUnitManager(CDOPackageUnitManager packageUnitManager);

  public void setID(String id);

  public void setState(State state);

  public void setTimeStamp(long timeStamp);

  public void setPackageInfos(CDOPackageInfo[] packageInfos);

  public void write(CDODataOutput out) throws IOException;

  public void read(CDODataInput in) throws IOException;
}
