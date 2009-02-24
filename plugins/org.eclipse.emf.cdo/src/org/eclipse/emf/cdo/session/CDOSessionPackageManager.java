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
package org.eclipse.emf.cdo.session;

import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;

import org.eclipse.emf.ecore.EPackage;

/**
 * Represents the CDO {@link EPackage packages} currently stored in the {@link CDOSession.Repository repository} of a
 * {@link CDOSession session}. A package manager can be used to query information about the CDO {@link EPackage
 * packages} in the repository as well as convert between the EMF and CDO instances of these packages.
 * 
 * @author Eike Stepper
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
public interface CDOSessionPackageManager extends CDOPackageRegistry
{
  /**
   * Returns the session this package manager is associated with.
   */
  public CDOSession getSession();
}
