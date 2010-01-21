/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Simon McDuff - initial API and implementation
 *    Eike Stepper - maintenance
 */
package org.eclipse.emf.cdo.server.internal.net4j.protocol;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDAndBranch;
import org.eclipse.emf.cdo.common.id.CDOIDAndVersionAndBranch;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;
import org.eclipse.emf.cdo.server.IView;

import org.eclipse.net4j.util.WrappedException;
import org.eclipse.net4j.util.concurrent.IRWLockManager.LockType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Simon McDuff
 */
public class LockObjectsIndication extends AbstractSyncRevisionsIndication
{
  private LockType lockType;

  private List<CDOIDAndBranch> idAndBranches = new ArrayList<CDOIDAndBranch>();

  private List<CDOIDAndVersionAndBranch> idAndVersionAndBranches = new ArrayList<CDOIDAndVersionAndBranch>();

  private IView view;

  public LockObjectsIndication(CDOServerProtocol protocol)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_LOCK_OBJECTS);
  }

  @Override
  protected void indicating(CDODataInput in) throws IOException
  {
    super.indicating(in);

    int viewID = in.readInt();
    lockType = in.readCDOLockType();
    long timeout = in.readLong();

    try
    {
      view = getSession().getView(viewID);
      getRepository().getLockManager().lock(lockType, view, idAndBranches, timeout);
    }
    catch (InterruptedException ex)
    {
      throw WrappedException.wrap(ex);
    }
  }

  @Override
  protected void responding(CDODataOutput out) throws IOException
  {
    for (CDOIDAndVersionAndBranch idAndVersionAndBranch : idAndVersionAndBranches)
    {
      // $$$ Fix
      // updateObjectList(idAndVersionAndBranch);
    }

    if (!detachedData.isEmpty())
    {
      getRepository().getLockManager().unlock(lockType, view, idAndBranches);
      throw new IllegalArgumentException(detachedData.size() + " objects are not persistent anymore"); //$NON-NLS-1$
    }

    super.responding(out);
  }

  @Override
  protected void process(CDOID id, int viewBranchID, int revisionBranchID, int revisionVersion)
  {
    // $$$ Fix
    /*
     * CDOBranch branch = getRepository().getBranchManager().getBranch(idAndVersionAndBranch.getBranchID());
     * idAndBranches.add(CDOIDUtil.createIDAndBranch(idAndVersionAndBranch.getID(), branch));
     * idAndVersionAndBranches.add(idAndVersionAndBranch);
     */
  }
}
