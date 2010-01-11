/***************************************************************************
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo.internal.net4j.protocol;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;
import org.eclipse.emf.cdo.internal.net4j.bundle.OM;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.net4j.util.om.trace.ContextTracer;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Eike Stepper
 */
public class LoadRevisionByVersionRequest extends CDOClientRequest<InternalCDORevision>
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_PROTOCOL, LoadRevisionByVersionRequest.class);

  private CDOID id;

  private CDOBranch branch;

  private int version;

  private int referenceChunk;

  public LoadRevisionByVersionRequest(CDOClientProtocol protocol, CDOID id, CDOBranch branch, int version,
      int referenceChunk)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_LOAD_REVISION_BY_VERSION);
    this.id = id;
    this.branch = branch;
    this.version = version;
    this.referenceChunk = referenceChunk;
  }

  @Override
  protected void requesting(CDODataOutput out) throws IOException
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Writing id: {0}", id); //$NON-NLS-1$
    }

    out.writeCDOID(id);
    if (TRACER.isEnabled())
    {
      TRACER.format("Writing branch: {0}", branch); //$NON-NLS-1$
    }

    out.writeCDOBranch(branch);
    if (TRACER.isEnabled())
    {
      TRACER.format("Writing version: {0}", version); //$NON-NLS-1$
    }

    out.writeInt(version);
    if (TRACER.isEnabled())
    {
      TRACER.format("Writing referenceChunk: {0}", referenceChunk); //$NON-NLS-1$
    }

    out.writeInt(referenceChunk);
  }

  @Override
  protected InternalCDORevision confirming(CDODataInput in) throws IOException
  {
    return (InternalCDORevision)in.readCDORevision();
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("LoadRevisionByVersionRequest(id={0}, branch={1}, version={2}, referenceChunk={3})",
        id, branch, version, referenceChunk);
  }
}
