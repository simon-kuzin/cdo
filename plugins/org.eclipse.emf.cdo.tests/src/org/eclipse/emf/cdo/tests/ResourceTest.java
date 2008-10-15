/***************************************************************************
 * Copyright (c) 2004 - 2008 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Simon McDuff - initial API and implementation
 *    Eike Stepper - maintenance
 **************************************************************************/
package org.eclipse.emf.cdo.tests;

import org.eclipse.emf.cdo.CDOAudit;
import org.eclipse.emf.cdo.CDOSession;
import org.eclipse.emf.cdo.CDOState;
import org.eclipse.emf.cdo.CDOTransaction;
import org.eclipse.emf.cdo.CDOView;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.eresource.CDOResourceNode;
import org.eclipse.emf.cdo.tests.model1.Product1;
import org.eclipse.emf.cdo.tests.model1.VAT;
import org.eclipse.emf.cdo.util.CDOURIUtil;
import org.eclipse.emf.cdo.util.CDOUtil;

import org.eclipse.net4j.util.transaction.TransactionException;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

/**
 * @author Eike Stepper
 */
public class ResourceTest extends AbstractCDOTest
{
  public void testCreateResource_FromResourceSet() throws Exception
  {
    CDOSession session = openModel1Session();
    ResourceSet resourceSet = new ResourceSetImpl();
    CDOTransaction transaction = session.openTransaction(resourceSet);

    final URI uri = URI.createURI("cdo:/test1");
    CDOResource resource = (CDOResource)resourceSet.createResource(uri);
    assertActive(resource);
    assertNew(resource, transaction);
    assertEquals(transaction.getResourceSet(), resource.getResourceSet());
    assertEquals(CDOURIUtil.createResourceURI(session, "test1"), resource.getURI());
    assertEquals("test1", resource.getName());
    assertEquals(null, resource.getFolder());
  }

  public void testCreateNestedResource_FromResourceSet() throws Exception
  {
    CDOSession session = openModel1Session();
    ResourceSet resourceSet = new ResourceSetImpl();
    CDOTransaction transaction = session.openTransaction(resourceSet);

    final URI uri = URI.createURI("cdo:/folder/test1");
    CDOResource resource = (CDOResource)resourceSet.createResource(uri);
    assertActive(resource);
    assertNew(resource, transaction);
    assertEquals(transaction.getResourceSet(), resource.getResourceSet());
    assertEquals(CDOURIUtil.createResourceURI(session, "folder/test1"), resource.getURI());
    assertEquals("test1", resource.getName());

    CDOResourceFolder folder = resource.getFolder();
    assertNotNull(folder);
    assertEquals("folder", folder.getName());
    assertEquals(null, folder.getFolder());
  }

  public void testCreateResource_FromTransaction() throws Exception
  {
    msg("Opening session");
    CDOSession session = openModel1Session();

    msg("Opening transaction");
    CDOTransaction transaction = session.openTransaction();

    // Test if Resource is well-formed after CDOResourceFactoryImpl.
    // Adapter will be called right after and could be used!
    transaction.getResourceSet().eAdapters().add(new TestAdapter());

    msg("Creating resource");
    CDOResource resource = transaction.createResource("/test1");
    assertActive(resource);
    CDOResource resourceCopy = transaction.getOrCreateResource("/test1");
    assertEquals(resource, resourceCopy);

    msg("Verifying resource");
    assertNew(resource, transaction);
    assertEquals(CDOURIUtil.createResourceURI(session, "test1"), resource.getURI());
    assertEquals(transaction.getResourceSet(), resource.getResourceSet());
  }

  public void testRemoveResourceWithCloseView() throws Exception
  {
    {
      msg("Opening session");
      CDOSession session = openModel1Session();

      msg("Opening transaction");
      CDOTransaction transaction = session.openTransaction();
      ResourceSet rset = transaction.getResourceSet();
      msg("Creating resource");
      CDOResource resource = transaction.createResource("/test1");
      assertActive(resource);
      transaction.commit();

      Assert.assertEquals(1, rset.getResources().size());
      Assert.assertEquals(1, CDOUtil.getViewSet(rset).getViews().length);

      transaction.close();

      Assert.assertEquals(0, CDOUtil.getViewSet(rset).getViews().length);
      Assert.assertEquals(0, rset.getResources().size());
      session.close();
    }

    {
      CDOSession session = openModel1Session();
      msg("Opening transaction");
      CDOTransaction transaction = session.openTransaction();

      msg("Getting resource");
      CDOResource resource = (CDOResource)transaction.getResourceSet().getResource(
          CDOURIUtil.createResourceURI(transaction, "/test1"), true);
      assertNotNull(resource);
      assertEquals(transaction.getResourceSet(), resource.getResourceSet());
      assertEquals(1, transaction.getResourceSet().getResources().size());
      assertEquals(CDOState.PROXY, resource.cdoState());
      assertEquals(transaction, resource.cdoView());
      assertNull(resource.cdoRevision());
    }
  }

  public void testAttachManyResources() throws Exception
  {
    CDOSession session = openModel1Session();
    CDOTransaction transaction = session.openTransaction();

    CDOResource resource1 = transaction.createResource("/my/resource1");
    CDOResource resource2 = transaction.createResource("/my/resource2");
    CDOResource resource3 = transaction.createResource("/my/resource3");

    List<Resource> tobeRemoved = new ArrayList<Resource>();
    tobeRemoved.add(resource1);
    tobeRemoved.add(resource3);

    assertEquals(3, transaction.getResourceSet().getResources().size());

    transaction.getResourceSet().getResources().removeAll(tobeRemoved);

    assertEquals(1, transaction.getResourceSet().getResources().size());
    assertEquals(null, transaction.getResourceSet().getResource(resource1.getURI(), false));
    assertEquals(resource2, transaction.getResourceSet().getResource(resource2.getURI(), false));
    assertEquals(null, transaction.getResourceSet().getResource(resource3.getURI(), false));

    transaction.getResourceSet().getResources().addAll(tobeRemoved);

    assertEquals(3, transaction.getResourceSet().getResources().size());
    assertEquals(resource1, transaction.getResourceSet().getResource(resource1.getURI(), false));
    assertEquals(resource2, transaction.getResourceSet().getResource(resource2.getURI(), false));
    assertEquals(resource3, transaction.getResourceSet().getResource(resource3.getURI(), false));

    transaction.commit();

    session.close();
  }

  public void testDetachManyResources() throws Exception
  {
    CDOSession session = openModel1Session();
    CDOTransaction transaction = session.openTransaction();

    CDOResource resource1 = transaction.createResource("/my/resource1");
    CDOResource resource2 = transaction.createResource("/my/resource2");
    CDOResource resource3 = transaction.createResource("/my/resource3");

    List<Resource> tobeRemoved = new ArrayList<Resource>();
    tobeRemoved.add(resource1);
    tobeRemoved.add(resource3);

    assertEquals(3, transaction.getResourceSet().getResources().size());

    transaction.getResourceSet().getResources().removeAll(tobeRemoved);

    assertEquals(1, transaction.getResourceSet().getResources().size());
    assertEquals(null, transaction.getResourceSet().getResource(resource1.getURI(), false));
    assertEquals(resource2, transaction.getResourceSet().getResource(resource2.getURI(), false));
    assertEquals(null, transaction.getResourceSet().getResource(resource3.getURI(), false));

    transaction.commit();
    session.close();
  }

  public void testDuplicatePath() throws Exception
  {
    CDOSession session = openModel1Session();
    CDOTransaction transaction = session.openTransaction();
    transaction.createResource("/my/resource");
    transaction.commit();

    transaction.createResource("/my/resource");

    try
    {
      transaction.commit();
      fail("TransactionException expected");
    }
    catch (TransactionException expected)
    {
      // Success
    }
    finally
    {
      session.close();
    }
  }

  public void testDuplicatePathAfterDetach() throws Exception
  {
    CDOSession session = openModel1Session();
    CDOTransaction transaction = session.openTransaction();
    CDOResource resource = transaction.createResource("/my/resource");
    transaction.commit();

    resource.delete(null);
    transaction.commit();

    transaction.createResource("/my/resource");
    transaction.commit();
    session.close();
  }

  public void testChangePath() throws Exception
  {
    long commitTime1;
    long commitTime2;

    {
      CDOSession session = openModel1Session();
      CDOTransaction transaction = session.openTransaction();
      CDOResource resource = transaction.createResource("/my/resource");
      transaction.commit();
      commitTime1 = transaction.getLastCommitTime();

      resource.setPath("/renamed");
      transaction.commit();
      commitTime2 = transaction.getLastCommitTime();
      session.close();
    }

    CDOSession session = openModel1Session();
    CDOAudit audit1 = session.openAudit(commitTime1);
    assertEquals(true, audit1.hasResource("/my/resource"));
    assertEquals(false, audit1.hasResource("/renamed"));

    CDOAudit audit2 = session.openAudit(commitTime2);
    assertEquals(false, audit2.hasResource("/my/resource"));
    assertEquals(true, audit2.hasResource("/renamed"));
    session.close();
  }

  public void testChangeURI() throws Exception
  {
    long commitTime1;
    long commitTime2;

    {
      CDOSession session = openModel1Session();
      CDOTransaction transaction = session.openTransaction();
      CDOResource resource = transaction.createResource("/my/resource");
      transaction.commit();
      commitTime1 = transaction.getLastCommitTime();

      URI uri = URI.createURI("cdo://repo1/renamed");
      assertEquals(CDOURIUtil.createResourceURI(session, "/renamed"), uri);
      resource.setURI(uri);

      transaction.commit();
      commitTime2 = transaction.getLastCommitTime();
      session.close();
    }

    CDOSession session = openModel1Session();
    CDOAudit audit1 = session.openAudit(commitTime1);
    assertEquals(true, audit1.hasResource("/my/resource"));
    assertEquals(false, audit1.hasResource("/renamed"));

    CDOAudit audit2 = session.openAudit(commitTime2);
    assertEquals(false, audit2.hasResource("/my/resource"));
    assertEquals(true, audit2.hasResource("/renamed"));
    session.close();
  }

  public void testPathNotNull() throws Exception
  {
    {
      CDOSession session = openModel1Session();
      CDOTransaction transaction = session.openTransaction();
      CDOResource resource = transaction.createResource("/res1");
      assertEquals("/res1", resource.getPath());
      assertEquals(CDOURIUtil.createResourceURI(session, "/res1"), resource.getURI());

      transaction.commit();
      assertEquals("/res1", resource.getPath());
      assertEquals(CDOURIUtil.createResourceURI(session, "/res1"), resource.getURI());
      session.close();
    }

    {
      CDOSession session = openModel1Session();
      CDOTransaction transaction = session.openTransaction();
      CDOResource resource = transaction.getResource("/res1");
      assertEquals("/res1", resource.getPath());
      assertEquals(CDOURIUtil.createResourceURI(session, "/res1"), resource.getURI());

      CDOResource resource2 = transaction.getOrCreateResource("/res2");
      assertEquals("/res2", resource2.getPath());
      assertEquals(CDOURIUtil.createResourceURI(session, "/res2"), resource2.getURI());

      transaction.commit();
      assertEquals("/res2", resource2.getPath());
      assertEquals(CDOURIUtil.createResourceURI(session, "/res2"), resource2.getURI());
      session.close();
    }

    {
      CDOSession session = openModel1Session();
      CDOView view = session.openView();
      CDOResource resource2 = view.getResource("/res2");
      assertEquals("/res2", resource2.getPath());
      assertEquals(CDOURIUtil.createResourceURI(session, "/res2"), resource2.getURI());
      session.close();
    }
  }

  /**
   * http://bugs.eclipse.org/208689
   */
  public void testQueryResources() throws Exception
  {
    {
      CDOSession session = openModel1Session();
      CDOTransaction transaction = session.openTransaction();
      createResource(transaction, "/aresource");
      createResource(transaction, "/aaresource");
      createResource(transaction, "/abresource");
      createResource(transaction, "/acresource");
      createResource(transaction, "/adresource");
      createResource(transaction, "/aeresource");
      createResource(transaction, "/bresource");
      createResource(transaction, "/baresource");
      createResource(transaction, "/bbresource");
      createResource(transaction, "/bcresource");
      createResource(transaction, "/bdresource");
      createResource(transaction, "/beresource");
      createResource(transaction, "/bearesource");
      createResource(transaction, "/bebresource");
      createResource(transaction, "/cresource");
      createResource(transaction, "/caresource");
      createResource(transaction, "/caresource2");
      createResource(transaction, "/caresource3");
      createResource(transaction, "/cbresource");
      createResource(transaction, "/ccresource");
      createResource(transaction, "/cdresource");
      createResource(transaction, "/ceresource");
      transaction.commit();
      session.close();
    }

    CDOSession session = openModel1Session();
    CDOView view = session.openView();
    queryResources(view, "a", 6);
    queryResources(view, "b", 8);
    queryResources(view, "c", 8);
    queryResources(view, "be", 3);
    queryResources(view, "ca", 3);
    session.close();
  }

  /**
   * http://bugs.eclipse.org/208689
   */
  public void testQueryModifiedResources() throws Exception
  {
    {
      CDOSession session = openModel1Session();
      CDOTransaction transaction = session.openTransaction();
      createResource(transaction, "/aresource");
      createResource(transaction, "/aaresource");
      createResource(transaction, "/abresource");
      createResource(transaction, "/acresource");
      createResource(transaction, "/adresource");
      createResource(transaction, "/aeresource");
      createResource(transaction, "/bresource");
      createResource(transaction, "/baresource");
      createResource(transaction, "/bbresource");
      createResource(transaction, "/bcresource");
      createResource(transaction, "/bdresource");
      createResource(transaction, "/beresource");
      createResource(transaction, "/bearesource");
      createResource(transaction, "/bebresource");
      createResource(transaction, "/cresource");
      createResource(transaction, "/caresource");
      createResource(transaction, "/caresource2");
      createResource(transaction, "/caresource3");
      createResource(transaction, "/cbresource");
      createResource(transaction, "/ccresource");
      createResource(transaction, "/cdresource");
      createResource(transaction, "/ceresource");
      transaction.commit();
      modifyResource(transaction, "/aresource");
      modifyResource(transaction, "/aaresource");
      modifyResource(transaction, "/abresource");
      modifyResource(transaction, "/acresource");
      modifyResource(transaction, "/adresource");
      modifyResource(transaction, "/aeresource");
      modifyResource(transaction, "/bresource");
      modifyResource(transaction, "/baresource");
      modifyResource(transaction, "/bbresource");
      modifyResource(transaction, "/bcresource");
      modifyResource(transaction, "/bdresource");
      modifyResource(transaction, "/beresource");
      modifyResource(transaction, "/bearesource");
      modifyResource(transaction, "/bebresource");
      modifyResource(transaction, "/cresource");
      modifyResource(transaction, "/caresource");
      modifyResource(transaction, "/caresource2");
      modifyResource(transaction, "/caresource3");
      modifyResource(transaction, "/cbresource");
      modifyResource(transaction, "/ccresource");
      modifyResource(transaction, "/cdresource");
      modifyResource(transaction, "/ceresource");
      transaction.commit();
      session.close();
    }

    CDOSession session = openModel1Session();
    CDOView view = session.openView();
    queryResources(view, "a", 6);
    queryResources(view, "b", 8);
    queryResources(view, "c", 8);
    queryResources(view, "be", 3);
    queryResources(view, "ca", 3);
    session.close();
  }

  private CDOResource createResource(CDOTransaction transaction, String path)
  {
    Product1 p = getModel1Factory().createProduct1();
    p.setName("test-" + path);
    p.setVat(VAT.VAT0);

    CDOResource resource = transaction.createResource(path);
    resource.getContents().add(p);
    return resource;
  }

  private CDOResource modifyResource(CDOTransaction transaction, String path)
  {
    Product1 p = getModel1Factory().createProduct1();
    p.setName("test-" + path + "-modified");
    p.setVat(VAT.VAT0);

    CDOResource resource = transaction.getResource(path);
    resource.getContents().add(p);
    return resource;
  }

  private void queryResources(CDOView view, String pathPrefix, int expected)
  {
    msg("Path prefix: " + pathPrefix);
    List<CDOResourceNode> nodes = view.queryResources(null, pathPrefix, false);
    for (CDOResourceNode node : nodes)
    {
      msg("Result: " + node.getPath());
    }

    assertEquals(expected, nodes.size());
  }

  static class TestAdapter extends AdapterImpl
  {

    @Override
    public void notifyChanged(Notification msg)
    {
      super.notifyChanged(msg);
      if (msg.getNewValue() instanceof CDOResource)
      {
        ((CDOResource)msg.getNewValue()).getPath();
      }
    }

    @Override
    public void setTarget(Notifier newTarget)
    {

    }

    @Override
    public boolean isAdapterForType(Object type)
    {
      return super.isAdapterForType(type);
    }

  };

}
