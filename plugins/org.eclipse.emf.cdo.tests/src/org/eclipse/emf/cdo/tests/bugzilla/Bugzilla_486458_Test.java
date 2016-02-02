/*
 * Copyright (c) 2016 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.tests.bugzilla;

import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.server.IRepository.Props;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.tests.AbstractCDOTest;
import org.eclipse.emf.cdo.tests.config.IRepositoryConfig;
import org.eclipse.emf.cdo.tests.config.impl.ConfigTest.CleanRepositoriesAfter;
import org.eclipse.emf.cdo.tests.config.impl.ConfigTest.CleanRepositoriesBefore;
import org.eclipse.emf.cdo.tests.config.impl.ConfigTest.Requires;
import org.eclipse.emf.cdo.tests.config.impl.ConfigTest.Skips;
import org.eclipse.emf.cdo.tests.model1.Category;
import org.eclipse.emf.cdo.tests.model1.Company;
import org.eclipse.emf.cdo.tests.model1.Customer;
import org.eclipse.emf.cdo.tests.model1.OrderDetail;
import org.eclipse.emf.cdo.tests.model1.Product1;
import org.eclipse.emf.cdo.tests.model1.PurchaseOrder;
import org.eclipse.emf.cdo.tests.model1.SalesOrder;
import org.eclipse.emf.cdo.tests.model1.Supplier;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ConcurrentAccessException;
import org.eclipse.emf.cdo.view.CDOUnit;

import org.eclipse.emf.internal.cdo.view.CDOViewImpl.CDOUnitManagerImpl.CDOUnitImpl;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.InternalEList;

import java.util.Iterator;
import java.util.Map;

/**
 * Bug 486458 - Provide support for optimized loading and notifying of object units
 *
 * @author Eike Stepper
 */
@Requires({ IRepositoryConfig.CAPABILITY_AUDITING, "DB.ranges" })
@Skips(IRepositoryConfig.CAPABILITY_BRANCHING)
@CleanRepositoriesBefore(reason = "Instrumented repository")
@CleanRepositoriesAfter(reason = "Instrumented repository")
public class Bugzilla_486458_Test extends AbstractCDOTest
{
  @Override
  protected void doSetUp() throws Exception
  {
    Map<String, Object> map = getTestProperties();
    map.put(Props.SUPPORTING_UNITS, Boolean.toString(true));

    super.doSetUp();
  }

  public void testPrefetchBigModel() throws Exception
  {
    fillRepository();
    clearCache(getRepository().getRevisionManager());

    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    CDOResource resource = transaction.getResource(getResourcePath("test"));

    long start = System.currentTimeMillis();
    resource.cdoPrefetch(CDORevision.DEPTH_INFINITE);
    long stop = System.currentTimeMillis();
    System.out.println("Prefetched: " + (stop - start));

    int count = iterateResource(resource);
    assertEquals(7714, count);
  }

  public void testCreateUnit() throws Exception
  {
    fillRepository();
    clearCache(getRepository().getRevisionManager());

    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    CDOResource resource = transaction.getResource(getResourcePath("test"));

    long start = System.currentTimeMillis();
    transaction.getUnitManager().createUnit(resource);
    long stop = System.currentTimeMillis();
    System.out.println("Created Unit: " + (stop - start));

    int count = iterateResource(resource);
    assertEquals(7714, count);
  }

  public void testOpenUnit() throws Exception
  {
    fillRepository();

    {
      CDOSession session = openSession();
      CDOTransaction transaction = session.openTransaction();
      CDOResource resource = transaction.getResource(getResourcePath("test"));
      CDOUnit createdUnit = transaction.getUnitManager().createUnit(resource);
      assertEquals(7714, ((CDOUnitImpl)createdUnit).getInitialElements());

      session.close();
      clearCache(getRepository().getRevisionManager());
    }

    {
      CDOSession session = openSession();
      CDOTransaction transaction = session.openTransaction();
      CDOResource resource = transaction.getResource(getResourcePath("test"));

      long start = System.currentTimeMillis();

      CDOUnit openedUnit = transaction.getUnitManager().openUnit(resource);
      assertEquals(7714, ((CDOUnitImpl)openedUnit).getInitialElements());

      long stop = System.currentTimeMillis();
      System.out.println("Opened Unit: " + (stop - start));

      int count = iterateResource(resource);
      assertEquals(7714, count);
    }
  }

  private void fillRepository() throws ConcurrentAccessException, CommitException
  {
    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    CDOResource resource = transaction.createResource(getResourcePath("test"));

    long start = System.currentTimeMillis();
    for (int i = 0; i < 3; i++)
    {
      Company company = getModel1Factory().createCompany();
      addUnique(resource.getContents(), company);
      fillCompany(company);
      long stop = System.currentTimeMillis();
      System.out.println("Filled: " + (stop - start));

      start = stop;
      transaction.commit();
      stop = System.currentTimeMillis();
      System.out.println("Committed: " + (stop - start));
      start = stop;
    }

    session.close();
  }

  private void fillCompany(Company company)
  {
    for (int i = 0; i < 5; i++)
    {
      Category category = getModel1Factory().createCategory();
      addUnique(company.getCategories(), category);
      fillCategory(category, 3);
    }

    for (int i = 0; i < 10; i++)
    {
      Supplier supplier = getModel1Factory().createSupplier();
      addUnique(company.getSuppliers(), supplier);
    }

    for (int i = 0; i < 10; i++)
    {
      Customer customer = getModel1Factory().createCustomer();
      addUnique(company.getCustomers(), customer);
    }

    for (int i = 0; i < 10; i++)
    {
      PurchaseOrder order = getModel1Factory().createPurchaseOrder();
      order.setSupplier(company.getSuppliers().get(i));
      addUnique(company.getPurchaseOrders(), order);

      for (int j = 0; j < 10; j++)
      {
        OrderDetail orderDetail = getModel1Factory().createOrderDetail();
        addUnique(order.getOrderDetails(), orderDetail);
      }
    }

    for (int i = 0; i < 10; i++)
    {
      SalesOrder order = getModel1Factory().createSalesOrder();
      order.setCustomer(company.getCustomers().get(i));
      addUnique(company.getSalesOrders(), order);

      for (int j = 0; j < 10; j++)
      {
        OrderDetail orderDetail = getModel1Factory().createOrderDetail();
        addUnique(order.getOrderDetails(), orderDetail);
      }
    }
  }

  private void fillCategory(Category category, int depth)
  {
    for (int i = 0; i < 5; i++)
    {
      Category child = getModel1Factory().createCategory();
      addUnique(category.getCategories(), child);
      if (depth > 1)
      {
        fillCategory(child, depth - 1);
      }
    }

    for (int i = 0; i < 10; i++)
    {
      Product1 product = getModel1Factory().createProduct1();
      addUnique(category.getProducts(), product);
    }
  }

  private static <T extends EObject> void addUnique(EList<T> list, T object)
  {
    ((InternalEList<T>)list).addUnique(object);
  }

  private static int iterateResource(CDOResource resource)
  {
    int count = 1;
    long start = System.currentTimeMillis();

    for (Iterator<EObject> it = resource.eAllContents(); it.hasNext();)
    {
      it.next();
      ++count;
    }

    long stop = System.currentTimeMillis();
    System.out.println("Iterated: " + (stop - start));

    return count;
  }
}
