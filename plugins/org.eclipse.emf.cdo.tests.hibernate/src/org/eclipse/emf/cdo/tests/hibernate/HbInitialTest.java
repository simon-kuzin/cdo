/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Simon McDuff - initial API and implementation
 *    Eike Stepper - maintenance
 */
package org.eclipse.emf.cdo.tests.hibernate;

import org.eclipse.emf.cdo.tests.InitialTest;
import org.eclipse.emf.cdo.tests.StoreRepositoryProvider;

/**
 * @author Martin Taal
 */
public class HbInitialTest extends InitialTest
{
  public HbInitialTest()
  {
    StoreRepositoryProvider.setInstance(HbStoreRepositoryProvider.getInstance());
  }
}
