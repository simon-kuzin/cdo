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
package org.eclipse.emf.cdo.spi.common.revision;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;
import org.eclipse.emf.cdo.common.revision.CDORevisionUtil;

import java.text.MessageFormat;

/**
 * @author Eike Stepper
 * @since 3.0
 */
public abstract class SyntheticCDORevision extends StubCDORevision
{
  private CDOBranch branch;

  private long revised;

  public SyntheticCDORevision(CDOBranch branch)
  {
    this.branch = branch;
    revised = UNSPECIFIED_DATE;
  }

  @Override
  public CDOBranch getBranch()
  {
    return branch;
  }

  @Override
  public final int getVersion()
  {
    return UNSPECIFIED_VERSION;
  }

  @Override
  public long getTimeStamp()
  {
    return branch.getBase().getTimeStamp();
  }

  @Override
  public long getRevised()
  {
    return revised;
  }

  @Override
  public void setRevised(long revised)
  {
    this.revised = revised;
  }

  /**
   * @author Eike Stepper
   * @since 3.0
   */
  public static class DetachMarker extends SyntheticCDORevision
  {
    private CDOID id;

    public DetachMarker(CDOID id, CDOBranch branch, long revised)
    {
      super(branch);
      this.id = id;
      setRevised(revised);
    }

    @Override
    public CDOID getID()
    {
      return id;
    }

    @Override
    public String toString()
    {
      return MessageFormat.format("DetachMarker[branch={0}, id={1}]", getBranch().getID(), id);
    }
  }

  /**
   * @author Eike Stepper
   * @since 3.0
   */
  public static class CachePointer extends SyntheticCDORevision
  {
    private CDORevisionKey target;

    public CachePointer(CDOBranch branch, CDORevisionKey target)
    {
      super(branch);
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
      return MessageFormat.format("CachePointer[branch={0}, target={1}]", getBranch().getID(), target);
    }
  }
}
