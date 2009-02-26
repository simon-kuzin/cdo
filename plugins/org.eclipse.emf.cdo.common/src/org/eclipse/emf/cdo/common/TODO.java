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
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.common.model.CDOType;
import org.eclipse.emf.cdo.common.revision.CDOList;
import org.eclipse.emf.cdo.common.revision.CDOListFactory;
import org.eclipse.emf.cdo.common.revision.CDOReferenceAdjuster;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDOList;

import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.io.IOException;
import java.util.Date;

/**
 * @author Eike Stepper
 */
public final class TODO
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, TODO.class);

  private TODO()
  {
  }

  public static Object adjustReferences(CDOReferenceAdjuster revisionAdjuster, Object element, EClass type)
  {
    // TODO: implement TODO.adjustReferences(revisionAdjuster, element, type)
    throw new UnsupportedOperationException();
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

  public static EClassifier getClassifier(EPackage cdoPackage, int classifierID)
  {
    EList<EClassifier> classifiers = cdoPackage.getEClassifiers();
    for (EClassifier classifier : classifiers)
    {
      if (classifier.getClassifierID() == classifierID)
      {
        return classifier;
      }
    }

    return null;
  }

  public static EClassifier getClassifier(EPackage cdoPackage, String name)
  {
    EList<EClassifier> classifiers = cdoPackage.getEClassifiers();
    for (EClassifier classifier : classifiers)
    {
      if (name.equals(classifier.getName()))
      {
        return classifier;
      }
    }

    return null;
  }

  public static CDOType getCDOType(Class<? extends Object> primitiveType)
  {
    if (primitiveType == String.class)
    {
      return CDOType.STRING;
    }

    if (primitiveType == Boolean.class)
    {
      return CDOType.BOOLEAN;
    }

    if (primitiveType == Integer.class)
    {
      return CDOType.INT;
    }

    if (primitiveType == Double.class)
    {
      return CDOType.DOUBLE;
    }

    if (primitiveType == Float.class)
    {
      return CDOType.FLOAT;
    }

    if (primitiveType == Long.class)
    {
      return CDOType.LONG;
    }

    if (primitiveType == Date.class)
    {
      return CDOType.DATE;
    }

    if (primitiveType == Byte.class)
    {
      return CDOType.BYTE;
    }

    if (primitiveType == Character.class)
    {
      return CDOType.CHAR;
    }

    throw new IllegalArgumentException("Not a primitive type nor String nor Date: " + primitiveType);
  }

  public static EClass getResourceNodeClass(CDOPackageRegistry packageManager)
  {
    EPackage resourcePackage = packageManager.getEPackage("http://www.eclipse.org/emf/CDO/resource/2.0.0");
    return (EClass)getClassifier(resourcePackage, "CDOResourceNode");
  }

  public static boolean isResource(EClass cdoClass)
  {
    // TODO: implement TODO.isResource(cdoClass)
    throw new UnsupportedOperationException();
  }

  public static boolean isResourceFolder(EClass cdoClass)
  {
    // TODO: implement TODO.isResourceFolder(cdoClass)
    throw new UnsupportedOperationException();
  }

  public static boolean isResourceNode(EClass cdoClass)
  {
    // TODO: implement TODO.isResourceNode(cdoClass)
    throw new UnsupportedOperationException();
  }

  public static boolean isRoot(EClass cdoClass)
  {
    // TODO: implement TODO.isRoot(cdoClass)
    throw new UnsupportedOperationException();
  }

  public static CDOList readCDOList(CDODataInput in, CDORevision revision, EStructuralFeature feature,
      CDOListFactory listFactory) throws IOException
  {
    // TODO Simon: Could most of this stuff be moved into the list?
    // (only if protected methods of this class don't need to become public)

    EClassifier type = feature.getEType();
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

    InternalCDOList list = (InternalCDOList)listFactory.createList(size, size, referenceChunk);
    for (int j = 0; j < referenceChunk; j++)
    {
      Object value = readValue(in, type);
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
    // TODO: implement TODO.readFeatureValue(feature)
    throw new UnsupportedOperationException();
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
    // TODO: implement TODO.writeFeatureValue(out, value, feature)
    throw new UnsupportedOperationException();
  }

  public static void writeValue(CDODataOutput out, Object value, EClassifier type)
  {
    // TODO: implement TODO.writeValue(out, value, type)
    throw new UnsupportedOperationException();
  }
}
