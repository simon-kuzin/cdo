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
package org.eclipse.emf.cdo.internal.common.model;

import org.eclipse.emf.cdo.common.model.CDOClassAdapter;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * @author Eike Stepper
 */
public class CDOClassAdapterImpl extends AdapterImpl implements CDOClassAdapter
{
  private EStructuralFeature[] allPersistentFeatures;

  public CDOClassAdapterImpl()
  {
  }

  @Override
  public boolean isAdapterForType(Object type)
  {
    return EClass.class.isInstance(type);
  }

  public EClass getEClass()
  {
    return (EClass)getTarget();
  }

  public EStructuralFeature[] getAllPersistentFeatures()
  {
    return allPersistentFeatures;
  }

  public void setAllPersistentFeatures(EStructuralFeature[] allPersistentFeatures)
  {
    this.allPersistentFeatures = allPersistentFeatures;
  }
}
