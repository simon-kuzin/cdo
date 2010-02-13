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
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Simon McDuff
 */
public class RefreshSessionIndication extends CDOReadIndication
{
  private Map<CDOBranch, Map<CDOID, CDORevisionKey>> viewedRevisions = new HashMap<CDOBranch, Map<CDOID, CDORevisionKey>>();

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
      Map<CDOID, CDORevisionKey> revisions = new HashMap<CDOID, CDORevisionKey>();
      viewedRevisions.put(branch, revisions);
      int size = in.readInt();
      for (int j = 0; j < size; j++)
      {
        CDORevisionKey revision = in.readCDORevisionKey();
        revisions.put(revision.getID(), revision);
      }
    }
  }

  @Override
  protected void responding(CDODataOutput out) throws IOException
  {
  }
}
