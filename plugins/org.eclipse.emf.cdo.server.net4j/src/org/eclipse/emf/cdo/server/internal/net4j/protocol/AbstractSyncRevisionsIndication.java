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

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.branch.CDOBranchVersion;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDAndVersionAndBranch;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.server.internal.net4j.bundle.OM;
import org.eclipse.emf.cdo.spi.common.branch.CDOBranchUtil;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.net4j.util.collection.Pair;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Simon McDuff
 */
public abstract class AbstractSyncRevisionsIndication extends CDOReadIndication
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_PROTOCOL, SyncRevisionsIndication.class);

  protected List<Pair<InternalCDORevision, Long>> dirtyObjects = new ArrayList<Pair<InternalCDORevision, Long>>();

  protected List<Pair<CDOID, CDOBranchPoint>> detachedObjects = new ArrayList<Pair<CDOID, CDOBranchPoint>>();

  protected int referenceChunk = CDORevision.UNCHUNKED;

  public AbstractSyncRevisionsIndication(CDOServerProtocol protocol, short signalID)
  {
    super(protocol, signalID);
  }

  @Override
  protected void indicating(CDODataInput in) throws IOException
  {
    referenceChunk = in.readInt();
    int size = in.readInt();
    for (int i = 0; i < size; i++)
    {
      process(in.readCDOIDAndVersionAndBranch());
    }
  }

  @Override
  protected void responding(CDODataOutput out) throws IOException
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Sync found " + dirtyObjects.size() + " dirty objects"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    out.writeInt(dirtyObjects.size());
    for (Pair<InternalCDORevision, Long> revisionAndOldRevised : dirtyObjects)
    {
      out.writeCDORevision(revisionAndOldRevised.getElement1(), referenceChunk);
      out.writeLong(revisionAndOldRevised.getElement2());
    }

    out.writeInt(detachedObjects.size());
    for (Pair<CDOID, CDOBranchPoint> idAndRevised : detachedObjects)
    {
      out.writeCDOID(idAndRevised.getElement1());
      out.writeCDOBranchPoint(idAndRevised.getElement2());
    }
  }

  protected abstract void process(CDOIDAndVersionAndBranch idAndVersionAndBranch);

  protected void updateObjectList(CDOIDAndVersionAndBranch idAndVersionAndBranch)
  {
    CDOID id = idAndVersionAndBranch.getID();
    int version = idAndVersionAndBranch.getVersion();
    int branchID = idAndVersionAndBranch.getBranchID();
    CDOBranch branch = getRepository().getBranchManager().getBranch(branchID);
    CDOBranchPoint branchPoint = CDOBranchUtil.createBranchPoint(branch, CDORevision.UNSPECIFIED_DATE);

    try
    {
      InternalCDORevision revision = (InternalCDORevision)getRepository().getRevisionManager().getRevision(id,
          branchPoint, referenceChunk, CDORevision.DEPTH_NONE, true);
      if (revision == null)
      {
        long revisedTimestamp = getRevisedTimestamp(id, version, branch);
        CDOBranchPoint revisedBranchPoint = CDOBranchUtil.createBranchPoint(branch, revisedTimestamp);
        detachedObjects.add(new Pair<CDOID, CDOBranchPoint>(id, revisedBranchPoint));
      }
      else if (revision.getVersion() > version || version == CDORevision.UNSPECIFIED_VERSION)
      {
        dirtyObjects.add(new Pair<InternalCDORevision, Long>(revision, getRevisedTimestamp(id, version, branch)));
      }
      else if (revision.getVersion() < version)
      {
        throw new IllegalStateException("The object " + revision.getID() + " has a lower version (" //$NON-NLS-1$ //$NON-NLS-2$
            + revision.getVersion() + ") in the repository than the version (" + version + ") submitted."); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }
    catch (IllegalArgumentException revisionIsNullException)
    {
      // $$$ Factor (these 3 lines copied from 102-104)
      long revisedTimestamp = getRevisedTimestamp(id, version, branch);
      CDOBranchPoint revisedBranchPoint = CDOBranchUtil.createBranchPoint(branch, revisedTimestamp);
      detachedObjects.add(new Pair<CDOID, CDOBranchPoint>(id, revisedBranchPoint));
    }
  }

  protected long getRevisedTimestamp(CDOID id, int version, CDOBranch branch)
  {
    CDOBranchVersion branchVersion = CDOBranchUtil.createBranchVersion(branch, version);
    CDORevision revision = getRepository().getRevisionManager().getRevisionByVersion(id, branchVersion,
        CDORevision.DEPTH_NONE, false);
    if (revision != null)
    {
      return revision.getRevised() + 1;
    }

    return CDORevision.UNSPECIFIED_DATE;
  }
}
