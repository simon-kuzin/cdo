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
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcorePackage;

import java.util.Map;

/**
 * @author Eike Stepper
 */
public class RevisionManagerTest extends AbstractCDOTest
{
  private static final CDOID ID = CDOIDUtil.createLong(1);

  private static final EClass CLASS = EcorePackage.Literals.EANNOTATION;

  private static final int DETACH = -1;

  private MEMStore store;

  private CDOSession session;

  private CDOBranchManager branchManager;

  private CDOBranch branch0;

  private CDOBranch branch1;

  private CDOBranch branch2;

  private CDOBranch branch3;

  private CDOBranch branch4;

  private InternalCDORevision[] revisions0;

  private InternalCDORevision[] revisions1;

  private InternalCDORevision[] revisions2;

  private InternalCDORevision[] revisions3;

  private InternalCDORevision[] revisions4;

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
    revisions0 = fillBranch(branch0, 10, 10, 20, 50, 20);

    revisions1 = createBranch(revisions0[1], 5, 10, 20, 10, DETACH);
    branch1 = revisions0[0].getBranch();

    revisions2 = createBranch(revisions1[3], 5, 20, 20);
    branch2 = revisions2[0].getBranch();

    revisions3 = new InternalCDORevision[0];
    branch3 = createBranch(revisions1[1]);

    revisions4 = createBranch(branch3, 10, 30, DETACH);
    branch4 = revisions4[0].getBranch();

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

      if (duration != DETACH)
      {
        timeStamp += duration;

        revisions[i] = new CDORevisionImpl(CLASS);
        revisions[i].setID(ID);
        revisions[i].setBranchPoint(branchPoint);
        revisions[i].setRevised(timeStamp - 1);
        revisions[i].setVersion(i + 1);
        store.addRevision(revisions[i]);
      }
      else
      {
        revisions[i] = store.detachObject(ID, branch, timeStamp - 1);
      }
    }

    return revisions;
  }

  private InternalCDORevision[] createBranch(CDOBranch baseBranch, long baseTimeStamp, long offset, long... durations)
  {
    CDOBranch branch = baseBranch.createBranch("branch" + (baseBranch.getID() + 1), baseTimeStamp);
    return fillBranch(branch, offset, durations);
  }

  private InternalCDORevision[] createBranch(InternalCDORevision revision, long offset, long... durations)
  {
    CDOBranch baseBranch = revision.getBranch();
    long baseTimeStamp = getMiddleOfValidity(revision);
    return createBranch(baseBranch, baseTimeStamp, offset, durations);
  }

  private CDOBranch createBranch(InternalCDORevision revision)
  {
    CDOBranch baseBranch = revision.getBranch();
    long baseTimeStamp = getMiddleOfValidity(revision);
    return baseBranch.createBranch("branch" + (baseBranch.getID() + 1), baseTimeStamp);
  }

  // private CDOBranch createBranch(CDOBranch parent)
  // {
  // long revised = revision.getRevised();
  // if (revised == CDOBranchPoint.UNSPECIFIED_DATE)
  // {
  // revised = revision.getTimeStamp() + 10000;
  // }
  //
  // long timeStamp = revision.getTimeStamp() / 2 + revised / 2;
  //
  // CDOBranch parent = revision.getBranch();
  // CDOBranch branch = parent.createBranch("branch" + (parent.getID() + 1), timeStamp);
  // return fillBranch(branch, offset, durations);
  // }

  private long getMiddleOfValidity(InternalCDORevision revision)
  {
    long revised = revision.getRevised();
    if (revised == CDOBranchPoint.UNSPECIFIED_DATE)
    {
      revised = revision.getTimeStamp() + 10000;
    }

    long baseTimeStamp = revision.getTimeStamp() / 2 + revised / 2;
    return baseTimeStamp;
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
