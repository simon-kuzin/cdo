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
package org.eclipse.emf.cdo.internal.common.model.core;

import org.eclipse.emf.cdo.common.model.EPackage;
import org.eclipse.emf.cdo.common.model.core.CDOObjectClass;
import org.eclipse.emf.cdo.internal.common.model.EClassImpl;

/**
 * @author Eike Stepper
 */
public class CDOObjectClassImpl extends EClassImpl implements CDOObjectClass
{
  public CDOObjectClassImpl(EPackage containingPackage)
  {
    super(containingPackage, CLASSIFIER_ID, NAME, false);
  }

  @Override
  public boolean isRoot()
  {
    return true;
  }
}
