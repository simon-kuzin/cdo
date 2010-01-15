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

import org.eclipse.emf.cdo.common.CDOTimeProvider;
import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchManager;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;

import org.eclipse.net4j.util.lifecycle.ILifecycle;

import java.io.IOException;

/**
 * @author Eike Stepper
 * @since 3.0
 */
public interface InternalCDOBranchManager extends CDOBranchManager, ILifecycle
{
  public BranchLoader getBranchLoader();

  public void setBranchLoader(BranchLoader branchLoader);

  public CDOTimeProvider getTimeProvider();

  public void setTimeProvider(CDOTimeProvider timeProvider);

  public void initMainBranch(long timestamp);

  public CDOBranch createBranch(String name, CDOBranch baseBranch, long baseTimeStamp);

  public void handleBranchCreated(CDOBranch branch);

  /**
   * @author Eike Stepper
   * @since 3.0
   */
  public interface BranchLoader
  {
    public BranchInfo loadBranch(int branchID);

    public int createBranch(BranchInfo branchInfo);

    /**
     * @author Eike Stepper
     * @since 3.0
     */
    public static final class BranchInfo
    {
      private String name;

      private int baseBranchID;

      private long baseTimeStamp;

      public BranchInfo(String name, int baseBranchID, long baseTimeStamp)
      {
        this.name = name;
        this.baseBranchID = baseBranchID;
        this.baseTimeStamp = baseTimeStamp;
      }

      public BranchInfo(CDODataInput in) throws IOException
      {
        name = in.readString();
        baseBranchID = in.readInt();
        baseTimeStamp = in.readLong();
      }

      public void write(CDODataOutput out) throws IOException
      {
        out.writeString(name);
        out.writeInt(baseBranchID);
        out.writeLong(baseTimeStamp);
      }

      public String getName()
      {
        return name;
      }

      public int getBaseBranchID()
      {
        return baseBranchID;
      }

      public long getBaseTimeStamp()
      {
        return baseTimeStamp;
      }
    }
  }
}
