/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Stefan Winkler - https://bugs.eclipse.org/bugs/show_bug.cgi?id=259402
 */
package org.eclipse.emf.cdo.server.internal.db;

import org.eclipse.emf.cdo.common.CDOQueryInfo;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDMeta;
import org.eclipse.emf.cdo.common.id.CDOIDMetaRange;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.model.CDOClassifierRef;
import org.eclipse.emf.cdo.common.model.CDOModelUtil;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.model.EMFUtil;
import org.eclipse.emf.cdo.common.revision.CDORevisionUtil;
import org.eclipse.emf.cdo.server.IQueryContext;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.ISession;
import org.eclipse.emf.cdo.server.IStoreAccessor;
import org.eclipse.emf.cdo.server.ITransaction;
import org.eclipse.emf.cdo.server.IStore.RevisionTemporality;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.server.db.mapping.IClassMapping;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.emf.cdo.server.internal.db.bundle.OM;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageInfo;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionDelta;
import org.eclipse.emf.cdo.spi.server.LongIDStoreAccessor;

import org.eclipse.net4j.db.DBException;
import org.eclipse.net4j.db.DBUtil;
import org.eclipse.net4j.db.IDBRowHandler;
import org.eclipse.net4j.db.ddl.IDBTable;
import org.eclipse.net4j.util.ReflectUtil.ExcludeFromDump;
import org.eclipse.net4j.util.collection.CloseableIterator;
import org.eclipse.net4j.util.om.monitor.OMMonitor;
import org.eclipse.net4j.util.om.monitor.ProgressDistributable;
import org.eclipse.net4j.util.om.monitor.ProgressDistributor;
import org.eclipse.net4j.util.om.monitor.OMMonitor.Async;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * @author Eike Stepper
 */
public class DBStoreAccessor extends LongIDStoreAccessor implements IDBStoreAccessor
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, DBStoreAccessor.class);

  /** @deprecated move to meta manager */
  @Deprecated
  private static final boolean ZIP_PACKAGE_BYTES = true;

  private Connection connection = null;

  @ExcludeFromDump
  @SuppressWarnings("unchecked")
  private final ProgressDistributable<CommitContext>[] ops = ProgressDistributor.array( //
      new ProgressDistributable.Default<CommitContext>()
      {
        public void runLoop(int index, CommitContext commitContext, OMMonitor monitor) throws Exception
        {
          DBStoreAccessor.super.write(commitContext, monitor.fork());
        }
      }, //
      new ProgressDistributable.Default<CommitContext>()
      {
        public void runLoop(int index, CommitContext commitContext, OMMonitor monitor) throws Exception
        {
          // TODO - reenable when reimplementing stmt caching
          // flush(monitor.fork());
        }
      });

  public DBStoreAccessor(DBStore store, ISession session) throws DBException
  {
    super(store, session);
  }

  public DBStoreAccessor(DBStore store, ITransaction transaction) throws DBException
  {
    super(store, transaction);
  }

  @Override
  public DBStore getStore()
  {
    return (DBStore)super.getStore();
  }

  /** TODO: check how to handle */
  public DBStoreChunkReader createChunkReader(InternalCDORevision revision, EStructuralFeature feature)
  {
    return new DBStoreChunkReader(this, revision, feature);
  }

  /**
   * @deprecated move to meta manager
   */
  @Deprecated
  public final Collection<InternalCDOPackageUnit> readPackageUnits()
  {
    final Map<String, InternalCDOPackageUnit> packageUnits = new HashMap<String, InternalCDOPackageUnit>();
    IDBRowHandler unitRowHandler = new IDBRowHandler()
    {
      public boolean handle(int row, final Object... values)
      {
        InternalCDOPackageUnit packageUnit = createPackageUnit();
        packageUnit.setOriginalType(CDOPackageUnit.Type.values()[(Integer)values[1]]);
        packageUnit.setTimeStamp((Long)values[2]);
        packageUnits.put((String)values[0], packageUnit);
        return true;
      }
    };

    DBUtil.select(getConnection(), unitRowHandler, CDODBSchema.PACKAGE_UNITS_ID,
        CDODBSchema.PACKAGE_UNITS_ORIGINAL_TYPE, CDODBSchema.PACKAGE_UNITS_TIME_STAMP);

    final Map<String, List<InternalCDOPackageInfo>> packageInfos = new HashMap<String, List<InternalCDOPackageInfo>>();
    IDBRowHandler infoRowHandler = new IDBRowHandler()
    {
      public boolean handle(int row, final Object... values)
      {
        long metaLB = (Long)values[3];
        long metaUB = (Long)values[4];
        CDOIDMetaRange metaIDRange = metaLB == 0 ? null : CDOIDUtil.createMetaRange(CDOIDUtil.createMeta(metaLB),
            (int)(metaUB - metaLB) + 1);

        InternalCDOPackageInfo packageInfo = createPackageInfo();
        packageInfo.setPackageURI((String)values[1]);
        packageInfo.setParentURI((String)values[2]);
        packageInfo.setMetaIDRange(metaIDRange);

        String unit = (String)values[0];
        List<InternalCDOPackageInfo> list = packageInfos.get(unit);
        if (list == null)
        {
          list = new ArrayList<InternalCDOPackageInfo>();
          packageInfos.put(unit, list);
        }

        list.add(packageInfo);
        return true;
      }
    };

    DBUtil.select(getConnection(), infoRowHandler, CDODBSchema.PACKAGE_INFOS_UNIT, CDODBSchema.PACKAGE_INFOS_URI,
        CDODBSchema.PACKAGE_INFOS_PARENT, CDODBSchema.PACKAGE_INFOS_META_LB, CDODBSchema.PACKAGE_INFOS_META_UB);

    for (Entry<String, InternalCDOPackageUnit> entry : packageUnits.entrySet())
    {
      String id = entry.getKey();
      InternalCDOPackageUnit packageUnit = entry.getValue();

      List<InternalCDOPackageInfo> list = packageInfos.get(id);
      InternalCDOPackageInfo[] array = list.toArray(new InternalCDOPackageInfo[list.size()]);
      packageUnit.setPackageInfos(array);
    }

    return packageUnits.values();
  }

  /**
   * @deprecated move to meta manager
   */
  @Deprecated
  public final EPackage[] loadPackageUnit(InternalCDOPackageUnit packageUnit)
  {
    String where = CDODBSchema.PACKAGE_UNITS_ID.getName() + "='" + packageUnit.getID() + "'";
    Object[] values = DBUtil.select(getConnection(), where, CDODBSchema.PACKAGE_UNITS_PACKAGE_DATA);
    byte[] bytes = (byte[])values[0];
    EPackage ePackage = createEPackage(packageUnit, bytes);
    return EMFUtil.getAllPackages(ePackage);
  }

  /**
   * TODO caching?
   */
  public CloseableIterator<CDOID> readObjectIDs()
  {
    if (TRACER.isEnabled())
    {
      TRACER.trace("Selecting object IDs");
    }

    return getStore().getMappingStrategy().readObjectIDs(this);
  }

  /**
   * TODO caching?
   */
  public CDOClassifierRef readObjectType(CDOID id)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Selecting object type: {0}", id);
    }

    return getStore().getMappingStrategy().readObjectType(this, id);
  }

  protected EClass getObjectType(CDOID id)
  {
    // TODO Replace calls to getObjectType by optimized calls to RevisionManager.getObjectType (cache!)
    CDOClassifierRef type = readObjectType(id);
    if (type == null)
    {
      return null;
    }

    IRepository repository = getStore().getRepository();
    CDOPackageRegistry packageRegistry = repository.getPackageRegistry();
    return (EClass)type.resolve(packageRegistry);
  }

  public InternalCDORevision readRevision(CDOID id, int referenceChunk, AdditionalRevisionCache cache)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Selecting revision: {0}", id);
    }

    EClass eClass = getObjectType(id);
    if (eClass == null)
    {
      return null;
    }

    InternalCDORevision revision = (InternalCDORevision)CDORevisionUtil.create(eClass, id);

    IMappingStrategy mappingStrategy = getStore().getMappingStrategy();
    IClassMapping mapping = mappingStrategy.getClassMapping(eClass);
    if (mapping.readRevision(this, revision, referenceChunk))
    {
      return revision;
    }

    // Reading failed - revision does not exist.
    return null;
  }

  public InternalCDORevision readRevisionByTime(CDOID id, int referenceChunk, AdditionalRevisionCache cache,
      long timeStamp)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Selecting revision: {0}, timestamp={1,date} {1,time}", id, timeStamp);
    }

    EClass eClass = getObjectType(id);
    InternalCDORevision revision = (InternalCDORevision)CDORevisionUtil.create(eClass, id);

    IMappingStrategy mappingStrategy = getStore().getMappingStrategy();
    IClassMapping mapping = mappingStrategy.getClassMapping(eClass);
    if (mapping.readRevisionByTime(this, revision, timeStamp, referenceChunk))
    {
      return revision;
    }

    // Reading failed - revision does not exist.
    return null;
  }

  public InternalCDORevision readRevisionByVersion(CDOID id, int referenceChunk, AdditionalRevisionCache cache,
      int version)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Selecting revision: {0}, version={1}", id, version);
    }

    EClass eClass = getObjectType(id);
    InternalCDORevision revision = (InternalCDORevision)CDORevisionUtil.create(eClass, id);

    IMappingStrategy mappingStrategy = getStore().getMappingStrategy();
    IClassMapping mapping = mappingStrategy.getClassMapping(eClass);
    if (mapping.readRevisionByVersion(this, revision, version, referenceChunk))
    {
      return revision;
    }

    // Reading failed - revision does not exist.
    return null;
  }

  /**
   * TODO: implement as query when query implementation is done?
   * 
   * @since 2.0
   */
  public void queryResources(QueryResourcesContext context)
  {
    IMappingStrategy mappingStrategy = getStore().getMappingStrategy();
    mappingStrategy.queryResources(this, context);
  }

  /**
   * @since 2.0
   */
  public void executeQuery(CDOQueryInfo info, IQueryContext context)
  {
    // TODO: implement DBStoreAccessor.executeQuery(info, context)
    throw new UnsupportedOperationException();
  }

  public CloseableIterator<Object> createQueryIterator(CDOQueryInfo queryInfo)
  {
    throw new UnsupportedOperationException();
  }

  public void refreshRevisions()
  {
    // TODO is this empty on purpose or should it be implemented (how?)
  }

  @Override
  public void write(CommitContext context, OMMonitor monitor)
  {
    ProgressDistributor distributor = getStore().getAccessorWriteDistributor();
    distributor.run(ops, context, monitor);
  }

  /**
   * @deprecated move to meta manager
   */
  @Deprecated
  public final void writePackageUnits(InternalCDOPackageUnit[] packageUnits, OMMonitor monitor)
  {
    try
    {
      monitor.begin(2);
      fillSystemTables(packageUnits, monitor.fork());
      createModelTables(packageUnits, monitor.fork());
    }
    finally
    {
      monitor.done();
    }
  }

  /**
   * @deprecated move to meta manager
   */
  @Deprecated
  private void fillSystemTables(InternalCDOPackageUnit[] packageUnits, OMMonitor monitor)
  {
    try
    {
      monitor.begin(packageUnits.length);
      for (InternalCDOPackageUnit packageUnit : packageUnits)
      {
        fillSystemTables(packageUnit, monitor.fork());
      }
    }
    finally
    {
      monitor.done();
    }
  }

  /**
   * @deprecated move to meta manager
   */
  @Deprecated
  private void fillSystemTables(InternalCDOPackageUnit packageUnit, OMMonitor monitor)
  {
    try
    {
      InternalCDOPackageInfo[] packageInfos = packageUnit.getPackageInfos();
      monitor.begin(1 + packageInfos.length);

      if (TRACER.isEnabled())
      {
        TRACER.format("Writing package unit: {0}", packageUnit);
      }

      String sql = "INSERT INTO " + CDODBSchema.PACKAGE_UNITS + " VALUES (?, ?, ?, ?)";
      DBUtil.trace(sql);
      PreparedStatement pstmt = null;
      Async async = monitor.forkAsync();

      try
      {
        pstmt = getPreparedStatement(sql);
        pstmt.setString(1, packageUnit.getID());
        pstmt.setInt(2, packageUnit.getOriginalType().ordinal());
        pstmt.setLong(3, packageUnit.getTimeStamp());
        pstmt.setBytes(4, getEPackageBytes(packageUnit));

        if (pstmt.execute())
        {
          throw new DBException("No result set expected");
        }

        if (pstmt.getUpdateCount() == 0)
        {
          throw new DBException("No row inserted into table " + CDODBSchema.PACKAGE_UNITS);
        }
      }
      catch (SQLException ex)
      {
        throw new DBException(ex);
      }
      finally
      {
        DBUtil.close(pstmt);
        async.stop();
      }

      for (InternalCDOPackageInfo packageInfo : packageInfos)
      {
        fillSystemTables(packageInfo, monitor); // Don't fork monitor
      }
    }
    finally
    {
      monitor.done();
    }
  }

  /**
   * TODO - inline statement preparing to simplify handling
   */
  private PreparedStatement getPreparedStatement(String sql) throws SQLException
  {
    return getConnection().prepareStatement(sql);
  }

  /**
   * @deprecated move to meta manager
   */
  @Deprecated
  private byte[] getEPackageBytes(InternalCDOPackageUnit packageUnit)
  {
    EPackage ePackage = packageUnit.getTopLevelPackageInfo().getEPackage();
    CDOPackageRegistry packageRegistry = getStore().getRepository().getPackageRegistry();
    return EMFUtil.getEPackageBytes(ePackage, ZIP_PACKAGE_BYTES, packageRegistry);
  }

  /**
   * @deprecated move to meta manager
   */
  @Deprecated
  private EPackage createEPackage(InternalCDOPackageUnit packageUnit, byte[] bytes)
  {
    CDOPackageRegistry packageRegistry = getStore().getRepository().getPackageRegistry();
    return EMFUtil.createEPackage(packageUnit.getID(), bytes, ZIP_PACKAGE_BYTES, packageRegistry);
  }

  /**
   * @deprecated move to meta manager
   */
  @Deprecated
  private void fillSystemTables(InternalCDOPackageInfo packageInfo, OMMonitor monitor)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Writing package info: {0}", packageInfo);
    }

    String packageURI = packageInfo.getPackageURI();
    String parentURI = packageInfo.getParentURI();
    String unitID = packageInfo.getPackageUnit().getID();
    CDOIDMetaRange metaIDRange = packageInfo.getMetaIDRange();
    long metaLB = metaIDRange == null ? 0L : ((CDOIDMeta)metaIDRange.getLowerBound()).getLongValue();
    long metaUB = metaIDRange == null ? 0L : ((CDOIDMeta)metaIDRange.getUpperBound()).getLongValue();

    String sql = "INSERT INTO " + CDODBSchema.PACKAGE_INFOS + " VALUES (?, ?, ?, ?, ?)";
    DBUtil.trace(sql);
    PreparedStatement pstmt = null;
    Async async = monitor.forkAsync();

    try
    {
      pstmt = getPreparedStatement(sql);
      pstmt.setString(1, packageURI);
      pstmt.setString(2, parentURI);
      pstmt.setString(3, unitID);
      pstmt.setLong(4, metaLB);
      pstmt.setLong(5, metaUB);

      if (pstmt.execute())
      {
        throw new DBException("No result set expected");
      }

      if (pstmt.getUpdateCount() == 0)
      {
        throw new DBException("No row inserted into table " + CDODBSchema.PACKAGE_INFOS);
      }
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
    finally
    {
      DBUtil.close(pstmt);
      async.stop();
    }
  }

  /**
   * @deprecated move to meta manager
   */
  @Deprecated
  private void createModelTables(InternalCDOPackageUnit[] packageUnits, OMMonitor monitor)
  {
    monitor.begin();
    Async async = monitor.forkAsync();

    try
    {
      Set<IDBTable> affectedTables = mapPackageUnits(packageUnits);
      getStore().getDBAdapter().createTables(affectedTables, getConnection());
    }
    finally
    {
      async.stop();
      monitor.done();
    }
  }

  @Override
  protected void writeRevisionDeltas(InternalCDORevisionDelta[] revisionDeltas, long created, OMMonitor monitor)
  {
    // TODO move check to mapping strategy

    if (!(getStore().getRevisionTemporality() == RevisionTemporality.NONE))
    {
      throw new UnsupportedOperationException("Revision Deltas are only supported in non-auditing mode!");
    }

    monitor.begin(revisionDeltas.length);
    try
    {
      for (InternalCDORevisionDelta delta : revisionDeltas)
      {
        writeRevisionDelta(delta, created, monitor.fork());
      }
    }
    finally
    {
      monitor.done();
    }
  }

  protected void writeRevisionDelta(InternalCDORevisionDelta delta, long created, OMMonitor monitor)
  {
    EClass eClass = getObjectType(delta.getID());
    IClassMapping mapping = getStore().getMappingStrategy().getClassMapping(eClass);
    mapping.writeRevisionDelta(this, delta, created, monitor);
  }

  @Override
  protected void writeRevisions(InternalCDORevision[] revisions, OMMonitor monitor)
  {
    try
    {
      monitor.begin(revisions.length);
      for (InternalCDORevision revision : revisions)
      {
        writeRevision(revision, monitor.fork());
      }
    }
    finally
    {
      monitor.done();
    }
  }

  protected void writeRevision(InternalCDORevision revision, OMMonitor monitor)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Writing revision: {0}", revision);
    }

    EClass eClass = revision.getEClass();
    IClassMapping mapping = getStore().getMappingStrategy().getClassMapping(eClass);
    mapping.writeRevision(this, revision, monitor);
  }

  @Override
  protected void detachObjects(CDOID[] detachedObjects, long revised, OMMonitor monitor)
  {
    try
    {
      monitor.begin(detachedObjects.length);
      for (CDOID id : detachedObjects)
      {
        detachObject(id, revised, monitor.fork());
      }
    }
    finally
    {
      monitor.done();
    }
  }

  /**
   * @since 2.0
   */
  protected void detachObject(CDOID id, long revised, OMMonitor monitor)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Detaching object: {0}", id);
    }

    EClass eClass = getObjectType(id);
    IClassMapping mapping = getStore().getMappingStrategy().getClassMapping(eClass);
    mapping.detachObject(this, id, revised, monitor);
  }

  /**
   * @deprecated TODO Move this somehow to DBAdapter
   */
  @Deprecated
  protected Boolean getBoolean(Object value)
  {
    if (value == null)
    {
      return null;
    }

    if (value instanceof Boolean)
    {
      return (Boolean)value;
    }

    if (value instanceof Number)
    {
      return ((Number)value).intValue() != 0;
    }

    throw new IllegalArgumentException("Not a boolean value: " + value);
  }

  /** @deprecated move to meta manager */
  @Deprecated
  protected Set<IDBTable> mapPackageUnits(InternalCDOPackageUnit[] packageUnits)
  {
    Set<IDBTable> affectedTables = new HashSet<IDBTable>();
    if (packageUnits != null && packageUnits.length != 0)
    {
      for (InternalCDOPackageUnit packageUnit : packageUnits)
      {
        mapPackageInfos(packageUnit.getPackageInfos(), affectedTables);
      }
    }

    return affectedTables;
  }

  /** @deprecated move to meta manager */
  @Deprecated
  protected void mapPackageInfos(InternalCDOPackageInfo[] packageInfos, Set<IDBTable> affectedTables)
  {
    for (InternalCDOPackageInfo packageInfo : packageInfos)
    {
      EPackage ePackage = packageInfo.getEPackage();
      if (!CDOModelUtil.isCorePackage(ePackage))
      {
        EClass[] persistentClasses = EMFUtil.getPersistentClasses(ePackage);
        Set<IDBTable> tables = mapClasses(persistentClasses);
        affectedTables.addAll(tables);
      }
    }
  }

  /** @deprecated move to meta manager */
  @Deprecated
  protected Set<IDBTable> mapClasses(EClass... eClasses)
  {
    Set<IDBTable> affectedTables = new HashSet<IDBTable>();
    if (eClasses != null && eClasses.length != 0)
    {
      IMappingStrategy mappingStrategy = getStore().getMappingStrategy();
      for (EClass eClass : eClasses)
      {
        IClassMapping mapping = mappingStrategy.getClassMapping(eClass);
        if (mapping != null)
        {
          affectedTables.addAll(mapping.getAffectedTables());
        }
      }
    }

    return affectedTables;
  }

  /** @deprecated move to meta manager */
  @Deprecated
  protected InternalCDOPackageUnit createPackageUnit()
  {
    return (InternalCDOPackageUnit)CDOModelUtil.createPackageUnit();
  }

  /** @deprecated move to meta manager */
  @Deprecated
  protected InternalCDOPackageInfo createPackageInfo()
  {
    return (InternalCDOPackageInfo)CDOModelUtil.createPackageInfo();
  }

  public Connection getConnection()
  {
    return connection;
  }

  public final void commit(OMMonitor monitor)
  {
    monitor.begin();
    Async async = monitor.forkAsync();

    try
    {
      getConnection().commit();
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
    finally
    {
      async.stop();
      monitor.done();
    }
  }

  @Override
  protected final void rollback(IStoreAccessor.CommitContext commitContext)
  {
    try
    {
      getConnection().rollback();
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
  }

  @Override
  protected void doActivate() throws Exception
  {
    connection = getStore().getDBConnectionProvider().getConnection();
  }

  @Override
  protected void doDeactivate() throws Exception
  {
    DBUtil.close(connection);
    connection = null;
  }

  @Override
  protected void doPassivate() throws Exception
  {
    // Do nothing
  }

  @Override
  protected void doUnpassivate() throws Exception
  {
    // TODO Check if connection is still valid.
  }
}
