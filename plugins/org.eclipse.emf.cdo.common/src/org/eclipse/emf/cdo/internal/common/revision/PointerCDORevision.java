package org.eclipse.emf.cdo.internal.common.revision;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;
import org.eclipse.emf.cdo.common.revision.CDORevisionUtil;
import org.eclipse.emf.cdo.spi.common.revision.SyntheticCDORevision;

import java.text.MessageFormat;

/**
 * @author Eike Stepper
 * @since 3.0
 */
public class PointerCDORevision extends SyntheticCDORevision
{
  private CDORevisionKey target;

  public PointerCDORevision(CDOBranch branch, long revised, CDORevisionKey target)
  {
    super(branch, revised);
    this.target = CDORevisionUtil.createRevisionKey(target);
  }

  public CDORevisionKey getTarget()
  {
    return target;
  }

  @Override
  public CDOID getID()
  {
    return target.getID();
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("PointerCDORevision[{0}:{1}v0 --> {2}v{3}]", target.getID(), getBranch().getID(),
        target.getBranch().getID(), target.getVersion());
  }
}
