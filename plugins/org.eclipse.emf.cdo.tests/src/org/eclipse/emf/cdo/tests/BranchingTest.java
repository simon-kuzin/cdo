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
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.session.CDOSession;

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
    mainBranch.createBranch("testing1").createBranch("subsub");
    mainBranch.createBranch("testing2");
    closeSession1();

    session = openSession2();
    mainBranch = branchManager.getMainBranch();
    assertEquals(mainBranch, branchManager.getBranch("/"));
    session.close();
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
