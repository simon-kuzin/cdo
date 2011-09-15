/*
 * Copyright (c) 2004 - 2011 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Simon McDuff - initial API and implementation
 *    Ibrahim Sallam - code refactoring for CDO 3.0
 */
package org.eclipse.emf.cdo.server.objectivity;

import org.eclipse.emf.cdo.server.IStoreAccessor;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * @author Ibrahim Sallam
 */
public interface IObjectivityStoreAccessor extends IStoreAccessor.Raw
{
  public IObjectivityStore getStore();

  /**
   * @since 2.0
   */
  public IObjectivityStoreChunkReader createChunkReader(InternalCDORevision revision, EStructuralFeature feature);
}
