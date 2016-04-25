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
package org.eclipse.emf.cdo.tests.db;

import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.tests.AbstractCDOTest;
import org.eclipse.emf.cdo.transaction.CDOTransaction;

/**
 * @author Eike Stepper
 */
public class Bugzilla_XXXXXX_Test extends AbstractCDOTest
{
  @CleanRepositoriesBefore(reason = "Package mapping")
  public void testConsecutivePackageAdditions() throws Exception
  {
    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    CDOResource resource = transaction.createResource(getResourcePath("/my/resource"));

    resource.getContents().add(getModel1Factory().createCompany());
    transaction.commit();

    session.close();
    restartRepository();
    session = openSession();
    transaction = session.openTransaction();
    resource = transaction.getResource(getResourcePath("/my/resource"));

    resource.getContents().add(getModel2Factory().createSpecialPurchaseOrder());
    resource.getContents().add(getModel1Factory().createCompany());
    transaction.commit();
  }
}
