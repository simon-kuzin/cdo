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
package org.eclipse.emf.cdo.spi.common.branch;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchManager;

import org.eclipse.net4j.util.lifecycle.ILifecycle;

/**
 * @author Eike Stepper
 * @since 3.0
 */
public interface InternalCDOBranchManager extends CDOBranchManager, ILifecycle
{
  public BranchLoader getBranchLoader();

  public void setBranchLoader(BranchLoader branchLoader);

  public void initMainBranch(long timestamp);

  public void handleBranchCreated(CDOBranch branch);

  /**
   * @author Eike Stepper
   * @since 3.0
   */
  public interface BranchLoader
  {
    public CDOBranch loadBranch(int branchID);

    public CDOBranch createBranch(int baseBranchID, long baseTimeStamp, String name);
  }
}
