/**
 *  Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Stefan Winkler - major refactoring
 */
package org.eclipse.emf.cdo.server.internal.db.mapping.horizontal;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.server.IStoreChunkReader.Chunk;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.server.db.IDBStoreChunkReader;
import org.eclipse.emf.cdo.server.db.mapping.IListMapping;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.emf.cdo.server.db.mapping.ITypeMapping;
import org.eclipse.emf.cdo.server.internal.db.CDODBSchema;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Eike Stepper
 * @author Stefan Winkler
 * @since 2.0
 */
public class ListTableAuditMapping implements IListMapping
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, ListTableAuditMapping.class);

  private EStructuralFeature feature;

  private IDBTable table;

  private ITypeMapping typeMapping;

  private IMappingStrategy mappingStrategy;

  private String sqlSelectChunksPrefix;

  private String sqlOrderByIndex;

  private String sqlInsertEntry;

  private EClass containingClass;

  public ListTableAuditMapping(IMappingStrategy mappingStrategy, EClass eClass, EStructuralFeature feature)
  {
    this.mappingStrategy = mappingStrategy;
    this.feature = feature;
    containingClass = eClass;

    initTable();
    initSqlStrings();
  }

  private void initTable()
  {
    String tableName = mappingStrategy.getTableName(containingClass, feature);
    table = mappingStrategy.getStore().getDBSchema().addTable(tableName);

    IDBField sourceField = table.addField(CDODBSchema.FEATURE_REVISION_ID, DBType.BIGINT);
    IDBField versionField = table.addField(CDODBSchema.FEATURE_REVISION_VERSION, DBType.INTEGER);
    IDBField idxField = table.addField(CDODBSchema.FEATURE_IDX, DBType.INTEGER);

    typeMapping = mappingStrategy.createValueMapping(feature);
    typeMapping.createDBField(table, CDODBSchema.FEATURE_TARGET);

    table.addIndex(Type.NON_UNIQUE, sourceField, versionField);
    table.addIndex(Type.NON_UNIQUE, idxField);
  }

  public Collection<IDBTable> getDBTables()
  {
    return Arrays.asList(table);
  }

  private void initSqlStrings()
  {
    String tableName = getTable().getName();

    // ---------------- SELECT to read chunks ----------------------------
    StringBuilder builder = new StringBuilder();
    builder.append("SELECT ");
    builder.append(CDODBSchema.FEATURE_TARGET);
    builder.append(" FROM ");
    builder.append(tableName);
    builder.append(" WHERE ");
    builder.append(CDODBSchema.FEATURE_REVISION_ID);
    builder.append("= ? AND ");
    builder.append(CDODBSchema.FEATURE_REVISION_VERSION);
    builder.append("= ? ");

    sqlSelectChunksPrefix = builder.toString();

    sqlOrderByIndex = " ORDER BY " + CDODBSchema.FEATURE_IDX;

    // ----------------- INSERT - reference entry -----------------
    builder = new StringBuilder("INSERT INTO ");
    builder.append(tableName);
    builder.append(" VALUES (?, ?, ?, ?)");
    sqlInsertEntry = builder.toString();
  }

  protected final EStructuralFeature getFeature()
  {
    return feature;
  }

  protected final IDBTable getTable()
  {
    return table;
  }

  protected final ITypeMapping getTypeMapping()
  {
    return typeMapping;
  }

  public void readValues(IDBStoreAccessor accessor, InternalCDORevision revision, int referenceChunk)
  {
    MoveableList<Object> list = revision.getList(getFeature());

    CDOID id = revision.getID();
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

      pstmt.setLong(1, CDOIDUtil.getLong(id));
      pstmt.setInt(2, version);
      resultSet = pstmt.executeQuery();

      while (resultSet.next() && (referenceChunk == CDORevision.UNCHUNKED || --referenceChunk >= 0))
      {
        list.add(typeMapping.readValue(resultSet, 1));
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

      pstmt.setLong(1, CDOIDUtil.getLong(source));
      pstmt.setInt(2, version);
      resultSet = pstmt.executeQuery();

      Chunk chunk = null;
      int chunkSize = 0;
      int chunkIndex = 0;
      int indexInChunk = 0;

      while (resultSet.next())
      {
        Object value = typeMapping.readValue(resultSet, 1);
        if (chunk == null)
        {
          chunk = chunks.get(chunkIndex++);
          chunkSize = chunk.size();
        }

        chunk.add(indexInChunk++, value);
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

  public void writeValues(IDBStoreAccessor accessor, InternalCDORevision revision)
  {
    int idx = 0;
    for (Object element : revision.getList(getFeature()))
    {
      writeValue(accessor, revision.getID(), revision.getVersion(), idx++, element);
    }
  }

  protected final void writeValue(IDBStoreAccessor accessor, CDOID id, int version, int idx, Object value)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlInsertEntry);

      stmt.setLong(1, CDOIDUtil.getLong(id));
      stmt.setInt(2, version);
      stmt.setInt(3, idx++);

      typeMapping.setValue(stmt, 4, value);

      if (TRACER.isEnabled())
      {
        TRACER.trace(stmt.toString());
      }

      int result = stmt.executeUpdate();
      // INSERT should affect one row
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
}
