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

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDMetaRange;
import org.eclipse.emf.cdo.common.id.CDOIDTemp;
import org.eclipse.emf.cdo.common.id.CDOIDTempMeta;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.model.CDOModelUtil;
import org.eclipse.emf.cdo.common.model.CDOPackageInfo;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.model.EMFUtil;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageAdapter;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageDescriptor;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageInfo;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageRegistry;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnitManager;

import org.eclipse.net4j.util.ReflectUtil.ExcludeFromDump;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public class CDOPackageRegistryImpl extends EPackageRegistryImpl implements InternalCDOPackageRegistry
{
  private static final long serialVersionUID = 1L;

  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, CDOPackageRegistryImpl.class);

  private InternalCDOPackageUnitManager packageUnitManager;

  @ExcludeFromDump
  private transient Map<CDOID, InternalEObject> idToMetaInstanceMap = new HashMap<CDOID, InternalEObject>();

  @ExcludeFromDump
  private transient Map<InternalEObject, CDOID> metaInstanceToIDMap = new HashMap<InternalEObject, CDOID>();

  @ExcludeFromDump
  private transient int lastTempMetaID;

  private transient Object lastTempMetaIDLock = new Object();

  public CDOPackageRegistryImpl()
  {
  }

  public CDOPackageRegistryImpl(EPackage.Registry delegateRegistry)
  {
    super(delegateRegistry);
  }

  public InternalCDOPackageUnitManager getPackageUnitManager()
  {
    return packageUnitManager;
  }

  public void setPackageUnitManager(InternalCDOPackageUnitManager packageUnitManager)
  {
    this.packageUnitManager = packageUnitManager;
  }

  public void putEPackage(EPackage ePackage)
  {
    put(ePackage.getNsURI(), ePackage);
  }

  @Override
  public Object put(String nsURI, Object value)
  {
    if (value instanceof EPackage)
    {
      EPackage ePackage = (EPackage)value;
      InternalCDOPackageAdapter adapter = getPackageAdapter(ePackage);
      if (adapter != null)
      {
        return ePackage;
      }

      InternalCDOPackageUnit packageUnit = createPackageUnit(ePackage);
      addPackageUnit(packageUnit);
      return null;
    }

    if (value instanceof InternalCDOPackageUnit)
    {
      InternalCDOPackageUnit packageUnit = (InternalCDOPackageUnit)value;
      addPackageUnit(packageUnit);
    }

    return super.put(nsURI, value);
  }

  protected void addPackageUnit(InternalCDOPackageUnit packageUnit)
  {
    for (InternalCDOPackageInfo packageInfo : packageUnit.getPackageInfos())
    {
      EPackage ePackage = packageInfo.getEPackage(false);
      if (ePackage != null)
      {
        InternalCDOPackageAdapter adapter = createPackageAdapter();
        adapter.setPackageRegistry(this);
        adapter.setPackageInfo(packageInfo);

        ePackage.eAdapters().add(adapter);
        registerPackage(ePackage);
      }
      else
      {
        InternalCDOPackageDescriptor packageDescriptor = createPackageDescriptor();
        packageDescriptor.setPackageRegistry(this);
        packageDescriptor.setPackageInfo(packageInfo);
        registerPackageDescriptor(packageDescriptor);
      }
    }

    packageUnitManager.addPackageUnit(packageUnit);
  }

  protected void registerPackage(EPackage ePackage)
  {
    String packageURI = ePackage.getNsURI();
    if (TRACER.isEnabled())
    {
      TRACER.format("Registering package: {0}", packageURI);
    }
  
    super.put(packageURI, ePackage);
  }

  protected void registerPackageDescriptor(InternalCDOPackageDescriptor packageDescriptor)
  {
    String packageURI = packageDescriptor.getPackageInfo().getPackageURI();
    if (TRACER.isEnabled())
    {
      TRACER.format("Registering package descriptor: {0}", packageURI);
    }
  
    super.put(packageURI, packageDescriptor);
  }

  protected InternalCDOPackageAdapter getPackageAdapter(EPackage ePackage)
  {
    return (InternalCDOPackageAdapter)CDOModelUtil.getPackageAdapter(ePackage, this);
  }

  protected InternalCDOPackageAdapter createPackageAdapter()
  {
    return new CDOPackageAdapterImpl();
  }

  protected InternalCDOPackageDescriptor createPackageDescriptor()
  {
    return new CDOPackageDescriptorImpl();
  }

  protected InternalCDOPackageUnit createPackageUnit(EPackage topLevelPackage)
  {
    InternalCDOPackageUnit packageUnit;
    if (EMFUtil.isDynamicEPackage(topLevelPackage))
    {
      packageUnit = createDynamicPackageUnit();
    }
    else
    {
      packageUnit = createGeneratedPackageUnit();
    }

    packageUnit.initNew(topLevelPackage);
    return packageUnit;
  }

  protected InternalCDOPackageUnit createDynamicPackageUnit()
  {
    return new CDOPackageUnitImpl.Dynamic();
  }

  protected InternalCDOPackageUnit createGeneratedPackageUnit()
  {
    return new CDOPackageUnitImpl.Generated();
  }

  public CDOIDMetaRange getTempMetaIDRange(int count)
  {
    CDOIDTemp lowerBound;
    synchronized (lastTempMetaIDLock)
    {
      lowerBound = CDOIDUtil.createTempMeta(lastTempMetaID + 1);
      lastTempMetaID += count;
    }

    return CDOIDUtil.createMetaRange(lowerBound, count);
  }

  public InternalEObject lookupMetaInstance(CDOID id)
  {
    InternalEObject metaInstance = idToMetaInstanceMap.get(id);
    if (metaInstance == null)
    {
      for (CDOPackageUnit packageUnit : packageUnitManager.getPackageUnits())
      {
        for (CDOPackageInfo packageInfo : packageUnit.getPackageInfos())
        {
          CDOIDMetaRange metaIDRange = packageInfo.getMetaIDRange();
          if (metaIDRange != null && metaIDRange.contains(id))
          {
            EPackage ePackage = getEPackage(packageInfo.getPackageURI());
            mapMetaInstances(ePackage);
            metaInstance = idToMetaInstanceMap.get(id);
            break;
          }
        }
      }
    }

    return metaInstance;
  }

  public CDOID lookupMetaInstanceID(InternalEObject metaInstance)
  {
    return metaInstanceToIDMap.get(metaInstance);
  }

  public void mapMetaInstances(EPackage ePackage, CDOIDMetaRange metaIDRange)
  {
    if (metaIDRange.isTemporary())
    {
      throw new IllegalArgumentException("metaIDRange.isTemporary()");
    }

    CDOIDMetaRange range = CDOIDUtil.createMetaRange(metaIDRange.getLowerBound(), 0);
    range = mapMetaInstance((InternalEObject)ePackage, range, idToMetaInstanceMap, metaInstanceToIDMap);
    if (range.size() != metaIDRange.size())
    {
      throw new IllegalStateException("range.size() != metaIDRange.size()");
    }
  }

  public CDOIDMetaRange mapMetaInstances(EPackage ePackage)
  {
    synchronized (lastTempMetaIDLock)
    {
      CDOIDMetaRange range = mapMetaInstances(ePackage, lastTempMetaID + 1, idToMetaInstanceMap, metaInstanceToIDMap);
      lastTempMetaID = ((CDOIDTempMeta)range.getUpperBound()).getIntValue();
      return range;
    }
  }

  public CDOIDMetaRange mapMetaInstances(EPackage ePackage, int firstMetaID,
      Map<CDOID, InternalEObject> idToMetaInstances, Map<InternalEObject, CDOID> metaInstanceToIDs)
  {
    CDOIDTemp lowerBound = CDOIDUtil.createTempMeta(firstMetaID);
    CDOIDMetaRange range = CDOIDUtil.createMetaRange(lowerBound, 0);
    range = mapMetaInstance((InternalEObject)ePackage, range, idToMetaInstances, metaInstanceToIDs);
    return range;
  }

  public CDOIDMetaRange mapMetaInstance(InternalEObject metaInstance, CDOIDMetaRange range,
      Map<CDOID, InternalEObject> idToMetaInstances, Map<InternalEObject, CDOID> metaInstanceToIDs)
  {
    range = range.increase();
    CDOID id = range.getUpperBound();
    if (TRACER.isEnabled())
    {
      TRACER.format("Registering meta instance: {0} <-> {1}", id, metaInstance);
    }

    if (idToMetaInstances != null)
    {
      if (idToMetaInstances.put(id, metaInstance) != null)
      {
        throw new IllegalStateException("Duplicate meta ID: " + id + " --> " + metaInstance);
      }
    }

    if (metaInstanceToIDs != null)
    {
      if (metaInstanceToIDs.put(metaInstance, id) != null)
      {
        throw new IllegalStateException("Duplicate metaInstance: " + metaInstance + " --> " + id);
      }
    }

    for (EObject content : metaInstance.eContents())
    {
      range = mapMetaInstance((InternalEObject)content, range, idToMetaInstances, metaInstanceToIDs);
    }

    return range;
  }

  public void remapMetaInstance(CDOID oldID, CDOID newID)
  {
    InternalEObject metaInstance = idToMetaInstanceMap.remove(oldID);
    if (metaInstance == null)
    {
      throw new IllegalArgumentException("Unknown meta instance id: " + oldID);
    }

    if (TRACER.isEnabled())
    {
      TRACER.format("Remapping meta instance: {0} --> {1} <-> {2}", oldID, newID, metaInstance);
    }

    idToMetaInstanceMap.put(newID, metaInstance);
    metaInstanceToIDMap.put(metaInstance, newID);
  }
}
