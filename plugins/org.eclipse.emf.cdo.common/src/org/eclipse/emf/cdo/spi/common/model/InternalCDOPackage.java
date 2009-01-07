/***************************************************************************
 * Copyright (c) 2004 - 2008 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo.spi.common.model;

import org.eclipse.emf.cdo.common.id.CDOIDMetaRange;
import org.eclipse.emf.cdo.common.model.CDOClassifier;
import org.eclipse.emf.cdo.common.model.CDOPackage;
import org.eclipse.emf.cdo.common.model.CDOPackageManager;

import java.util.List;

/**
 * @author Eike Stepper
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
public interface InternalCDOPackage extends CDOPackage, InternalCDONamedElement
{
  public State getState();

  public void setState(State state);

  public void setPackageManager(CDOPackageManager packageManager);

  public void setPackageURI(String packageURI);

  public void setParentURI(String parentURI);

  public void setDynamic(boolean dynamic);

  public void setMetaIDRange(CDOIDMetaRange metaIDRange);

  public void setEcore(String ecore);

  public String basicGetEcore();

  public void setClassifiers(List<CDOClassifier> classifiers);

  public void addClassifier(CDOClassifier cdoClassifier);

  /**
   * @author Eike Stepper
   */
  public enum State
  {
    NEW, CLEAN, PROXY
  }
}
