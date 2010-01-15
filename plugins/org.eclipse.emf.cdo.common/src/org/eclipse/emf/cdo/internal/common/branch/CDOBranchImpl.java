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
import org.eclipse.emf.cdo.common.branch.CDOBranchVersion;
import org.eclipse.emf.cdo.spi.common.branch.InternalCDOBranchManager;
import org.eclipse.emf.cdo.spi.common.branch.InternalCDOBranchManager.BranchLoader.BranchInfo;

import java.text.MessageFormat;

/**
 * @author Eike Stepper
 */
public class CDOBranchImpl implements CDOBranch
{
  public static final int ILLEGAL_BRANCH_ID = Integer.MIN_VALUE;

  private int id;

  private String name;

  private Object baseOrBranchManager;

  private CDOBranchPoint head;

  public CDOBranchImpl(int id, String name, CDOBranchPoint base)
  {
    this.id = id;
    this.name = name;
    baseOrBranchManager = base;
    head = new CDOBranchPointImpl(this);
  }

  public CDOBranchImpl(int id, InternalCDOBranchManager branchManager)
  {
    this.id = id;
    baseOrBranchManager = branchManager;
  }

  public InternalCDOBranchManager getBranchManager()
  {
    if (isProxy())
    {
      return (InternalCDOBranchManager)baseOrBranchManager;
    }

    CDOBranchPoint base = (CDOBranchPoint)baseOrBranchManager;
    return (InternalCDOBranchManager)base.getBranch().getBranchManager();
  }

  public int getID()
  {
    return id;
  }

  public String getName()
  {
    loadIdNeeded();
    return name;
  }

  public CDOBranchPoint getBase()
  {
    loadIdNeeded();
    return (CDOBranchPoint)baseOrBranchManager;
  }

  public CDOBranchPoint getHead()
  {
    return head;
  }

  public CDOBranchPoint getPoint(long timeStamp)
  {
    return new CDOBranchPointImpl(this, timeStamp);
  }

  public CDOBranchVersion getVersion(int version)
  {
    return new CDOBranchVersionImpl(this, version);
  }

  public CDOBranch createBranch(String name, long timeStamp)
  {
    return getBranchManager().createBranch(name, this, timeStamp);
  }

  public CDOBranch createBranch(String name)
  {
    return getBranchManager().createBranch(name, this, CDOBranchPoint.UNSPECIFIED_DATE);
  }

  @Override
  public int hashCode()
  {
    return id;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == this)
    {
      return true;
    }

    if (obj instanceof CDOBranch)
    {
      CDOBranch that = (CDOBranch)obj;
      return id == that.getID();
    }

    return false;
  }

  @Override
  public String toString()
  {
    if (isProxy())
    {
      return MessageFormat.format("Branch[id={0}, PROXY]", id);
    }

    return MessageFormat.format("Branch[id={0}, name={1}]", id, name);
  }

  private boolean isProxy()
  {
    return name == null;
  }

  private void loadIdNeeded()
  {
    if (isProxy())
    {
      InternalCDOBranchManager branchManager = (InternalCDOBranchManager)baseOrBranchManager;
      BranchInfo branchInfo = branchManager.getBranchLoader().loadBranch(id);
      CDOBranch baseBranch = branchManager.getBranch(branchInfo.getBaseBranchID());
      name = branchInfo.getName();
      baseOrBranchManager = new CDOBranchPointImpl(baseBranch, branchInfo.getBaseTimeStamp());
    }
  }

  /**
   * @author Eike Stepper
   */
  public static class Main extends CDOBranchImpl
  {
    private InternalCDOBranchManager branchManager;

    public Main(InternalCDOBranchManager branchManager, long timeStamp)
    {
      super(MAIN_BRANCH_ID, MAIN_BRANCH_NAME, new CDOBranchPointImpl(null, timeStamp));
      this.branchManager = branchManager;
    }

    @Override
    public InternalCDOBranchManager getBranchManager()
    {
      return branchManager;
    }
  }
}
