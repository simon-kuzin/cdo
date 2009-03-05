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

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDMeta;
import org.eclipse.emf.cdo.common.id.CDOIDMetaRange;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.eresource.EresourcePackage;
import org.eclipse.emf.cdo.server.ISession;
import org.eclipse.emf.cdo.server.ITransaction;
import org.eclipse.emf.cdo.server.IView;
import org.eclipse.emf.cdo.server.StoreThreadLocal;
import org.eclipse.emf.cdo.server.db.IDBStore;
import org.eclipse.emf.cdo.server.db.IJDBCDelegateProvider;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.emf.cdo.server.internal.db.bundle.OM;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageInfo;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageRegistry;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageRegistry.MetaInstanceMapper;
import org.eclipse.emf.cdo.spi.server.LongIDStore;
import org.eclipse.emf.cdo.spi.server.StoreAccessorPool;

import org.eclipse.net4j.db.DBException;
import org.eclipse.net4j.db.DBType;
import org.eclipse.net4j.db.DBUtil;
import org.eclipse.net4j.db.IDBAdapter;
import org.eclipse.net4j.db.IDBConnectionProvider;
import org.eclipse.net4j.db.ddl.IDBSchema;
import org.eclipse.net4j.db.ddl.IDBTable;
import org.eclipse.net4j.spi.db.DBSchema;
import org.eclipse.net4j.util.ReflectUtil.ExcludeFromDump;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.eclipse.net4j.util.om.monitor.Monitor;
import org.eclipse.net4j.util.om.monitor.ProgressDistributor;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.InternalEObject;

import javax.sql.DataSource;

import java.sql.Connection;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Eike Stepper
 */
public class DBStore extends LongIDStore implements IDBStore
{
  public static final String TYPE = "db";

  private long creationTime;

  private IMappingStrategy mappingStrategy;

  private IDBSchema dbSchema;

  private IDBAdapter dbAdapter;

  private IDBConnectionProvider dbConnectionProvider;

  private IJDBCDelegateProvider jdbcDelegateProvider;

  @ExcludeFromDump
  private transient ProgressDistributor accessorWriteDistributor = new ProgressDistributor.Geometric()
  {
    @Override
    public String toString()
    {
      String result = "accessorWriteDistributor";
      if (getRepository() != null)
      {
        result += ": " + getRepository().getName();
      }

      return result;
    }
  };

  @ExcludeFromDump
  private transient StoreAccessorPool readerPool = new StoreAccessorPool(this, null);

  @ExcludeFromDump
  private transient StoreAccessorPool writerPool = new StoreAccessorPool(this, null);

  private static Map<EClassifier, DBType> typeMap = new HashMap<EClassifier, DBType>();

  static
  {
    typeMap.put(EcorePackage.eINSTANCE.getEDate(), DBType.TIMESTAMP);
    typeMap.put(EcorePackage.eINSTANCE.getEString(), DBType.VARCHAR);

    typeMap.put(EcorePackage.eINSTANCE.getEBoolean(), DBType.BOOLEAN);
    typeMap.put(EcorePackage.eINSTANCE.getEByte(), DBType.SMALLINT);
    typeMap.put(EcorePackage.eINSTANCE.getEChar(), DBType.CHAR);
    typeMap.put(EcorePackage.eINSTANCE.getEDouble(), DBType.DOUBLE);
    typeMap.put(EcorePackage.eINSTANCE.getEFloat(), DBType.FLOAT);
    typeMap.put(EcorePackage.eINSTANCE.getEInt(), DBType.INTEGER);
    typeMap.put(EcorePackage.eINSTANCE.getELong(), DBType.BIGINT);
    typeMap.put(EcorePackage.eINSTANCE.getEShort(), DBType.SMALLINT);

    typeMap.put(EcorePackage.eINSTANCE.getEBooleanObject(), DBType.BOOLEAN);
    typeMap.put(EcorePackage.eINSTANCE.getEByteObject(), DBType.SMALLINT);
    typeMap.put(EcorePackage.eINSTANCE.getECharacterObject(), DBType.CHAR);
    typeMap.put(EcorePackage.eINSTANCE.getEDoubleObject(), DBType.DOUBLE);
    typeMap.put(EcorePackage.eINSTANCE.getEFloatObject(), DBType.FLOAT);
    typeMap.put(EcorePackage.eINSTANCE.getEIntegerObject(), DBType.INTEGER);
    typeMap.put(EcorePackage.eINSTANCE.getELongObject(), DBType.BIGINT);
    typeMap.put(EcorePackage.eINSTANCE.getEShortObject(), DBType.SMALLINT);
  }

  public DBStore()
  {
    super(TYPE, set(ChangeFormat.REVISION, ChangeFormat.DELTA), set(RevisionTemporality.AUDITING,
        RevisionTemporality.NONE), set(RevisionParallelism.NONE));
    setRevisionTemporality(RevisionTemporality.AUDITING);
  }

  @Override
  public Set<ChangeFormat> getSupportedChangeFormats()
  {
    if (getRevisionTemporality() == RevisionTemporality.AUDITING)
    {
      return set(ChangeFormat.REVISION);
    }
    else
    {
      return set(ChangeFormat.REVISION, ChangeFormat.DELTA);
    }
  }

  public IMappingStrategy getMappingStrategy()
  {
    return mappingStrategy;
  }

  public void setMappingStrategy(IMappingStrategy mappingStrategy)
  {
    this.mappingStrategy = mappingStrategy;
    mappingStrategy.setStore(this);
  }

  public IDBAdapter getDBAdapter()
  {
    return dbAdapter;
  }

  public void setDbAdapter(IDBAdapter dbAdapter)
  {
    this.dbAdapter = dbAdapter;
  }

  public IDBConnectionProvider getDBConnectionProvider()
  {
    return dbConnectionProvider;
  }

  public void setDbConnectionProvider(IDBConnectionProvider dbConnectionProvider)
  {
    // FIXME: need to update provider in JDBCWrapper, too?
    this.dbConnectionProvider = dbConnectionProvider;
  }

  public void setDataSource(DataSource dataSource)
  {
    dbConnectionProvider = DBUtil.createConnectionProvider(dataSource);
  }

  public IJDBCDelegateProvider getJDBCDelegateProvider()
  {
    return jdbcDelegateProvider;
  }

  public void setJDBCDelegateProvider(IJDBCDelegateProvider provider)
  {
    jdbcDelegateProvider = provider;
  }

  public ProgressDistributor getAccessorWriteDistributor()
  {
    return accessorWriteDistributor;
  }

  public synchronized IDBSchema getDBSchema()
  {
    // TODO Better synchronization or eager init
    if (dbSchema == null)
    {
      dbSchema = createSchema();
    }

    return dbSchema;
  }

  @Override
  protected StoreAccessorPool getReaderPool(ISession session, boolean forReleasing)
  {
    return readerPool;
  }

  @Override
  protected StoreAccessorPool getWriterPool(IView view, boolean forReleasing)
  {
    return writerPool;
  }

  @Override
  protected DBStoreAccessor createReader(ISession session) throws DBException
  {
    return new DBStoreAccessor(this, session);
  }

  @Override
  protected DBStoreAccessor createWriter(ITransaction transaction) throws DBException
  {
    return new DBStoreAccessor(this, transaction);
  }

  public long getMetaID(EModelElement modelElement)
  {
    InternalCDOPackageRegistry packageRegistry = (InternalCDOPackageRegistry)getRepository().getPackageRegistry();
    CDOID cdoid = packageRegistry.getMetaInstanceMapper().lookupMetaInstanceID((InternalEObject)modelElement);
    if (cdoid instanceof CDOIDMeta)
    {
      return ((CDOIDMeta)cdoid).getLongValue();
    }

    throw new IllegalStateException("No permanent meta ID available for " + modelElement);
  }

  public EModelElement getMetaInstance(long id)
  {
    CDOIDMeta cdoid = CDOIDUtil.createMeta(id);
    InternalCDOPackageRegistry packageRegistry = (InternalCDOPackageRegistry)getRepository().getPackageRegistry();
    InternalEObject metaInstance = packageRegistry.getMetaInstanceMapper().lookupMetaInstance(cdoid);
    if (metaInstance instanceof EModelElement)
    {
      return (EModelElement)metaInstance;
    }

    throw new IllegalStateException("No meta instance available for " + id);
  }

  public Connection getConnection()
  {
    Connection connection = dbConnectionProvider.getConnection();
    if (connection == null)
    {
      throw new DBException("No connection from connection provider: " + dbConnectionProvider);
    }

    return connection;
  }

  public void repairAfterCrash()
  {
    try
    {
      DBStoreAccessor accessor = (DBStoreAccessor)getWriter(null);
      StoreThreadLocal.setAccessor(accessor);

      Connection connection = accessor.getJDBCDelegate().getConnection();
      long maxObjectID = mappingStrategy.repairAfterCrash(dbAdapter, connection);
      long maxMetaID = DBUtil.selectMaximumLong(connection, CDODBSchema.PACKAGE_INFOS_META_UB);

      OM.LOG.info(MessageFormat.format("Repaired after crash: maxObjectID={0}, maxMetaID={1}", maxObjectID, maxMetaID));
      setLastObjectID(maxObjectID);
      setLastMetaID(maxMetaID);
    }
    finally
    {
      StoreThreadLocal.release();
    }
  }

  public long getCreationTime()
  {
    return creationTime;
  }

  @Override
  protected void doBeforeActivate() throws Exception
  {
    super.doBeforeActivate();
    checkNull(mappingStrategy, "mappingStrategy is null");
    checkNull(dbAdapter, "dbAdapter is null");
    checkNull(dbConnectionProvider, "dbConnectionProvider is null");
  }

  @Override
  protected void doActivate() throws Exception
  {
    super.doActivate();
    long startupTime = getStartupTime();

    DBStoreAccessor storeAccessor = createWriter(null);
    LifecycleUtil.activate(storeAccessor);

    try
    {
      Connection connection = storeAccessor.getJDBCDelegate().getConnection();
      Set<IDBTable> createdTables = CDODBSchema.INSTANCE.create(dbAdapter, dbConnectionProvider);
      if (createdTables.contains(CDODBSchema.REPOSITORY))
      {
        // First start
        creationTime = startupTime;
        DBUtil.insertRow(connection, dbAdapter, CDODBSchema.REPOSITORY, creationTime, 1, startupTime, 0, CRASHED,
            CRASHED);

        InternalCDOPackageRegistry packageRegistry = (InternalCDOPackageRegistry)getRepository().getPackageRegistry();

        InternalCDOPackageInfo ecoreInfo = packageRegistry.getPackageInfo(EcorePackage.eINSTANCE);
        CDOIDMetaRange ecoreRange = getNextMetaIDRange(ecoreInfo.getMetaIDRange().size());
        ecoreInfo.setMetaIDRange(ecoreRange);
        InternalCDOPackageUnit ecoreUnit = ecoreInfo.getPackageUnit();
        ecoreUnit.setTimeStamp(creationTime);

        InternalCDOPackageInfo eresourceInfo = packageRegistry.getPackageInfo(EresourcePackage.eINSTANCE);
        CDOIDMetaRange eresourceRange = getNextMetaIDRange(eresourceInfo.getMetaIDRange().size());
        eresourceInfo.setMetaIDRange(eresourceRange);
        InternalCDOPackageUnit eresourceUnit = eresourceInfo.getPackageUnit();
        eresourceUnit.setTimeStamp(creationTime);

        MetaInstanceMapper metaInstanceMapper = packageRegistry.getMetaInstanceMapper();
        metaInstanceMapper.clear();
        metaInstanceMapper.mapMetaInstances(EcorePackage.eINSTANCE, ecoreRange);
        metaInstanceMapper.mapMetaInstances(EresourcePackage.eINSTANCE, eresourceRange);

        InternalCDOPackageUnit[] systemUnits = { ecoreUnit, eresourceUnit };
        storeAccessor.writePackageUnits(systemUnits, new Monitor());
      }
      else
      {
        // Restart
        creationTime = DBUtil.selectMaximumLong(connection, CDODBSchema.REPOSITORY_CREATED);
        long lastObjectID = DBUtil.selectMaximumLong(connection, CDODBSchema.REPOSITORY_NEXT_CDOID);
        setLastMetaID(DBUtil.selectMaximumLong(connection, CDODBSchema.REPOSITORY_NEXT_METAID));
        if (lastObjectID == CRASHED || getLastMetaID() == CRASHED)
        {
          OM.LOG.warn("Detected restart after crash");
        }

        setLastObjectID(lastObjectID);

        StringBuilder builder = new StringBuilder();
        builder.append("UPDATE ");
        builder.append(CDODBSchema.REPOSITORY);
        builder.append(" SET ");
        builder.append(CDODBSchema.REPOSITORY_STARTS);
        builder.append("=");
        builder.append(CDODBSchema.REPOSITORY_STARTS);
        builder.append("+1, ");
        builder.append(CDODBSchema.REPOSITORY_STARTED);
        builder.append("=");
        builder.append(startupTime);
        builder.append(", ");
        builder.append(CDODBSchema.REPOSITORY_STOPPED);
        builder.append("=0, ");
        builder.append(CDODBSchema.REPOSITORY_NEXT_CDOID);
        builder.append("=");
        builder.append(CRASHED);
        builder.append(", ");
        builder.append(CDODBSchema.REPOSITORY_NEXT_METAID);
        builder.append("=");
        builder.append(CRASHED);

        String sql = builder.toString();
        int count = DBUtil.update(connection, sql);
        if (count == 0)
        {
          throw new DBException("No row updated in table " + CDODBSchema.REPOSITORY);
        }
      }

      storeAccessor.commit(new Monitor());
      LifecycleUtil.activate(mappingStrategy);
    }
    catch (RuntimeException ex)
    {
      OM.LOG.error(ex);
      throw ex;
    }
    finally
    {
      storeAccessor.deactivate();
    }
  }

  @Override
  protected void doDeactivate() throws Exception
  {
    Connection connection = null;

    try
    {
      connection = getConnection();

      LifecycleUtil.deactivate(mappingStrategy);

      StringBuilder builder = new StringBuilder();
      builder.append("UPDATE ");
      builder.append(CDODBSchema.REPOSITORY);
      builder.append(" SET ");
      builder.append(CDODBSchema.REPOSITORY_STOPPED);
      builder.append("=");
      builder.append(getShutdownTime());
      builder.append(", ");
      builder.append(CDODBSchema.REPOSITORY_NEXT_CDOID);
      builder.append("=");
      builder.append(getLastObjectID());
      builder.append(", ");
      builder.append(CDODBSchema.REPOSITORY_NEXT_METAID);
      builder.append("=");
      builder.append(getLastMetaID());

      String sql = builder.toString();
      int count = DBUtil.update(connection, sql);
      if (count == 0)
      {
        throw new DBException("No row updated in table " + CDODBSchema.REPOSITORY);
      }
    }
    finally
    {
      DBUtil.close(connection);
    }

    readerPool.dispose();
    writerPool.dispose();
    super.doDeactivate();
  }

  protected IDBSchema createSchema()
  {
    String name = getRepository().getName();
    return new DBSchema(name);
  }

  protected long getStartupTime()
  {
    return System.currentTimeMillis();
  }

  protected long getShutdownTime()
  {
    return System.currentTimeMillis();
  }

  public static DBType getDBType(EClassifier type)
  {
    if (type instanceof EClass)
    {
      return DBType.BIGINT;
    }

    DBType dbType = typeMap.get(type);
    if (dbType != null)
    {
      return dbType;
    }

    return DBType.VARCHAR;
  }
}
