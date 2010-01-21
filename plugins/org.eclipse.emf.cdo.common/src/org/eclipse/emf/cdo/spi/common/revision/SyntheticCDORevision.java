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
}
