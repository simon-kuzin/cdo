/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.common;

import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.model.CDOModelUtil;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.common.model.CDOType;
import org.eclipse.emf.cdo.common.revision.CDOList;
import org.eclipse.emf.cdo.common.revision.CDOListFactory;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDOList;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public final class TODO
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, TODO.class);

  private TODO()
  {
  }

  public static Object copyValue(Object value, EClassifier type)
  {
    // TODO: implement TODO.copyValue(value, type)
    throw new UnsupportedOperationException();
  }

  public static EClass[] getAllPersistentClasses(EPackage cdoPackage)
  {
    // TODO: implement TODO.getAllPersistentClasses(cdoPackage)
    throw new UnsupportedOperationException();
  }

  public static CDOList readCDOList(CDODataInput in, CDORevision revision, EStructuralFeature feature,
      CDOListFactory listFactory) throws IOException
  {
    // TODO Simon: Could most of this stuff be moved into the list?
    // (only if protected methods of this class don't need to become public)

    int referenceChunk;
    int size = in.readInt();
    if (size < 0)
    {
      size = -size;
      referenceChunk = in.readInt();
      if (TRACER.isEnabled())
      {
        TRACER.format("Read feature {0}: size={1}, referenceChunk={2}", feature, size, referenceChunk);
      }
    }
    else
    {
      referenceChunk = size;
      if (TRACER.isEnabled())
      {
        TRACER.format("Read feature {0}: size={1}", feature, size);
      }
    }

    CDOType type = CDOModelUtil.getType(feature.getEType());
    InternalCDOList list = (InternalCDOList)listFactory.createList(size, size, referenceChunk);
    for (int j = 0; j < referenceChunk; j++)
    {
      Object value = type.readValue(in);
      list.set(j, value);
      if (TRACER.isEnabled())
      {
        TRACER.trace("    " + value);
      }
    }

    return list;
  }

  public static void readEPackage(CDODataInput in, EPackage cdoPackage) throws IOException
  {
    // TODO: implement TODO.readEPackage(in, cdoPackage)
    throw new UnsupportedOperationException();
  }

  public static EPackage readEPackageDescriptor(CDODataInput in, CDOPackageRegistry packageManager) throws IOException
  {
    // TODO: implement TODO.readEPackageDescriptor(in, packageManager)
    throw new UnsupportedOperationException();
  }

  public static Object readFeatureValue(CDODataInput in, EStructuralFeature feature) throws IOException
  {
    CDOType type = CDOModelUtil.getType(feature.getEType());
    if (type.canBeNull() && !feature.isMany())
    {
      if (in.readBoolean())
      {
        return InternalCDORevision.NIL;
      }
    }

    return type.readValue(in);
  }

  public static Object readValue(CDODataInput in, EClassifier type)
  {
    // TODO: implement TODO.readValue(in, type)
    throw new UnsupportedOperationException();
  }

  public static void writeEPackage(CDODataOutput out, EPackage cdoPackage) throws IOException
  {
    // TODO: implement TODO.writeEPackage(cdoDataOutputImpl, cdoPackage)
    throw new UnsupportedOperationException();
  }

  public static void writeFeatureValue(CDODataOutput out, Object value, EStructuralFeature feature) throws IOException
  {
    // TODO We could certainly optimized this: When a feature is a reference, NIL is only possible in the case where
    // unsettable == true. (TO be verified)

    CDOType type = CDOModelUtil.getType(feature.getEType());
    if (type.canBeNull())
    {
      if (!feature.isMany())
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
    else
    {
      if (value == null)
      {
        value = feature.getDefaultValue();
      }
    }

    type.writeValue(out, value);
  }

  public static void writeValue(CDODataOutput out, Object value, EClassifier type)
  {
    // TODO: implement TODO.writeValue(out, value, type)
    throw new UnsupportedOperationException();
  }
}
