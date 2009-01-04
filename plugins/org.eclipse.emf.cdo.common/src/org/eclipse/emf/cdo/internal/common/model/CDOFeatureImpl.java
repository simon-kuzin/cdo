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
import org.eclipse.emf.cdo.common.model.CDOClassProxy;
import org.eclipse.emf.cdo.common.model.CDOType;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.InternalCDOClass;
import org.eclipse.emf.cdo.spi.common.InternalCDOFeature;

import org.eclipse.net4j.util.om.trace.ContextTracer;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Eike Stepper
 */
public class CDOFeatureImpl extends CDOTypedElementImpl implements InternalCDOFeature
{
  private static final int UNKNOWN_FEATURE_INDEX = Integer.MIN_VALUE;

  private static final ContextTracer MODEL_TRACER = new ContextTracer(OM.DEBUG_MODEL, CDOFeatureImpl.class);

  private static final ContextTracer PROTOCOL_TRACER = new ContextTracer(OM.DEBUG_PROTOCOL, CDOFeatureImpl.class);

  private CDOClass containingClass;

  private int featureID;

  private int featureIndex = UNKNOWN_FEATURE_INDEX;

  private boolean containment;

  private Object defaultValue;

  /**
   * Creates an uninitialized instance.
   */
  public CDOFeatureImpl()
  {
  }

  /**
   * Creates an attribute feature.
   */
  public CDOFeatureImpl(CDOClass containingClass, int featureID, String name, CDOType type, Object defaultValue,
      boolean many)
  {
    super(containingClass.getContainingPackage(), name, type, many, null);
    if (type == CDOType.OBJECT)
    {
      throw new IllegalArgumentException("type == OBJECT");
    }

    this.containingClass = containingClass;
    this.featureID = featureID;
    this.defaultValue = defaultValue;
    if (MODEL_TRACER.isEnabled())
    {
      MODEL_TRACER.format("Created attribute {0}", this);
    }
  }

  /**
   * Creates a reference feature.
   */
  public CDOFeatureImpl(CDOClass containingClass, int featureID, String name, CDOClassProxy referenceTypeProxy,
      boolean many, boolean containment)
  {
    super(containingClass.getContainingPackage(), name, CDOType.OBJECT, many, referenceTypeProxy);
    if (referenceTypeProxy == null)
    {
      throw new IllegalArgumentException("referenceTypeProxy == null");
    }

    this.containingClass = containingClass;
    this.featureID = featureID;
    this.containment = containment;
    if (MODEL_TRACER.isEnabled())
    {
      MODEL_TRACER.format("Created reference {0}", this);
    }
  }

  /**
   * Reads a feature from a stream.
   */
  public CDOFeatureImpl(CDOClass containingClass, CDODataInput in) throws IOException
  {
    super(containingClass.getContainingPackage(), in);
    this.containingClass = containingClass;
    featureID = in.readInt();
    if (in.readBoolean())
    {
      defaultValue = getType().readValue(in);
    }

    containment = in.readBoolean();
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Read feature: ID={0}, name={1}, type={2}, many={3}, containment={4}", featureID,
          getName(), getType(), isMany(), containment);
    }
  }

  @Override
  public void write(CDODataOutput out) throws IOException
  {
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Writing feature: ID={0}, name={1}, type={2}, many={3}, containment={4}", featureID,
          getName(), getType(), isMany(), containment);
    }

    super.write(out);
    out.writeInt(featureID);
    if (defaultValue != null)
    {
      out.writeBoolean(true);
      getType().writeValue(out, defaultValue);
    }
    else
    {
      out.writeBoolean(false);
    }

    out.writeBoolean(containment);
  }

  public CDOClass getContainingClass()
  {
    return containingClass;
  }

  public void setContainingClass(CDOClass containingClass)
  {
    this.containingClass = containingClass;
    setContainingPackage(containingClass.getContainingPackage());
  }

  public int getFeatureID()
  {
    return featureID;
  }

  public void setFeatureID(int featureID)
  {
    this.featureID = featureID;
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

  public String getQualifiedName()
  {
    return getContainingClass().getQualifiedName() + "." + getName();
  }

  public Object getDefaultValue()
  {
    return defaultValue;
  }

  public void setDefaultValue(Object defaultValue)
  {
    this.defaultValue = defaultValue;
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
  public String toString()
  {
    if (isReference())
    {
      return MessageFormat.format("CDOFeature(ID={0}, name={1}, type={2})", featureID, getName(),
          getReferenceTypeProxy());
    }
    else
    {
      return MessageFormat.format("CDOFeature(ID={0}, name={1}, type={2})", featureID, getName(), getType());
    }
  }

  @Override
  public void writeValue(CDODataOutput out, Object value) throws IOException
  {
    if (!getType().canBeNull() && value == null)
    {
      // TODO Simon: Is this special handling needed?
      getType().writeValue(out, getDefaultValue());
    }
    else
    {
      super.writeValue(out, value);
    }
  }
}
