package org.eclipse.emf.cdo.spi.common.revision;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchVersion;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.spi.common.branch.CDOBranchUtil;

import java.text.MessageFormat;

/**
 * @author Eike Stepper
 * @since 3.0
 */
public class PointerCDORevision extends SyntheticCDORevision
{
  private CDOBranchVersion target;

  public PointerCDORevision(CDOID id, CDOBranch branch)
  {
    super(id, branch);
  }

  public CDOBranchVersion getTarget()
  {
    return target;
  }

  public void setTarget(CDOBranchVersion target)
  {
    this.target = CDOBranchUtil.createBranchVersion(target);
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("PointerCDORevision[{0}:{1}v0 --> {2}v{3}]", getID(), getBranch().getID(), target
        .getBranch().getID(), target.getVersion());
  }
}
