/*
 * Copyright (c) 2016 Eike Stepper (Berlin, Germany) and others.
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
import org.eclipse.emf.cdo.common.lock.CDOLockState;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.tests.AbstractCDOTest;
import org.eclipse.emf.cdo.tests.model1.Company;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CDOUtil;

import org.eclipse.emf.internal.cdo.view.CDOViewImpl;

import org.eclipse.net4j.util.ReflectUtil;

import org.eclipse.emf.ecore.EObject;

import java.lang.reflect.Method;

/**
 * @author Eike Stepper
 */
public class Bugzilla_387563b_Test extends AbstractCDOTest
{
  private static final Method GET_LOCK_STATE_METHOD = ReflectUtil.getMethod(CDOViewImpl.class, "getLockState",
      CDOObject.class);

  public void testNoImplicitLockingOfNewObject() throws Exception
  {
    Company company = getModel1Factory().createCompany();

    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    transaction.options().setAutoReleaseLocksEnabled(true);

    CDOResource resource = transaction.createResource(getResourcePath("test1"));
    resource.getContents().add(company);

    CDOObject cdoObject = CDOUtil.getCDOObject(company);
    CDOLockState lockState = cdoObject.cdoLockState();
    assertNull(lockState.getWriteLockOwner());

    transaction.commit();
    assertNull(getLockState(cdoObject));
  }

  public void testExplicitLockingOfNewObject() throws Exception
  {
    Company company = getModel1Factory().createCompany();

    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    transaction.options().setAutoReleaseLocksEnabled(true);

    CDOResource resource = transaction.createResource(getResourcePath("test1"));
    resource.getContents().add(company);

    CDOObject cdoObject = CDOUtil.getCDOObject(company);
    cdoObject.cdoWriteLock().lock();

    CDOLockState lockState = cdoObject.cdoLockState();
    assertEquals(transaction, lockState.getWriteLockOwner());

    transaction.commit();
    assertNull(getLockState(cdoObject));
  }

  private static CDOLockState getLockState(EObject object)
  {
    CDOObject cdoObject = CDOUtil.getCDOObject(object);
    CDOViewImpl view = (CDOViewImpl)cdoObject.cdoView();

    return (CDOLockState)ReflectUtil.invokeMethod(GET_LOCK_STATE_METHOD, view, cdoObject);
  }
}
