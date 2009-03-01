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

import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageInfo;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageRegistry;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;

import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EPackage;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Eike Stepper
 */
public class CDOPackageUnitImpl implements InternalCDOPackageUnit
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, CDOPackageUnitImpl.class);

  private InternalCDOPackageRegistry packageRegistry;

  private long timeStamp;

  private boolean dynamic;

  private boolean legacy;

  private InternalCDOPackageInfo[] packageInfos;

  private transient boolean loaded;

  public CDOPackageUnitImpl()
  {
  }

  public CDOPackageUnitImpl(EPackage topLevelPackage)
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

  public String getID()
  {
    return packageInfos == null ? "" : packageInfos[0].getPackageURI();
  }

  public long getTimeStamp()
  {
    return timeStamp;
  }

  public void setTimeStamp(long timeStamp)
  {
    this.timeStamp = timeStamp;
  }

  public boolean isDynamic()
  {
    return dynamic;
  }

  public void setDynamic(boolean dynamic)
  {
    this.dynamic = dynamic;
  }

  public boolean isLegacy()
  {
    return legacy;
  }

  public void setLegacy(boolean legacy)
  {
    this.legacy = legacy;
  }

  public InternalCDOPackageInfo getPackageInfo(String packageURI)
  {
    for (InternalCDOPackageInfo packageInfo : packageInfos)
    {
      if (packageInfo.getPackageURI().equals(packageURI))
      {
        return packageInfo;
      }
    }

    return null;
  }

  public InternalCDOPackageInfo[] getPackageInfos()
  {
    return packageInfos;
  }

  public void setPackageInfos(InternalCDOPackageInfo[] packageInfos)
  {
    this.packageInfos = packageInfos;
  }

  public synchronized boolean isLoaded()
  {
    return loaded;
  }

  public synchronized void load()
  {
    if (!loaded)
    {
      EPackage[] ePackages = packageRegistry.getPackageLoader().loadPackages(this);
      for (EPackage ePackage : ePackages)
      {
        String packageURI = ePackage.getNsURI();
        InternalCDOPackageInfo packageInfo = getPackageInfo(packageURI);
        ePackage.eAdapters().add(packageInfo);
      }

      loaded = true;
    }
  }

  public void write(CDODataOutput out) throws IOException
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Writing {0}", this);
    }

    out.writeLong(timeStamp);
    out.writeBoolean(dynamic);
    out.writeBoolean(legacy);
    out.writeInt(packageInfos.length);
    for (InternalCDOPackageInfo packageInfo : packageInfos)
    {
      out.writeCDOPackageInfo(packageInfo);
    }
  }

  public void read(CDODataInput in) throws IOException
  {
    timeStamp = in.readLong();
    dynamic = in.readBoolean();
    legacy = in.readBoolean();
    packageInfos = new InternalCDOPackageInfo[in.readInt()];
    for (int i = 0; i < packageInfos.length; i++)
    {
      packageInfos[i] = (InternalCDOPackageInfo)in.readCDOPackageInfo();
      packageInfos[i].setPackageUnit(this);
    }

    if (TRACER.isEnabled())
    {
      TRACER.format("Read {0}", this);
    }
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("CDOPackageUnit[id={0}, timeStamp={1,date} {1,time}, dynamic={2}, legacy={3}]",
        getID(), timeStamp, isDynamic(), isLegacy());
  }
}
