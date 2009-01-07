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
package org.eclipse.emf.cdo.internal.common.model;

import org.eclipse.emf.cdo.common.CDODataInput;
import org.eclipse.emf.cdo.common.CDODataOutput;
import org.eclipse.emf.cdo.spi.common.model.InternalCDONamedElement;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public abstract class CDONamedElementImpl extends CDOModelElementImpl implements InternalCDONamedElement
{
  private String name;

  protected CDONamedElementImpl()
  {
  }

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public void read(CDODataInput in, boolean proxy) throws IOException
  {
    name = in.readString();
  }

  public void write(CDODataOutput out, boolean proxy) throws IOException
  {
    out.writeString(name);
  }
}
