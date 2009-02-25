/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Simon McDuff - http://bugs.eclipse.org/213402
 */
package org.eclipse.emf.cdo.internal.common.io;

import org.eclipse.emf.cdo.common.TODO;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDAndVersion;
import org.eclipse.emf.cdo.common.id.CDOIDMetaRange;
import org.eclipse.emf.cdo.common.id.CDOIDObject;
import org.eclipse.emf.cdo.common.id.CDOIDObjectFactory;
import org.eclipse.emf.cdo.common.id.CDOID.Type;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.model.CDOClassifierRef;
import org.eclipse.emf.cdo.common.model.CDOPackageInfo;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.common.model.CDOPackageURICompressor;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.model.CDOPackageUnitManager;
import org.eclipse.emf.cdo.common.revision.CDOList;
import org.eclipse.emf.cdo.common.revision.CDOListFactory;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.CDORevisionResolver;
import org.eclipse.emf.cdo.common.revision.CDORevisionUtil;
import org.eclipse.emf.cdo.common.revision.delta.CDOFeatureDelta;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.internal.common.id.CDOIDAndVersionImpl;
import org.eclipse.emf.cdo.internal.common.id.CDOIDExternalImpl;
import org.eclipse.emf.cdo.internal.common.id.CDOIDExternalTempImpl;
import org.eclipse.emf.cdo.internal.common.id.CDOIDMetaImpl;
import org.eclipse.emf.cdo.internal.common.id.CDOIDMetaRangeImpl;
import org.eclipse.emf.cdo.internal.common.id.CDOIDTempMetaImpl;
import org.eclipse.emf.cdo.internal.common.id.CDOIDTempObjectImpl;
import org.eclipse.emf.cdo.internal.common.model.CDOPackageInfoImpl;
import org.eclipse.emf.cdo.internal.common.model.CDOPackageUnitImpl;
import org.eclipse.emf.cdo.internal.common.revision.delta.CDOAddFeatureDeltaImpl;
import org.eclipse.emf.cdo.internal.common.revision.delta.CDOClearFeatureDeltaImpl;
import org.eclipse.emf.cdo.internal.common.revision.delta.CDOContainerFeatureDeltaImpl;
import org.eclipse.emf.cdo.internal.common.revision.delta.CDOListFeatureDeltaImpl;
import org.eclipse.emf.cdo.internal.common.revision.delta.CDOMoveFeatureDeltaImpl;
import org.eclipse.emf.cdo.internal.common.revision.delta.CDORemoveFeatureDeltaImpl;
import org.eclipse.emf.cdo.internal.common.revision.delta.CDORevisionDeltaImpl;
import org.eclipse.emf.cdo.internal.common.revision.delta.CDOSetFeatureDeltaImpl;
import org.eclipse.emf.cdo.internal.common.revision.delta.CDOUnsetFeatureDeltaImpl;
import org.eclipse.emf.cdo.spi.common.id.AbstractCDOID;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageInfo;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;

import org.eclipse.net4j.util.ImplementationError;
import org.eclipse.net4j.util.concurrent.RWLockManager;
import org.eclipse.net4j.util.io.ExtendedDataInput;
import org.eclipse.net4j.util.io.ExtendedIOUtil.ClassResolver;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public abstract class CDODataInputImpl implements CDODataInput
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_PROTOCOL, CDODataInputImpl.class);

  private ExtendedDataInput in;

  public CDODataInputImpl(ExtendedDataInput in)
  {
    this.in = in;
  }

  public ExtendedDataInput getDelegate()
  {
    return in;
  }

  public boolean readBoolean() throws IOException
  {
    return in.readBoolean();
  }

  public byte readByte() throws IOException
  {
    return in.readByte();
  }

  public byte[] readByteArray() throws IOException
  {
    return in.readByteArray();
  }

  public char readChar() throws IOException
  {
    return in.readChar();
  }

  public double readDouble() throws IOException
  {
    return in.readDouble();
  }

  public float readFloat() throws IOException
  {
    return in.readFloat();
  }

  public void readFully(byte[] b, int off, int len) throws IOException
  {
    in.readFully(b, off, len);
  }

  public void readFully(byte[] b) throws IOException
  {
    in.readFully(b);
  }

  public int readInt() throws IOException
  {
    return in.readInt();
  }

  public String readLine() throws IOException
  {
    return in.readLine();
  }

  public long readLong() throws IOException
  {
    return in.readLong();
  }

  public Object readObject() throws IOException
  {
    return in.readObject();
  }

  public Object readObject(ClassLoader classLoader) throws IOException
  {
    return in.readObject(classLoader);
  }

  public Object readObject(ClassResolver classResolver) throws IOException
  {
    return in.readObject(classResolver);
  }

  public short readShort() throws IOException
  {
    return in.readShort();
  }

  public String readString() throws IOException
  {
    return in.readString();
  }

  public int readUnsignedByte() throws IOException
  {
    return in.readUnsignedByte();
  }

  public int readUnsignedShort() throws IOException
  {
    return in.readUnsignedShort();
  }

  public String readUTF() throws IOException
  {
    return in.readUTF();
  }

  public int skipBytes(int n) throws IOException
  {
    return in.skipBytes(n);
  }

  public CDOPackageUnit readCDOPackageUnit() throws IOException
  {
    boolean dynamic = in.readBoolean();
    InternalCDOPackageUnit packageUnit = dynamic ? new CDOPackageUnitImpl.Dynamic()
        : new CDOPackageUnitImpl.Generated();
    packageUnit.read(this);
    return packageUnit;
  }

  public CDOPackageInfo readCDOPackageInfo() throws IOException
  {
    InternalCDOPackageInfo packageInfo = new CDOPackageInfoImpl();
    packageInfo.read(this);
    return packageInfo;
  }

  public String readEPackageURI() throws IOException
  {
    return getPackageURICompressor().readPackageURI(this);
  }

  public void readEPackage(EPackage cdoPackage) throws IOException
  {
    TODO.readEPackage(this, cdoPackage);
  }

  public CDOClassifierRef readEClassifierRef() throws IOException
  {
    return new CDOClassifierRef(this);
  }

  public EClassifier readEClassifierRefAndResolve() throws IOException
  {
    CDOClassifierRef classRef = readEClassifierRef();
    EClassifier cdoClass = classRef.resolve(getPackageRegistry());
    if (cdoClass == null)
    {
      throw new IllegalStateException("CDOClassifierRef unresolveable: " + classRef);
    }

    return cdoClass;
  }

  public CDOID readCDOID() throws IOException
  {
    byte ordinal = readByte();
    if (TRACER.isEnabled())
    {
      String type;
      try
      {
        type = Type.values()[ordinal].toString();
      }
      catch (RuntimeException ex)
      {
        type = ex.getMessage();
      }

      TRACER.format("Reading CDOID of type {0} ({1})", ordinal, type);
    }

    Type type = Type.values()[ordinal];
    switch (type)
    {
    case NULL:
      return CDOID.NULL;

    case TEMP_OBJECT:
      return new CDOIDTempObjectImpl(readInt());

    case TEMP_META:
      return new CDOIDTempMetaImpl(readInt());

    case META:
      return new CDOIDMetaImpl(readLong());

    case EXTERNAL_OBJECT:
      return new CDOIDExternalImpl(readString());

    case EXTERNAL_TEMP_OBJECT:
      return new CDOIDExternalTempImpl(readString());

    case OBJECT:
    {
      CDOIDObject id = getIDFactory().createCDOIDObject(this);
      ((AbstractCDOID)id).read(this);
      return id;
    }

    default:
      throw new ImplementationError();
    }
  }

  public CDOIDAndVersion readCDOIDAndVersion() throws IOException
  {
    return new CDOIDAndVersionImpl(this);
  }

  public CDOIDMetaRange readCDOIDMetaRange() throws IOException
  {
    boolean exist = readBoolean();
    if (exist)
    {
      return new CDOIDMetaRangeImpl(readCDOID(), readInt());
    }

    return null;
  }

  public CDORevision readCDORevision() throws IOException
  {
    boolean notNull = readBoolean();
    if (notNull)
    {
      return readCDORevisionData();
    }

    return null;
  }

  public CDOList readCDOList(CDORevision revision, EStructuralFeature feature) throws IOException
  {
    return TODO.readCDOList(this, revision, feature, getListFactory());
  }

  public CDORevisionDelta readCDORevisionDelta() throws IOException
  {
    return new CDORevisionDeltaImpl(this);
  }

  public CDOFeatureDelta readCDOFeatureDelta(EClass cdoClass) throws IOException
  {
    int typeOrdinal = readInt();
    CDOFeatureDelta.Type type = CDOFeatureDelta.Type.values()[typeOrdinal];
    switch (type)
    {
    case ADD:
      return new CDOAddFeatureDeltaImpl(this, cdoClass);

    case SET:
      return new CDOSetFeatureDeltaImpl(this, cdoClass);

    case LIST:
      return new CDOListFeatureDeltaImpl(this, cdoClass);

    case MOVE:
      return new CDOMoveFeatureDeltaImpl(this, cdoClass);

    case CLEAR:
      return new CDOClearFeatureDeltaImpl(this, cdoClass);

    case REMOVE:
      return new CDORemoveFeatureDeltaImpl(this, cdoClass);

    case CONTAINER:
      return new CDOContainerFeatureDeltaImpl(this, cdoClass);

    case UNSET:
      return new CDOUnsetFeatureDeltaImpl(this, cdoClass);

    default:
      throw new IOException("Invalid type " + typeOrdinal);
    }
  }

  public Object readCDORevisionOrPrimitive() throws IOException
  {
    return TODO.readCDORevisionOrPrimitive(this);
  }

  public Object readCDORevisionOrPrimitiveOrClass() throws IOException
  {
    boolean isClass = readBoolean();
    if (isClass)
    {
      return readEClassifierRefAndResolve();
    }

    return readCDORevisionOrPrimitive();
  }

  public RWLockManager.LockType readCDOLockType() throws IOException
  {
    return readBoolean() ? RWLockManager.LockType.WRITE : RWLockManager.LockType.READ;
  }

  protected CDORevision readCDORevisionData() throws IOException
  {
    return CDORevisionUtil.read(this);
  }

  protected CDOPackageUnitManager getPackageUnitManager()
  {
    return getPackageRegistry().getPackageUnitManager();
  }

  protected abstract CDOPackageRegistry getPackageRegistry();

  protected abstract CDOPackageURICompressor getPackageURICompressor();

  protected abstract CDORevisionResolver getRevisionResolver();

  protected abstract CDOIDObjectFactory getIDFactory();

  protected abstract CDOListFactory getListFactory();
}
