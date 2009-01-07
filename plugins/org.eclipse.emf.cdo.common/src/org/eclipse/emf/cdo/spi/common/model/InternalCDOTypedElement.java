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

import org.eclipse.emf.cdo.common.model.CDOClassifier;
import org.eclipse.emf.cdo.common.model.CDOTypedElement;

/**
 * @author Eike Stepper
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
public interface InternalCDOTypedElement extends CDOTypedElement, InternalCDONamedElement
{
  public void setType(CDOClassifier type);

  public void setLowerBound(int lowerBound);

  public void setUpperBound(int upperBound);

  // public Object copyValue(Object value);
  //
  // public Object adjustReferences(CDOReferenceAdjuster adjuster, Object value);
  //
  // public Object readValue(CDODataInput in) throws IOException;
  //
  // public void writeValue(CDODataOutput out, Object value) throws IOException;
}
