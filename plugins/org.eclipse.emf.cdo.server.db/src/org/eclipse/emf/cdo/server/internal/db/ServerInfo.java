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
package org.eclipse.emf.cdo.server.internal.db;

import org.eclipse.emf.cdo.common.model.EMFUtil;
import org.eclipse.emf.cdo.server.db.IClassMapping;
import org.eclipse.emf.cdo.server.db.IDBStore;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * @author Eike Stepper
 */
public abstract class ServerInfo extends AdapterImpl
{
  private IDBStore store;

  private int id;

  protected ServerInfo(IDBStore store, EModelElement modelElement, int id)
  {
    this.store = store;
    this.id = id;
    EMFUtil.addAdapter(modelElement, this);
  }

  public IDBStore getStore()
  {
    return store;
  }

  public int getID()
  {
    return id;
  }

  @Override
  public String toString()
  {
    return String.valueOf(id);
  }

  public static ServerInfo getServerInfo(EModelElement modelElement, IDBStore store)
  {
    EList<Adapter> adapters = modelElement.eAdapters();
    for (int i = 0, size = adapters.size(); i < size; ++i)
    {
      Adapter adapter = adapters.get(i);
      if (adapter instanceof ServerInfo)
      {
        ServerInfo serverInfo = (ServerInfo)adapter;
        if (serverInfo.getStore() == store)
        {
          return serverInfo;
        }
      }
    }

    throw new IllegalStateException("No server info attached to " + modelElement);
  }

  public static int getID(EModelElement modelElement, IDBStore store)
  {
    return getServerInfo(modelElement, store).getID();
  }

  /**
   * @author Eike Stepper
   */
  public final class PackageServerInfo extends ServerInfo
  {
    public PackageServerInfo(IDBStore store, EPackage ePackage, int id)
    {
      super(store, ePackage, id);
    }

    @Override
    public boolean isAdapterForType(Object type)
    {
      return EPackage.class.isInstance(type);
    }
  }

  /**
   * @author Eike Stepper
   */
  public final class ClassServerInfo extends ServerInfo
  {
    private IClassMapping classMapping;

    public ClassServerInfo(IDBStore store, EClass eClass, int id)
    {
      super(store, eClass, id);
    }

    public IClassMapping getClassMapping()
    {
      return classMapping;
    }

    public void setClassMapping(IClassMapping classMapping)
    {
      this.classMapping = classMapping;
    }

    @Override
    public boolean isAdapterForType(Object type)
    {
      return EClass.class.isInstance(type);
    }
  }

  /**
   * @author Eike Stepper
   */
  public final class FeatureServerInfo extends ServerInfo
  {
    public FeatureServerInfo(IDBStore store, EStructuralFeature feature, int id)
    {
      super(store, feature, id);
    }

    @Override
    public boolean isAdapterForType(Object type)
    {
      return EStructuralFeature.class.isInstance(type);
    }
  }
}
