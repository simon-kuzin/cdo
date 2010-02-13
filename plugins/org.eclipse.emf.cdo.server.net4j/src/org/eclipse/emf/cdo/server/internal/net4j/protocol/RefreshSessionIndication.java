/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
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

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Simon McDuff
 */
public class RefreshSessionIndication extends CDOReadIndication
{
  private Map<CDOBranch, List<CDORevisionKey>> viewedRevisions = new HashMap<CDOBranch, List<CDORevisionKey>>();

  private int initialChunkSize;

  private boolean enablePassiveUpdates;

  public RefreshSessionIndication(CDOServerProtocol protocol)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_REFRESH_SESSION);
  }

  @Override
  protected void indicating(CDODataInput in) throws IOException
  {
    initialChunkSize = in.readInt();
    enablePassiveUpdates = in.readBoolean();

    int branches = in.readInt();
    for (int i = 0; i < branches; i++)
    {
      CDOBranch branch = in.readCDOBranch();
      List<CDORevisionKey> revisions = new ArrayList<CDORevisionKey>();
      viewedRevisions.put(branch, revisions);
      int size = in.readInt();
      for (int j = 0; j < size; j++)
      {
        CDORevisionKey revision = in.readCDORevisionKey();
        revisions.add(revision);
      }
    }
  }

  @Override
  protected void responding(CDODataOutput out) throws IOException
  {
    InternalCDORevisionManager revisionManager = getRepository().getRevisionManager();
    for (Entry<CDOBranch, List<CDORevisionKey>> entry : viewedRevisions.entrySet())
    {
      CDOBranch branch = entry.getKey();
      CDOBranchPoint head = branch.getHead();

      List<CDORevisionKey> keys = entry.getValue();
      for (CDORevisionKey key : keys)
      {
        CDOID id = key.getID();
        InternalCDORevision revision = revisionManager.getRevision(id, head, CDORevision.UNCHUNKED,
            CDORevision.DEPTH_NONE, true);

        if (revision == null)
        {
          out.writeByte(CDOProtocolConstants.REFRESH_DETACHED);
          out.writeCDOID(id);
        }
        else
        {
          out.writeByte(CDOProtocolConstants.REFRESH_CHANGED);
          out.writeCDORevision(revision, initialChunkSize);
        }
      }
    }

    getSession().setPassiveUpdateEnabled(enablePassiveUpdates);
    out.writeByte(CDOProtocolConstants.REFRESH_FINISHED);
  }
}
