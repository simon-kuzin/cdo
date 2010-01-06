/***************************************************************************
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo.internal.net4j.protocol;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public class CreateBranchRequest extends CDOClientRequest<CDOBranch>
{
  private int baseBranchID;

  private long baseTimeStamp;

  private String name;

  public CreateBranchRequest(CDOClientProtocol protocol, int baseBranchID, long baseTimeStamp, String name)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_CREATE_BRANCH);
    this.baseBranchID = baseBranchID;
    this.baseTimeStamp = baseTimeStamp;
    this.name = name;
  }

  @Override
  protected void requesting(CDODataOutput out) throws IOException
  {
    out.writeInt(baseBranchID);
    out.writeLong(baseTimeStamp);
    out.writeString(name);
  }

  @Override
  protected CDOBranch confirming(CDODataInput in) throws IOException
  {
    return in.readCDOBranch();
  }
}
