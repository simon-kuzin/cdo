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
package org.eclipse.emf.cdo.server.internal.db.mapping;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.server.IStoreChunkReader.Chunk;
import org.eclipse.emf.cdo.server.db.CDODBUtil;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.server.db.IDBStoreChunkReader;
import org.eclipse.emf.cdo.server.db.mapping.IReferenceMapping;
import org.eclipse.emf.cdo.server.internal.db.CDODBSchema;
import org.eclipse.emf.cdo.server.internal.db.ToMany;
import org.eclipse.emf.cdo.server.internal.db.bundle.OM;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.net4j.db.DBException;
import org.eclipse.net4j.db.DBType;
import org.eclipse.net4j.db.DBUtil;
import org.eclipse.net4j.db.ddl.IDBField;
import org.eclipse.net4j.db.ddl.IDBTable;
import org.eclipse.net4j.db.ddl.IDBIndex.Type;
import org.eclipse.net4j.util.collection.MoveableList;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public class ReferenceMapping extends FeatureMapping implements IReferenceMapping
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, ReferenceMapping.class);

  private static final int MOVE_UNBOUNDED = -1;

  private IDBTable table;

  private String sqlSelectChunksPrefix;

  private String sqlOrderByIndex;

  private String sqlClearReference;

  private String sqlUpdateRefVersion;

  private String sqlUpdateTarget;

  private String sqlMoveUp;

  private String sqlMoveUpWithLimit;

  private String sqlMoveDown;

  private String sqlMoveDownWithLimit;

  private String sqlUpdateIndex;

  private String sqlInsertEntry;

  private String sqlDeleteEntry;

  public ReferenceMapping(ClassMapping classMapping, EStructuralFeature feature, ToMany toMany)
  {
    super(classMapping, feature);
    mapReference(classMapping.getEClass(), feature);

    initSqlStrings();
  }

  public IDBTable getTable()
  {
    return table;
  }

  protected void mapReference(EClass eClass, EStructuralFeature feature)
  {
    MappingStrategy mappingStrategy = getClassMapping().getMappingStrategy();

    String tableName = mappingStrategy.getReferenceTableName(eClass, feature);
    Object referenceMappingKey = getReferenceMappingKey(feature);
    table = mapReferenceTable(referenceMappingKey, tableName);
  }

  protected Object getReferenceMappingKey(EStructuralFeature feature)
  {
    return getClassMapping().createReferenceMappingKey(feature);
  }

  protected IDBTable mapReferenceTable(Object key, String tableName)
  {
    Map<Object, IDBTable> referenceTables = getClassMapping().getMappingStrategy().getReferenceTables();
    IDBTable table = referenceTables.get(key);
    if (table == null)
    {
      table = addReferenceTable(tableName);
      referenceTables.put(key, table);
    }

    return table;
  }

  protected IDBTable addReferenceTable(String tableName)
  {
    IDBTable table = getClassMapping().addTable(tableName);
    IDBField sourceField = table.addField(CDODBSchema.REFERENCES_SOURCE, DBType.BIGINT);
    IDBField versionField = table.addField(CDODBSchema.REFERENCES_VERSION, DBType.INTEGER);
    IDBField idxField = table.addField(CDODBSchema.REFERENCES_IDX, DBType.INTEGER);
    table.addField(CDODBSchema.REFERENCES_TARGET, DBType.BIGINT);

    table.addIndex(Type.NON_UNIQUE, sourceField, versionField);
    table.addIndex(Type.NON_UNIQUE, idxField);
    return table;
  }

  public final void writeReference(IDBStoreAccessor accessor, InternalCDORevision revision)
  {
    int idx = 0;
    for (Object element : revision.getList(getFeature()))
    {
      writeReferenceEntry(accessor, revision.getID(), revision.getVersion(), idx++, (CDOID)element);
    }
  }

  public final void writeReferenceEntry(IDBStoreAccessor accessor, CDOID id, int version, int idx, CDOID targetId)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlInsertEntry);

      stmt.setLong(1, CDOIDUtil.getLong(id));
      stmt.setInt(2, version);
      stmt.setInt(3, idx++);
      stmt.setLong(4, CDODBUtil.getLong(targetId));
      if (TRACER.isEnabled())
      {
        TRACER.trace(stmt.toString());
      }

      int result = stmt.executeUpdate();
      // UPDATE should affect one row
      if (result != 1)
      {
        throw new IllegalStateException(stmt.toString() + " returned Update count " + result + " (expected: 1)");
      }
    }
    catch (SQLException e)
    {
      throw new DBException(e);
    }
    finally
    {
      DBUtil.close(stmt);
    }
  }

  public void deleteReferenceEntry(IDBStoreAccessor accessor, CDOID id, int index)
  {
    PreparedStatement stmt = null;
    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlDeleteEntry);

      stmt.setLong(1, CDOIDUtil.getLong(id));
      stmt.setInt(2, index);

      if (TRACER.isEnabled())
      {
        TRACER.trace(stmt.toString());
      }

      int result = stmt.executeUpdate();
      // DELETE should affect one row
      if (result != 1)
      {
        throw new IllegalStateException(stmt.toString() + " returned Update count " + result + " (expected: 1)");
      }
    }
    catch (SQLException e)
    {
      throw new DBException(e);
    }
    finally
    {
      DBUtil.close(stmt);
    }
  }

  public void removeReferenceEntry(IDBStoreAccessor accessor, CDOID id, int index, int newVersion)
  {
    deleteReferenceEntry(accessor, id, index);
    move1down(accessor, id, newVersion, index, MOVE_UNBOUNDED);
  }

  public void insertReferenceEntry(IDBStoreAccessor accessor, CDOID id, int newVersion, int index, CDOID value)
  {
    move1up(accessor, id, newVersion, index, MOVE_UNBOUNDED);
    insertReferenceEntry(accessor, id, newVersion, index, value);
  }

  public void moveReferenceEntry(IDBStoreAccessor accessor, CDOID id, int newVersion, int oldPosition, int newPosition)
  {
    if (oldPosition == newPosition)
    {
      return;
    }

    // move element away temporarily
    updateOneIndex(accessor, id, newVersion, oldPosition, -1);

    // move elements in between
    if (oldPosition < newPosition)
    {
      move1down(accessor, id, newVersion, oldPosition, newPosition);
    }
    else
    {
      // oldPosition > newPosition -- equal case is handled above
      move1up(accessor, id, newVersion, newPosition, oldPosition);
    }

    // move temporary element to new position
    updateOneIndex(accessor, id, newVersion, -1, newPosition);
  }

  // ----------------------------------------------------------
  // List management helpers
  // ----------------------------------------------------------

  public void updateOneIndex(IDBStoreAccessor accessor, CDOID cdoid, int newVersion, int oldIndex, int newIndex)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlUpdateIndex);

      int idx = 1;
      stmt.setInt(1, newIndex);
      stmt.setInt(2, newVersion);
      stmt.setLong(3, CDOIDUtil.getLong(cdoid));
      stmt.setLong(4, oldIndex);

      if (TRACER.isEnabled())
      {
        TRACER.trace(stmt.toString());
      }

      int result = stmt.executeUpdate();
      // UPDATE should affect one row
      if (result != 1)
      {
        throw new IllegalStateException(stmt.toString() + " returned Update count " + result + " (expected: 1)");
      }
    }
    catch (SQLException e)
    {
      throw new DBException(e);
    }
    finally
    {
      DBUtil.close(stmt);
    }
  }

  /**
   * Move references downwards to close a gap at position <code>index</code>. Only indexes starting with
   * <code>index + 1</code> and ending with <code>upperIndex</code> are moved down.
   */
  public void move1down(IDBStoreAccessor accessor, CDOID cdoid, int newVersion, int index, int upperIndex)
  {
    PreparedStatement stmt = null;

    try
    {
      if (upperIndex == MOVE_UNBOUNDED)
      {
        stmt = accessor.getConnection().prepareStatement(sqlMoveDown);
      }
      else
      {
        stmt = accessor.getConnection().prepareStatement(sqlMoveDownWithLimit);
      }

      stmt.setInt(1, newVersion);
      stmt.setLong(2, CDOIDUtil.getLong(cdoid));
      stmt.setInt(3, index);
      if (upperIndex != MOVE_UNBOUNDED)
      {
        stmt.setInt(4, upperIndex);
      }

      int result = stmt.executeUpdate();
      if (result == Statement.EXECUTE_FAILED)
      {
        throw new IllegalStateException(stmt.toString() + " returned execution failure.");
      }
    }
    catch (SQLException e)
    {
      throw new DBException(e);
    }
    finally
    {
      DBUtil.close(stmt);
    }
  }

  /**
   * Move references upwards to make room at position <code>index</code>. Only indexes starting with <code>index</code>
   * and ending with <code>upperIndex - 1</code> are moved up. <code>upperIndex</code> may be MOVE_UNBOUNDED in which
   * case the upper bound is determined by the list size.
   */
  public void move1up(IDBStoreAccessor accessor, CDOID cdoid, int newVersion, int index, int upperIndex)
  {
    PreparedStatement stmt = null;

    try
    {
      if (upperIndex == MOVE_UNBOUNDED)
      {
        stmt = accessor.getConnection().prepareStatement(sqlMoveUp);
      }
      else
      {
        stmt = accessor.getConnection().prepareStatement(sqlMoveUpWithLimit);
      }

      stmt.setInt(1, newVersion);
      stmt.setLong(2, CDOIDUtil.getLong(cdoid));
      stmt.setInt(3, index);
      if (upperIndex != MOVE_UNBOUNDED)
      {
        stmt.setInt(4, upperIndex);
      }

      int result = stmt.executeUpdate();
      if (result != Statement.EXECUTE_FAILED)
      {
        throw new IllegalStateException(stmt.toString() + " returned execution failure.");
      }
    }
    catch (SQLException e)
    {
      throw new DBException(e);
    }
    finally
    {
      DBUtil.close(stmt);
    }
  }

  public void updateReference(IDBStoreAccessor accessor, CDOID id, int newVersion, int index, CDOID value)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlUpdateTarget);

      stmt.setLong(1, CDODBUtil.getLong(value));
      stmt.setInt(2, newVersion);
      stmt.setLong(3, CDOIDUtil.getLong(id));
      stmt.setLong(4, index);

      if (TRACER.isEnabled())
      {
        TRACER.trace(stmt.toString());
      }

      int result = stmt.executeUpdate();
      if (result == Statement.EXECUTE_FAILED)
      {
        throw new IllegalStateException(stmt.toString() + " returned execution failure.");
      }
    }
    catch (SQLException e)
    {
      throw new DBException(e);
    }
    finally
    {
      DBUtil.close(stmt);
    }
  }

  public void updateReferenceVersion(IDBStoreAccessor accessor, CDOID id, int newVersion)
  {
    PreparedStatement stmt = null;
    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlUpdateRefVersion);

      stmt.setInt(1, newVersion);
      stmt.setLong(2, CDOIDUtil.getLong(id));

      if (TRACER.isEnabled())
      {
        TRACER.trace(stmt.toString());
      }

      int result = stmt.executeUpdate();
      if (result == Statement.EXECUTE_FAILED)
      {
        throw new IllegalStateException(stmt.toString() + " returned execution failure.");
      }
    }
    catch (SQLException e)
    {
      throw new DBException(e);
    }
    finally
    {
      DBUtil.close(stmt);
    }
  }

  public void clearReference(IDBStoreAccessor accessor, CDOID id)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlClearReference);

      stmt.setLong(1, CDOIDUtil.getLong(id));

      int result = stmt.executeUpdate();
      if (result == Statement.EXECUTE_FAILED)
      {
        throw new IllegalStateException(stmt.toString() + " returned execution failure.");
      }
    }
    catch (SQLException e)
    {
      throw new DBException(e);
    }
    finally
    {
      DBUtil.close(stmt);
    }
  }

  public final void readReference(IDBStoreAccessor accessor, InternalCDORevision revision, int referenceChunk)
  {
    MoveableList<Object> list = revision.getList(getFeature());

    CDOID source = revision.getID();
    long sourceId = CDOIDUtil.getLong(source);
    int version = revision.getVersion();

    PreparedStatement pstmt = null;
    ResultSet resultSet = null;

    try
    {
      String sql = sqlSelectChunksPrefix + sqlOrderByIndex;

      if (TRACER.isEnabled())
      {
        TRACER.trace(sql);
      }

      pstmt = accessor.getConnection().prepareStatement(sql);

      pstmt.setLong(1, sourceId);
      pstmt.setInt(2, version);
      resultSet = pstmt.executeQuery();

      while (resultSet.next() && (referenceChunk == CDORevision.UNCHUNKED || --referenceChunk >= 0))
      {
        long target = resultSet.getLong(1);
        list.add(CDOIDUtil.createLong(target));
      }

      // TODO Optimize this?
      while (resultSet.next())
      {
        list.add(InternalCDORevision.UNINITIALIZED);
      }
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
    finally
    {
      DBUtil.close(resultSet);
      DBUtil.close(pstmt);
    }
  }

  public final void readChunks(IDBStoreChunkReader chunkReader, List<Chunk> chunks, String where)
  {
    CDOID source = chunkReader.getRevision().getID();
    long sourceId = CDOIDUtil.getLong(source);
    int version = chunkReader.getRevision().getVersion();

    PreparedStatement pstmt = null;
    ResultSet resultSet = null;

    try
    {
      StringBuilder builder = new StringBuilder(sqlSelectChunksPrefix);
      if (where != null)
      {
        builder.append(where);
      }
      builder.append(sqlOrderByIndex);

      String sql = builder.toString();
      if (TRACER.isEnabled())
      {
        TRACER.trace(sql);
      }

      pstmt = chunkReader.getAccessor().getConnection().prepareStatement(sql);

      pstmt.setLong(1, sourceId);
      pstmt.setInt(2, version);
      resultSet = pstmt.executeQuery();

      Chunk chunk = null;
      int chunkSize = 0;
      int chunkIndex = 0;
      int indexInChunk = 0;

      while (resultSet.next())
      {
        long target = resultSet.getLong(1);
        if (chunk == null)
        {
          chunk = chunks.get(chunkIndex++);
          chunkSize = chunk.size();
        }

        chunk.add(indexInChunk++, CDOIDUtil.createLong(target));
        if (indexInChunk == chunkSize)
        {
          chunk = null;
          indexInChunk = 0;
        }
      }
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
    finally
    {
      DBUtil.close(resultSet);
      DBUtil.close(pstmt);
    }
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("ReferenceMapping[feature={0}, table={1}]", getFeature(), getTable());
  }

  private void initSqlStrings()
  {
    String tableName = getTable().getName();

    // ---------------- SELECT to read chunks ----------------------------
    StringBuilder builder = new StringBuilder();
    builder.append("SELECT ");
    builder.append(CDODBSchema.REFERENCES_TARGET);
    builder.append(" FROM ");
    builder.append(tableName);
    builder.append(" WHERE ");
    builder.append(CDODBSchema.REFERENCES_SOURCE);
    builder.append("= ? AND ");
    builder.append(CDODBSchema.REFERENCES_VERSION);
    builder.append("= ? ");

    sqlSelectChunksPrefix = builder.toString();

    sqlOrderByIndex = " ORDER BY " + CDODBSchema.REFERENCES_IDX;

    // ---------------- DELETE - to clear reference ----------------------------
    builder = new StringBuilder("DELETE FROM ");
    builder.append(tableName);
    builder.append(" WHERE ");
    builder.append(CDODBSchema.REFERENCES_SOURCE);
    builder.append(" = ? ");
    sqlClearReference = builder.toString();

    // ----------------- UPDATE - reference version ------------------
    // TODO - remove this in the future
    builder = new StringBuilder();
    builder.append("UPDATE ");
    builder.append(tableName);
    builder.append(" SET ");
    builder.append(CDODBSchema.REFERENCES_VERSION);
    builder.append(" = ? ");
    builder.append(" WHERE ");
    builder.append(CDODBSchema.REFERENCES_SOURCE);
    builder.append(" = ?");

    sqlUpdateRefVersion = builder.toString();

    // ----------------- UPDATE - reference target -----------------
    builder = new StringBuilder("UPDATE ");
    builder.append(tableName);
    builder.append(" SET ");
    builder.append(CDODBSchema.REFERENCES_TARGET);
    builder.append(" = ?, ");
    builder.append(CDODBSchema.REFERENCES_VERSION);
    builder.append(" = ? WHERE ");
    builder.append(CDODBSchema.REFERENCES_SOURCE);
    builder.append("= ? AND ");
    builder.append(CDODBSchema.REFERENCES_IDX);
    builder.append(" = ?");

    sqlUpdateTarget = builder.toString();

    // ----------------- UPDATE - reference index -----------------
    builder = new StringBuilder("UPDATE ");
    builder.append(tableName);
    builder.append(" SET ");
    builder.append(CDODBSchema.REFERENCES_IDX);
    builder.append(" = ?, ");
    builder.append(CDODBSchema.REFERENCES_VERSION);
    builder.append(" = ? WHERE ");
    builder.append(CDODBSchema.REFERENCES_SOURCE);
    builder.append("= ? AND ");
    builder.append(CDODBSchema.REFERENCES_IDX);
    builder.append(" = ?");
    sqlUpdateIndex = builder.toString();

    // ----------------- UPDATE - move1up -----------------
    builder = new StringBuilder();
    builder.append("UPDATE ");
    builder.append(tableName);
    builder.append(" SET ");
    builder.append(CDODBSchema.REFERENCES_IDX);
    builder.append(" = ");
    builder.append(CDODBSchema.REFERENCES_IDX);
    builder.append("+1 ,");
    builder.append(CDODBSchema.REFERENCES_VERSION);
    builder.append(" = ? WHERE ");
    builder.append(CDODBSchema.REFERENCES_SOURCE);
    builder.append("= ? AND ");
    builder.append(CDODBSchema.REFERENCES_IDX);
    builder.append(" >= ?");
    sqlMoveUp = builder.toString();
    builder.append(" AND ");
    builder.append(CDODBSchema.REFERENCES_IDX);
    builder.append(" < ?");
    sqlMoveUpWithLimit = builder.toString();

    // ----------------- UPDATE - move1down -----------------
    builder = new StringBuilder();
    builder.append("UPDATE ");
    builder.append(tableName);
    builder.append(" SET ");
    builder.append(CDODBSchema.REFERENCES_IDX);
    builder.append(" = ");
    builder.append(CDODBSchema.REFERENCES_IDX);
    builder.append("-1 ,");
    builder.append(CDODBSchema.REFERENCES_VERSION);
    builder.append(" = ? WHERE ");
    builder.append(CDODBSchema.REFERENCES_SOURCE);
    builder.append("= ? AND ");
    builder.append(CDODBSchema.REFERENCES_IDX);
    builder.append(" > ?");
    sqlMoveDown = builder.toString();

    builder.append(" AND ");
    builder.append(CDODBSchema.REFERENCES_IDX);
    builder.append(" <= ?");
    sqlMoveDownWithLimit = builder.toString();

    // ----------------- INSERT - reference entry -----------------
    builder = new StringBuilder("INSERT INTO ");
    builder.append(tableName);
    builder.append(" VALUES (?, ?, ?, ?)");
    sqlInsertEntry = builder.toString();

    // ----------------- DELETE - reference entry -----------------
    builder = new StringBuilder("DELETE FROM ");
    builder.append(tableName);
    builder.append(" WHERE ");
    builder.append(CDODBSchema.REFERENCES_SOURCE);
    builder.append(" = ? AND ");
    builder.append(CDODBSchema.REFERENCES_IDX);
    builder.append(" = ? ");

    sqlDeleteEntry = builder.toString();
  }
}
