/*
 * Copyright (c) 2004-2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.tests.bugzilla;

import org.eclipse.emf.cdo.common.CDOCommonSession.Options.PassiveUpdateMode;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.security.Access;
import org.eclipse.emf.cdo.security.Group;
import org.eclipse.emf.cdo.security.Realm;
import org.eclipse.emf.cdo.security.ResourcePermission;
import org.eclipse.emf.cdo.security.Role;
import org.eclipse.emf.cdo.security.SecurityFactory;
import org.eclipse.emf.cdo.security.User;
import org.eclipse.emf.cdo.server.security.ISecurityManager;
import org.eclipse.emf.cdo.server.security.SecurityManagerUtil;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.session.CDOSessionInvalidationEvent;
import org.eclipse.emf.cdo.tests.AbstractCDOTest;
import org.eclipse.emf.cdo.tests.config.impl.ConfigTest.CleanRepositoriesAfter;
import org.eclipse.emf.cdo.tests.config.impl.ConfigTest.CleanRepositoriesBefore;
import org.eclipse.emf.cdo.tests.config.impl.RepositoryConfig;
import org.eclipse.emf.cdo.tests.config.impl.SessionConfig;
import org.eclipse.emf.cdo.tests.model1.Category;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ConcurrentAccessException;

import org.eclipse.net4j.util.event.IEvent;
import org.eclipse.net4j.util.event.IListener;
import org.eclipse.net4j.util.security.IPasswordCredentials;
import org.eclipse.net4j.util.security.IPasswordCredentialsProvider;
import org.eclipse.net4j.util.security.PasswordCredentials;
import org.eclipse.net4j.util.security.PasswordCredentialsProvider;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests ensuring that the permissions are correctly computed, no matter what passive update mode is chosen.
 * @author Alex Lagarde <alex.lagarde@obeo.fr>
 */
@CleanRepositoriesBefore
@CleanRepositoriesAfter
public class Bugzilla_417483_Test extends AbstractCDOTest
{

  /**
   * Login for use with write rights.
   */
  private static final String USER_WITH_WRITE_RIGHTS_ID = "Stepper";

  /**
   * Password for use with write rights.
   */
  private static final String USER_WITH_WRITE_RIGHTS_PASSWORD = "12345";

  /**
   * Login for use without write rights.
   */
  private static final String USER_WITHOUT_WRITE_RIGHTS_ID = "Lagarde";

  /**
   * Password for use without write rights.
   */
  private static final String USER_WITHOUT_WRITE_RIGHTS_PASSWORD = "54321";

  /**
   * Boolean used by the 
   */
  private boolean loginWithUserWithoutRights;

  @Override
  protected void doSetUp() throws Exception
  {
    super.doSetUp();

    // Define 2 users:
    // - one with all rights
    // - one without write rights (but all read rights)
    ISecurityManager securityManager = startRepository();
    securityManager.modify(new ISecurityManager.RealmOperation()
    {
      public void execute(Realm realm)
      {
        // User with write rights
        User userwithWriteRights = realm.addUser(USER_WITH_WRITE_RIGHTS_ID, USER_WITH_WRITE_RIGHTS_PASSWORD);
        userwithWriteRights.getGroups().add(realm.getGroup("Users"));
        userwithWriteRights.getRoles().add(realm.getRole("All Objects Writer"));

        // User with only read rights
        ResourcePermission readlAllPermision = SecurityFactory.eINSTANCE.createResourcePermission();
        readlAllPermision.setPattern(".*");
        readlAllPermision.setAccess(Access.READ);
        Role roleWithoutWriteRights = realm.addRole("User without Write - Role");
        roleWithoutWriteRights.getPermissions().add(readlAllPermision);
        Group groupWithoutWriteRights = realm.addGroup("User without Write - Group");
        groupWithoutWriteRights.getRoles().add(roleWithoutWriteRights);
        User userwithoutWriteRights = realm.addUser(USER_WITHOUT_WRITE_RIGHTS_ID, USER_WITHOUT_WRITE_RIGHTS_PASSWORD);
        userwithoutWriteRights.getGroups().add(groupWithoutWriteRights);
      }
    });
  }

  /**
   * Ensures that the permissions are correctly computed when integrating changes with the {@link PassiveUpdateMode#CHANGES} update mode.
   */
  public void testPermissionWithChangesPassiveUpdateMode() throws Exception
  {
    doTestPermissionWithPassiveUpdates(PassiveUpdateMode.CHANGES);
  }

  /**
   * Ensures that the permissions are correctly computed when integrating changes with the {@link PassiveUpdateMode#ADDITIONS} update mode.
   */
  public void testPermissionWithAdditionPassiveUpdateMode() throws Exception
  {
    doTestPermissionWithPassiveUpdates(PassiveUpdateMode.ADDITIONS);
  }

  /**
   * Ensures that the permissions are correctly computed when integrating changes with the {@link PassiveUpdateMode#INVALIDATIONS} update mode.
   */
  public void testPermissionWithInvalidationPassiveUpdateMode() throws Exception
  {
    doTestPermissionWithPassiveUpdates(PassiveUpdateMode.INVALIDATIONS);
  }

  /**
   * Ensures that the permissions are correctly computed, with the given {@link PassiveUpdateMode}.
   */
  protected void doTestPermissionWithPassiveUpdates(PassiveUpdateMode passiveUpdateMode)
      throws ConcurrentAccessException, CommitException, InterruptedException
  {
    // Step 1: Both users open transaction on the repository
    CDOSession userWithWriteRightSession = openSession();
    CDOTransaction userWithWriteRightTransaction = userWithWriteRightSession.openTransaction();
    loginWithUserWithoutRights = true;
    CDOSession userWithoutWriteRightSession = openSession();
    userWithoutWriteRightSession.options().setPassiveUpdateMode(passiveUpdateMode);

    CDOTransaction userWithoutWriteRightTransaction = userWithoutWriteRightSession.openTransaction();
    assertEquals(USER_WITH_WRITE_RIGHTS_ID, userWithWriteRightSession.getUserID());
    assertEquals(USER_WITHOUT_WRITE_RIGHTS_ID, userWithoutWriteRightSession.getUserID());

    // Step 2: User with write rights creates a resource and commits
    CDOResource resource = userWithWriteRightTransaction.createResource(getResourcePath("/res"));
    assertEquals(true, resource.cdoRevision().isWritable());
    Category resourceRoot = getModel1Factory().createCategory();
    resource.getContents().add(resourceRoot);
    userWithWriteRightTransaction.commit();

    // => User without rights should be able to integrate changes without permission issues
    CDOResource resourceWithoutWrite = userWithoutWriteRightTransaction.getResource(getResourcePath("/res"));
    assertEquals(1, resourceWithoutWrite.getContents().size());
    // => Trigger loading of resource root so that invalidation are sent
    resourceWithoutWrite.getContents().iterator().next();

    // Step 3: User with write rights modifies the resource root and commits
    resourceRoot.setName("RENAMMED");
    final CountDownLatch latch = new CountDownLatch(1);
    userWithoutWriteRightSession.addListener(new IListener()
    {
      public void notifyEvent(IEvent event)
      {
        if (event instanceof CDOSessionInvalidationEvent)
        {
          latch.countDown();
        }
      }
    });
    userWithWriteRightTransaction.commit();

    // => User without rights should be able to integrate changes without permission issues
    boolean notified = latch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    assertEquals("Timeout: user without Write Rights should have been notified of changes", true, notified);
  }

  /**
   * Starts a repository with security enabled.
   * @return the {@link ISecurityManager} associated to the started repository
   */
  private ISecurityManager startRepository()
  {
    // Create & register security manager
    ISecurityManager securityManager = SecurityManagerUtil.createSecurityManager("/security", getServerContainer());
    getTestProperties().put(RepositoryConfig.PROP_TEST_SECURITY_MANAGER, securityManager);

    // Create & register a credential provider allowing to logging with one user or another
    IPasswordCredentialsProvider credentialsProvider = new PasswordCredentialsProvider(USER_WITH_WRITE_RIGHTS_ID,
        USER_WITH_WRITE_RIGHTS_PASSWORD)
    {
      @Override
      public IPasswordCredentials getCredentials()
      {
        if (loginWithUserWithoutRights)
        {
          return new PasswordCredentials(USER_WITHOUT_WRITE_RIGHTS_ID, USER_WITHOUT_WRITE_RIGHTS_PASSWORD.toCharArray());
        }
        return super.getCredentials();
      }
    };
    getTestProperties().put(SessionConfig.PROP_TEST_CREDENTIALS_PROVIDER, credentialsProvider);

    // Start repository
    getRepository();

    return securityManager;
  }

}
