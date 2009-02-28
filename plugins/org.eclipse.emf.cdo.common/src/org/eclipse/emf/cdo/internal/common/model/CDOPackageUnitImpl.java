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
import org.eclipse.emf.cdo.common.model.EMFUtil;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageInfo;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnitManager;

import org.eclipse.net4j.util.ObjectUtil;
import org.eclipse.net4j.util.om.OMPlatform;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EPackage;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

  private Map<String, InternalCDOPackageInfo> packageInfos = new HashMap<String, InternalCDOPackageInfo>();

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

  public InternalCDOPackageInfo getPackageInfo(String packageURI)
  {
    return packageInfos.get(packageURI);
  }

  public InternalCDOPackageInfo[] getPackageInfos()
  {
    return packageInfos.values().toArray(new InternalCDOPackageInfo[packageInfos.size()]);
  }

  public void addPackageInfo(InternalCDOPackageInfo packageInfo)
  {
    packageInfos.put(packageInfo.getPackageURI(), packageInfo);
  }

  public synchronized boolean isLoaded()
  {
    return loaded;
  }

  public synchronized void load()
  {
    if (loaded)
    {
      return;
    }

    EPackage[] ePackages = manager.loadPackageUnit(this);
    for (EPackage ePackage : ePackages)
    {
      String packageURI = ePackage.getNsURI();
      InternalCDOPackageInfo packageInfo = getPackageInfo(packageURI);
      packageInfo.setEPackage(ePackage);
    }

    loaded = true;
  }

  public void write(CDODataOutput out) throws IOException
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Writing {0}", this);
    }

    out.writeString(id);
    out.writeLong(timeStamp);

    InternalCDOPackageInfo[] packageInfos = getPackageInfos();
    out.writeInt(packageInfos.length);
    for (InternalCDOPackageInfo packageInfo : packageInfos)
    {
      out.writeCDOPackageInfo(packageInfo);
    }
  }

  public void read(CDODataInput in) throws IOException
  {
    id = in.readString();
    timeStamp = in.readLong();

    int size = in.readInt();
    for (int i = 0; i < size; i++)
    {
      InternalCDOPackageInfo packageInfo = (InternalCDOPackageInfo)in.readCDOPackageInfo();
      packageInfo.setPackageUnit(this);
      addPackageInfo(packageInfo);
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

  public void initNew(EPackage ePackage)
  {
    InternalCDOPackageInfo packageInfo = initNewPackage(ePackage);
    addPackageInfo(packageInfo);

    for (EPackage subPackage : ePackage.getESubpackages())
    {
      initNew(subPackage);
    }
  }

  protected InternalCDOPackageInfo initNewPackage(EPackage ePackage)
  {
    CDOPackageInfoImpl packageInfo = new CDOPackageInfoImpl();
    packageInfo.setPackageUnit(this);
    packageInfo.setPackageURI(ePackage.getNsURI());

    EPackage parentPackage = ePackage.getESuperPackage();
    packageInfo.setParentURI(parentPackage == null ? null : parentPackage.getNsURI());

    // InternalCDOPackageRegistry packageRegistry = manager.getPackageRegistry();
    // CDOPackageAdapterImpl packageAdapter = new CDOPackageAdapterImpl();
    // packageAdapter.setPackageRegistry(packageRegistry);
    // packageAdapter.setPackageInfo(packageInfo);
    //
    // ePackage.eAdapters().add(packageAdapter);
    // packageRegistry.putEPackageBasic(ePackage);
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

    public boolean isDynamic()
    {
      return true;
    }

    public boolean isLegacy()
    {
      return true;
    }

    @Override
    public void initNew(EPackage topLevelPackage)
    {
      setID(topLevelPackage.getNsURI());
      super.initNew(topLevelPackage);
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

    @Override
    public void initNew(EPackage topLevelPackage)
    {
      if (OMPlatform.INSTANCE.isOSGiRunning())
      {
        initBundle(topLevelPackage);
      }
      else
      {
        initStandalone(topLevelPackage);
      }

      super.initNew(topLevelPackage);
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
