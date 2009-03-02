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
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

  public static final String ROOT_CLASS_NAME = "EObject";

  private static CDOType[] coreTypes;

  static
  {
    List<CDOType> types = new ArrayList<CDOType>();
    registerCoreType(types, EcorePackage.eINSTANCE.getEBigDecimal(), null);
    registerCoreType(types, EcorePackage.eINSTANCE.getEBigInteger(), null);
    registerCoreType(types, EcorePackage.eINSTANCE.getEBooleanObject(), CDOType.BOOLEAN_OBJECT);
    registerCoreType(types, EcorePackage.eINSTANCE.getEBoolean(), CDOType.BOOLEAN);
    registerCoreType(types, EcorePackage.eINSTANCE.getEByteArray(), CDOType.BYTE_ARRAY);
    registerCoreType(types, EcorePackage.eINSTANCE.getEByteObject(), CDOType.BYTE_OBJECT);
    registerCoreType(types, EcorePackage.eINSTANCE.getEByte(), CDOType.BYTE);
    registerCoreType(types, EcorePackage.eINSTANCE.getECharacterObject(), CDOType.CHARACTER_OBJECT);
    registerCoreType(types, EcorePackage.eINSTANCE.getEChar(), CDOType.CHAR);
    registerCoreType(types, EcorePackage.eINSTANCE.getEDate(), CDOType.DATE);
    registerCoreType(types, EcorePackage.eINSTANCE.getEDoubleObject(), CDOType.DOUBLE_OBJECT);
    registerCoreType(types, EcorePackage.eINSTANCE.getEDouble(), CDOType.DOUBLE);
    registerCoreType(types, EcorePackage.eINSTANCE.getEFloatObject(), CDOType.FLOAT_OBJECT);
    registerCoreType(types, EcorePackage.eINSTANCE.getEFloat(), CDOType.FLOAT);
    registerCoreType(types, EcorePackage.eINSTANCE.getEIntegerObject(), CDOType.INTEGER_OBJECT);
    registerCoreType(types, EcorePackage.eINSTANCE.getEInt(), CDOType.INT);
    registerCoreType(types, EcorePackage.eINSTANCE.getEJavaClass(), null);
    registerCoreType(types, EcorePackage.eINSTANCE.getEJavaObject(), null);
    registerCoreType(types, EcorePackage.eINSTANCE.getELongObject(), CDOType.LONG_OBJECT);
    registerCoreType(types, EcorePackage.eINSTANCE.getELong(), CDOType.LONG);
    registerCoreType(types, EcorePackage.eINSTANCE.getEShortObject(), CDOType.SHORT_OBJECT);
    registerCoreType(types, EcorePackage.eINSTANCE.getEShort(), CDOType.SHORT);
    registerCoreType(types, EcorePackage.eINSTANCE.getEString(), CDOType.STRING);
    coreTypes = types.toArray(new CDOType[types.size()]);
  }

  private static void registerCoreType(List<CDOType> types, EDataType eDataType, CDOType type)
  {
    int index = eDataType.getClassifierID();
    while (index >= types.size())
    {
      types.add(null);
    }

    types.set(index, type);
  }

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

  public static boolean isRoot(EClass eClass)
  {
    return isCorePackage(eClass.getEPackage()) && ROOT_CLASS_NAME.equals(eClass.getName());
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

  public static CDOType getType(EClassifier eClassifier)
  {
    if (eClassifier instanceof EClass)
    {
      return CDOType.OBJECT;
    }

    if (eClassifier instanceof EEnum)
    {
      return CDOType.ENUM;
    }

    EDataType eDataType = (EDataType)eClassifier;
    if (isCorePackage(eDataType.getEPackage()))
    {
      return getCoreType(eDataType);
    }

    return CDOType.CUSTOM;
  }

  public static CDOType getCoreType(EDataType eDataType)
  {
    int index = eDataType.getClassifierID();
    return coreTypes[index];
  }

  public static CDOType getPrimitiveType(Class<? extends Object> primitiveType)
  {
    if (primitiveType == String.class)
    {
      return CDOType.STRING;
    }

    if (primitiveType == Boolean.class)
    {
      return CDOType.BOOLEAN;
    }

    if (primitiveType == Integer.class)
    {
      return CDOType.INT;
    }

    if (primitiveType == Double.class)
    {
      return CDOType.DOUBLE;
    }

    if (primitiveType == Float.class)
    {
      return CDOType.FLOAT;
    }

    if (primitiveType == Long.class)
    {
      return CDOType.LONG;
    }

    if (primitiveType == Date.class)
    {
      return CDOType.DATE;
    }

    if (primitiveType == Byte.class)
    {
      return CDOType.BYTE;
    }

    if (primitiveType == Character.class)
    {
      return CDOType.CHAR;
    }

    throw new IllegalArgumentException("Not a primitive type nor String nor Date: " + primitiveType);
  }

  public static CDOPackageInfo getPackageInfo(EPackage ePackage, CDOPackageRegistry packageRegistry)
  {
    EList<Adapter> adapters = ePackage.eAdapters();
    for (int i = 0, size = adapters.size(); i < size; ++i)
    {
      Adapter adapter = adapters.get(i);
      if (adapter instanceof CDOPackageInfo)
      {
        CDOPackageInfo packageInfo = (CDOPackageInfo)adapter;
        if (packageInfo.getPackageUnit().getPackageRegistry() == packageRegistry)
        {
          return packageInfo;
        }
      }
    }

    return null;
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
