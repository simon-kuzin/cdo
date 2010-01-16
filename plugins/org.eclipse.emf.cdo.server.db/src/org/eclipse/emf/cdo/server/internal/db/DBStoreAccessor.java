/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Stefan Winkler - bug 259402
 *    Stefan Winkler - 271444: [DB] Multiple refactorings bug 271444
 */
package org.eclipse.emf.cdo.server.internal.db;

import org.eclipse.emf.cdo.common.CDOQueryInfo;
import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.branch.CDOBranchVersion;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.model.CDOClassifierRef;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.common.revision.cache.CDORevisionCacheAdder;
import org.eclipse.emf.cdo.server.IQueryHandler;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.ISession;
import org.eclipse.emf.cdo.server.IStoreAccessor;
import org.eclipse.emf.cdo.server.ITransaction;
import org.eclipse.emf.cdo.server.db.CDODBUtil;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.server.db.IPreparedStatementCache;
import org.eclipse.emf.cdo.server.db.IPreparedStatementCache.ReuseProbability;
import org.eclipse.emf.cdo.server.db.mapping.IClassMapping;
import org.eclipse.emf.cdo.server.db.mapping.IClassMappingAuditSupport;
import org.eclipse.emf.cdo.server.db.mapping.IClassMappingDeltaSupport;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.emf.cdo.server.internal.db.bundle.OM;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionDelta;
import org.eclipse.emf.cdo.spi.server.LongIDStoreAccessor;

import org.eclipse.net4j.db.DBException;
import org.eclipse.net4j.db.DBUtil;
import org.eclipse.net4j.util.ReflectUtil.ExcludeFromDump;
import org.eclipse.net4j.util.collection.CloseableIterator;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Eike Stepper
 */
public class DBStoreAccessor extends LongIDStoreAccessor implements IDBStoreAccessor
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, DBStoreAccessor.class);

  private Connection connection = null;

  private IPreparedStatementCache statementCache = null;

  private Timer connectionKeepAliveTimer = null;

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

  public IPreparedStatementCache getStatementCache()
  {
    return statementCache;
  }

  public DBStoreChunkReader createChunkReader(InternalCDORevision revision, EStructuralFeature feature)
  {
    return new DBStoreChunkReader(this, revision, feature);
  }

  /**
   * Returns an iterator that iterates over all objects in the store and makes their CDOIDs available for processing.
   * This method is supposed to be called very infrequently, for example during the recovery from a crash.
   * 
   * @since 2.0
   * @deprecated Not used by the framework anymore.
   */
  @Deprecated
  public CloseableIterator<CDOID> readObjectIDs()
  {
    if (TRACER.isEnabled())
    {
      TRACER.trace("Selecting object IDs"); //$NON-NLS-1$
    }

    return getStore().getMappingStrategy().readObjectIDs(this);
  }

  public CDOClassifierRef readObjectType(CDOID id)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Selecting object type: {0}", id); //$NON-NLS-1$
    }

    return getStore().getMappingStrategy().readObjectType(this, id);
  }

  protected EClass getObjectType(CDOID id)
  {
    EClass result = getStore().getRepository().getRevisionManager().getObjectType(id);
    if (result == null)
    {
      CDOClassifierRef type = readObjectType(id);
      if (type == null)
      {
        return null;
      }

      IRepository repository = getStore().getRepository();
      CDOPackageRegistry packageRegistry = repository.getPackageRegistry();
      result = (EClass)type.resolve(packageRegistry);
    }

    return result;
  }

  public InternalCDORevision readRevision(CDOID id, CDOBranchPoint branchPoint, int listChunk,
      CDORevisionCacheAdder cache)
  {
    IMappingStrategy mappingStrategy = getStore().getMappingStrategy();
    if (!mappingStrategy.hasAuditSupport())
    {
      throw new UnsupportedOperationException("Mapping strategy does not support audits."); //$NON-NLS-1$
    }

    if (TRACER.isEnabled())
    {
      TRACER.format("Selecting revision {0} from {1}", id, branchPoint); //$NON-NLS-1$
    }

    EClass eClass = getObjectType(id);
    InternalCDORevision revision = getStore().createRevision(eClass, id);

    IClassMappingAuditSupport mapping = (IClassMappingAuditSupport)mappingStrategy.getClassMapping(eClass);
    if (mapping.readRevision(this, revision, branchPoint, listChunk))
    {
      return revision;
    }

    // Reading failed - revision does not exist.
    return null;
  }

  public InternalCDORevision readRevisionByVersion(CDOID id, CDOBranchVersion branchVersion, int listChunk,
      CDORevisionCacheAdder cache)
  {
    IMappingStrategy mappingStrategy = getStore().getMappingStrategy();

    EClass eClass = getObjectType(id);
    InternalCDORevision revision = getStore().createRevision(eClass, id);
    IClassMapping mapping = mappingStrategy.getClassMapping(eClass);

    boolean success = false;

    if (mappingStrategy.hasAuditSupport())
    {
      if (TRACER.isEnabled())
      {
        TRACER.format("Selecting revision {0} from {1}", id, branchVersion); //$NON-NLS-1$
      }

      // if audit support is present, just use the audit method
      success = ((IClassMappingAuditSupport)mapping).readRevisionByVersion(this, revision, branchVersion, listChunk);
    }
    else
    {
      // if audit support is not present, we still have to provide a method
      // to readRevisionByVersion because TransactionCommitContext.computeDirtyObject
      // needs to lookup the base revision for a change. Hence we emulate this
      // behavior by getting the current revision and asserting that the version
      // has not changed. This is valid because if the version has changed,
      // we are in trouble because of a conflict anyways.
      if (TRACER.isEnabled())
      {
        TRACER.format("Selecting current base revision: {0}", id); //$NON-NLS-1$
      }

      success = mapping.readRevision(this, revision, listChunk);

      if (success && revision.getVersion() != branchVersion.getVersion())
      {
        throw new IllegalStateException("Can only retrieve current version " + revision.getVersion() + " for " + id //$NON-NLS-1$ //$NON-NLS-2$
            + " - version requested was " + branchVersion + "."); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }

    return success ? revision : null;
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

  public IQueryHandler getQueryHandler(CDOQueryInfo info)
  {
    if (SQLQueryHandler.QUERY_LANGUAGE.equals(info.getQueryLanguage()))
    {
      return new SQLQueryHandler(this);
    }

    return null;
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

  @Override
  protected void writeRevisionDeltas(InternalCDORevisionDelta[] revisionDeltas, CDOBranch branch, long created,
      OMMonitor monitor)
  {
    IMappingStrategy mappingStrategy = getStore().getMappingStrategy();

    if (!mappingStrategy.hasDeltaSupport())
    {
      throw new UnsupportedOperationException("Mapping strategy does not support revision deltas."); //$NON-NLS-1$
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
    IClassMappingDeltaSupport mapping = (IClassMappingDeltaSupport)getStore().getMappingStrategy().getClassMapping(
        eClass);
    mapping.writeRevisionDelta(this, delta, created, monitor);
  }

  @Override
  protected void writeRevisions(InternalCDORevision[] revisions, CDOBranch branch, OMMonitor monitor)
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
      TRACER.format("Writing revision: {0}", revision); //$NON-NLS-1$
    }

    EClass eClass = revision.getEClass();
    IClassMapping mapping = getStore().getMappingStrategy().getClassMapping(eClass);
    mapping.writeRevision(this, revision, monitor);
  }

  @Override
  protected void detachObjects(CDOID[] detachedObjects, CDOBranch branch, long revised, OMMonitor monitor)
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
      TRACER.format("Detaching object: {0}", id); //$NON-NLS-1$
    }

    EClass eClass = getObjectType(id);
    IClassMapping mapping = getStore().getMappingStrategy().getClassMapping(eClass);
    mapping.detachObject(this, id, revised, monitor);
  }

  public Connection getConnection()
  {
    return connection;
  }

  public final void commit(OMMonitor monitor)
  {
    monitor.begin();
    Async async = monitor.forkAsync();

    if (TRACER.isEnabled())
    {
      TRACER.format("--- DB COMMIT ---"); //$NON-NLS-1$
    }

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
    if (TRACER.isEnabled())
    {
      TRACER.format("--- DB ROLLBACK ---"); //$NON-NLS-1$
    }

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
    connection = getStore().getConnection();

    connectionKeepAliveTimer = new Timer("Connection-Keep-Alive-" + toString()); //$NON-NLS-1$
    connectionKeepAliveTimer.schedule(new ConnectionKeepAliveTask(), ConnectionKeepAliveTask.EXECUTION_PERIOD,
        ConnectionKeepAliveTask.EXECUTION_PERIOD);

    // TODO - make this configurable?
    statementCache = CDODBUtil.createStatementCache();
    statementCache.setConnection(connection);

    LifecycleUtil.activate(statementCache);
  }

  @Override
  protected void doDeactivate() throws Exception
  {
    LifecycleUtil.deactivate(statementCache);

    connectionKeepAliveTimer.cancel();
    connectionKeepAliveTimer = null;

    DBUtil.close(connection);
    connection = null;
  }

  @Override
  protected void doPassivate() throws Exception
  {
    // this is called when the accessor is put back into the pool
    // we want to make sure that no DB lock is held (see Bug 276926)
    connection.rollback();
  }

  @Override
  protected void doUnpassivate() throws Exception
  {
    // do nothing
  }

  public EPackage[] loadPackageUnit(InternalCDOPackageUnit packageUnit)
  {
    return getStore().getMetaDataManager().loadPackageUnit(getConnection(), packageUnit);
  }

  public Collection<InternalCDOPackageUnit> readPackageUnits()
  {
    return getStore().getMetaDataManager().readPackageUnits(getConnection());
  }

  public void writePackageUnits(InternalCDOPackageUnit[] packageUnits, OMMonitor monitor)
  {
    monitor.begin(2);
    try
    {
      getStore().getMetaDataManager().writePackageUnits(getConnection(), packageUnits, monitor.fork());
      getStore().getMappingStrategy().createMapping(getConnection(), packageUnits, monitor.fork());
    }
    finally
    {
      monitor.done();
    }
  }

  public int createBranch(BranchInfo branchInfo)
  {
    int id = getStore().getNextBranchID();
    PreparedStatement pstmt = null;

    try
    {
      pstmt = statementCache.getPreparedStatement(CDODBSchema.SQL_CREATE_BRANCH, ReuseProbability.LOW);
      pstmt.setInt(1, id);
      pstmt.setString(2, branchInfo.getName());
      pstmt.setInt(3, branchInfo.getBaseBranchID());
      pstmt.setLong(4, branchInfo.getBaseTimeStamp());

      int updatedRows = pstmt.executeUpdate();
      if (updatedRows < 1)
      {
        throw new DBException("Branch with name " + branchInfo.getName() + " was not inserted");
      }

      if (updatedRows > 1)
      {
        // Should not happen
        throw new DBException("Branch with name " + branchInfo.getName() + " was inserted multiple times");
      }

      return id;
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
    finally
    {
      statementCache.releasePreparedStatement(pstmt);
    }
  }

  public BranchInfo loadBranch(int branchID)
  {
    PreparedStatement pstmt = null;
    ResultSet resultSet = null;

    try
    {
      pstmt = statementCache.getPreparedStatement(CDODBSchema.SQL_LOAD_BRANCH, ReuseProbability.HIGH);
      pstmt.setInt(1, branchID);

      resultSet = pstmt.executeQuery();
      if (!resultSet.next())
      {
        throw new DBException("Branch with ID " + branchID + " does not exist");
      }

      String name = resultSet.getString(1);
      int baseBranchID = resultSet.getInt(2);
      long baseTimeStamp = resultSet.getLong(3);
      return new BranchInfo(name, baseBranchID, baseTimeStamp);
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
    finally
    {
      DBUtil.close(resultSet);
      statementCache.releasePreparedStatement(pstmt);
    }
  }

  public SubBranchInfo[] loadSubBranches(int baseID)
  {
    PreparedStatement pstmt = null;
    ResultSet resultSet = null;

    try
    {
      pstmt = statementCache.getPreparedStatement(CDODBSchema.SQL_LOAD_SUB_BRANCHES, ReuseProbability.HIGH);
      pstmt.setInt(1, baseID);

      resultSet = pstmt.executeQuery();
      List<SubBranchInfo> result = new ArrayList<SubBranchInfo>();
      while (resultSet.next())
      {
        int id = resultSet.getInt(1);
        String name = resultSet.getString(2);
        long baseTimeStamp = resultSet.getLong(3);
        result.add(new SubBranchInfo(id, name, baseTimeStamp));
      }

      return result.toArray(new SubBranchInfo[result.size()]);
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
    finally
    {
      DBUtil.close(resultSet);
      statementCache.releasePreparedStatement(pstmt);
    }
  }

  /**
   * @author Stefan Winkler
   */
  private class ConnectionKeepAliveTask extends TimerTask
  {
    public static final long EXECUTION_PERIOD = 1000 * 60 * 60 * 4; // 4 hours

    @Override
    public void run()
    {
      Statement stmt = null;

      try
      {
        if (TRACER.isEnabled())
        {
          TRACER.trace("DB connection keep-alive task activated."); //$NON-NLS-1$
        }

        stmt = connection.createStatement();
        stmt.executeQuery("SELECT 1 FROM " + CDODBSchema.REPOSITORY); //$NON-NLS-1$
      }
      catch (SQLException ex)
      {
        OM.LOG.error("DB connection keep-alive failed.", ex); //$NON-NLS-1$
      }
      finally
      {
        DBUtil.close(stmt);
      }
    }
  }
}
