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

import org.eclipse.emf.cdo.common.model.CDOClassifier;
import org.eclipse.emf.cdo.common.model.CDOType;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOAttribute;

import org.eclipse.net4j.util.CheckUtil;

import java.text.MessageFormat;

/**
 * @author Eike Stepper
 */
public class CDOAttributeImpl extends CDOFeatureImpl implements InternalCDOAttribute
{
  public CDOAttributeImpl()
  {
  }

  @Override
  public CDOType getType()
  {
    return (CDOType)super.getType();
  }

  @Override
  public void setType(CDOClassifier type)
  {
    CheckUtil.checkArg(type instanceof CDOType, "type");
    super.setType(type);
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("CDOAttribute[featureID={0}, name={1}, type={2}[{3}..{4}]]", getFeatureID(), getName(),
        getType(), getLowerBound(), getUpperBound());
  }
}
