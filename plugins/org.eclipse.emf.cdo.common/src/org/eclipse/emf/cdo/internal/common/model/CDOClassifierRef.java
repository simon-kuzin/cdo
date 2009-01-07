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
import org.eclipse.emf.cdo.common.model.CDOClassifier;
import org.eclipse.emf.cdo.common.model.CDOPackage;
import org.eclipse.emf.cdo.common.model.CDOPackageManager;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public abstract class CDOClassifierRef implements CDOClassifier
{
  private CDOPackage containingPackage;

  private int classifierID;

  protected CDOClassifierRef()
  {
  }

  public CDOPackage getContainingPackage()
  {
    return containingPackage;
  }

  public void setContainingPackage(CDOPackage containingPackage)
  {
    this.containingPackage = containingPackage;
  }

  public int getClassifierID()
  {
    return classifierID;
  }

  public void setClassifierID(int classifierID)
  {
    this.classifierID = classifierID;
  }

  public CDOPackageManager getPackageManager()
  {
    return containingPackage.getPackageManager();
  }

  @Override
  public void read(CDODataInput in, boolean proxy) throws IOException
  {
    if (proxy)
    {
      classifierID = in.readInt();
    }
  }

  @Override
  public void write(CDODataOutput out, boolean proxy) throws IOException
  {
    super.write(out, proxy);
    out.writeInt(classifierID);
  }
}
