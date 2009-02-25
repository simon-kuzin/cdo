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
import org.eclipse.emf.cdo.common.model.CDOPackageInfo;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.common.model.CDOPackageUnitManager;
import org.eclipse.emf.cdo.common.model.EMFUtil;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageInfo;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageRegistry;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;

import org.eclipse.net4j.util.ObjectUtil;
import org.eclipse.net4j.util.om.OMPlatform;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EPackage;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Eike Stepper
 */
public abstract class CDOPackageUnitImpl implements InternalCDOPackageUnit
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, CDOPackageUnitImpl.class);

  private CDOPackageUnitManager packageUnitManager;

  private String id;

  private State state;

  private long timeStamp;

  private CDOPackageInfo[] packageInfos;

  public CDOPackageUnitImpl()
  {
  }

  public CDOPackageUnitManager getPackageUnitManager()
  {
    return packageUnitManager;
  }

  public void setPackageUnitManager(CDOPackageUnitManager packageUnitManager)
  {
    this.packageUnitManager = packageUnitManager;
  }

  public String getID()
  {
    return id;
  }

  public void setID(String id)
  {
    this.id = id;
  }

  public State getState()
  {
    return state;
  }

  public void setState(State state)
  {
    this.state = state;
  }

  public long getTimeStamp()
  {
    return timeStamp;
  }

  public void setTimeStamp(long timeStamp)
  {
    this.timeStamp = timeStamp;
  }

  public CDOPackageInfo[] getPackageInfos()
  {
    return packageInfos;
  }

  public void setPackageInfos(CDOPackageInfo[] packageInfos)
  {
    this.packageInfos = packageInfos;
  }

  public void write(CDODataOutput out) throws IOException
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Writing {0}", this);
    }

    out.writeString(id);
    out.writeLong(timeStamp);

    if (packageInfos == null)
    {
      out.writeInt(0);
    }
    else
    {
      out.writeInt(packageInfos.length);
      for (CDOPackageInfo packageInfo : packageInfos)
      {
        out.writeCDOPackageInfo(packageInfo);
      }
    }
  }

  public void read(CDODataInput in) throws IOException
  {
    id = in.readString();
    timeStamp = in.readLong();
    if (TRACER.isEnabled())
    {
      TRACER.format("Read {0}", this);
    }

    int size = in.readInt();
    packageInfos = new InternalCDOPackageInfo[size];
    for (int i = 0; i < size; i++)
    {
      packageInfos[i] = in.readCDOPackageInfo();
      ((InternalCDOPackageInfo)packageInfos[i]).setPackageUnit(this);
    }
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("CDOPackageUnit[id={0}, timeStamp={1,date} {1,time}, dynamic={2}, legacy={3}]", id,
        timeStamp, isDynamic(), isLegacy());
  }

  protected void initNew(String id, CDOPackageRegistry packageRegistry, EPackage... topLevelPackages)
  {
    setID(id);
    setState(State.NEW);

    List<CDOPackageInfo> result = new ArrayList<CDOPackageInfo>();
    for (EPackage topLevelPackage : topLevelPackages)
    {
      initNewPackages(packageRegistry, topLevelPackage, result);
    }

    packageInfos = result.toArray(new CDOPackageInfo[result.size()]);
  }

  protected void initNewPackages(CDOPackageRegistry packageRegistry, EPackage ePackage, List<CDOPackageInfo> result)
  {
    CDOPackageInfo packageInfo = initNewPackage(packageRegistry, ePackage);
    result.add(packageInfo);

    for (EPackage subPackage : ePackage.getESubpackages())
    {
      initNewPackages(packageRegistry, subPackage, result);
    }
  }

  protected CDOPackageInfo initNewPackage(CDOPackageRegistry packageRegistry, EPackage ePackage)
  {
    CDOPackageInfoImpl packageInfo = new CDOPackageInfoImpl();
    packageInfo.setPackageUnit(this);
    packageInfo.setPackageURI(ePackage.getNsURI());

    EPackage parentPackage = ePackage.getESuperPackage();
    packageInfo.setParentURI(parentPackage == null ? null : parentPackage.getNsURI());

    CDOPackageAdapterImpl packageAdapter = new CDOPackageAdapterImpl();
    packageAdapter.setPackageRegistry(packageRegistry);
    packageAdapter.setPackageInfo(packageInfo);

    ePackage.eAdapters().add(packageAdapter);
    ((InternalCDOPackageRegistry)packageRegistry).putEPackageBasic(ePackage);
    return packageInfo;
  }

  public static byte[] serializeBundle(String bundleID)
  {
    throw new UnsupportedOperationException();
  }

  public static byte[] serializePackage(EPackage topLevelPackage)
  {
    // TODO: implement CDOPackageUnitImpl.serializePackage(topLevelPackage)
    throw new UnsupportedOperationException();
  }

  /**
   * @author Eike Stepper
   */
  public static class Dynamic extends CDOPackageUnitImpl
  {
    public Dynamic()
    {
    }

    public Dynamic(CDOPackageRegistry packageRegistry, EPackage topLevelPackage)
    {
      initNew(topLevelPackage.getNsURI(), packageRegistry, topLevelPackage);
    }

    public boolean isDynamic()
    {
      return true;
    }

    public boolean isLegacy()
    {
      return true;
    }
  }

  /**
   * @author Eike Stepper
   */
  public static class Generated extends CDOPackageUnitImpl
  {
    private boolean legacy;

    public Generated()
    {
    }

    public Generated(CDOPackageRegistry packageRegistry, EPackage topLevelPackage)
    {
      if (OMPlatform.INSTANCE.isOSGiRunning())
      {
        initBundle(packageRegistry, topLevelPackage);
      }
      else
      {
        initStandalone(packageRegistry, topLevelPackage);
      }
    }

    public boolean isDynamic()
    {
      return false;
    }

    public boolean isLegacy()
    {
      return legacy;
    }

    public void setLegacy(boolean legacy)
    {
      this.legacy = legacy;
    }

    @Override
    public void read(CDODataInput in) throws IOException
    {
      legacy = in.readBoolean();
      super.read(in);
    }

    @Override
    public void write(CDODataOutput out) throws IOException
    {
      out.writeBoolean(legacy);
      super.write(out);
    }

    protected void initBundle(CDOPackageRegistry packageRegistry, EPackage topLevelPackage)
    {
      String nsURI = topLevelPackage.getNsURI();
      org.eclipse.core.runtime.IConfigurationElement[] elements = org.eclipse.core.runtime.Platform
          .getExtensionRegistry().getConfigurationElementsFor("org.eclipse.emf.ecore.generated_package");

      String contributorName = null;
      for (org.eclipse.core.runtime.IConfigurationElement element : elements)
      {
        String uri = element.getAttribute("uri");
        if (ObjectUtil.equals(uri, nsURI))
        {
          contributorName = element.getContributor().getName();
          break;
        }
      }

      if (contributorName == null)
      {
        throw new IllegalStateException("Package not contributed: " + nsURI);
      }

      Set<EPackage> topLevelPackages = new HashSet<EPackage>();
      for (org.eclipse.core.runtime.IConfigurationElement element : elements)
      {
        if (element.getContributor().getName().equals(contributorName))
        {
          String uri = element.getAttribute("uri");
          EPackage ePackage = EPackage.Registry.INSTANCE.getEPackage(uri);
          topLevelPackage = EMFUtil.getTopLevelPackage(ePackage);
          topLevelPackages.add(topLevelPackage);
        }
      }

      initNew(contributorName, packageRegistry, topLevelPackages.toArray(new EPackage[topLevelPackages.size()]));
    }

    protected void initStandalone(CDOPackageRegistry packageRegistry, EPackage topLevelPackage)
    {
      initNew(topLevelPackage.getNsURI(), packageRegistry, topLevelPackage);
    }
  }
}
