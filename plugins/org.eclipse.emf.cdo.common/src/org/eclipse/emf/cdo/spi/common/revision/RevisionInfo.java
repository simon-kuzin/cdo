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

import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.branch.CDOBranchVersion;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.revision.CDORevision;

import java.io.IOException;

/**
 * @author Eike Stepper
 * @since 3.0
 */
public abstract class RevisionInfo
{
  private CDOID id;

  private CDOBranchPoint requestedBranchPoint;

  private InternalCDORevision result;

  private SyntheticCDORevision synthetic;

  protected RevisionInfo(CDOID id, CDOBranchPoint requestedBranchPoint)
  {
    this.id = id;
    this.requestedBranchPoint = requestedBranchPoint;
  }

  protected RevisionInfo(CDODataInput in, CDOBranchPoint requestedBranchPoint) throws IOException
  {
    id = in.readCDOID();
    this.requestedBranchPoint = requestedBranchPoint;
  }

  public abstract Type getType();

  public final CDOID getID()
  {
    return id;
  }

  public final CDOBranchPoint getRequestedBranchPoint()
  {
    return requestedBranchPoint;
  }

  public InternalCDORevision getResult()
  {
    return result;
  }

  public void setResult(InternalCDORevision result)
  {
    this.result = result;
  }

  public SyntheticCDORevision getSynthetic()
  {
    return synthetic;
  }

  public void setSynthetic(SyntheticCDORevision synthetic)
  {
    this.synthetic = synthetic;
  }

  public abstract boolean isLoadNeeded();

  public void write(CDODataOutput out) throws IOException
  {
    out.writeByte(getType().ordinal());
    out.writeCDOID(getID());
  }

  public static RevisionInfo read(CDODataInput in, CDOBranchPoint requestedBranchPoint) throws IOException
  {
    byte ordinal = in.readByte();
    Type type = Type.values()[ordinal];
    switch (type)
    {
    case AVAILABLE_NORMAL:
      return new Available.Normal(in, requestedBranchPoint);

    case AVAILABLE_POINTER:
      return new Available.Pointer(in, requestedBranchPoint);

    case AVAILABLE_DETACHED:
      return new Available.Detached(in, requestedBranchPoint);

    case MISSING_MAINBRANCH:
      return new Missing.MainBranch(in, requestedBranchPoint);

    case MISSING_SUBBRANCH:
      return new Missing.SubBranch(in, requestedBranchPoint);

    default:
      throw new IOException("Invalid revision info type: " + type);
    }
  }

  public void execute(InternalCDORevisionManager revisionManager, int referenceChunk)
  {
    SyntheticCDORevision[] synthetics = new SyntheticCDORevision[1];
    result = (InternalCDORevision)revisionManager.getRevision(getID(), requestedBranchPoint, referenceChunk,
        CDORevision.DEPTH_NONE, true, synthetics);
    synthetic = synthetics[0];
  }

  public final void writeResult(CDODataOutput out, int referenceChunk) throws IOException
  {
    // result.write(out, referenceChunk);
  }

  public final void readResult(CDODataInput in) throws IOException
  {
    // result = RevisionResult.read(in);
  }

  /**
   * @author Eike Stepper
   * @since 3.0
   */
  public static enum Type
  {
    AVAILABLE_NORMAL, AVAILABLE_POINTER, AVAILABLE_DETACHED, MISSING_MAINBRANCH, MISSING_SUBBRANCH
  }

  /**
   * @author Eike Stepper
   * @since 3.0
   */
  public static abstract class Available extends RevisionInfo
  {
    private CDOBranchVersion availableBranchVersion;

    protected Available(CDOID id, CDOBranchPoint requestedBranchPoint, CDOBranchVersion availableBranchVersion)
    {
      super(id, requestedBranchPoint);
      this.availableBranchVersion = availableBranchVersion;
    }

    protected Available(CDODataInput in, CDOBranchPoint requestedBranchPoint) throws IOException
    {
      super(in, requestedBranchPoint);
      availableBranchVersion = in.readCDOBranchVersion();
    }

    public CDOBranchVersion getAvailableBranchVersion()
    {
      return availableBranchVersion;
    }

    public boolean isDirect()
    {
      return availableBranchVersion.getBranch() == getRequestedBranchPoint().getBranch();
    }

    @Override
    public boolean isLoadNeeded()
    {
      return !isDirect();
    }

    @Override
    public void write(CDODataOutput out) throws IOException
    {
      super.write(out);
      out.writeCDOBranchVersion(availableBranchVersion);
    }

    /**
     * @author Eike Stepper
     * @since 3.0
     */
    public static class Normal extends Available
    {
      public Normal(CDOID id, CDOBranchPoint requestedBranchPoint, CDOBranchVersion availableBranchVersion)
      {
        super(id, requestedBranchPoint, availableBranchVersion);
      }

      private Normal(CDODataInput in, CDOBranchPoint requestedBranchPoint) throws IOException
      {
        super(in, requestedBranchPoint);
      }

      @Override
      public Type getType()
      {
        return Type.AVAILABLE_NORMAL;
      }

      @Override
      public InternalCDORevision getResult()
      {
        if (isDirect())
        {
          return (InternalCDORevision)getAvailableBranchVersion();
        }

        return super.getResult();
      }
    }

    /**
     * @author Eike Stepper
     * @since 3.0
     */
    public static class Pointer extends Available
    {
      private CDOBranchVersion targetBranchVersion;

      public Pointer(CDOID id, CDOBranchPoint requestedBranchPoint, CDOBranchVersion availableBranchVersion,
          CDOBranchVersion targetBranchVersion)
      {
        super(id, requestedBranchPoint, availableBranchVersion);
        this.targetBranchVersion = targetBranchVersion;
      }

      private Pointer(CDODataInput in, CDOBranchPoint requestedBranchPoint) throws IOException
      {
        super(in, requestedBranchPoint);
        boolean hasTarget = in.readBoolean();
        if (!hasTarget)
        {
          targetBranchVersion = in.readCDOBranchVersion();
        }
      }

      public CDOBranchVersion getTargetBranchVersion()
      {
        return targetBranchVersion;
      }

      @Override
      public Type getType()
      {
        return Type.AVAILABLE_POINTER;
      }

      public boolean hasTarget()
      {
        return targetBranchVersion != null;
      }

      @Override
      public boolean isLoadNeeded()
      {
        return !isDirect() || !hasTarget();
      }

      @Override
      public void write(CDODataOutput out) throws IOException
      {
        super.write(out);
        if (targetBranchVersion != null)
        {
          out.writeBoolean(true);
          out.writeCDOBranchVersion(targetBranchVersion);
        }
        else
        {
          out.writeBoolean(false);
        }
      }
    }

    /**
     * @author Eike Stepper
     * @since 3.0
     */
    public static class Detached extends Available
    {
      public Detached(CDOID id, CDOBranchPoint requestedBranchPoint, CDOBranchVersion availableBranchVersion)
      {
        super(id, requestedBranchPoint, availableBranchVersion);
      }

      private Detached(CDODataInput in, CDOBranchPoint requestedBranchPoint) throws IOException
      {
        super(in, requestedBranchPoint);
      }

      @Override
      public Type getType()
      {
        return Type.AVAILABLE_DETACHED;
      }
    }
  }

  /**
   * @author Eike Stepper
   * @since 3.0
   */
  public static abstract class Missing extends RevisionInfo
  {
    protected Missing(CDOID id, CDOBranchPoint requestedBranchPoint)
    {
      super(id, requestedBranchPoint);
    }

    protected Missing(CDODataInput in, CDOBranchPoint requestedBranchPoint) throws IOException
    {
      super(in, requestedBranchPoint);
    }

    /**
     * @author Eike Stepper
     * @since 3.0
     */
    public static class MainBranch extends Missing
    {
      public MainBranch(CDOID id, CDOBranchPoint requestedBranchPoint)
      {
        super(id, requestedBranchPoint);
      }

      private MainBranch(CDODataInput in, CDOBranchPoint requestedBranchPoint) throws IOException
      {
        super(in, requestedBranchPoint);
      }

      @Override
      public Type getType()
      {
        return Type.MISSING_MAINBRANCH;
      }

      @Override
      public boolean isLoadNeeded()
      {
        return false;
      }
    }

    /**
     * @author Eike Stepper
     * @since 3.0
     */
    public static class SubBranch extends Missing
    {
      public SubBranch(CDOID id, CDOBranchPoint requestedBranchPoint)
      {
        super(id, requestedBranchPoint);
      }

      private SubBranch(CDODataInput in, CDOBranchPoint requestedBranchPoint) throws IOException
      {
        super(in, requestedBranchPoint);
      }

      @Override
      public Type getType()
      {
        return Type.MISSING_SUBBRANCH;
      }

      @Override
      public boolean isLoadNeeded()
      {
        return true;
      }
    }
  }
}
