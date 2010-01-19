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
package org.eclipse.emf.cdo.tests;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchCreatedEvent;
import org.eclipse.emf.cdo.common.branch.CDOBranchManager;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.commit.CDOCommit;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.server.IMEMStore;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.tests.model1.OrderDetail;
import org.eclipse.emf.cdo.tests.model1.Product1;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.view.CDOView;

import org.eclipse.net4j.util.event.IEvent;
import org.eclipse.net4j.util.event.IListener;

import java.util.Map;

/**
 * @author Eike Stepper
 */
public class BranchingTest extends AbstractCDOTest
{
  protected CDOSession session1;

  @Override
  public Map<String, Object> getTestProperties()
  {
    Map<String, Object> testProperties = super.getTestProperties();
    testProperties.put(IRepository.Props.SUPPORTING_AUDITS, "true");
    testProperties.put(IRepository.Props.SUPPORTING_BRANCHES, "true");
    return testProperties;
  }

  protected CDOSession openSession1()
  {
    session1 = openModel1Session();
    return session1;
  }

  protected void closeSession1()
  {
    session1.close();
  }

  protected CDOSession openSession2()
  {
    return openModel1Session();
  }

  public void testMainBranch() throws Exception
  {
    CDOSession session = openSession1();
    CDOBranch mainBranch = session.getBranchManager().getMainBranch();
    assertEquals(CDOBranch.MAIN_BRANCH_ID, mainBranch.getID());
    assertEquals(CDOBranch.MAIN_BRANCH_NAME, mainBranch.getName());
    assertEquals(null, mainBranch.getBase().getBranch());
    assertEquals(0, mainBranch.getBranches().length);
    closeSession1();

    session = openSession2();
    mainBranch = session.getBranchManager().getBranch(CDOBranch.MAIN_BRANCH_ID);
    assertEquals(CDOBranch.MAIN_BRANCH_ID, mainBranch.getID());
    assertEquals(CDOBranch.MAIN_BRANCH_NAME, mainBranch.getName());
    assertEquals(null, mainBranch.getBase().getBranch());
    assertEquals(0, mainBranch.getBranches().length);
    session.close();
  }

  public void testCreateBranch() throws Exception
  {
    CDOSession session = openSession1();
    CDOBranch mainBranch = session.getBranchManager().getMainBranch();
    CDOBranch branch = mainBranch.createBranch("testing");
    assertEquals(CDOBranch.MAIN_BRANCH_ID + 1, branch.getID());
    assertEquals("testing", branch.getName());
    assertEquals(CDOBranch.MAIN_BRANCH_ID, branch.getBase().getBranch().getID());
    assertEquals(0, branch.getBranches().length);
    session.close();
  }

  public void testGetBranch() throws Exception
  {
    CDOSession session = openSession1();
    CDOBranch mainBranch = session.getBranchManager().getMainBranch();
    CDOBranch branch = mainBranch.createBranch("testing");
    closeSession1();

    session = openSession2();
    branch = session.getBranchManager().getBranch(CDOBranch.MAIN_BRANCH_ID + 1);
    assertEquals(CDOBranch.MAIN_BRANCH_ID + 1, branch.getID());
    assertEquals("testing", branch.getName());
    assertEquals(CDOBranch.MAIN_BRANCH_ID, branch.getBase().getBranch().getID());
    assertEquals(0, branch.getBranches().length);
    session.close();
  }

  public void testGetSubBranches() throws Exception
  {
    CDOSession session = openSession1();
    CDOBranch mainBranch = session.getBranchManager().getMainBranch();
    mainBranch.createBranch("testing1");
    mainBranch.createBranch("testing2");
    closeSession1();

    session = openSession2();
    mainBranch = session.getBranchManager().getMainBranch();
    CDOBranch[] branches = mainBranch.getBranches();
    assertEquals(2, branches.length);
    assertEquals("testing1", branches[0].getName());
    assertEquals(CDOBranch.MAIN_BRANCH_ID, branches[0].getBase().getBranch().getID());
    assertEquals("testing2", branches[1].getName());
    assertEquals(CDOBranch.MAIN_BRANCH_ID, branches[1].getBase().getBranch().getID());
    session.close();
  }

  public void testEvent() throws Exception
  {
    CDOSession session1 = openSession1();
    CDOSession session2 = openSession2();

    final AsyncResult<CDOBranch> result = new AsyncResult<CDOBranch>();
    session2.getBranchManager().addListener(new IListener()
    {
      public void notifyEvent(IEvent event)
      {
        if (event instanceof CDOBranchCreatedEvent)
        {
          CDOBranchCreatedEvent e = (CDOBranchCreatedEvent)event;
          result.setValue(e.getBranch());
        }
      }
    });

    CDOBranch mainBranch = session1.getBranchManager().getMainBranch();
    CDOBranch branch = mainBranch.createBranch("testing");
    CDOBranch resultBranch = result.getValue();
    assertEquals(branch, resultBranch);

    closeSession1();
    session2.close();
  }

  public void testGetPath() throws Exception
  {
    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();
    CDOBranch mainBranch = branchManager.getMainBranch();
    CDOBranch testing1 = mainBranch.createBranch("testing1");
    CDOBranch testing2 = mainBranch.createBranch("testing2");
    CDOBranch subsub = testing1.createBranch("subsub");
    closeSession1();

    session = openSession2();
    branchManager = session.getBranchManager();
    mainBranch = branchManager.getMainBranch();
    assertEquals(mainBranch, branchManager.getBranch("MAIN"));
    assertEquals(testing1, branchManager.getBranch("MAIN/testing1"));
    assertEquals(testing2, branchManager.getBranch("MAIN/testing2"));
    assertEquals(subsub, branchManager.getBranch("MAIN/testing1/subsub"));
    assertEquals(testing1, mainBranch.getBranch("testing1"));
    assertEquals(testing2, mainBranch.getBranch("testing2"));
    assertEquals(subsub, mainBranch.getBranch("testing1/subsub"));
    assertEquals(subsub, testing1.getBranch("subsub"));
    session.close();
  }

  public void testCommit() throws Exception
  {
    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();

    // Commit to main branch
    CDOBranch mainBranch = branchManager.getMainBranch();
    CDOTransaction transaction = session.openTransaction(mainBranch);
    assertEquals(mainBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    Product1 product = getModel1Factory().createProduct1();
    product.setName("CDO");

    OrderDetail orderDetail = getModel1Factory().createOrderDetail();
    orderDetail.setProduct(product);
    orderDetail.setPrice(5);

    CDOResource resource = transaction.createResource("/res");
    resource.getContents().add(product);
    resource.getContents().add(orderDetail);

    CDOCommit commit = transaction.commit();
    assertEquals(mainBranch, commit.getBranch());
    long commitTime1 = commit.getTimeStamp();
    transaction.close();

    // Commit to sub branch
    CDOBranch subBranch = mainBranch.createBranch("subBranch", commitTime1);
    transaction = session.openTransaction(subBranch);
    assertEquals(subBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    resource = transaction.getResource("/res");
    orderDetail = (OrderDetail)resource.getContents().get(1);
    assertEquals(5.0f, orderDetail.getPrice());
    product = orderDetail.getProduct();
    assertEquals("CDO", product.getName());

    orderDetail.setPrice(10);
    commit = transaction.commit();
    assertEquals(subBranch, commit.getBranch());
    long commitTime2 = commit.getTimeStamp();
    transaction.close();
    closeSession1();

    session = openSession2();
    branchManager = session.getBranchManager();
    mainBranch = branchManager.getMainBranch();
    subBranch = mainBranch.getBranch("subBranch");

    // check(session, mainBranch, commitTime1, 5, "CDO");
    // check(session, mainBranch, commitTime2, 5, "CDO");
    // check(session, mainBranch, CDOBranchPoint.UNSPECIFIED_DATE, 5, "CDO");
    // check(session, subBranch, commitTime1, 5, "CDO");
    IMEMStore store = (IMEMStore)getRepository().getStore();
    store.dumpAllRevisions(System.out);

    check(session, subBranch, commitTime2, 10, "CDO");
    check(session, subBranch, CDOBranchPoint.UNSPECIFIED_DATE, 10, "CDO");
    session.close();
  }

  private void check(CDOSession session, CDOBranch branch, long timeStamp, float price, String name)
  {
    CDOView view = session.openView(branch, timeStamp);
    CDOResource resource = view.getResource("/res");

    OrderDetail orderDetail = (OrderDetail)resource.getContents().get(1);
    assertEquals(price, orderDetail.getPrice());

    Product1 product = orderDetail.getProduct();
    assertEquals(name, product.getName());

    view.close();
  }

  /**
   * @author Eike Stepper
   */
  public static class SameSession extends BranchingTest
  {
    @Override
    protected void closeSession1()
    {
      // Do nothing
    }

    @Override
    protected CDOSession openSession2()
    {
      return session1;
    }
  }
}
