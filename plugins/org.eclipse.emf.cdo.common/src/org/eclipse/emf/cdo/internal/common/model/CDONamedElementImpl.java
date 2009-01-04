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
import org.eclipse.emf.cdo.common.model.CDOPackage;
import org.eclipse.emf.cdo.spi.common.InternalCDONamedElement;

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

  protected CDONamedElementImpl(CDOPackage containingPackage, String name)
  {
    super(containingPackage);
    this.name = name;
  }

  protected CDONamedElementImpl(CDOPackage containingPackage, CDODataInput in) throws IOException
  {
    super(containingPackage);
    name = in.readString();
  }

  public void write(CDODataOutput out) throws IOException
  {
    out.writeString(name);
  }

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }
}
