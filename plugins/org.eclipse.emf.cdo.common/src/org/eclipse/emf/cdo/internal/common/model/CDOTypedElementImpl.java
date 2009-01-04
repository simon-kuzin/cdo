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
import org.eclipse.emf.cdo.common.model.CDOClassRef;
import org.eclipse.emf.cdo.common.model.CDOPackage;
import org.eclipse.emf.cdo.common.model.CDOType;
import org.eclipse.emf.cdo.spi.common.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.InternalCDOTypedElement;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public abstract class CDOTypedElementImpl extends CDONamedElementImpl implements InternalCDOTypedElement
{
  private CDOType type;

  private boolean many;

  private CDOClassProxy referenceTypeProxy;

  /**
   * Creates an uninitialized instance.
   */
  protected CDOTypedElementImpl()
  {
  }

  protected CDOTypedElementImpl(CDOPackage containingPackage, String name, CDOType type, boolean many,
      CDOClassProxy referenceTypeProxy)
  {
    super(containingPackage, name);
    this.type = type;
    this.many = many;
    this.referenceTypeProxy = referenceTypeProxy;
  }

  /**
   * Reads a typed element from a stream.
   */
  protected CDOTypedElementImpl(CDOPackage containingPackage, CDODataInput in) throws IOException
  {
    super(containingPackage, in);
    type = in.readCDOType();
    many = in.readBoolean();
    if (isReference())
    {
      CDOClassRef classRef = in.readCDOClassRef();
      referenceTypeProxy = new CDOClassProxy(classRef, getPackageManager());
    }
  }

  @Override
  public void write(CDODataOutput out) throws IOException
  {
    super.write(out);
    out.writeCDOType(type);
    out.writeBoolean(many);
    if (isReference())
    {
      CDOClassRef classRef = referenceTypeProxy.getClassRef();
      out.writeCDOClassRef(classRef);
    }
  }

  public CDOType getType()
  {
    return type;
  }

  public void setType(CDOType type)
  {
    this.type = type;
  }

  public boolean isMany()
  {
    return many;
  }

  public void setMany(boolean many)
  {
    this.many = many;
  }

  public boolean isReference()
  {
    return type == CDOType.OBJECT;
  }

  public CDOClass getReferenceType()
  {
    if (referenceTypeProxy == null)
    {
      return null;
    }

    return referenceTypeProxy.getCdoClass();
  }

  public void setReferenceType(CDOClassRef cdoClassRef)
  {
    referenceTypeProxy = new CDOClassProxy(cdoClassRef, getPackageManager());
  }

  public CDOClassProxy getReferenceTypeProxy()
  {
    return referenceTypeProxy;
  }

  public Object readValue(CDODataInput in) throws IOException
  {
    CDOType type = getType();
    if (type.canBeNull() && !isMany())
    {
      if (in.readBoolean())
      {
        return InternalCDORevision.NIL;
      }
    }

    return type.readValue(in);
  }

  public void writeValue(CDODataOutput out, Object value) throws IOException
  {
    // TODO We could certainly optimized this: When a feature is a reference, NIL is only possible in the case where
    // unsettable == true. (TO be verified)
    if (type.canBeNull())
    {
      if (!isMany())
      {
        if (value == InternalCDORevision.NIL)
        {
          out.writeBoolean(true);
          return;
        }
        else
        {
          out.writeBoolean(false);
        }
      }
    }

    type.writeValue(out, value);
  }
}
