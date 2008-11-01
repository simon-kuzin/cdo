/***************************************************************************
 * Copyright (c) 2004 - 2008 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo.internal.server.protocol;

import org.eclipse.emf.cdo.common.CDODataInput;
import org.eclipse.emf.cdo.common.CDODataOutput;
import org.eclipse.emf.cdo.common.CDOProtocolConstants;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.internal.server.bundle.OM;
import org.eclipse.emf.cdo.server.IAudit;
import org.eclipse.emf.cdo.server.IView;

import org.eclipse.net4j.util.om.trace.ContextTracer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eike Stepper
 */
public class SetAuditIndication extends CDOReadIndication
{
  private static final ContextTracer PROTOCOL_TRACER = new ContextTracer(OM.DEBUG_PROTOCOL, SetAuditIndication.class);

  private List<CDORevision> revisions;

  public SetAuditIndication(CDOServerProtocol protocol)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_SET_AUDIT);
  }

  @Override
  protected void indicating(CDODataInput in) throws IOException
  {
    int viewID = in.readInt();
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Read viewID: {0}", viewID);
    }

    long timeStamp = in.readLong();
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Read timeStamp: {0,date} {0,time}", timeStamp);
    }

    int size = in.readInt();
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Reading {0} IDs", size);
    }

    List<CDOID> invalidObjects = new ArrayList<CDOID>(size);
    for (int i = 0; i < size; i++)
    {
      CDOID id = in.readCDOID();
      invalidObjects.add(id);
      if (PROTOCOL_TRACER.isEnabled())
      {
        PROTOCOL_TRACER.format("Read ID: {0}", id);
      }
    }

    IView view = getSession().getView(viewID);
    if (view instanceof IAudit)
    {
      IAudit audit = (IAudit)view;
      revisions = audit.setTimeStamp(timeStamp, invalidObjects);
    }
  }

  @Override
  protected void responding(CDODataOutput out) throws IOException
  {
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Writing {0} existanceFlags", revisions.size());
    }

    out.writeInt(revisions.size());
    for (CDORevision revision : revisions)
    {
      boolean existanceFlag = revision != null;
      if (PROTOCOL_TRACER.isEnabled())
      {
        PROTOCOL_TRACER.format("Writing existanceFlag: {0}", existanceFlag);
      }

      out.writeBoolean(existanceFlag);
    }
  }
}
