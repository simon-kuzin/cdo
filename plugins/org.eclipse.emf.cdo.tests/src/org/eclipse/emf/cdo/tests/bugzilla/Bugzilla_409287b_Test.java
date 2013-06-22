/*
 * Copyright (c) 2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.tests.bugzilla;

import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.tests.AbstractCDOTest;
import org.eclipse.emf.cdo.tests.model1.Category;
import org.eclipse.emf.cdo.tests.model1.Company;
import org.eclipse.emf.cdo.transaction.CDOTransaction;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EReference;

/**
 * Bug 409287: ArrayIndexOutOfBoundsException on rollback
 *
 * @author Jack Lechner
 */
public class Bugzilla_409287b_Test extends AbstractCDOTest
{
  public void testIsSetAfterRollback() throws Exception
  {
    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    CDOResource resource = transaction.createResource(getResourcePath("/test"));
    Company company = getModel1Factory().createCompany();
    resource.getContents().add(company);
    transaction.commit();

    Category category = getModel1Factory().createCategory();
    company.getCategories().add(category);

    transaction.rollback();

    EReference feature = getModel1Package().getCategory_Categories();
    category.eIsSet(feature);
  }

  public void testListenersOnRollback() throws Exception
  {
    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    CDOResource resource = transaction.createResource(getResourcePath("/test"));
    Company company = getModel1Factory().createCompany();
    resource.getContents().add(company);
    transaction.commit();

    final Category category = getModel1Factory().createCategory();
    company.getCategories().add(category);

    // Add regular EMF adapter
    company.eAdapters().add(new AdapterImpl()
    {
      @Override
      public void notifyChanged(Notification notification)
      {
        try
        {
          EReference feature = getModel1Package().getCategory_Categories();
          category.eIsSet(feature);
        }
        catch (ArrayIndexOutOfBoundsException ex)
        {
          // Found my exception
          throw ex;
        }
      }
    });

    transaction.rollback();
  }
}
