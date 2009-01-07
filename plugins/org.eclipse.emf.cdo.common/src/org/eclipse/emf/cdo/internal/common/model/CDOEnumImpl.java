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

import org.eclipse.emf.cdo.common.model.CDOEnum;
import org.eclipse.emf.cdo.common.model.CDOPackageManager;

import java.text.MessageFormat;

/**
 * @author Eike Stepper
 */
public abstract class CDOEnumImpl extends CDOTypeImpl implements CDOEnum
{
  protected CDOEnumImpl()
  {
  }

  @Override
  public Kind getClassifierKind()
  {
    return Kind.ENUM;
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("CDOEnum[classifierID={0}, name={1}]", getClassifierID(), getName());
  }

  /**
   * @author Eike Stepper
   */
  public static final class Ref extends CDOClassifierImpl.Ref implements CDOEnum
  {
    public Ref(CDOPackageManager packageManager, String packageURI, int classifierID)
    {
      super(packageManager, packageURI, classifierID);
    }

    public Kind getClassifierKind()
    {
      return Kind.ENUM;
    }

    @Override
    public CDOEnum resolve()
    {
      return (CDOEnum)super.resolve();
    }

    @Override
    public String toString()
    {
      if (isResolved())
      {
        resolve().toString();
      }

      return MessageFormat.format("CDOEnumRef[packageURI={0}, classifierID={1}]", getPackageURI(), getClassifierID());
    }
  }
}
