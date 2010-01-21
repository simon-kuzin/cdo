/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.spi.common.revision;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.branch.CDOBranchVersion;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.CDORevisionFactory;
import org.eclipse.emf.cdo.common.revision.CDORevisionManager;
import org.eclipse.emf.cdo.common.revision.cache.CDORevisionCache;
import org.eclipse.emf.cdo.common.revision.cache.CDORevisionCacheAdder;
import org.eclipse.emf.cdo.common.revision.cache.InternalCDORevisionCache;

import org.eclipse.net4j.util.lifecycle.ILifecycle;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eike Stepper
 * @since 3.0
 */
public interface InternalCDORevisionManager extends CDORevisionManager, CDORevisionCacheAdder, ILifecycle
{
  public boolean isSupportingBranches();

  public void setSupportingBranches(boolean on);

  public RevisionLoader getRevisionLoader();

  public void setRevisionLoader(RevisionLoader revisionLoader);

  public RevisionLocker getRevisionLocker();

  public void setRevisionLocker(RevisionLocker revisionLocker);

  public CDORevisionFactory getFactory();

  public void setFactory(CDORevisionFactory factory);

  public InternalCDORevisionCache getCache();

  public void setCache(CDORevisionCache cache);

  public void reviseLatest(CDOID id, CDOBranch branch);

  public void reviseVersion(CDOID id, CDOBranchVersion branchVersion, long timeStamp);

  public CDORevision getRevision(CDOID id, CDOBranchPoint branchPoint, int referenceChunk, int prefetchDepth,
      boolean loadOnDemand, Map<CDORevision, Long> revisedPointers);

  public List<CDORevision> getRevisions(List<CDOID> ids, CDOBranchPoint branchPoint, int referenceChunk,
      int prefetchDepth, boolean loadOnDemand, Map<CDORevision, Long> revisedPointers);

  /**
   * @author Eike Stepper
   * @since 3.0
   */
  public interface RevisionLoader
  {
    public List<InternalCDORevision> loadRevisions(Collection<MissingRevisionInfo> infos, CDOBranchPoint branchPoint,
        int referenceChunk, int prefetchDepth);

    public InternalCDORevision loadRevisionByVersion(CDOID id, CDOBranchVersion branchVersion, int referenceChunk);

    /**
     * @author Eike Stepper
     * @since 3.0
     */
    public static class MissingRevisionInfo
    {
      protected static final byte NO_REVISION = 0;

      protected static final byte NORMAL_REVISION = 1;

      protected static final byte POINTER_REVISION = 2;

      protected static final byte DETACHED_REVISION = 3;

      private CDOID id;

      private long revised = CDORevision.UNSPECIFIED_DATE;

      public MissingRevisionInfo(CDOID id)
      {
        this.id = id;
      }

      MissingRevisionInfo(CDODataInput in) throws IOException
      {
        id = in.readCDOID();
      }

      public Type getType()
      {
        return Type.MISSING;
      }

      public CDOID getID()
      {
        return id;
      }

      public CDOBranchVersion getBranchVersion()
      {
        return null;
      }

      public long getRevised()
      {
        return revised;
      }

      public void setRevised(long revised)
      {
        this.revised = revised;
      }

      public void write(CDODataOutput out) throws IOException
      {
        out.writeByte(getType().ordinal());
        out.writeCDOID(id);
      }

      public InternalCDORevision execute(InternalCDORevisionManager revisionManager, CDOBranchPoint branchPoint,
          int referenceChunk)
      {
        Map<CDORevision, Long> revisedPointers = new HashMap<CDORevision, Long>();
        CDORevision revision = revisionManager.getRevision(id, branchPoint, referenceChunk, CDORevision.DEPTH_NONE,
            true, revisedPointers);
        Long revisedPointer = revisedPointers.get(revision);
        if (revisedPointer != null)
        {
          revised = revisedPointer;
        }

        return (InternalCDORevision)revision;
      }

      public void writeResult(CDODataOutput out, CDOBranch branch, CDORevision revision, int referenceChunk)
          throws IOException
      {
        if (revision.getBranch() != branch)
        {
          out.writeByte(POINTER_REVISION);
          out.writeLong(revised);
          out.writeCDORevision(revision, referenceChunk);
        }
        else if (revision == null)
        {
          out.writeByte(DETACHED_REVISION);
          out.writeLong(revised);
        }
        else
        {
          out.writeByte(NORMAL_REVISION);
          out.writeCDORevision(revision, referenceChunk);
        }
      }

      public final InternalCDORevision readResult(CDODataInput in, CDOBranch branch) throws IOException
      {
        byte resultType = in.readByte();
        return readResult(in, branch, resultType);
      }

      protected InternalCDORevision readResult(CDODataInput in, CDOBranch branch, byte resultType) throws IOException
      {
        switch (resultType)
        {
        case NORMAL_REVISION:
          return (InternalCDORevision)in.readCDORevision();

        case POINTER_REVISION:
          revised = in.readLong();
          return (InternalCDORevision)in.readCDORevision();

        case DETACHED_REVISION:
          return new DetachedCDORevision(id, branch, in.readLong());

        default:
          throw new IllegalStateException("Invalid result type: " + resultType);
        }
      }

      @Override
      public String toString()
      {
        return MessageFormat.format("Missing[{0}]", id);
      }

      public static MissingRevisionInfo read(CDODataInput in) throws IOException
      {
        byte ordinal = in.readByte();
        switch (Type.values()[ordinal])
        {
        case MISSING:
          return new MissingRevisionInfo(in);
        case POSSIBLY_AVAILABLE:
          return new PossiblyAvailable(in);
        case EXACTLY_KNOWN:
          return new ExactlyKnown(in);
        default:
          throw new IOException(); // Can not happen
        }
      }

      /**
       * @author Eike Stepper
       * @since 3.0
       */
      public static enum Type
      {
        /**
         * Indicates that the revision must be normally loaded by {@link MissingRevisionInfo#getID() ID} starting from
         * the branch point of the request/view, possibly recursing up to the main branch. If the requested revision
         * exists, it will be loaded.
         */
        MISSING,

        /**
         * Indicates that the revision must be normally loaded by {@link MissingRevisionInfo#getID() ID} starting from
         * the <b>base</b> of the branch point of the request/view, possibly recursing up to the given
         * {@link MissingRevisionInfo#getBranchVersion() branch version} which is already available to the requestor.If
         * the requested revision exists and is <b>different</b> from the one that is possibly available, it will be
         * loaded.
         */
        POSSIBLY_AVAILABLE,

        /**
         * Indicates that the requestor already knows the exact {@link MissingRevisionInfo#getBranchVersion() branch
         * version}. If the requested revision exists, it will be loaded.
         */
        EXACTLY_KNOWN
      }

      /**
       * @author Eike Stepper
       * @since 3.0
       */
      public static class PossiblyAvailable extends MissingRevisionInfo
      {
        private CDOBranchVersion available;

        public PossiblyAvailable(CDOID id, InternalCDORevision available)
        {
          super(id);
          this.available = available;
        }

        PossiblyAvailable(CDODataInput in) throws IOException
        {
          super(in);
          available = in.readCDOBranchVersion();
        }

        @Override
        public Type getType()
        {
          return Type.POSSIBLY_AVAILABLE;
        }

        @Override
        public CDOBranchVersion getBranchVersion()
        {
          return available;
        }

        @Override
        public void write(CDODataOutput out) throws IOException
        {
          super.write(out);
          out.writeCDOBranchVersion(available);
        }

        @Override
        public void writeResult(CDODataOutput out, CDOBranch branch, CDORevision revision, int referenceChunk)
            throws IOException
        {
          boolean useAvailable = revision.getBranch().equals(available.getBranch())
              && revision.getVersion() == available.getVersion();
          if (useAvailable)
          {
            out.writeByte(NO_REVISION);
          }
          else
          {
            super.writeResult(out, branch, revision, referenceChunk);
          }
        }

        @Override
        protected InternalCDORevision readResult(CDODataInput in, CDOBranch branch, byte resultType) throws IOException
        {
          if (resultType == NO_REVISION)
          {
            return (InternalCDORevision)available;
          }

          return super.readResult(in, branch, resultType);
        }

        @Override
        public String toString()
        {
          return MessageFormat.format("PossiblyAvailable[{0}, {1}]", getID(), available);
        }
      }

      /**
       * @author Eike Stepper
       * @since 3.0
       */
      public static class ExactlyKnown extends MissingRevisionInfo
      {
        private CDOBranchVersion branchVersion;

        public ExactlyKnown(CDOID id, CDOBranchVersion branchVersion)
        {
          super(id);
          this.branchVersion = branchVersion;
        }

        ExactlyKnown(CDODataInput in) throws IOException
        {
          super(in);
          branchVersion = in.readCDOBranchVersion();
        }

        @Override
        public Type getType()
        {
          return Type.EXACTLY_KNOWN;
        }

        @Override
        public CDOBranchVersion getBranchVersion()
        {
          return branchVersion;
        }

        @Override
        public void write(CDODataOutput out) throws IOException
        {
          super.write(out);
          out.writeCDOBranchVersion(branchVersion);
        }

        @Override
        public String toString()
        {
          return MessageFormat.format("PossiblyAvailable[{0}, {1}]", getID(), branchVersion);
        }
      }
    }
  }

  /**
   * @author Eike Stepper
   * @since 3.0
   */
  public interface RevisionLocker
  {
    public void acquireAtomicRequestLock(Object key);

    public void releaseAtomicRequestLock(Object key);
  }
}
