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
package org.eclipse.emf.cdo.common.model;

import org.eclipse.emf.cdo.internal.common.model.CDOClassAdapterImpl;
import org.eclipse.emf.cdo.internal.common.model.CDOTypeImpl;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * @since 2.0
 * @author Eike Stepper
 */
public final class CDOModelUtil
{
  public static final String CORE_PACKAGE_URI = "http://www.eclipse.org/emf/2002/Ecore";

  public static final String RESOURCE_PACKAGE_URI = "http://www.eclipse.org/emf/CDO/resource/2.0.0";

  public static final String RESOURCE_NODE_CLASS_NAME = "CDOResourceNode";

  public static final String RESOURCE_FOLDER_CLASS_NAME = "CDOResourceFolder";

  public static final String RESOURCE_CLASS_NAME = "CDOResource";

  private CDOModelUtil()
  {
  }

  public static boolean isCorePackage(EPackage ePackage)
  {
    return CORE_PACKAGE_URI.equals(ePackage.getNsURI());
  }

  public static boolean isResourcePackage(EPackage ePackage)
  {
    return RESOURCE_PACKAGE_URI.equals(ePackage.getNsURI());
  }

  public static boolean isSystemPackage(EPackage ePackage)
  {
    return isCorePackage(ePackage) || isResourcePackage(ePackage);
  }

  public static boolean isResource(EClass eClass)
  {
    return isResourcePackage(eClass.getEPackage()) && RESOURCE_CLASS_NAME.equals(eClass.getName());
  }

  public static boolean isResourceFolder(EClass eClass)
  {
    return isResourcePackage(eClass.getEPackage()) && RESOURCE_FOLDER_CLASS_NAME.equals(eClass.getName());
  }

  public static boolean isResourceNode(EClass eClass)
  {
    return isResourcePackage(eClass.getEPackage()) && RESOURCE_NODE_CLASS_NAME.equals(eClass.getName());
  }

  public static CDOType getType(int typeID)
  {
    CDOTypeImpl type = CDOTypeImpl.ids.get(typeID);
    if (type == null)
    {
      throw new IllegalStateException("No type for id " + typeID);
    }

    return type;
  }

  public static CDOPackageAdapter getPackageAdapter(EPackage ePackage, CDOPackageRegistry packageRegistry)
  {
    EList<Adapter> adapters = ePackage.eAdapters();
    for (int i = 0, size = adapters.size(); i < size; ++i)
    {
      Adapter adapter = adapters.get(i);
      if (adapter instanceof CDOPackageAdapter)
      {
        CDOPackageAdapter packageAdapter = (CDOPackageAdapter)adapter;
        if (packageAdapter.getPackageRegistry() == packageRegistry)
        {
          return packageAdapter;
        }
      }
    }

    return null;
  }

  public static CDOPackageInfo getPackageInfo(EPackage ePackage, CDOPackageRegistry packageRegistry)
  {
    CDOPackageAdapter adapter = getPackageAdapter(ePackage, packageRegistry);
    return adapter.getPackageInfo();
  }

  public static CDOPackageUnit getPackageUnit(EPackage ePackage, CDOPackageRegistry packageRegistry)
  {
    CDOPackageInfo packageInfo = getPackageInfo(ePackage, packageRegistry);
    return packageInfo.getPackageUnit();
  }

  public static CDOClassAdapter getClassAdapter(EClass eClass)
  {
    EList<Adapter> adapters = eClass.eAdapters();
    CDOClassAdapter adapter = (CDOClassAdapter)EcoreUtil.getAdapter(adapters, CDOClassAdapter.class);
    if (adapter == null)
    {
      adapter = new CDOClassAdapterImpl();
      adapters.add(adapter);
    }

    return adapter;
  }

  public static EStructuralFeature[] getAllPersistentFeatures(EClass eClass)
  {
    CDOClassAdapter adapter = getClassAdapter(eClass);
    return adapter.getAllPersistentFeatures();
  }
}
