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
package org.eclipse.emf.cdo.internal.common.model.core;

import org.eclipse.emf.cdo.common.model.CDOType;
import org.eclipse.emf.cdo.common.model.core.CDOCorePackage;
import org.eclipse.emf.cdo.internal.common.model.CDOPackageImpl;
import org.eclipse.emf.cdo.internal.common.model.CDOPackageManagerImpl;

/**
 * @author Eike Stepper
 */
public final class CDOCorePackageImpl extends CDOPackageImpl implements CDOCorePackage
{
  private CDOObjectClassImpl cdoObjectClass;

  private CDOType cdoBoolean;

  private CDOType cdoBooleanObject;

  private CDOType cdoByte;

  private CDOType cdoByteObject;

  private CDOType cdoChar;

  private CDOType cdoCharacterObject;

  private CDOType cdoDate;

  private CDOType cdoDouble;

  private CDOType cdoDoubleObject;

  private CDOType cdoFloat;

  private CDOType cdoFloatObject;

  private CDOType cdoInt;

  private CDOType cdoIntegerObject;

  private CDOType cdoLong;

  private CDOType cdoLongObject;

  private CDOType cdoShort;

  private CDOType cdoShortObject;

  private CDOType cdoString;

  private CDOType cdoByteArray;

  private CDOType cdoFeatureMapEntry;

  public CDOCorePackageImpl(CDOPackageManagerImpl packageManager)
  {
    super(packageManager, PACKAGE_URI, NAME, null, false, null, null);
    addClassifier(cdoObjectClass = new CDOObjectClassImpl(this));
  }

  @Override
  public String getEcore()
  {
    return null;
  }

  @Override
  public boolean isSystem()
  {
    return true;
  }

  public CDOObjectClassImpl getCDOObjectClass()
  {
    return cdoObjectClass;
  }

  public CDOType getCDOBoolean()
  {
    return cdoBoolean;
  }

  public CDOType getCDOBooleanObject()
  {
    return cdoBooleanObject;
  }

  public CDOType getCDOByte()
  {
    return cdoByte;
  }

  public CDOType getCDOByteObject()
  {
    return cdoByteObject;
  }

  public CDOType getCDOChar()
  {
    return cdoChar;
  }

  public CDOType getCDOCharacterObject()
  {
    return cdoCharacterObject;
  }

  public CDOType getCDODate()
  {
    return cdoDate;
  }

  public CDOType getCDODouble()
  {
    return cdoDouble;
  }

  public CDOType getCDODoubleObject()
  {
    return cdoDoubleObject;
  }

  public CDOType getCDOFloat()
  {
    return cdoFloat;
  }

  public CDOType getCDOFloatObject()
  {
    return cdoFloatObject;
  }

  public CDOType getCDOInt()
  {
    return cdoInt;
  }

  public CDOType getCDOIntegerObject()
  {
    return cdoIntegerObject;
  }

  public CDOType getCDOLong()
  {
    return cdoLong;
  }

  public CDOType getCDOLongObject()
  {
    return cdoLongObject;
  }

  public CDOType getCDOShort()
  {
    return cdoShort;
  }

  public CDOType getCDOShortObject()
  {
    return cdoShortObject;
  }

  public CDOType getCDOString()
  {
    return cdoString;
  }

  public CDOType getCDOByteArray()
  {
    return cdoByteArray;
  }

  public CDOType getCDOFeatureMapEntry()
  {
    return cdoFeatureMapEntry;
  }
}
