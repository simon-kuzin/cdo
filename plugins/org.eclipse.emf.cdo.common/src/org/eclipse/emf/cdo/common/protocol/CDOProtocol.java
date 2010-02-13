/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.common.protocol;

import org.eclipse.emf.cdo.common.CDOCommonSession;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

/**
 * @author Eike Stepper
 * @since 2.0
 */
public interface CDOProtocol
{
  public CDOCommonSession getSession();

  /**
   * @author Eike Stepper
   */
  public interface RefreshSessionHandler
  {
    public void handleNewPackageUnit(InternalCDOPackageUnit packageUnit);

    public void handleChangedObject(CDOBranchPoint branchPoint, InternalCDORevision revision);

    public void handleDetachedObject(CDOBranchPoint branchPoint, CDOID id);
  }
}
