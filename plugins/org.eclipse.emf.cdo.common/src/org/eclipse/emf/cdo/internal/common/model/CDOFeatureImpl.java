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
import org.eclipse.emf.cdo.common.model.CDOPackageManager;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOClass;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOFeature;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public abstract class CDOFeatureImpl extends CDOTypedElementImpl implements InternalCDOFeature
{
  private transient CDOClass containingClass;

  private transient int featureIndex = UNKNOWN_FEATURE_INDEX;

  private int featureID;

  private boolean unsettable;

  private Object defaultValue;

  public CDOFeatureImpl()
  {
  }

  public CDOClass getContainingClass()
  {
    return containingClass;
  }

  public void setContainingClass(CDOClass containingClass)
  {
    this.containingClass = containingClass;
  }

  public int getFeatureIndex()
  {
    if (featureIndex == UNKNOWN_FEATURE_INDEX)
    {
      featureIndex = ((InternalCDOClass)containingClass).getFeatureIndex(featureID);
    }

    return featureIndex;
  }

  public void setFeatureIndex(int featureIndex)
  {
    this.featureIndex = featureIndex;
  }

  public int getFeatureID()
  {
    return featureID;
  }

  public void setFeatureID(int featureID)
  {
    this.featureID = featureID;
  }

  public boolean isUnsettable()
  {
    return unsettable;
  }

  public void setUnsettable(boolean unsettable)
  {
    this.unsettable = unsettable;
  }

  public Object getDefaultValue()
  {
    return defaultValue;
  }

  public void setDefaultValue(Object defaultValue)
  {
    this.defaultValue = defaultValue;
  }

  public CDOPackageManager getPackageManager()
  {
    return containingClass.getPackageManager();
  }

  public String getQualifiedName()
  {
    return getContainingClass().getQualifiedName() + "." + getName();
  }

  // @Override
  // public void writeValue(CDODataOutput out, Object value) throws IOException
  // {
  // if (!getType().canBeNull() && value == null)
  // {
  // // TODO Simon: Is this special handling needed?
  // getType().writeValue(out, getDefaultValue());
  // }
  // else
  // {
  // super.writeValue(out, value);
  // }
  // }

  @Override
  public void read(CDODataInput in, boolean proxy) throws IOException
  {
    super.read(in, proxy);
    featureID = in.readInt();
    unsettable = in.readBoolean();
    if (in.readBoolean())
    {
      defaultValue = getType().readValue(in);
    }
  }

  @Override
  public void write(CDODataOutput out, boolean proxy) throws IOException
  {
    super.write(out, proxy);
    out.writeInt(featureID);
    out.writeBoolean(unsettable);
    if (defaultValue != null)
    {
      out.writeBoolean(true);
      getType().writeValue(out, defaultValue);
    }
    else
    {
      out.writeBoolean(false);
    }
  }
}
