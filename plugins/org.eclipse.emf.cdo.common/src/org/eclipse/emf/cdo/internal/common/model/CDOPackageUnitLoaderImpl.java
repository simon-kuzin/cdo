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

import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.model.CDOPackageUnitLoader;
import org.eclipse.emf.cdo.common.model.EMFUtil;

import org.eclipse.net4j.util.ObjectUtil;

import org.eclipse.emf.ecore.EPackage;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Eike Stepper
 */
public abstract class CDOPackageUnitLoaderImpl implements CDOPackageUnitLoader
{
  public CDOPackageUnitLoaderImpl()
  {
  }

  public final EPackage[] loadPackages(CDOPackageUnit packageUnit)
  {
    if (!canLoadPackages(packageUnit))
    {
      throw new IllegalStateException("Can not load " + packageUnit);
    }

    return doLoadPackages(packageUnit);
  }

  protected abstract EPackage[] doLoadPackages(CDOPackageUnit packageUnit);

  /**
   * @author Eike Stepper
   */
  public static class ExtensionRegistryBased extends CDOPackageUnitLoaderImpl
  {
    public ExtensionRegistryBased()
    {
    }

    public CDOPackageUnit[] loadPackageUnits()
    {
      return null;
    }

    public boolean canLoadPackages(CDOPackageUnit packageUnit)
    {
      return false;
    }

    @Override
    protected EPackage[] doLoadPackages(CDOPackageUnit packageUnit)
    {
      return null;
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
  }
}
