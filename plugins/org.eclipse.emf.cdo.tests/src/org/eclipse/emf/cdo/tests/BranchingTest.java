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
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.session.CDOSession;

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

  public void testCreateBranch() throws Exception
  {
    CDOSession session = openSession1();
    CDOBranch mainBranch = session.getBranchManager().getMainBranch();
    CDOBranch branch = mainBranch.createBranch("testing");
    session.close();
  }

  /**
   * @author Eike Stepper
   */
  public static class SameSession extends BranchingTest
  {
    public void testRepositoryCreationTime() throws Exception
    {
      CDOSession session = openSession();
      long repositoryCreationTime = session.getRepositoryInfo().getCreationTime();
      assertEquals(getRepository().getCreationTime(), repositoryCreationTime);
      assertEquals(getRepository().getStore().getCreationTime(), repositoryCreationTime);
    }

    public void testRepositoryTime() throws Exception
    {
      CDOSession session = openSession();
      long repositoryTime = session.getRepositoryInfo().getTimeStamp();
      assertEquals(true, Math.abs(System.currentTimeMillis() - repositoryTime) < 500);
    }

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
