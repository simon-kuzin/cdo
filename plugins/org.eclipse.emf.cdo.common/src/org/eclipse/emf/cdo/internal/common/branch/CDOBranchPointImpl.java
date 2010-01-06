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
package org.eclipse.emf.cdo.internal.common.branch;

import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.io.CDODataInput;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public class CDOBranchPointImpl implements CDOBranchPoint
{
  private int branchID;

  private long timeStamp;

  public CDOBranchPointImpl(int branchID, long timeStamp)
  {
    this.branchID = branchID;
    this.timeStamp = timeStamp;
  }

  public CDOBranchPointImpl(CDODataInput in) throws IOException
  {
    branchID = in.readInt();
    timeStamp = in.readLong();
  }

  public int getBranchID()
  {
    return branchID;
  }

  public long getTimeStamp()
  {
    return timeStamp;
  }

  public boolean isHistorical()
  {
    return timeStamp != UNSPECIFIED_DATE;
  }
}
