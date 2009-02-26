/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Martin Taal  - moved code from HibernateStore to this class
 */
package org.eclipse.emf.cdo.server.internal.hibernate;

import org.eclipse.emf.cdo.common.model.CDOPackageInfo;
import org.eclipse.emf.cdo.internal.server.TransactionCommitContextImpl;
import org.eclipse.emf.cdo.server.IStoreAccessor.CommitContext;
import org.eclipse.emf.cdo.server.internal.hibernate.bundle.OM;

import org.eclipse.net4j.util.ObjectUtil;
import org.eclipse.net4j.util.WrappedException;
import org.eclipse.net4j.util.io.IOUtil;
import org.eclipse.net4j.util.lifecycle.Lifecycle;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Expression;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Delegate which stores and retrieves cdo packages.
 * <p>
 * TODO extend {@link Lifecycle}. See {@link #doActivate()} and {@link #doDeactivate()}.
 * 
 * @author Eike Stepper
 * @author Martin Taal
 */
public class HibernatePackageHandler extends Lifecycle
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, HibernatePackageHandler.class);

  private Configuration configuration;

  private SessionFactory sessionFactory;

  private int nextPackageID;

  private int nextClassID;

  private int nextFeatureID;

  private Collection<CDOPackageInfo> cdoPackageInfos = null;

  private HibernateStore hibernateStore;

  /**
   * TODO Necessary to pass/store/dump the properties from the store?
   */
  public HibernatePackageHandler(HibernateStore store)
  {
    hibernateStore = store;
  }

  public List<EPackage> getEPackages()
  {
    List<EPackage> cdoPackages = new ArrayList<EPackage>();
    if (HibernateThreadContext.isHibernateCommitContextSet())
    {
      CommitContext cc = HibernateThreadContext.getHibernateCommitContext().getCommitContext();
      if (cc instanceof TransactionCommitContextImpl)
      {
        TransactionCommitContextImpl tx = (TransactionCommitContextImpl)cc;
        for (EPackage cdoPackage : tx.getNewPackages())
        {
          cdoPackages.add(cdoPackage);
        }
      }
    }

    for (EPackage cdoPackage : hibernateStore.getRepository().getPackageRegistry().getPackages())
    {
      cdoPackages.add(cdoPackage);
    }

    for (EPackage cdoPackage : cdoPackages)
    {
      // force resolve
      if (cdoPackage.getClassCount() == 0)
      {
        if (TRACER.isEnabled())
        {
          TRACER.trace("Returning " + cdoPackage.getNsURI());
        }
      }
    }

    return cdoPackages;
  }

  public void writePackages(EPackage... cdoPackages)
  {
    if (TRACER.isEnabled())
    {
      TRACER.trace("Persisting new EPackages");
    }

    Session session = getSessionFactory().openSession();
    Transaction tx = session.beginTransaction();
    boolean err = true;
    boolean updated = false;

    try
    {
      for (EPackage cdoPackage : cdoPackages)
      {
        if (cdoPackageExistsAndIsUnchanged(cdoPackage))
        {
          OM.LOG.warn("EPackage " + cdoPackage.getNsURI() + " already exists not persisting it again!");
          continue;
        }

        if (TRACER.isEnabled())
        {
          TRACER.trace("Persisting EPackage " + cdoPackage.getNsURI());
        }

        session.saveOrUpdate(cdoPackage);
        updated = true;
      }

      tx.commit();
      err = false;
    }
    finally
    {
      if (err)
      {
        tx.rollback();
      }

      session.close();
    }

    if (updated)
    {
      reset();
      hibernateStore.reInitialize();
    }
  }

  protected boolean cdoPackageExistsAndIsUnchanged(EPackage newEPackage)
  {
    EPackage[] cdoPackages = hibernateStore.getRepository().getPackageRegistry().getPackages();
    for (EPackage cdoPackage : cdoPackages)
    {
      if (cdoPackage.getClassCount() > 0 && cdoPackage.getNsURI().equals(newEPackage.getNsURI()))
      {
        String ecore = cdoPackage.getEcore();
        String newEcore = newEPackage.getEcore();
        return ObjectUtil.equals(ecore, newEcore);
      }
    }

    return false;
  }

  public void writePackage(EPackage cdoPackage)
  {
    if (cdoPackageExistsAndIsUnchanged(cdoPackage))
    {
      OM.LOG.warn("EPackage " + cdoPackage.getNsURI() + " already exists not persisting it again!");
      return;
    }

    Session session = getSessionFactory().openSession();
    Transaction tx = session.beginTransaction();
    boolean err = true;
    try
    {
      if (TRACER.isEnabled())
      {
        TRACER.trace("Persisting EPackage " + cdoPackage.getNsURI());
      }

      session.saveOrUpdate(cdoPackage);
      tx.commit();
      err = false;
    }
    finally
    {
      if (err)
      {
        tx.rollback();
      }

      session.close();
    }

    reset();
    hibernateStore.reInitialize();
  }

  public Collection<CDOPackageInfo> getEPackageDescriptors()
  {
    readPackageInfos();
    return cdoPackageInfos;
  }

  protected void readPackage(EPackage cdoPackage)
  {
    if (cdoPackage.getClassCount() > 0)
    { // already initialized go away
      return;
    }

    if (TRACER.isEnabled())
    {
      TRACER.trace("Reading EPackage with uri " + cdoPackage.getNsURI() + " from db");
    }

    Session session = getSessionFactory().openSession();

    try
    {
      Criteria criteria = session.createCriteria(EPackage.class);
      criteria.add(Expression.eq("packageURI", cdoPackage.getNsURI()));
      List<?> list = criteria.list();
      if (list.size() != 1)
      {
        throw new IllegalArgumentException("EPackage with uri " + cdoPackage.getNsURI() + " not present in the db");
      }

      if (TRACER.isEnabled())
      {
        TRACER.trace("Found " + list.size() + " EPackages in DB");
      }

      EPackage dbPackage = (EPackage)list.get(0);
      if (TRACER.isEnabled())
      {
        TRACER.trace("Read EPackage: " + cdoPackage.getName());
      }

      ((InternalEPackage)cdoPackage).setServerInfo(dbPackage.getServerInfo());
      ((InternalEPackage)cdoPackage).setName(dbPackage.getName());
      ((InternalEPackage)cdoPackage).setEcore(dbPackage.getEcore());
      ((InternalEPackage)cdoPackage).setMetaIDRange(cdoPackage.getMetaIDRange());

      final List<EClass> cdoClasses = new ArrayList<EClass>();
      for (EClass cdoClass : dbPackage.getClasses())
      {
        cdoClasses.add(cdoClass);
        for (EClassProxy proxy : ((InternalEClass)cdoClass).getSuperTypeProxies())
        {
          proxy.setCDOPackageManager(hibernateStore.getRepository().getPackageRegistry());
        }

        for (EStructuralFeature cdoFeature : cdoClass.getFeatures())
        {
          final InternalCDOFeature internalFeature = (InternalCDOFeature)cdoFeature;
          internalFeature.setContainingClass(cdoClass);
          if (internalFeature.getReferenceTypeProxy() != null)
          {
            internalFeature.getReferenceTypeProxy().setCDOPackageManager(
                hibernateStore.getRepository().getPackageRegistry());
          }
        }

        // // force indices to be set
        // if (TODO.getAllPersistentFeatures(cdoClass).length > 0)
        // {
        // ((InternalEClass)cdoClass).getFeatureIndex(0);
        // }
      }

      ((InternalEPackage)cdoPackage).setClasses(cdoClasses);
    }
    finally
    {
      session.close();
    }

    if (TRACER.isEnabled())
    {
      TRACER.trace("Finished reading EPackages");
    }
  }

  protected void readPackageInfos()
  {
    if (cdoPackageInfos == null || cdoPackageInfos.size() == 0)
    {
      if (TRACER.isEnabled())
      {
        TRACER.trace("Reading EPackages from db");
      }

      Collection<CDOPackageInfo> result = new ArrayList<CDOPackageInfo>();
      Session session = getSessionFactory().openSession();

      try
      {
        Criteria criteria = session.createCriteria(EPackage.class);
        List<?> list = criteria.list();
        if (TRACER.isEnabled())
        {
          TRACER.trace("Found " + list.size() + " EPackages in DB");
        }

        for (Object object : list)
        {
          EPackage cdoPackage = (EPackage)object;
          if (TRACER.isEnabled())
          {
            TRACER.trace("Read EPackage: " + cdoPackage.getName());
          }

          result.add(new CDOPackageInfo(cdoPackage.getNsURI(), cdoPackage.getParentURI(), cdoPackage.isDynamic(),
              cdoPackage.getMetaIDRange()));
          ((InternalEPackage)cdoPackage).setPackageManager(hibernateStore.getRepository().getPackageRegistry());
        }

        cdoPackageInfos = result;
      }
      finally
      {
        session.close();
      }
    }

    if (TRACER.isEnabled())
    {
      TRACER.trace("Finished reading EPackages");
    }
  }

  void doDropSchema()
  {
    final SchemaExport se = new SchemaExport(configuration);
    se.drop(false, true);
  }

  public synchronized SessionFactory getSessionFactory()
  {
    if (sessionFactory == null)
    {
      sessionFactory = configuration.buildSessionFactory();
    }

    return sessionFactory;
  }

  public synchronized int getNextPackageID()
  {
    return nextPackageID++;
  }

  public synchronized int getNextClassID()
  {
    return nextClassID++;
  }

  public synchronized int getNextFeatureID()
  {
    return nextFeatureID++;
  }

  public void reset()
  {
    cdoPackageInfos = null;
  }

  @Override
  protected void doActivate() throws Exception
  {
    super.doActivate();
    initConfiguration();
    initSchema();
  }

  @Override
  protected void doDeactivate() throws Exception
  {
    if (sessionFactory != null)
    {
      sessionFactory.close();
      sessionFactory = null;
    }

    super.doDeactivate();
  }

  protected void initConfiguration()
  {
    if (TRACER.isEnabled())
    {
      TRACER.trace("Initializing configuration for CDO metadata");
    }

    InputStream in = null;

    try
    {
      in = OM.BUNDLE.getInputStream("mappings/meta.hbm.xml");
      configuration = new Configuration();
      configuration.addInputStream(in);
      configuration.setProperties(HibernateUtil.getInstance().getPropertiesFromStore(hibernateStore));
    }
    catch (Exception ex)
    {
      throw WrappedException.wrap(ex);
    }
    finally
    {
      IOUtil.close(in);
    }
  }

  protected void initSchema()
  {
    if (TRACER.isEnabled())
    {
      TRACER.trace("Updating db schema for Hibernate PackageHandler");
    }

    new SchemaUpdate(configuration).execute(true, true);
  }
}
