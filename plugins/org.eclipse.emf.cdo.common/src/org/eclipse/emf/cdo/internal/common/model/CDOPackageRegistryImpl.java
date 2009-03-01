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
import org.eclipse.emf.cdo.common.model.CDOPackageLoader;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.model.CDOMetaInstanceMapper;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageInfo;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageRegistry;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;

import org.eclipse.net4j.util.ReflectUtil.ExcludeFromDump;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Eike Stepper
 */
public class CDOPackageRegistryImpl extends EPackageRegistryImpl implements InternalCDOPackageRegistry
{
  private static final long serialVersionUID = 1L;

  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, CDOPackageRegistryImpl.class);

  private CDOPackageLoader packageLoader;

  private MetaInstanceMapper metaInstanceMapper = new MetaInstanceMapper();

  @ExcludeFromDump
  private transient InternalCDOPackageUnit[] packageunits;

  public CDOPackageRegistryImpl()
  {
  }

  public CDOPackageLoader getPackageLoader()
  {
    return packageLoader;
  }

  public void setPackageLoader(CDOPackageLoader packageLoader)
  {
    this.packageLoader = packageLoader;
  }

  public Object basicPut(String nsURI, Object value)
  {
    return super.put(nsURI, value);
  }

  @Override
  public synchronized Object put(String nsURI, Object value)
  {
    if (value instanceof EPackage)
    {
      EPackage ePackage = (EPackage)value;
      InternalCDOPackageInfo packageInfo = getPackageInfo(ePackage);
      if (packageInfo == null)
      {
        initPackageUnit(ePackage);
      }

      return null;
    }

    return basicPut(nsURI, value);
  }

  public synchronized Object putEPackage(EPackage ePackage)
  {
    return put(ePackage.getNsURI(), ePackage);
  }

  public synchronized void putPackageUnit(InternalCDOPackageUnit packageUnit)
  {
    packageunits = null;
    packageUnit.setPackageRegistry(this);
    for (InternalCDOPackageInfo packageInfo : packageUnit.getPackageInfos())
    {
      EPackage ePackage = packageInfo.getEPackage(false);
      if (ePackage != null)
      {
        ePackage.eAdapters().add(packageInfo);
        basicPut(ePackage.getNsURI(), ePackage);
      }
      else
      {
        basicPut(packageInfo.getPackageURI(), packageInfo);
      }
    }
  }

  public synchronized InternalCDOPackageInfo getPackageInfo(Object value)
  {
    if (value instanceof CDOPackageInfo)
    {
      return (InternalCDOPackageInfo)value;
    }

    if (value instanceof EPackage)
    {
      return (InternalCDOPackageInfo)CDOModelUtil.getPackageInfo((EPackage)value, this);
    }

    return null;
  }

  public synchronized InternalCDOPackageUnit[] getPackageUnits()
  {
    if (packageunits == null)
    {
      Set<InternalCDOPackageUnit> result = new HashSet<InternalCDOPackageUnit>();
      for (Object value : values())
      {
        InternalCDOPackageInfo packageInfo = getPackageInfo(value);
        if (packageInfo != null)
        {
          result.add(packageInfo.getPackageUnit());
        }
      }

      packageunits = result.toArray(new InternalCDOPackageUnit[result.size()]);
    }

    return packageunits;
  }

  public InternalEObject lookupMetaInstance(CDOID id)
  {
    return metaInstanceMapper.lookupMetaInstance(id);
  }

  public CDOID lookupMetaInstanceID(InternalEObject metaInstance)
  {
    return metaInstanceMapper.lookupMetaInstanceID(metaInstance);
  }

  public void remapMetaInstance(CDOID oldId, CDOID newId)
  {
    metaInstanceMapper.remapMetaInstance(oldId, newId);
  }

  protected void initPackageUnit(EPackage ePackage)
  {
    InternalCDOPackageUnit packageUnit = createPackageUnit();
    packageUnit.setPackageRegistry(this);
    packageUnit.init(ePackage);
    packageunits = null;
  }

  protected InternalCDOPackageUnit createPackageUnit()
  {
    return new CDOPackageUnitImpl();
  }

  /**
   * @author Eike Stepper
   */
  public class MetaInstanceMapper implements CDOMetaInstanceMapper
  {
    private Map<CDOID, InternalEObject> idToMetaInstanceMap = new HashMap<CDOID, InternalEObject>();

    private Map<InternalEObject, CDOID> metaInstanceToIDMap = new HashMap<InternalEObject, CDOID>();

    @ExcludeFromDump
    private transient int lastTempMetaID;

    public MetaInstanceMapper()
    {
    }

    public synchronized InternalEObject lookupMetaInstance(CDOID id)
    {
      InternalEObject metaInstance = idToMetaInstanceMap.get(id);
      if (metaInstance == null)
      {
        for (Object value : values())
        {
          CDOPackageInfo packageInfo = getPackageInfo(value);
          if (packageInfo != null)
          {
            CDOIDMetaRange metaIDRange = packageInfo.getMetaIDRange();
            if (metaIDRange != null && metaIDRange.contains(id))
            {
              EPackage ePackage = packageInfo.getEPackage(true);
              mapMetaInstances(ePackage);
              metaInstance = idToMetaInstanceMap.get(id);
              break;
            }
          }
        }
      }

      return metaInstance;
    }

    public synchronized CDOID lookupMetaInstanceID(InternalEObject metaInstance)
    {
      return metaInstanceToIDMap.get(metaInstance);
    }

    public synchronized void remapMetaInstance(CDOID oldID, CDOID newID)
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
      CDOIDMetaRange range = mapMetaInstances(ePackage, lastTempMetaID + 1, idToMetaInstanceMap, metaInstanceToIDMap);
      lastTempMetaID = ((CDOIDTempMeta)range.getUpperBound()).getIntValue();
      return range;
    }

    /**
     * TODO Remove map params?
     */
    public CDOIDMetaRange mapMetaInstances(EPackage ePackage, int firstMetaID,
        Map<CDOID, InternalEObject> idToMetaInstances, Map<InternalEObject, CDOID> metaInstanceToIDs)
    {
      CDOIDTemp lowerBound = CDOIDUtil.createTempMeta(firstMetaID);
      CDOIDMetaRange range = CDOIDUtil.createMetaRange(lowerBound, 0);
      range = mapMetaInstance((InternalEObject)ePackage, range, idToMetaInstances, metaInstanceToIDs);
      return range;
    }

    /**
     * TODO Remove map params?
     */
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

    public CDOIDMetaRange getTempMetaIDRange(int count)
    {
      CDOIDTemp lowerBound = CDOIDUtil.createTempMeta(lastTempMetaID + 1);
      lastTempMetaID += count;
      return CDOIDUtil.createMetaRange(lowerBound, count);
    }
  }
}
