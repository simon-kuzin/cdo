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
import org.eclipse.emf.cdo.common.model.EMFUtil;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageInfo;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageRegistry;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnitManager;

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

  private InternalCDOPackageUnitManager manager;

  private String id;

  private long timeStamp;

  private InternalCDOPackageInfo[] packageInfos;

  private boolean loaded;

  public CDOPackageUnitImpl()
  {
  }

  public InternalCDOPackageUnitManager getManager()
  {
    return manager;
  }

  public void setManager(InternalCDOPackageUnitManager manager)
  {
    this.manager = manager;
  }

  public String getID()
  {
    return id;
  }

  public void setID(String id)
  {
    this.id = id;
  }

  public long getTimeStamp()
  {
    return timeStamp;
  }

  public void setTimeStamp(long timeStamp)
  {
    this.timeStamp = timeStamp;
  }

  public InternalCDOPackageInfo[] getPackageInfos()
  {
    return packageInfos;
  }

  public void setPackageInfos(InternalCDOPackageInfo[] packageInfos)
  {
    this.packageInfos = packageInfos;
  }

  public boolean isLoaded()
  {
    return loaded;
  }

  public void load()
  {
    // TODO: implement CDOPackageUnitImpl.load()
    throw new UnsupportedOperationException();
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

    int size = in.readInt();
    packageInfos = new InternalCDOPackageInfo[size];
    for (int i = 0; i < size; i++)
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
    return MessageFormat.format("CDOPackageUnit[id={0}, timeStamp={1,date} {1,time}, dynamic={2}, legacy={3}]", id,
        timeStamp, isDynamic(), isLegacy());
  }

  protected void initNew(String id, EPackage... topLevelPackages)
  {
    setID(id);
    List<InternalCDOPackageInfo> result = new ArrayList<InternalCDOPackageInfo>();
    for (EPackage topLevelPackage : topLevelPackages)
    {
      initNewPackages(topLevelPackage, result);
    }

    packageInfos = result.toArray(new InternalCDOPackageInfo[result.size()]);
  }

  protected void initNewPackages(EPackage ePackage, List<InternalCDOPackageInfo> result)
  {
    InternalCDOPackageInfo packageInfo = initNewPackage(ePackage);
    result.add(packageInfo);

    for (EPackage subPackage : ePackage.getESubpackages())
    {
      initNewPackages(subPackage, result);
    }
  }

  protected InternalCDOPackageInfo initNewPackage(EPackage ePackage)
  {
    InternalCDOPackageRegistry packageRegistry = manager.getPackageRegistry();
    CDOPackageInfoImpl packageInfo = new CDOPackageInfoImpl();
    packageInfo.setPackageUnit(this);
    packageInfo.setPackageURI(ePackage.getNsURI());

    EPackage parentPackage = ePackage.getESuperPackage();
    packageInfo.setParentURI(parentPackage == null ? null : parentPackage.getNsURI());

    CDOPackageAdapterImpl packageAdapter = new CDOPackageAdapterImpl();
    packageAdapter.setPackageRegistry(packageRegistry);
    packageAdapter.setPackageInfo(packageInfo);

    ePackage.eAdapters().add(packageAdapter);
    packageRegistry.putEPackageBasic(ePackage);
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

    public Dynamic(EPackage topLevelPackage)
    {
      initNew(topLevelPackage.getNsURI(), topLevelPackage);
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

    public Generated(EPackage topLevelPackage)
    {
      if (OMPlatform.INSTANCE.isOSGiRunning())
      {
        initBundle(topLevelPackage);
      }
      else
      {
        initStandalone(topLevelPackage);
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

    protected void initBundle(EPackage topLevelPackage)
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

      initNew(contributorName, topLevelPackages.toArray(new EPackage[topLevelPackages.size()]));
    }

    protected void initStandalone(EPackage topLevelPackage)
    {
      initNew(topLevelPackage.getNsURI(), topLevelPackage);
    }
  }
}
