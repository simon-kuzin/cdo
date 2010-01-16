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

  public InternalCDOBranch getMainBranch();

  public InternalCDOBranch getBranch(int branchID);

  public InternalCDOBranch getBranch(String path);

  public InternalCDOBranch createBranch(String name, InternalCDOBranch baseBranch, long baseTimeStamp);

  public void handleBranchCreated(InternalCDOBranch branch);

  /**
   * @author Eike Stepper
   * @since 3.0
   */
  public interface BranchLoader
  {
    public int createBranch(BranchInfo branchInfo);

    public BranchInfo loadBranch(int branchID);

    /**
     * @author Eike Stepper
     * @since 3.0
     */
    public static final class BranchInfo
    {
      public static final int[] NO_CHILDREN = {};

      private String name;

      private int baseBranchID;

      private long baseTimeStamp;

      private int[] childIDs;

      public BranchInfo(String name, int baseBranchID, long baseTimeStamp, int[] childIDs)
      {
        this.name = name;
        this.baseBranchID = baseBranchID;
        this.baseTimeStamp = baseTimeStamp;
        this.childIDs = childIDs;
      }

      public BranchInfo(String name, int baseBranchID, long baseTimeStamp)
      {
        this(name, baseBranchID, baseTimeStamp, NO_CHILDREN);
      }

      public BranchInfo(CDODataInput in) throws IOException
      {
        name = in.readString();
        baseBranchID = in.readInt();
        baseTimeStamp = in.readLong();

        int size = in.readInt();
        childIDs = new int[size];
        for (int i = 0; i < childIDs.length; i++)
        {
          childIDs[i] = in.readInt();
        }
      }

      public void write(CDODataOutput out) throws IOException
      {
        out.writeString(name);
        out.writeInt(baseBranchID);
        out.writeLong(baseTimeStamp);

        out.writeInt(childIDs.length);
        for (int id : childIDs)
        {
          out.writeInt(id);
        }
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

      public int[] getChildIDs()
      {
        return childIDs;
      }
    }
  }
}
