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

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.io.CDODataInput;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public class CDOBranchImpl implements CDOBranch
{
  private int id;

  private String name;

  private CDOBranchPoint base;

  public CDOBranchImpl(int id, String name, int baseBranchID, long baseTimeStamp)
  {
    this.id = id;
    this.name = name;
    base = new CDOBranchPointImpl(baseBranchID, baseTimeStamp);
  }

  public CDOBranchImpl(CDODataInput in) throws IOException
  {
    id = in.readInt();
    name = in.readString();
    base = in.readCDOBranchPoint();
  }

  public int getID()
  {
    return id;
  }

  public String getName()
  {
    return name;
  }

  public CDOBranchPoint getBase()
  {
    return base;
  }
}
