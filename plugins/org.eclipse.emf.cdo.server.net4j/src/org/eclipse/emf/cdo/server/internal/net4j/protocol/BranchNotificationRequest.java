/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Simon McDuff - bug 201266
 *    Simon McDuff - bug 233490
 */
package org.eclipse.emf.cdo.server.internal.net4j.protocol;

import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public class BranchNotificationRequest extends CDOServerRequest
{
  private int branchID;

  public BranchNotificationRequest(CDOServerProtocol serverProtocol, int branchID)
  {
    super(serverProtocol, CDOProtocolConstants.SIGNAL_BRANCH_NOTIFICATION);
    this.branchID = branchID;
  }

  @Override
  protected void requesting(CDODataOutput out) throws IOException
  {
    out.writeInt(branchID);
  }
}
