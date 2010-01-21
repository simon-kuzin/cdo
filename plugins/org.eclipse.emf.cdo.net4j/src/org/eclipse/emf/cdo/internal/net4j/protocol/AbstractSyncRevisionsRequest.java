/***************************************************************************
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
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

import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDAndBranch;
import org.eclipse.emf.cdo.common.id.CDOIDAndVersion;
import org.eclipse.emf.cdo.common.id.CDOIDAndVersionAndBranch;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.internal.net4j.bundle.OM;
import org.eclipse.emf.cdo.internal.net4j.messages.Messages;
import org.eclipse.emf.cdo.spi.common.branch.CDOBranchUtil;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionManager;
import org.eclipse.emf.cdo.transaction.CDORefreshContext;

import org.eclipse.emf.internal.cdo.transaction.CDORefreshContextImpl;

import org.eclipse.net4j.util.om.trace.ContextTracer;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * @author Simon McDuff
 * @since 2.0
 */
public abstract class AbstractSyncRevisionsRequest extends CDOClientRequest<Collection<CDORefreshContext>>
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_PROTOCOL, AbstractSyncRevisionsRequest.class);

  protected Map<CDOIDAndBranch, CDOIDAndVersionAndBranch> refreshables;

  protected int referenceChunk;

  public AbstractSyncRevisionsRequest(CDOClientProtocol protocol, short signalID,
      Map<CDOIDAndBranch, CDOIDAndVersionAndBranch> refreshables, int referenceChunk)
  {
    super(protocol, signalID);
    this.refreshables = refreshables;
    this.referenceChunk = referenceChunk;
  }

  @Override
  protected void requesting(CDODataOutput out) throws IOException
  {
    if (TRACER.isEnabled())
    {
      TRACER.trace("Synchronization " + refreshables.size() + " objects"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    out.writeInt(referenceChunk);
    out.writeInt(refreshables.size());
    for (Entry<CDOIDAndBranch, CDOIDAndVersionAndBranch> refreshable : refreshables.entrySet())
    {
      CDOIDAndBranch viewedIDandBranch = refreshable.getKey();
      CDOIDAndVersionAndBranch usedIDandVersionAndBranch = refreshable.getValue();

      // $$$ Remove this assertion
      if (!viewedIDandBranch.getID().equals(usedIDandVersionAndBranch.getID()))
      {
        throw new IllegalStateException("Logic error");
      }
      out.writeCDOID(viewedIDandBranch.getID());
      out.writeInt(viewedIDandBranch.getBranch().getID());
      out.writeInt(usedIDandVersionAndBranch.getBranchID());
      out.writeInt(usedIDandVersionAndBranch.getVersion());
    }
  }

  @Override
  protected Collection<CDORefreshContext> confirming(CDODataInput in) throws IOException
  {
    InternalCDORevisionManager revisionManager = getSession().getRevisionManager();
    Map<Long, CDORefreshContext> refreshContexts = new TreeMap<Long, CDORefreshContext>();

    int dirtyCount = in.readInt();
    for (int i = 0; i < dirtyCount; i++)
    {
      CDOIDAndBranch idAndBranch = in.readCDOIDAndBranch();
      CDORevision revision = in.readCDORevision();
      long revisedTimestamp = in.readLong();

      // $$$ Fix
      if (false)
      {
        // CDORevision revision = in.readCDORevision();
        long oldRevised = in.readLong();
        CDOBranchPoint branchPoint = CDOBranchUtil.createBranchPoint(revision.getBranch(), oldRevised);

        CDOIDAndVersionAndBranch idAndVersionAndBranch = refreshables.get(revision.getID());
        if (idAndVersionAndBranch == null)
        {
          throw new IllegalStateException(MessageFormat.format(
              Messages.getString("SyncRevisionsRequest.2"), revision.getID())); //$NON-NLS-1$
        }

        CDORefreshContext refreshContext = getRefreshContext(refreshContexts, branchPoint);
        Set<CDOIDAndVersion> dirtyObjects = refreshContext.getDirtyObjects();
        dirtyObjects.add(CDOIDUtil
            .createIDAndVersion(idAndVersionAndBranch.getID(), idAndVersionAndBranch.getVersion()));

        revisionManager.addRevision(revision);
      }
    }

    if (TRACER.isEnabled())
    {
      TRACER.trace("Synchronization received  " + dirtyCount + " dirty objects"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    int detachedCount = in.readInt();
    for (int i = 0; i < detachedCount; i++)
    {
      CDOIDAndBranch idAndBranch = in.readCDOIDAndBranch();
      long revisedTimestamp = in.readLong();

      // $$$ Fix
      if (false)
      {
        CDOID id = in.readCDOID();
        CDOBranchPoint branchPoint = in.readCDOBranchPoint();

        CDORefreshContext refreshContext = getRefreshContext(refreshContexts, branchPoint);
        Collection<CDOID> detachedObjects = refreshContext.getDetachedObjects();
        detachedObjects.add(id);
      }
    }

    if (TRACER.isEnabled())
    {
      TRACER.trace("Synchronization received  " + detachedCount + " detached objects"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    return Collections.unmodifiableCollection(refreshContexts.values());
  }

  private CDORefreshContext getRefreshContext(Map<Long, CDORefreshContext> refreshContexts, CDOBranchPoint branchPoint)
  {
    long timestamp = branchPoint.getTimeStamp();
    CDORefreshContext result = refreshContexts.get(timestamp);
    if (result == null)
    {
      result = new CDORefreshContextImpl(branchPoint);
      refreshContexts.put(timestamp, result);
    }

    return result;
  }
}
