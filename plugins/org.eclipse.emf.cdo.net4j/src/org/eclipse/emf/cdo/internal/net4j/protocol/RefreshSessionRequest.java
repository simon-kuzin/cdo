/***************************************************************************
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Simon McDuff - initial API and implementation
 *    Eike Stepper - maintenance
 **************************************************************************/
package org.eclipse.emf.cdo.internal.net4j.protocol;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;

import org.eclipse.emf.spi.cdo.CDOSessionProtocol.RefreshSessionResult;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Simon McDuff
 * @since 2.0
 */
public class RefreshSessionRequest extends CDOClientRequest<RefreshSessionResult>
{
  private long lastUpdateTime;

  private Map<CDOBranch, Map<CDOID, CDORevisionKey>> viewedRevisions;

  private int initialChunkSize;

  private boolean enablePassiveUpdates;

  public RefreshSessionRequest(CDOClientProtocol protocol, long lastUpdateTime,
      Map<CDOBranch, Map<CDOID, CDORevisionKey>> viewedRevisions, int initialChunkSize, boolean enablePassiveUpdates)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_REFRESH_SESSION);
    this.lastUpdateTime = lastUpdateTime;
    this.viewedRevisions = viewedRevisions;
    this.initialChunkSize = initialChunkSize;
    this.enablePassiveUpdates = enablePassiveUpdates;
  }

  @Override
  protected void requesting(CDODataOutput out) throws IOException
  {
    out.writeLong(lastUpdateTime);
    out.writeInt(initialChunkSize);
    out.writeBoolean(enablePassiveUpdates);

    out.writeInt(viewedRevisions.size());
    for (Entry<CDOBranch, Map<CDOID, CDORevisionKey>> entry : viewedRevisions.entrySet())
    {
      CDOBranch branch = entry.getKey();
      Map<CDOID, CDORevisionKey> revisions = entry.getValue();

      out.writeCDOBranch(branch);
      out.writeInt(revisions.size());
      for (CDORevisionKey revision : revisions.values())
      {
        out.writeCDORevisionKey(revision);
      }
    }
  }

  @Override
  protected RefreshSessionResult confirming(CDODataInput in) throws IOException
  {
    lastUpdateTime = in.readLong();
    RefreshSessionResult result = new RefreshSessionResult(lastUpdateTime);

    return result;

    // for (;;)
    // {
    // byte type = in.readByte();
    // switch (type)
    // {
    // case CDOProtocolConstants.REFRESH_PACKAGE_UNIT:
    // {
    // InternalCDOPackageUnit packageUnit = (InternalCDOPackageUnit)in.readCDOPackageUnit(null);
    // handler.handlePackageUnit(packageUnit);
    // break;
    // }
    //
    // case CDOProtocolConstants.REFRESH_CHANGED_OBJECT:
    // {
    // InternalCDORevision revision = (InternalCDORevision)in.readCDORevision();
    // handler.handleChangedObject(revision);
    // break;
    // }
    //
    // case CDOProtocolConstants.REFRESH_DETACHED_OBJECT:
    // {
    // CDOID id = in.readCDOID();
    // CDOBranchVersion branchVersion = in.readCDOBranchVersion();
    // long timeStamp = in.readLong();
    // handler.handleDetachedObject(id, branchVersion, timeStamp);
    // break;
    // }
    //
    // case CDOProtocolConstants.REFRESH_FINISHED:
    // return count;
    //
    // default:
    // throw new IOException("Invalid refresh type: " + type);
    // }
    //
    // }
  }
}
