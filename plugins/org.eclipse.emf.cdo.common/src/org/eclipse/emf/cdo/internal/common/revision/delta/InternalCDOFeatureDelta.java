/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Simon McDuff - initial API and implementation
 *    Eike Stepper - maintenance
 */
package org.eclipse.emf.cdo.internal.common.revision.delta;

import org.eclipse.emf.cdo.common.revision.delta.CDOFeatureDelta;

/**
 * @author Simon McDuff
 */
public interface InternalCDOFeatureDelta extends CDOFeatureDelta
{
  /**
   * Create a copy only for objects that keep references of objects
   * 
   * @since 2.0
   */
  public CDOFeatureDelta copy();
}
