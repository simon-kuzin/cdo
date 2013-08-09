package org.eclipse.emf.cdo.server.internal.commitables;

import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfoHandler;
import org.eclipse.emf.cdo.spi.common.branch.InternalCDOBranch;
import org.eclipse.emf.cdo.spi.common.branch.InternalCDOBranchManager;
import org.eclipse.emf.cdo.spi.common.commit.InternalCDOCommitInfoManager;

public class JSONCommitInfo
{
  private int branchID;

  private long timeStamp;

  private long previousTimeStamp;

  private String userID;

  private String comment;

  public JSONCommitInfo(int branchID, long timeStamp, long previousTimeStamp, String userID, String comment)
  {
    this.branchID = branchID;
    this.timeStamp = timeStamp;
    this.previousTimeStamp = previousTimeStamp;
    this.userID = userID;
    this.comment = comment;
  }

  public int getBranchID()
  {
    return branchID;
  }

  public long getTimeStamp()
  {
    return timeStamp;
  }

  public void handle(InternalCDOBranchManager branchManager, InternalCDOCommitInfoManager manager,
      CDOCommitInfoHandler handler)
  {
    InternalCDOBranch branch = branchManager.getBranch(branchID);
    CDOCommitInfo commitInfo = manager.createCommitInfo(branch, timeStamp, previousTimeStamp, userID, comment, null);
    handler.handleCommitInfo(commitInfo);
  }
}
