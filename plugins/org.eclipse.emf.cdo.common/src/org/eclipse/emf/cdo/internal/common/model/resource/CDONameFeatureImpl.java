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
package org.eclipse.emf.cdo.internal.common.model.resource;

import org.eclipse.emf.cdo.common.model.EClass;
import org.eclipse.emf.cdo.common.model.resource.CDONameFeature;
import org.eclipse.emf.cdo.internal.common.model.EStructuralFeatureImpl;
import org.eclipse.emf.cdo.internal.common.model.CDOTypeImpl;

/**
 * @author Eike Stepper
 */
public class CDONameFeatureImpl extends EStructuralFeatureImpl implements CDONameFeature
{
  public CDONameFeatureImpl(EClass containingClass)
  {
    super(containingClass, FEATURE_ID, NAME, CDOTypeImpl.STRING, null, false);
  }
}
