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
import org.eclipse.emf.cdo.spi.common.InternalCDOClassifier;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public abstract class CDOClassifierImpl extends CDONamedElementImpl implements InternalCDOClassifier
{
  private int classifierID;

  protected CDOClassifierImpl()
  {
  }

  protected CDOClassifierImpl(CDOPackage containingPackage, int classifierID, String name)
  {
    super(containingPackage, name);
    this.classifierID = classifierID;
  }

  protected CDOClassifierImpl(CDOPackage containingPackage, CDODataInput in) throws IOException
  {
    super(containingPackage, in);
    classifierID = in.readInt();
  }

  @Override
  public void write(CDODataOutput out) throws IOException
  {
    super.write(out);
    out.writeInt(classifierID);
  }

  public int getClassifierID()
  {
    return classifierID;
  }

  public void setClassifierID(int classifierID)
  {
    this.classifierID = classifierID;
  }

  public String getQualifiedName()
  {
    return getContainingPackage().getQualifiedName() + "." + getName();
  }
}
