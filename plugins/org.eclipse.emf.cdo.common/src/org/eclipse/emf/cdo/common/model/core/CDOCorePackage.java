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
package org.eclipse.emf.cdo.common.model.core;

import org.eclipse.emf.cdo.common.model.CDOPackage;
import org.eclipse.emf.cdo.common.model.CDOType;

/**
 * @author Eike Stepper
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
public interface CDOCorePackage extends CDOPackage
{
  public static final String PACKAGE_URI = "http://www.eclipse.org/emf/CDO/core/1.0.0";

  public static final String NAME = "cdocore";

  public CDOObjectClass getCDOObjectClass();

  /**
   * @since 2.0
   */
  public CDOType getCDOBooleanObject();

  /**
   * @since 2.0
   */
  public CDOType getCDOCharacterObject();

  /**
   * @since 2.0
   */
  public CDOType getCDODate();

  /**
   * @since 2.0
   */
  public CDOType getCDODoubleObject();

  /**
   * @since 2.0
   */
  public CDOType getCDOFloatObject();

  /**
   * @since 2.0
   */
  public CDOType getCDOIntegerObject();

  /**
   * @since 2.0
   */
  public CDOType getCDOBoolean();

  /**
   * @since 2.0
   */
  public CDOType getCDOByteObject();

  /**
   * @since 2.0
   */
  public CDOType getCDOByte();

  /**
   * @since 2.0
   */
  public CDOType getCDOByteArray();

  /**
   * @since 2.0
   */
  public CDOType getCDOChar();

  /**
   * @since 2.0
   */
  public CDOType getCDODouble();

  /**
   * @since 2.0
   */
  public CDOType getCDOFloat();

  /**
   * @since 2.0
   */
  public CDOType getCDOInt();

  /**
   * @since 2.0
   */
  public CDOType getCDOLongObject();

  /**
   * @since 2.0
   */
  public CDOType getCDOShortObject();

  /**
   * @since 2.0
   */
  public CDOType getCDOLong();

  /**
   * @since 2.0
   */
  public CDOType getCDOShort();

  /**
   * @since 2.0
   */
  public CDOType getCDOFeatureMapEntry();

  /**
   * @since 2.0
   */
  public CDOType getCDOString();
}
