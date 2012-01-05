/**
 * Copyright (c) 2004 - 2011 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.tests.bugzilla;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.CDOState;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.net4j.CDONet4jSession;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.tests.AbstractCDOTest;
import org.eclipse.emf.cdo.tests.model1.Category;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.emf.cdo.util.CommitException;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.spi.cdo.AbstractObjectConflictResolver;

import java.util.List;

/**
 * @author Egidijus Vaisnora
 */
public class Bugzilla_350120_Test extends AbstractCDOTest
{

  private static final String EXPECTED_TO_RECEIVE_JAVA_UTIL_CONCURRENT_MODIFICATION_EXCEPTION = "Expected to receive ConcurrentModificationException or IllegalStateException";

  public void testNewerVersionOnServer() throws CommitException
  {
    CDOSession user1Session = openSession();
    user1Session.options().setPassiveUpdateEnabled(false);

    CDOTransaction user1Transaction = user1Session.openTransaction();
    CDOResource createResource = user1Transaction.createResource(getResourcePath("test"));
    Category user1RootCategory = getModel1Factory().createCategory();
    createResource.getContents().add(user1RootCategory);
    user1Transaction.commit();

    // User 2
    CDONet4jSession user2Session = (CDONet4jSession)openSession();
    user2Session.options().setPassiveUpdateEnabled(false);
    CDOTransaction user2Transaction = user2Session.openTransaction();
    CDOObject user2RootCategory = user2Transaction.getObject(CDOUtil.getCDOObject(user1RootCategory).cdoID());

    // User 1
    Category user1ChildCategory = getModel1Factory().createCategory();
    user1RootCategory.getCategories().add(user1ChildCategory);
    user1Transaction.commit();

    // User 2
    EcoreUtil.delete(user2RootCategory);
    try
    {
      user2Transaction.commit();
      fail(EXPECTED_TO_RECEIVE_JAVA_UTIL_CONCURRENT_MODIFICATION_EXCEPTION);
    }
    catch (CommitException e)
    {
      boolean success = false;
      String message = e.getMessage();
      int index = message.indexOf('\n');
      if (index != -1)
      {
        String substring = message.substring(0, index);
        success = substring.contains("java.util.ConcurrentModificationException");
        if (!success)
        {
          // for non audit repository not concurrent modification exception is thrown
          success = substring.contains("java.lang.IllegalStateException");
        }
      }

      if (!success)
      {
        // EXPECTED_TO_RECEIVE_JAVA_UTIL_CONCURRENT_MODIFICATION_EXCEPTION
        throw e;
      }
    }
  }

  public void testConflict() throws CommitException
  {
    CDOSession user1Session = openSession();
    user1Session.options().setPassiveUpdateEnabled(false);

    CDOTransaction user1Transaction = user1Session.openTransaction();
    CDOResource createResource = user1Transaction.createResource(getResourcePath("test"));
    Category user1RootCategory = getModel1Factory().createCategory();
    createResource.getContents().add(user1RootCategory);
    user1Transaction.commit();

    // User 2
    CDOSession user2Session = openSession();
    user2Session.options().setPassiveUpdateEnabled(false);
    CDOTransaction user2Transaction = user2Session.openTransaction();
    CDOObject user2RootCatecory = user2Transaction.getObject(CDOUtil.getCDOObject(user1RootCategory).cdoID());

    // User 1
    Category user1ChildCategory = getModel1Factory().createCategory();
    user1RootCategory.getCategories().add(user1ChildCategory);
    user1Transaction.commit();

    // User 2
    EcoreUtil.delete(user2RootCatecory);
    assertEquals(CDOState.TRANSIENT, user2RootCatecory.cdoState());
    user2Transaction.options().addConflictResolver(new AbstractObjectConflictResolver()
    {

      @Override
      protected void resolveConflict(CDOObject conflict, CDORevision oldRemoteRevision, CDORevisionDelta localDelta,
          CDORevisionDelta remoteDelta, List<CDORevisionDelta> allRemoteDeltas)
      {
        // do nothing. It just test AbstractObjectConflictResolver from NPE
      }

    });
    user2Session.refresh();

    boolean expected = true;
    if (expected)
    {
      assertEquals(CDOState.CONFLICT, user2RootCatecory.cdoState());
    }
    else
    {
      // current flow
      assertEquals(CDOState.TRANSIENT, user2RootCatecory.cdoState());
      CDOObject user2ChildCategory = user2Transaction.getObject(CDOUtil.getCDOObject(user1ChildCategory).cdoID());
      assertEquals(CDOState.CLEAN, user2ChildCategory.cdoState());
      user2Transaction.commit();

      // User 1
      user1Session.refresh();
      assertEquals(CDOState.CLEAN, CDOUtil.getCDOObject(user1ChildCategory).cdoState());
    }

    user2Session.close();
    user1Session.close();
  }

  public void testMoveToOtherRepository() throws CommitException
  {
    CDOSession user1Session = openSession();
    user1Session.options().setPassiveUpdateEnabled(false);

    CDOTransaction user1Transaction = user1Session.openTransaction();
    CDOResource createResource = user1Transaction.createResource(getResourcePath("test"));
    Category user1RootCategory = getModel1Factory().createCategory();
    createResource.getContents().add(user1RootCategory);
    user1Transaction.commit();

    // User 2
    CDOSession user2Session = openSession();
    user2Session.options().setPassiveUpdateEnabled(false);
    CDOTransaction user2Transaction = user2Session.openTransaction();
    CDOObject user2RootCatecory = user2Transaction.getObject(CDOUtil.getCDOObject(user1RootCategory).cdoID());

    // User 1
    Category user1ChildCategory = getModel1Factory().createCategory();
    user1RootCategory.getCategories().add(user1ChildCategory);
    user1Transaction.commit();

    // User 2
    EcoreUtil.delete(user2RootCatecory);
    assertEquals(CDOState.TRANSIENT, user2RootCatecory.cdoState());

    getRepository("repo2");
    CDOSession repo2Session = openSession("repo2");
    CDOTransaction user3Repo2Transaction = repo2Session.openTransaction();
    CDOResource repo2Resource = user3Repo2Transaction.createResource(getResourcePath("repo2Res"));
    repo2Resource.getContents().add(user2RootCatecory);
    assertEquals(CDOState.NEW, user2RootCatecory.cdoState());

    user3Repo2Transaction.commit();
    assertEquals(CDOState.CLEAN, user2RootCatecory.cdoState());

    user2Session.refresh();

    boolean expected = true;
    if (expected)
    {
      assertEquals(CDOState.CLEAN, user2RootCatecory.cdoState());
      assertEquals(user3Repo2Transaction, user2RootCatecory.cdoView());

    }
    else
    {
      // current flow - object was invalidated updating another session
      assertEquals(CDOState.PROXY, user2RootCatecory.cdoState());
      assertEquals(user3Repo2Transaction, user2RootCatecory.cdoView());
    }

    // make rollback
    user2Transaction.rollback();
    if (expected)
    {
      assertEquals(CDOState.CLEAN, user2RootCatecory.cdoState());
      assertEquals(user3Repo2Transaction, user2RootCatecory.cdoView());

    }
    else
    {
      // current flow - object switched transaction
      assertEquals(CDOState.CLEAN, user2RootCatecory.cdoState());
      assertEquals(user2Transaction, user2RootCatecory.cdoView());
    }

    user2Session.close();
    user1Session.close();
  }

}
