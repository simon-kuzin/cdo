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
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.internal.common.revision.CDORevisionImpl;
import org.eclipse.emf.cdo.internal.common.revision.CDORevisionManagerImpl;
import org.eclipse.emf.cdo.internal.server.mem.MEMStore;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.StoreThreadLocal;
import org.eclipse.emf.cdo.spi.common.branch.InternalCDOBranchManager;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionManager;
import org.eclipse.emf.cdo.spi.server.InternalRepository;
import org.eclipse.emf.cdo.spi.server.InternalSession;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.spi.cdo.InternalCDOSession;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Eike Stepper
 */
public class RevisionManagerTest extends AbstractCDOTest
{
  private static final CDOID ID = CDOIDUtil.createLong(1);

  private static final EClass CLASS = EcorePackage.Literals.EANNOTATION;

  private static final int DETACH = -1;

  private InternalRepository repository;

  private MEMStore store;

  private InternalCDOSession session;

  private InternalSession serverSession;

  private InternalCDOBranchManager branchManager;

  private int branchID;

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

  private CDORevisionManagerImpl revisionManager;

  private static AtomicInteger loadCounter;

  /**
   * @author Eike Stepper
   */
  public static class ClientSide extends RevisionManagerTest
  {
    @Override
    protected InternalCDORevisionManager getRevisionManager(InternalRepository repository, InternalCDOSession session)
    {
      return session.getRevisionManager();
    }

    @Override
    protected String getLocation()
    {
      return "Client";
    }
  }

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

    repository = getRepository();
    store = (MEMStore)repository.getStore();

    session = (InternalCDOSession)openSession();
    serverSession = repository.getSessionManager().getSession(session.getSessionID());
    StoreThreadLocal.setSession(serverSession);

    branchManager = session.getBranchManager();
    branchID = 0;

    branch0 = branchManager.getMainBranch();
    revisions0 = fillBranch(branch0, 10, 10, 20, 50, 20, DETACH);

    revisions1 = createBranch(revisions0[1], 5, 10, 20, 10, DETACH);
    branch1 = revisions1[0].getBranch();

    revisions2 = createBranch(revisions1[3], 5, 20, 20);
    branch2 = revisions2[0].getBranch();

    revisions3 = new InternalCDORevision[0];
    branch3 = createBranch(revisions1[1]);

    revisions4 = createBranch(branch3, branch3.getBase().getTimeStamp() + 10, 30, DETACH);
    branch4 = revisions4[0].getBranch();

    BranchingTest.dump("MEMStore", store.getAllRevisions());
    revisionManager = (CDORevisionManagerImpl)getRevisionManager(repository, session);

    loadCounter = new AtomicInteger();
    revisionManager.loadCounterForTest = loadCounter;
  }

  @Override
  protected void doTearDown() throws Exception
  {
    StoreThreadLocal.release();
    session.close();
    super.doTearDown();
  }

  protected InternalCDORevisionManager getRevisionManager(InternalRepository repository, InternalCDOSession session)
  {
    return repository.getRevisionManager();
  }

  protected String getLocation()
  {
    return "Server";
  }

  private InternalCDORevision[] fillBranch(CDOBranch branch, long offset, long... durations)
  {
    InternalCDORevision[] revisions = new InternalCDORevision[durations.length];
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
    CDOBranch branch = baseBranch.createBranch("branch" + ++branchID, baseTimeStamp);
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
    return baseBranch.createBranch("branch" + ++branchID, baseTimeStamp);
  }

  private long getMiddleOfValidity(InternalCDORevision revision)
  {
    long timeStamp = revision.getTimeStamp();
    long revised = revision.getRevised();
    if (revised == CDOBranchPoint.UNSPECIFIED_DATE)
    {
      revised = timeStamp + 10000;
    }

    return timeStamp / 2 + revised / 2;
  }

  private InternalCDORevision getRevision(CDOBranch branch, long timeStamp)
  {
    CDOBranchPoint branchPoint = branch.getPoint(timeStamp);
    dumpCache(branchPoint);
    return (InternalCDORevision)revisionManager.getRevision(ID, branchPoint, CDORevision.UNCHUNKED,
        CDORevision.DEPTH_NONE, true);
  }

  private void dumpCache(CDOBranchPoint branchPoint)
  {
    BranchingTest.dump("Getting " + branchPoint + " from " + getLocation() + "Cache", revisionManager.getCache()
        .getAllRevisions());
  }

  private static void assertRevision(InternalCDORevision expected, InternalCDORevision actual)
  {
    assertEquals(expected, actual);
  }

  private static void assertLoads(int expected)
  {
    assertLoads(expected, true);
  }

  private static void assertLoads(int expected, boolean reset)
  {
    assertEquals(expected, loadCounter.get());
    if (reset)
    {
      loadCounter.set(0);
    }
  }

  public void testBranch0_Initial() throws Exception
  {
    long timeStamp = revisions0[0].getTimeStamp() - 1;

    InternalCDORevision revision = getRevision(branch0, timeStamp);
    assertRevision(null, revision);
    assertLoads(1);

    revision = getRevision(branch0, timeStamp);
    assertRevision(null, revision);
    assertLoads(0);
  }

  public void testBranch0_Normal() throws Exception
  {
    for (int i = 0; i < revisions0.length - 1; i++)
    {
      InternalCDORevision expected = revisions0[i];
      long timeStamp = getMiddleOfValidity(expected);

      InternalCDORevision revision = getRevision(branch0, timeStamp);
      assertRevision(expected, revision);
      assertLoads(1);

      revision = getRevision(branch0, timeStamp);
      assertRevision(expected, revision);
      assertLoads(0);
    }
  }

  public void testBranch0_Detached() throws Exception
  {
    long timeStamp = revisions0[4].getTimeStamp() + 1;

    InternalCDORevision revision = getRevision(branch0, timeStamp);
    assertRevision(null, revision);
    assertLoads(1);

    revision = getRevision(branch0, timeStamp);
    assertRevision(null, revision);
    assertLoads(0);
  }

  public void testBranch0_Head() throws Exception
  {
    long timeStamp = CDOBranchPoint.UNSPECIFIED_DATE;

    InternalCDORevision revision = getRevision(branch0, timeStamp);
    assertRevision(null, revision);
    assertLoads(1);

    revision = getRevision(branch0, timeStamp);
    assertRevision(null, revision);
    assertLoads(0);
  }

  public void testBranch1_Initial() throws Exception
  {
    long timeStamp = revisions1[0].getTimeStamp() - 1;

    InternalCDORevision revision = getRevision(branch1, timeStamp);
    assertRevision(null, revision);
    assertLoads(1);

    revision = getRevision(branch1, timeStamp);
    assertRevision(null, revision);
    assertLoads(0);
  }

  public void testBranch1_Normal() throws Exception
  {
    for (int i = 0; i < revisions1.length - 1; i++)
    {
      InternalCDORevision expected = revisions1[i];
      long timeStamp = getMiddleOfValidity(expected);

      InternalCDORevision revision = getRevision(branch1, timeStamp);
      assertRevision(expected, revision);
      assertLoads(1);

      revision = getRevision(branch1, timeStamp);
      assertRevision(expected, revision);
      assertLoads(0);
    }
  }

  public void testBranch1_Detached() throws Exception
  {
    long timeStamp = revisions1[3].getTimeStamp() + 1;

    InternalCDORevision revision = getRevision(branch1, timeStamp);
    assertRevision(null, revision);
    assertLoads(1);

    revision = getRevision(branch1, timeStamp);
    assertRevision(null, revision);
    assertLoads(0);
  }

  public void testBranch1_Head() throws Exception
  {
    long timeStamp = CDOBranchPoint.UNSPECIFIED_DATE;

    InternalCDORevision revision = getRevision(branch1, timeStamp);
    assertRevision(null, revision);
    assertLoads(1);

    revision = getRevision(branch1, timeStamp);
    assertRevision(null, revision);
    assertLoads(0);
  }
}
