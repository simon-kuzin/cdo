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
import org.eclipse.emf.cdo.common.model.CDOClass;
import org.eclipse.emf.cdo.common.model.CDOClassifier;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOReference;

import org.eclipse.net4j.util.CheckUtil;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Eike Stepper
 */
public class CDOReferenceImpl extends CDOFeatureImpl implements InternalCDOReference
{
  private boolean containment;

  public CDOReferenceImpl()
  {
  }

  @Override
  public CDOClass getType()
  {
    return (CDOClass)super.getType();
  }

  @Override
  public void setType(CDOClassifier type)
  {
    CheckUtil.checkArg(type instanceof CDOClass, "type");
    super.setType(type);
  }

  public boolean isContainment()
  {
    return containment;
  }

  public void setContainment(boolean containment)
  {
    this.containment = containment;
  }

  @Override
  public void read(CDODataInput in, boolean proxy) throws IOException
  {
    super.read(in, proxy);
    containment = in.readBoolean();
  }

  @Override
  public void write(CDODataOutput out, boolean proxy) throws IOException
  {
    super.write(out, proxy);
    out.writeBoolean(containment);
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("CDOReference[featureID={0}, name={1}, type={2}[{3}..{4}]], containment={5}]",
        getFeatureID(), getName(), getType(), getLowerBound(), getUpperBound(), containment);
  }
}
