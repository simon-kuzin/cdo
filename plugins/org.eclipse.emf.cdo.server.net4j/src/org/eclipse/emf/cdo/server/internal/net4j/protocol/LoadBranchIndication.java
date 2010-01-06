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
package org.eclipse.emf.cdo.server.internal.net4j.protocol;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;
import org.eclipse.emf.cdo.spi.common.branch.InternalCDOBranchManager;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public class LoadBranchIndication extends CDOReadIndication
{
  private int branchID;

  public LoadBranchIndication(CDOServerProtocol protocol)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_LOAD_BRANCH);
  }

  @Override
  protected void indicating(CDODataInput in) throws IOException
  {
    branchID = in.readInt();
  }

  @Override
  protected void responding(CDODataOutput out) throws IOException
  {
    writeBranches(out, getRepository().getBranchManager(), branchID);
  }

  public static void writeBranches(CDODataOutput out, InternalCDOBranchManager branchManager, int branchID)
      throws IOException
  {
    while (branchID != CDOBranch.MAIN_BRANCH_ID)
    {
      CDOBranch branch = branchManager.getBranch(branchID);
      out.writeBoolean(true);
      out.writeCDOBranch(branch);

      branchID = branch.getBase().getBranchID();
    }

    out.writeBoolean(false);
  }
}
