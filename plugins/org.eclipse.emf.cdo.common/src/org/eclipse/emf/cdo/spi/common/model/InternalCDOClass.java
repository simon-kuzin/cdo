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
package org.eclipse.emf.cdo.spi.common.model;

import org.eclipse.emf.cdo.common.model.EClass;
import org.eclipse.emf.cdo.common.model.EClassProxy;
import org.eclipse.emf.cdo.common.model.EClassRef;
import org.eclipse.emf.cdo.common.model.EStructuralFeature;
import org.eclipse.emf.cdo.common.model.EPackage;

import java.util.List;

/**
 * @author Eike Stepper
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
public interface InternalEClass extends EClass, InternalEModelElement
{
  public void setContainingPackage(EPackage containingPackage);

  public void addSuperType(EClassRef classRef);

  public void addFeature(EStructuralFeature cdoFeature);

  public int getFeatureIndex(int featureID);

  public List<EClassProxy> getSuperTypeProxies();
}
