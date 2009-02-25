/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Simon McDuff - maintenance
 */
package org.eclipse.emf.internal.cdo.session;

import org.eclipse.emf.cdo.common.id.CDOIDMetaRange;
import org.eclipse.emf.cdo.common.id.CDOIDObjectFactory;
import org.eclipse.emf.cdo.common.model.CDOPackageInfo;
import org.eclipse.emf.cdo.session.CDOSessionPackageManager;

import org.eclipse.emf.internal.cdo.bundle.OM;
import org.eclipse.emf.cdo.common.model.ModelUtil;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.spi.cdo.InternalCDOSession;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eike Stepper
 */
public class _CDOSessionPackageManagerImpl extends CDOPackageManagerImpl implements CDOSessionPackageManager
{
  private InternalCDOSession session;

  /**
   * For optimization only. Instead of doing 3 lookups we are doing only one.
   * <p>
   * We could apply the same strategy for EClass and EPackage, because this is an optimization it will be good to do it
   * only if it proof to make a difference. EPackage doesn't need to do it since we will do one lookup anyway...
   * otherwise we need to proof it is more efficient.
   * <p>
   * TODO Should we have a cache for EClass(to save 1 lookup), EPackage (doesn'T save any lookup) ? TODO A reverse
   * lookup cache is it worth it ?
   */
  private Map<EStructuralFeature, EStructuralFeature> featureCache = new ConcurrentHashMap<EStructuralFeature, EStructuralFeature>();

  /**
   * @since 2.0
   */
  public _CDOSessionPackageManagerImpl(InternalCDOSession session)
  {
    this.session = session;
    ModelUtil.addModelInfos(this);
  }

  /**
   * @since 2.0
   */
  public InternalCDOSession getSession()
  {
    return session;
  }

  public CDOIDObjectFactory getCDOIDObjectFactory()
  {
    return session;
  }

  public void addPackageProxies(Collection<CDOPackageInfo> packageInfos)
  {
    for (CDOPackageInfo info : packageInfos)
    {
      String packageURI = info.getPackageURI();
      boolean dynamic = info.isDynamic();
      CDOIDMetaRange metaIDRange = info.getMetaIDRange();
      String parentURI = info.getParentURI();

      EPackage proxy = CDOModelUtil.createProxyPackage(this, packageURI, dynamic, metaIDRange, parentURI);
      addPackage(proxy);
      session.getPackageRegistry().putPackageDescriptor(proxy);
    }
  }

  /**
   * @since 2.0
   */
  public void loadPackage(EPackage cdoPackage)
  {
    if (!cdoPackage.isDynamic())
    {
      String uri = cdoPackage.getNsURI();
      EPackage ePackage = session.getPackageRegistry().getEPackage(uri);
      if (ePackage != null)
      {
        ModelUtil.initializeEPackage(ePackage, cdoPackage);
        return;
      }
    }

    session.getSessionProtocol().loadPackage(cdoPackage, false);
    if (!cdoPackage.isDynamic())
    {
      OM.LOG.info("Dynamic package created for " + cdoPackage.getNsURI());
    }
  }

  /**
   * @since 2.0
   */
  public void loadPackageEcore(EPackage cdoPackage)
  {
    session.getSessionProtocol().loadPackage(cdoPackage, true);
  }
}
