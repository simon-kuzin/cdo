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
package org.eclipse.emf.cdo.spi.common;

import org.eclipse.emf.cdo.common.CDODataInput;
import org.eclipse.emf.cdo.common.CDODataOutput;
import org.eclipse.emf.cdo.common.model.CDOClassProxy;
import org.eclipse.emf.cdo.common.model.CDOClassRef;
import org.eclipse.emf.cdo.common.model.CDOTypedElement;

import java.io.IOException;

/**
 * @author Eike Stepper
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
public interface InternalCDOTypedElement extends CDOTypedElement, InternalCDONamedElement
{
  public CDOClassProxy getReferenceTypeProxy();

  public void setReferenceType(CDOClassRef cdoClassRef);

  /**
   * @since 2.0
   */
  public void writeValue(CDODataOutput out, Object value) throws IOException;

  /**
   * @since 2.0
   */
  public Object readValue(CDODataInput in) throws IOException;
}
