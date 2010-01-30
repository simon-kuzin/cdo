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
import org.eclipse.emf.cdo.common.branch.CDOBranchManager;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.internal.common.revision.CDORevisionImpl;
import org.eclipse.emf.cdo.internal.server.mem.MEMStore;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.spi.common.revision.DetachedCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import java.util.Map;

/**
 * @author Eike Stepper
 */
public class RevisionManagerTest extends AbstractCDOTest
{
  private static final long COMMIT_OFFSET = 1000;

  private static final long BRANCH_OFFSET = COMMIT_OFFSET * 2;

  private static final CDOID id = CDOIDUtil.createLong(1);

  private CDOSession session;

  private CDOBranchManager branchManager;

  private CDOBranch branch0;

  private CDOBranch branch1;

  private CDOBranch branch2;

  private CDOBranch branch3;

  private CDOBranch branch4;

  private MEMStore store;

  @Override
  public Map<String, Object> getTestProperties()
  {
    Map<String, Object> testProperties = super.getTestProperties();
    testProperties.put(IRepository.Props.SUPPORTING_AUDITS, "true");
    testProperties.put(IRepository.Props.SUPPORTING_BRANCHES, "true");
    return testProperties;
  }

  @Override
  protected void doSetUp() throws Exception
  {
    skipUnlessConfig(MEM);
    super.doSetUp();

    store = (MEMStore)getRepository().getStore();
    session = openSession();
    branchManager = session.getBranchManager();

    branch0 = branchManager.getMainBranch();
    InternalCDORevision[] revisions = fillBranch(branch0, 10, 10, 20, 50, 20);

    revisions = createBranch(revisions[1], 5, 10, 20, 10, -1);
    branch1 = revisions[0].getBranch();

    revisions = createBranch(revisions[3], 5, 20, 20);
    branch2 = revisions[0].getBranch();

    revisions = createBranch(revisions[1], 0);
    branch3 = revisions[0].getBranch();

    revisions = createBranch(revisions[1], 30, -1);
    branch4 = revisions[0].getBranch();

    BranchingTest.dump("MEMStore", store.getAllRevisions());
  }

  private InternalCDORevision[] fillBranch(CDOBranch branch, long offset, long... durations)
  {
    InternalCDORevision[] revisions = new InternalCDORevision[durations.length + 1];
    long timeStamp = branch.getBase().getTimeStamp() + offset;
    for (int i = 0; i < durations.length; i++)
    {
      long duration = durations[i];
      CDOBranchPoint branchPoint = branch.getPoint(timeStamp);

      if (duration == -1)
      {
        timeStamp += duration;

        revisions[i] = new CDORevisionImpl(null);
        revisions[i].setID(id);
        revisions[i].setBranchPoint(branchPoint);
        revisions[i].setRevised(timeStamp - 1);
        revisions[i].setVersion(i + 1);
      }
      else
      {
        revisions[i] = new DetachedCDORevision(id, branch, i + 1, timeStamp);
      }
    }

    return revisions;
  }

  private InternalCDORevision[] createBranch(InternalCDORevision revision, long offset, long... durations)
  {
    long revised = revision.getRevised();
    if (revised == CDOBranchPoint.UNSPECIFIED_DATE)
    {
      revised = revision.getTimeStamp() + 10000;
    }

    long timeStamp = revision.getTimeStamp() / 2 + revised / 2;

    CDOBranch parent = revision.getBranch();
    CDOBranch branch = parent.createBranch("branch" + (parent.getID() + 1), timeStamp);
    return fillBranch(branch, offset, durations);
  }

  @Override
  protected void doTearDown() throws Exception
  {
    session.close();
    super.doTearDown();
  }

  public void testMissingInBranch0() throws Exception
  {
  }
}
