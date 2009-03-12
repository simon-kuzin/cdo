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
package org.eclipse.emf.cdo.util;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.model.EMFUtil;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit.Type;

import org.eclipse.net4j.util.ObjectUtil;
import org.eclipse.net4j.util.om.OMPlatform;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.util.EcoreUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public final class CDOPackageTypeRegistry
{
  public static final CDOPackageTypeRegistry INSTANCE = new CDOPackageTypeRegistry();

  private static final String ECORE_ID = "org.eclipse.emf.ecore";

  private static final String PPID = EcorePlugin.GENERATED_PACKAGE_PPID;

  private static final String MARKER_FILE = "META-INF/CDO.MF";

  private Map<String, CDOPackageUnit.Type> types = new HashMap<String, CDOPackageUnit.Type>();

  private Map<String, CDOPackageUnit.Type> bundles = new HashMap<String, CDOPackageUnit.Type>();

  private CDOPackageTypeRegistry()
  {
  }

  public CDOPackageUnit.Type register(EPackage ePackage)
  {
    CDOPackageUnit.Type type = getPackageType(ePackage);
    types.put(ePackage.getNsURI(), type);
    return type;
  }

  public void registerNative(String packageURI)
  {
    types.put(packageURI, CDOPackageUnit.Type.NATIVE);
  }

  public void registerLegacy(String packageURI)
  {
    types.put(packageURI, CDOPackageUnit.Type.LEGACY);
  }

  public void registerDynamic(String packageURI)
  {
    types.put(packageURI, CDOPackageUnit.Type.DYNAMIC);
  }

  public CDOPackageUnit.Type deregister(String packageURI)
  {
    return types.remove(packageURI);
  }

  public CDOPackageUnit.Type lookup(String packageURI)
  {
    CDOPackageUnit.Type type = types.get(packageURI);
    if (type == null)
    {
      Object value = EPackage.Registry.INSTANCE.get(packageURI);
      if (value instanceof EPackage)
      {
        EPackage ePackage = (EPackage)value;
        type = register(ePackage);
      }

      if (type == null && OMPlatform.INSTANCE.isExtensionRegistryAvailable())
      {
        type = getTypeFromBundle(packageURI);
        types.put(packageURI, type);
      }
    }

    return type;
  }

  public void reset()
  {
    types.clear();
    bundles.clear();
  }

  private Type getTypeFromBundle(String packageURI)
  {
    String bundleID = getBundleID(packageURI);
    if (bundleID == null)
    {
      return CDOPackageUnit.Type.UNKNOWN;
    }

    CDOPackageUnit.Type type = bundles.get(bundleID);
    if (type == null)
    {
      org.osgi.framework.Bundle bundle = org.eclipse.core.runtime.Platform.getBundle(bundleID);
      if (bundle == null)
      {
        type = CDOPackageUnit.Type.UNKNOWN;
      }
      else if (bundle.getEntry(MARKER_FILE) != null)
      {
        type = CDOPackageUnit.Type.NATIVE;
      }
      else
      {
        type = CDOPackageUnit.Type.LEGACY;
      }

      bundles.put(bundleID, type);
    }

    return type;
  }

  private static String getBundleID(String packageURI)
  {
    org.eclipse.core.runtime.IExtensionRegistry registry = org.eclipse.core.runtime.Platform.getExtensionRegistry();
    for (org.eclipse.core.runtime.IConfigurationElement element : registry.getConfigurationElementsFor(ECORE_ID, PPID))
    {
      String uri = element.getAttribute("uri");
      if (ObjectUtil.equals(uri, packageURI))
      {
        return element.getContributor().getName();
      }
    }

    return null;
  }

  private static CDOPackageUnit.Type getPackageType(EPackage ePackage)
  {
    if (ePackage.getClass() == EPackageImpl.class)
    {
      EFactory factory = ePackage.getEFactoryInstance();
      if (factory instanceof CDOFactory)
      {
        return CDOPackageUnit.Type.NATIVE;
      }

      return CDOPackageUnit.Type.LEGACY;
    }

    EPackage topLevelPackage = EMFUtil.getTopLevelPackage(ePackage);
    EClass eClass = getAnyConcreteEClass(topLevelPackage);
    if (eClass != null)
    {
      EObject testObject = EcoreUtil.create(eClass);
      if (testObject instanceof CDOObject)
      {
        return CDOPackageUnit.Type.NATIVE;
      }

      return CDOPackageUnit.Type.LEGACY;
    }

    return null;
  }

  private static EClass getAnyConcreteEClass(EPackage ePackage)
  {
    for (EClassifier classifier : ePackage.getEClassifiers())
    {
      if (classifier instanceof EClass)
      {
        EClass eClass = (EClass)classifier;
        if (!(eClass.isAbstract() || eClass.isInterface()))
        {
          return eClass;
        }
      }
    }

    for (EPackage subpackage : ePackage.getESubpackages())
    {
      EClass eClass = getAnyConcreteEClass(subpackage);
      if (eClass != null)
      {
        return eClass;
      }
    }

    return null;
  }
}
