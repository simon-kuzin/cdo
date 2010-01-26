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

import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;

import java.io.IOException;

/**
 * @author Eike Stepper
 * @since 3.0
 */
public abstract class RevisionResult
{
  private InternalCDORevision revision;

  protected RevisionResult(InternalCDORevision revision)
  {
    this.revision = revision;
  }

  protected RevisionResult()
  {
  }

  public abstract Type getType();

  public final InternalCDORevision getRevision()
  {
    return revision;
  }

  public final void setRevision(InternalCDORevision revision)
  {
    this.revision = revision;
  }

  public void write(CDODataOutput out, int referenceChunk) throws IOException
  {
    out.writeByte(getType().ordinal());
  }

  public static RevisionResult read(CDODataInput in) throws IOException
  {
    byte ordinal = in.readByte();
    Type type = Type.values()[ordinal];
    switch (type)
    {
    case NONE:
      return new None(in);

    case NORMAL:
      return new Normal(in);

    case POINTER:
      return new Pointer(in);

    case DETACHED:
      return new Detached(in);

    default:
      throw new IOException("Invalid revision result type: " + type);
    }
  }

  /**
   * @author Eike Stepper
   * @since 3.0
   */
  public static enum Type
  {
    NONE, NORMAL, POINTER, DETACHED
  }

  /**
   * @author Eike Stepper
   * @since 3.0
   */
  public static final class None extends RevisionResult
  {
    private None(CDODataInput in) throws IOException
    {
    }

    @Override
    public Type getType()
    {
      return Type.NONE;
    }
  }

  /**
   * @author Eike Stepper
   * @since 3.0
   */
  public static final class Normal extends RevisionResult
  {
    public Normal(InternalCDORevision revision)
    {
      super(revision);
    }

    private Normal(CDODataInput in) throws IOException
    {
      super((InternalCDORevision)in.readCDORevision());
    }

    @Override
    public Type getType()
    {
      return Type.NORMAL;
    }

    @Override
    public void write(CDODataOutput out, int referenceChunk) throws IOException
    {
      super.write(out, referenceChunk);
      out.writeCDORevision(getRevision(), referenceChunk);
    }
  }

  /**
   * @author Eike Stepper
   * @since 3.0
   */
  public static final class Pointer extends RevisionResult
  {
    private long revised;

    public Pointer(PointerCDORevision pointer)
    {
      super(pointer);
    }

    private Pointer(CDODataInput in) throws IOException
    {
    }

    @Override
    public Type getType()
    {
      return Type.POINTER;
    }

    @Override
    public void write(CDODataOutput out, int referenceChunk) throws IOException
    {
      super.write(out, referenceChunk);
      out.writeLong(revised);
    }
  }

  /**
   * @author Eike Stepper
   * @since 3.0
   */
  public static final class Detached extends RevisionResult
  {
    public Detached(DetachedCDORevision detached)
    {
      super(detached);
    }

    private Detached(CDODataInput in) throws IOException
    {
    }

    @Override
    public Type getType()
    {
      return Type.DETACHED;
    }
  }
}
