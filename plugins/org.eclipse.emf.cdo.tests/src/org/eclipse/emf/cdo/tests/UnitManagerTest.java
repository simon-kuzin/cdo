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
package org.eclipse.emf.cdo.tests;

import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.tests.model1.Category;
import org.eclipse.emf.cdo.tests.model1.Company;
import org.eclipse.emf.cdo.tests.model1.Customer;
import org.eclipse.emf.cdo.tests.model1.OrderDetail;
import org.eclipse.emf.cdo.tests.model1.Product1;
import org.eclipse.emf.cdo.tests.model1.PurchaseOrder;
import org.eclipse.emf.cdo.tests.model1.SalesOrder;
import org.eclipse.emf.cdo.tests.model1.Supplier;
import org.eclipse.emf.cdo.transaction.CDOTransaction;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.InternalEList;

import java.util.Iterator;

/**
 * @author Eike Stepper
 */
public class UnitManagerTest extends AbstractCDOTest
{
  private static final String RESOURCE_NAME = "test";

  public void testPrefetchBigModel() throws Exception
  {
    {
      CDOSession session = openSession();
      CDOTransaction transaction = session.openTransaction();
      CDOResource resource = transaction.createResource(getResourcePath(RESOURCE_NAME));

      long start = System.currentTimeMillis();
      for (int i = 0; i < 10; i++)
      {
        Company company = getModel1Factory().createCompany();
        addUnique(resource.getContents(), company);
        fillCompany(company);
        long stop = System.currentTimeMillis();
        System.out.println("Filled " + i + ": " + (stop - start));

        start = stop;
        transaction.commit();
        stop = System.currentTimeMillis();
        System.out.println("Committed " + i + ": " + (stop - start));
        start = stop;
      }

      session.close();
      System.out.println();
    }

    if (true)
    {
      clearCache(getRepository().getRevisionManager());
    }

    {
      CDOSession session = openSession();
      CDOTransaction transaction = session.openTransaction();
      CDOResource resource = transaction.getResource(getResourcePath(RESOURCE_NAME));

      if (true)
      {
        long start = System.currentTimeMillis();
        resource.cdoPrefetch(CDORevision.DEPTH_INFINITE);
        long stop = System.currentTimeMillis();
        System.out.println("Prefetched: " + (stop - start));
      }

      long start = System.currentTimeMillis();
      for (Iterator<EObject> it = resource.eAllContents(); it.hasNext();)
      {
        it.next();
      }

      long stop = System.currentTimeMillis();
      System.out.println("Iterated: " + (stop - start));

      session.close();
      System.out.println();
    }
  }

  private void fillCompany(Company company)
  {
    for (int i = 0; i < 5; i++)
    {
      Category category = getModel1Factory().createCategory();
      addUnique(company.getCategories(), category);
      fillCategory(category, 5);
    }

    for (int i = 0; i < 1000; i++)
    {
      Supplier supplier = getModel1Factory().createSupplier();
      addUnique(company.getSuppliers(), supplier);
    }

    for (int i = 0; i < 1000; i++)
    {
      Customer customer = getModel1Factory().createCustomer();
      addUnique(company.getCustomers(), customer);
    }

    for (int i = 0; i < 1000; i++)
    {
      PurchaseOrder order = getModel1Factory().createPurchaseOrder();
      order.setSupplier(company.getSuppliers().get(i));
      addUnique(company.getPurchaseOrders(), order);

      for (int j = 0; j < 100; j++)
      {
        OrderDetail orderDetail = getModel1Factory().createOrderDetail();
        addUnique(order.getOrderDetails(), orderDetail);
      }
    }

    for (int i = 0; i < 1000; i++)
    {
      SalesOrder order = getModel1Factory().createSalesOrder();
      order.setCustomer(company.getCustomers().get(i));
      addUnique(company.getSalesOrders(), order);

      for (int j = 0; j < 100; j++)
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

    for (int i = 0; i < 20; i++)
    {
      Product1 product = getModel1Factory().createProduct1();
      addUnique(category.getProducts(), product);
    }
  }

  private static <T extends EObject> void addUnique(EList<T> list, T object)
  {
    ((InternalEList<T>)list).addUnique(object);
    // list.add(object);
  }
}
