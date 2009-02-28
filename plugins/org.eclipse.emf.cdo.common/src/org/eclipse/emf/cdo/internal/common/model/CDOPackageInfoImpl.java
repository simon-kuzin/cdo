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

import org.eclipse.emf.cdo.common.id.CDOIDMetaRange;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageAdapter;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageInfo;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;

import org.eclipse.net4j.util.om.trace.ContextTracer;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Eike Stepper
 */
public class CDOPackageInfoImpl implements InternalCDOPackageInfo
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, CDOPackageInfoImpl.class);

  private InternalCDOPackageUnit packageUnit;

  private String packageURI;

  private String parentURI;

  private CDOIDMetaRange metaIDRange;

  private InternalCDOPackageAdapter packageAdapter;

  public CDOPackageInfoImpl()
  {
  }

  public InternalCDOPackageUnit getPackageUnit()
  {
    return packageUnit;
  }

  public void setPackageUnit(InternalCDOPackageUnit packageUnit)
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

  public InternalCDOPackageAdapter getPackageAdapter(boolean loadOnDemand)
  {
    if (packageAdapter == null && loadOnDemand)
    {
    }

    return packageAdapter;
  }

  public void write(CDODataOutput out) throws IOException
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Writing {0}", this);
    }

    out.writeString(packageURI);
    out.writeString(parentURI);
    out.writeCDOIDMetaRange(metaIDRange);
  }

  public void read(CDODataInput in) throws IOException
  {
    packageURI = in.readEPackageURI();
    parentURI = in.readEPackageURI();
    metaIDRange = in.readCDOIDMetaRange();
    if (TRACER.isEnabled())
    {
      TRACER.format("Read {0}", this);
    }
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("CDOPackageInfo[packageURI={0}, parentURI={1}, metaIDRange={2}]", packageURI,
        parentURI, metaIDRange);
  }

}
