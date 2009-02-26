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

import org.eclipse.emf.cdo.internal.common.model.CDOTypeImpl;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EPackage;

/**
 * @since 2.0
 * @author Eike Stepper
 */
public final class CDOModelUtil
{
  private CDOModelUtil()
  {
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
}
