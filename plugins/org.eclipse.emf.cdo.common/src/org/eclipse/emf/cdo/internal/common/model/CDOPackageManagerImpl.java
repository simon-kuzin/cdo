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

import org.eclipse.emf.cdo.common.model.EPackage;
import org.eclipse.emf.cdo.common.model.core.CDOCorePackage;
import org.eclipse.emf.cdo.common.model.resource.CDOResourcePackage;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.internal.common.model.core.CDOCorePackageImpl;
import org.eclipse.emf.cdo.internal.common.model.resource.CDOResourcePackageImpl;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageManager;

import org.eclipse.net4j.util.container.Container;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Eike Stepper
 */
public abstract class CDOPackageManagerImpl extends Container<EPackage> implements InternalCDOPackageManager
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_MODEL, CDOPackageManagerImpl.class);

  private ConcurrentMap<String, EPackage> packages = new ConcurrentHashMap<String, EPackage>();

  private CDOCorePackage cdoCorePackage;

  private CDOResourcePackage cdoResourcePackage;

  public CDOPackageManagerImpl()
  {
    addPackage(cdoCorePackage = new CDOCorePackageImpl(this));
    addPackage(cdoResourcePackage = new CDOResourcePackageImpl(this));
  }

  public EPackage lookupPackage(String uri)
  {
    if (uri == null)
    {
      return null;
    }

    return packages.get(uri);
  }

  public int getPackageCount()
  {
    return packages.size();
  }

  public EPackage[] getPackages()
  {
    return packages.values().toArray(new EPackage[packages.size()]);
  }

  public EPackage[] getElements()
  {
    return getPackages();
  }

  @Override
  public boolean isEmpty()
  {
    return packages.isEmpty();
  }

  public CDOCorePackage getCDOCorePackage()
  {
    return cdoCorePackage;
  }

  public CDOResourcePackage getCDOResourcePackage()
  {
    return cdoResourcePackage;
  }

  public List<EPackage> getTransientPackages()
  {
    List<EPackage> result = new ArrayList<EPackage>();
    for (EPackage cdoPackage : packages.values())
    {
      if (!cdoPackage.isPersistent())
      {
        result.add(cdoPackage);
      }
    }

    return result;
  }

  public void addPackage(EPackage cdoPackage)
  {
    String uri = cdoPackage.getPackageURI();
    if (uri == null)
    {
      throw new IllegalArgumentException("uri == null");
    }

    EPackage existing = packages.putIfAbsent(uri, cdoPackage);
    if (existing == null)
    {
      if (TRACER.isEnabled())
      {
        TRACER.format("Added package: {0}", cdoPackage);
      }

      fireElementAddedEvent(cdoPackage);
    }
    else
    {
      throw new IllegalStateException("Duplicate package: " + cdoPackage);
    }
  }

  public void removePackage(EPackage cdoPackage)
  {
    packages.remove(cdoPackage.getPackageURI());
    fireElementRemovedEvent(cdoPackage);
  }
}
