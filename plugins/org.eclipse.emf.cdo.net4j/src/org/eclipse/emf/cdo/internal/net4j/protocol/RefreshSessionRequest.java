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
import org.eclipse.emf.cdo.common.protocol.CDOProtocol.RefreshSessionHandler;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Simon McDuff
 * @since 2.0
 */
public class RefreshSessionRequest extends CDOClientRequest<Integer>
{
  private Map<CDOBranch, Map<CDOID, CDORevisionKey>> viewedRevisions;

  private int initialChunkSize;

  private boolean enablePassiveUpdates;

  private RefreshSessionHandler handler;

  public RefreshSessionRequest(CDOClientProtocol protocol, Map<CDOBranch, Map<CDOID, CDORevisionKey>> viewedRevisions,
      int initialChunkSize, boolean enablePassiveUpdates, RefreshSessionHandler handler)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_REFRESH_SESSION);
    this.viewedRevisions = viewedRevisions;
    this.initialChunkSize = initialChunkSize;
    this.enablePassiveUpdates = enablePassiveUpdates;
    this.handler = handler;
  }

  @Override
  protected void requesting(CDODataOutput out) throws IOException
  {
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
  protected Integer confirming(CDODataInput in) throws IOException
  {
    int count = 0;
    while (in.readBoolean())
    {
      ++count;
      // handler.handleChange();
    }

    while (in.readBoolean())
    {
      ++count;
      // handler.handleDetach();
    }

    return count;
  }
}
