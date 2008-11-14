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
package org.eclipse.emf.internal.cdo;

import org.eclipse.emf.cdo.CDOViewSet;

import org.eclipse.emf.common.notify.Adapter;

/**
 * @author Eike Stepper
 * @since 2.0
 */
public interface InternalCDOViewSet extends CDOViewSet, Adapter
{
  public void add(InternalCDOView view);

  public void remove(InternalCDOView view);

  public InternalCDOView resolveView(String repositoryUUID);
}
