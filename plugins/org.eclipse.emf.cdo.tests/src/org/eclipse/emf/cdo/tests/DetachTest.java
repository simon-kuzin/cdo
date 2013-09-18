/*
 * Copyright (c) 2008-2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Simon McDuff - initial API and implementation
 *    Eike Stepper - maintenance
 */
package org.eclipse.emf.cdo.tests;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.tests.model1.Company;
import org.eclipse.emf.cdo.tests.model1.Order;
import org.eclipse.emf.cdo.tests.model1.OrderDetail;
import org.eclipse.emf.cdo.tests.model1.Product1;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.transaction.CDOUserSavepoint;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ObjectNotFoundException;

import org.eclipse.net4j.util.WrappedException;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.spi.cdo.FSMUtil;
import org.eclipse.emf.spi.cdo.InternalCDOObject;

/**
 * @author Simon McDuff
 */
public class DetachTest extends AbstractCDOTest
{
  public void testNewObjectDeletion() throws Exception
  {
    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    CDOResource resource = transaction.createResource(getResourcePath("/my/resource"));

    Company company = getModel1Factory().createCompany();
    company.setName("Test");
    resource.getContents().add(company);

    URI companyURI = EcoreUtil.getURI(company);
    assertEquals(company, transaction.getResourceSet().getEObject(companyURI, false));

    resource.getContents().remove(0); // remove object by index
    assertNull(transaction.getResourceSet().getEObject(companyURI, false));

    transaction.commit();
    assertNull(transaction.getResourceSet().getEObject(companyURI, false));
    session.close();
  }

  public void testCleanObjectDeletion() throws Exception
  {
    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    CDOResource resource = transaction.createResource(getResourcePath("/my/resource"));

    Company company = getModel1Factory().createCompany();
    company.setName("Test");
    resource.getContents().add(company);
    transaction.commit(); // (2)

    final URI companyURI = EcoreUtil.getURI(company);
    final CDOID id = CDOUtil.getCDOObject(company).cdoID();
    assertEquals(company, transaction.getResourceSet().getEObject(companyURI, false));

    resource.getContents().remove(company);
    assertTransient(company);
    assertSame(company, CDOUtil.getEObject(transaction.getObject(id)));
    assertSame(company, transaction.getResourceSet().getEObject(companyURI, false));

    transaction.commit();
    assertTransient(company);

    try
    {
      CDOObject object = transaction.getObject(id);
      msg(object);
      fail("ObjectNotFoundException expected");
    }
    catch (ObjectNotFoundException expected)
    {
      // SUCCESS
    }

    assertNull(transaction.getResourceSet().getEObject(companyURI, false));
    session.close();
  }

  public void testSavePointNewObjectDeletion() throws Exception
  {
    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    CDOResource resource = transaction.createResource(getResourcePath("/my/resource"));

    Company company = getModel1Factory().createCompany();
    company.setName("Test");
    resource.getContents().add(company);

    savePointObjectDeletion(transaction, resource);
  }

  public void testSavePointCleanObjectDeletion() throws Exception
  {
    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    CDOResource resource = transaction.createResource(getResourcePath("/my/resource"));

    Company company = getModel1Factory().createCompany();
    company.setName("Test");
    resource.getContents().add(company);

    URI companyURI = EcoreUtil.getURI(company);
    assertEquals(company, transaction.getResourceSet().getEObject(companyURI, false));

    transaction.commit();
    savePointObjectDeletion(transaction, resource);
  }

  private void savePointObjectDeletion(CDOTransaction transaction, CDOResource resource)
  {
    Company company = (Company)resource.getContents().get(0);

    URI companyURI = EcoreUtil.getURI(company);
    CDOObject companyCDOObject = CDOUtil.getCDOObject(company);
    boolean isPersisted = !FSMUtil.isNew(companyCDOObject);
    company.setName("SIMON");

    CDOUserSavepoint savepoint = transaction.setSavepoint();
    resource.getContents().remove(0); // remove object by index
    assertTransient(company);

    CDOUserSavepoint savepoint2 = transaction.setSavepoint();
    company.setName("SIMON2");
    if (isPersisted)
    {
      assertNotNull(transaction.getResourceSet().getEObject(companyURI, false));
    }
    else
    {
      assertNull(transaction.getResourceSet().getEObject(companyURI, false));
    }

    savepoint2.rollback();
    assertEquals("SIMON2", company.getName());
    assertTransient(company);

    if (isPersisted)
    {
      assertNotNull(transaction.getResourceSet().getEObject(companyURI, false));
    }
    else
    {
      assertNull(transaction.getResourceSet().getEObject(companyURI, false));
    }

    savepoint.rollback();
    assertEquals("SIMON", company.getName());
    assertEquals(company, transaction.getResourceSet().getEObject(companyURI, false));

    if (isPersisted)
    {
      assertDirty(company, transaction);
    }
    else
    {
      assertNew(company, transaction);
    }

    try
    {
      transaction.commit();
    }
    catch (CommitException ex)
    {
      throw WrappedException.wrap(ex);
    }
  }

  public void testKeepValue() throws Exception
  {
    {
      CDOSession session = openSession();
      CDOTransaction transaction = session.openTransaction();
      CDOResource resource = transaction.createResource(getResourcePath("/my/resource"));

      Company company = getModel1Factory().createCompany();
      company.setName("Test");
      resource.getContents().add(company);

      transaction.commit();
    }

    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    CDOResource resource = transaction.getOrCreateResource(getResourcePath("/my/resource"));

    Company company = (Company)resource.getContents().get(0);
    assertEquals("Test", company.getName());

    resource.getContents().remove(0); // Remove object by index
    assertEquals("Test", company.getName());

    transaction.commit();
    assertEquals("Test", company.getName());
  }

  private void detachResource(ResourceSet rset, CDOResource resource, boolean commitBeforeDelete) throws Exception
  {
    CDOTransaction transaction = (CDOTransaction)resource.cdoView();
    Order order = getModel1Factory().createPurchaseOrder();
    OrderDetail orderDetail = getModel1Factory().createOrderDetail();
    Product1 product = getModel1Factory().createProduct1();
    product.setName("product");

    resource.getContents().add(product);
    resource.getContents().add(order);

    order.getOrderDetails().add(orderDetail);
    orderDetail.setProduct(product);
    assertActive(resource);
    assertEquals(1, CDOUtil.getViewSet(rset).getViews().length);
    assertEquals(1, rset.getResources().size());// Bug 346636
    if (commitBeforeDelete == true)
    {
      transaction.commit();
    }

    InternalCDOObject orderInternal = FSMUtil.adapt(order, resource.cdoView());
    CDOID orderID = orderInternal.cdoID();

    resource.delete(null);
    assertTransient(resource);
    assertTransient(order);
    assertTransient(orderDetail);
    assertTransient(product);

    assertEquals(1, CDOUtil.getViewSet(rset).getViews().length);
    assertEquals(0, rset.getResources().size());// Bug 346636
    assertEquals(2, resource.getContents().size());
    assertEquals(true, resource.getContents().contains(order));
    assertEquals(true, resource.getContents().contains(product));
    assertEquals(true, order.getOrderDetails().contains(orderDetail));
    assertEquals(order, orderDetail.eContainer());
    assertEquals(resource, ((InternalEObject)order).eDirectResource());
    assertEquals(resource, ((InternalEObject)product).eDirectResource());

    assertEquals(true && commitBeforeDelete, transaction.getDetachedObjects().containsKey(orderID));
    assertEquals(false, transaction.getRevisionDeltas().containsKey(orderID));
  }

  public void testDetachNewResource() throws Exception
  {
    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    ResourceSet rset = transaction.getResourceSet();

    CDOResource resource = transaction.createResource(getResourcePath("/test1"));
    detachResource(rset, resource, false);

    transaction.commit();
  }

  public void testDetachPersistedResource() throws Exception
  {
    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    ResourceSet rset = transaction.getResourceSet();
    CDOResource resource = transaction.createResource(getResourcePath("/test1"));

    transaction.commit();
    CDOID resourceID = resource.cdoID();
    detachResource(rset, resource, false);
    assertEquals(true, transaction.getDetachedObjects().containsKey(resourceID));

    transaction.commit();
  }

  public void testDetachPersistedResourceWithPersistedData() throws Exception
  {
    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    ResourceSet rset = transaction.getResourceSet();
    CDOResource resource = transaction.createResource(getResourcePath("/test1"));

    transaction.commit();
    CDOID resourceID = resource.cdoID();
    detachResource(rset, resource, true);
    assertEquals(true, transaction.getDetachedObjects().containsKey(resourceID));

    transaction.commit();
  }

  public void testDetachEmptyNewResource() throws Exception
  {
    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    CDOResource resource = transaction.createResource(getResourcePath("/test1"));

    resource.delete(null);
    assertTransient(resource);
  }

  public void testDetachEmptyPersistedResource() throws Exception
  {
    CDOSession session = openSession();

    CDOTransaction transaction1 = session.openTransaction();
    String resourcePath = getResourcePath("/test1");
    CDOResource res = transaction1.createResource(resourcePath);
    res.getContents().add(getModel1Factory().createCompany());
    res.getContents().add(getModel1Factory().createCompany());
    res.getContents().add(getModel1Factory().createCompany());
    transaction1.commit();

    CDOTransaction transaction2 = session.openTransaction();
    CDOResource res2 = transaction2.getResource(resourcePath);

    ResourceSet rSet1 = res.getResourceSet();
    final ResourceSet rSet2 = res2.getResourceSet();

    res.delete(null);
    assertEquals(true, transaction1.isDirty());
    transaction1.commit();

    new PollingTimeOuter()
    {
      @Override
      protected boolean successful()
      {
        return rSet2.getResources().isEmpty();
      }
    }.assertNoTimeOut();

    assertEquals(0, rSet1.getResources().size());// Bug 346636
    assertTransient(res);
    assertInvalid(res2);
  }

  public void testDetachProxyResource() throws Exception
  {
    {
      CDOSession session = openSession();
      CDOTransaction transaction = session.openTransaction();
      transaction.createResource(getResourcePath("/test1"));
      transaction.commit();
    }

    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    CDOResource resource = transaction.getResource(getResourcePath("/test1"));

    resource.delete(null);
    assertEquals(true, resource.isExisting());
    transaction.commit();
  }

  /**
   * Bug 357469.
   */
  public void _testDetachConcurrently() throws Exception
  {
    String path = getResourcePath("/test1");

    {
      CDOSession session = openSession();
      CDOTransaction transaction = session.openTransaction();
      CDOResource resource = transaction.createResource(path);
      resource.getContents().add(getModel1Factory().createCompany());
      transaction.commit();
      session.close();
    }

    CDOSession session1 = openSession();
    session1.options().setPassiveUpdateEnabled(false);
    CDOTransaction transaction1 = session1.openTransaction();
    CDOResource resource1 = transaction1.getResource(path);
    EList<EObject> contents1 = resource1.getContents();
    contents1.get(0);

    CDOSession session2 = openSession();
    session2.options().setPassiveUpdateEnabled(false);
    CDOTransaction transaction2 = session2.openTransaction();
    CDOResource resource2 = transaction2.getResource(path);
    EList<EObject> contents2 = resource2.getContents();
    contents2.get(0);

    contents1.remove(0);
    transaction1.commit();

    contents2.remove(0);
    transaction2.commit();
  }
}
