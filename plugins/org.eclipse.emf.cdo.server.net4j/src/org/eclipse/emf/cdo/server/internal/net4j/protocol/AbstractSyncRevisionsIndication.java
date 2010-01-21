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
import org.eclipse.emf.cdo.common.id.CDOIDAndBranch;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.server.internal.net4j.bundle.OM;
import org.eclipse.emf.cdo.spi.common.branch.CDOBranchUtil;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.net4j.util.collection.Pair;
import org.eclipse.net4j.util.collection.Triplet;
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

  protected List<Triplet<CDOIDAndBranch, InternalCDORevision, Long>> dirtyData = new ArrayList<Triplet<CDOIDAndBranch, InternalCDORevision, Long>>();

  protected List<Pair<CDOIDAndBranch, Long>> detachedData = new ArrayList<Pair<CDOIDAndBranch, Long>>();

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
      CDOID id = in.readCDOID();
      int viewBranchID = in.readInt();
      int revisionBranchID = in.readInt();
      int revisionVersion = in.readInt();
      process(id, viewBranchID, revisionBranchID, revisionVersion);
    }
  }

  @Override
  protected void responding(CDODataOutput out) throws IOException
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Sync found " + dirtyData.size() + " dirty objects"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    out.writeInt(dirtyData.size());
    for (Triplet<CDOIDAndBranch, InternalCDORevision, Long> triplet : dirtyData)
    {
      out.writeCDOIDAndBranch(triplet.getElement1());
      out.writeCDORevision(triplet.getElement2(), referenceChunk);
      out.writeLong(triplet.getElement3());
    }

    out.writeInt(detachedData.size());
    for (Pair<CDOIDAndBranch, Long> pair : detachedData)
    {
      out.writeCDOIDAndBranch(pair.getElement1());
      out.writeLong(pair.getElement2());
    }
  }

  protected abstract void process(CDOID id, int viewBranchID, int revisionBranchID, int revisionVersion);

  /**
   * @param id
   *          Some object's CDOID
   * @param viewedBranchID
   *          The ID of the branch that the client is viewing the object in
   * @param revisionBranchID
   *          The ID of the branch of the revision that the client is using for this object
   * @param revisionVersion
   *          The version of the revision that the client is using for this object
   */
  protected void updateObjectList(CDOID id, int viewedBranchID, int revisionBranchID, int revisionVersion)
  {
    // Construct the branchPoint that expresses what the client is *viewing*
    CDOBranch viewedBranch = getRepository().getBranchManager().getBranch(viewedBranchID);
    CDOBranchPoint branchPoint = CDOBranchUtil.createBranchPoint(viewedBranch, CDORevision.UNSPECIFIED_DATE);

    // Get the branch of the revision that the client is *using* to back the object
    CDOBranch revisionBranch = getRepository().getBranchManager().getBranch(revisionBranchID);

    try
    {
      // Obtain the latest revision that can back the object that the client is viewing
      InternalCDORevision latestServerRev = (InternalCDORevision)getRepository().getRevisionManager().getRevision(id,
          branchPoint, referenceChunk, CDORevision.DEPTH_NONE, true);

      if (latestServerRev == null)
      {
        addDetachedData(id, viewedBranch, revisionBranch, revisionVersion);
      }
      else if (latestServerRev.getBranch().getID() != revisionBranchID
          || latestServerRev.getVersion() > revisionVersion)
      {
        addDirtyData(id, viewedBranch, latestServerRev, revisionBranch, revisionVersion);
      }
      else if (latestServerRev.getBranch().getID() == revisionBranchID
          && latestServerRev.getVersion() < revisionVersion)
      {
        // Same branch but server's latest version is older than client's version -- impossible!
        throw new IllegalStateException("The server's revision (" + latestServerRev + ") is on the same branch " //$NON-NLS-1$ //$NON-NLS-2$
            + "as the client's (" + revisionVersion + "), yet has a smaller version number."); //$NON-NLS-1$ //$NON-NLS-2$
      }
      else
      {
        // $$$ Remove this else branch when all seems well
        // $$$ It's only an assertion to ensure we cover all cases.
        boolean _assert = latestServerRev.getBranch().getID() == revisionBranchID
            && latestServerRev.getVersion() == revisionVersion;
        if (!_assert)
        {
          throw new IllegalStateException("Server logic error");
        }
      }
    }
    catch (IllegalArgumentException revisionIsNullException)
    {
      addDetachedData(id, viewedBranch, revisionBranch, revisionVersion);
    }
  }

  private void addDetachedData(CDOID id, CDOBranch viewedBranch, CDOBranch revisionBranch, int revisionVersion)
  {
    CDOIDAndBranch idAndBranch = CDOIDUtil.createIDAndBranch(id, viewedBranch);
    long revisedTimestamp = getRevisedTimestamp(id, revisionVersion, revisionBranch);
    detachedData.add(new Pair<CDOIDAndBranch, Long>(idAndBranch, revisedTimestamp));
  }

  private void addDirtyData(CDOID id, CDOBranch viewedBranch, InternalCDORevision latestServerRev,
      CDOBranch revisionBranch, int revisionVersion)
  {
    CDOIDAndBranch idAndBranch = CDOIDUtil.createIDAndBranch(id, viewedBranch);
    long revisedTimestamp = getRevisedTimestamp(id, revisionVersion, revisionBranch);
    dirtyData
        .add(new Triplet<CDOIDAndBranch, InternalCDORevision, Long>(idAndBranch, latestServerRev, revisedTimestamp));
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
