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
package org.eclipse.emf.cdo.common.model;

/**
 * @author Eike Stepper
 */
public interface CDOPackageUnit
{
  public CDOPackageRegistry getPackageRegistry();

  public String getID();

  public State getState();

  public long getTimeStamp();

  public boolean isDynamic();

  public boolean isLegacy();

  public CDOPackageInfo getPackageInfo(String packageURI);

  public CDOPackageInfo[] getPackageInfos();

  /**
   * @author Eike Stepper
   */
  public enum State
  {
    NEW, LOADED, NOT_LOADED
  }
}
