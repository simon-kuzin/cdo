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
import org.eclipse.emf.cdo.common.model.CDOType;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

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
  private TODO()
  {
  }

  public static EClass[] getAllPersistentClasses(EPackage cdoPackage)
  {
    // TODO: implement TODO.getAllPersistentClasses(cdoPackage)
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

  public static Object copyValue(Object value, EClassifier type)
  {
    // TODO: implement TODO.copyValue(value, type)
    throw new UnsupportedOperationException();
  }

  public static Object readValue(CDODataInput in, EClassifier type)
  {
    // TODO: implement TODO.readValue(in, type)
    throw new UnsupportedOperationException();
  }

  public static void writeValue(CDODataOutput out, Object value, EClassifier type)
  {
    // TODO: implement TODO.writeValue(out, value, type)
    throw new UnsupportedOperationException();
  }
}
