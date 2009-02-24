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
package org.eclipse.emf.cdo.common.model.internal;

import org.eclipse.emf.cdo.common.id.CDOIDMetaRange;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public class CDOPackageInfoImpl implements InternalCDOPackageInfo
{
  private CDOPackageUnit packageUnit;

  private String packageURI;

  private String parentURI;

  private CDOIDMetaRange metaIDRange;

  public CDOPackageInfoImpl()
  {
  }

  public CDOPackageUnit getPackageUnit()
  {
    return packageUnit;
  }

  public void setPackageUnit(CDOPackageUnit packageUnit)
  {
    this.packageUnit = packageUnit;
  }

  public String getPackageURI()
  {
    return packageURI;
  }

  public void setPackageURI(String packageUri)
  {
    packageURI = packageUri;
  }

  public String getParentURI()
  {
    return parentURI;
  }

  public void setParentURI(String parentUri)
  {
    parentURI = parentUri;
  }

  public CDOIDMetaRange getMetaIDRange()
  {
    return metaIDRange;
  }

  public void setMetaIDRange(CDOIDMetaRange metaIdRange)
  {
    metaIDRange = metaIdRange;
  }

  public void write(CDODataOutput out) throws IOException
  {
    out.writeString(packageURI);
    out.writeString(parentURI);
    out.writeCDOIDMetaRange(metaIDRange);
  }

  public void read(CDODataInput in) throws IOException
  {
    packageURI = in.readEPackageURI();
    parentURI = in.readEPackageURI();
    metaIDRange = in.readCDOIDMetaRange();
  }
}
