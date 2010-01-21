package org.eclipse.emf.cdo.spi.common.revision;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevisionManager;

import java.text.MessageFormat;

/**
 * A {@link SyntheticCDORevision synthetic} marker that indicates that an object is detached at the beginning of a
 * {@link CDOBranch branch}. It always has {@link #getVersion() version} zero and can only appear in branches below the
 * {@link CDOBranch#isMainBranch() main} branch. Instances of this marker revision are not supposed to be exposed
 * outside of a revision {@link CDORevisionManager manager}. They are mainly used in the communication between a
 * revision manager and its associated revision {@link InternalCDORevisionManager.RevisionLoader loader}.
 * 
 * @author Eike Stepper
 * @since 3.0
 */
public class DetachedCDORevision extends SyntheticCDORevision
{
  private CDOID id;

  public DetachedCDORevision(CDOID id, CDOBranch branch, long revised)
  {
    super(branch, revised);
    this.id = id;
  }

  @Override
  public CDOID getID()
  {
    return id;
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("DetachedCDORevision[{0}:{1}v0]", id, getBranch().getID());
  }
}
