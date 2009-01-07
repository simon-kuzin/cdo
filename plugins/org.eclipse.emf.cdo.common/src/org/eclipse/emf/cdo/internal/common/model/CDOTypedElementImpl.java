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
import org.eclipse.emf.cdo.spi.common.model.InternalCDOTypedElement;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public abstract class CDOTypedElementImpl extends CDONamedElementImpl implements InternalCDOTypedElement
{
  private CDOClassifier type;

  private int lowerBound;

  private int upperBound;

  protected CDOTypedElementImpl()
  {
  }

  public CDOClassifier getType()
  {
    return type;
  }

  public void setType(CDOClassifier type)
  {
    this.type = type;
  }

  public int getLowerBound()
  {
    return lowerBound;
  }

  public void setLowerBound(int lowerBound)
  {
    this.lowerBound = lowerBound;
  }

  public int getUpperBound()
  {
    return upperBound;
  }

  public void setUpperBound(int upperBound)
  {
    this.upperBound = upperBound;
  }

  public boolean isRequired()
  {
    return lowerBound > 0;
  }

  public boolean isMany()
  {
    return upperBound > 1;
  }

  @Override
  public void read(CDODataInput in, boolean proxy) throws IOException
  {
    super.read(in, proxy);
    type = in.readCDOClassifier(containingPackage);
    lowerBound = in.readInt();
    upperBound = in.readInt();
  }

  @Override
  public void write(CDODataOutput out, boolean proxy) throws IOException
  {
    super.write(out, proxy);
    out.writeCDOClassifierRef(type);
    out.writeInt(lowerBound);
    out.writeInt(upperBound);
  }

  // public Object readValue(CDODataInput in) throws IOException
  // {
  // CDOType type = getType();
  // if (type.canBeNull() && !isMany())
  // {
  // if (in.readBoolean())
  // {
  // return InternalCDORevision.NIL;
  // }
  // }
  //
  // return type.readValue(in);
  // }
  //
  // public void writeValue(CDODataOutput out, Object value) throws IOException
  // {
  // // TODO We could certainly optimized this: When a feature is a reference, NIL is only possible in the case where
  // // unsettable == true. (TO be verified)
  // if (type.canBeNull())
  // {
  // if (!isMany())
  // {
  // if (value == InternalCDORevision.NIL)
  // {
  // out.writeBoolean(true);
  // return;
  // }
  // else
  // {
  // out.writeBoolean(false);
  // }
  // }
  // }
  //
  // type.writeValue(out, value);
  // }
}
