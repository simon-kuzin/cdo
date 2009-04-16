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

import org.eclipse.emf.cdo.common.revision.CDOList;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.server.IStoreChunkReader.Chunk;
import org.eclipse.emf.cdo.server.db.CDODBUtil;
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
public abstract class AbstractListTableMapping implements IListMapping
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, AbstractListTableMapping.class);

  private EStructuralFeature feature;

  private IDBTable table;

  private ITypeMapping typeMapping;

  private IMappingStrategy mappingStrategy;

  private String sqlSelectChunksPrefix;

  private String sqlOrderByIndex;

  private String sqlInsertEntry;

  private EClass containingClass;

  public AbstractListTableMapping(IMappingStrategy mappingStrategy, EClass eClass, EStructuralFeature feature)
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

    // add fields for keys (cdo_id, version, feature_id)
    FieldInfo[] fields = getKeyFields();
    IDBField[] dbFields = new IDBField[fields.length];

    for (int i = 0; i < fields.length; i++)
    {
      dbFields[i] = table.addField(fields[i].getName(), fields[i].getDbType());
    }

    // add field for list index
    IDBField idxField = table.addField(CDODBSchema.FEATURE_IDX, DBType.INTEGER);

    // add field for value
    typeMapping = mappingStrategy.createValueMapping(feature);
    typeMapping.createDBField(table, CDODBSchema.FEATURE_TARGET);

    // add table indexes
    table.addIndex(Type.NON_UNIQUE, dbFields);
    table.addIndex(Type.NON_UNIQUE, idxField);
  }

  protected abstract FieldInfo[] getKeyFields();

  protected abstract void setKeyFields(PreparedStatement stmt, CDORevision revision) throws SQLException;

  public Collection<IDBTable> getDBTables()
  {
    return Arrays.asList(table);
  }

  private void initSqlStrings()
  {
    String tableName = getTable().getName();
    FieldInfo[] fields = getKeyFields();

    // ---------------- SELECT to read chunks ----------------------------
    StringBuilder builder = new StringBuilder();
    builder.append("SELECT ");
    builder.append(CDODBSchema.FEATURE_TARGET);
    builder.append(" FROM ");
    builder.append(tableName);
    builder.append(" WHERE ");

    for (int i = 0; i < fields.length; i++)
    {
      builder.append(fields[i].getName());
      if (i + 1 < fields.length)
      {
        // more to come
        builder.append("= ? AND ");
      }
      else
      {
        // last one
        builder.append("= ? ");
      }
    }

    sqlSelectChunksPrefix = builder.toString();

    sqlOrderByIndex = " ORDER BY " + CDODBSchema.FEATURE_IDX;

    // ----------------- INSERT - reference entry -----------------
    builder = new StringBuilder("INSERT INTO ");
    builder.append(tableName);
    builder.append(" VALUES (");
    for (int i = 0; i < fields.length; i++)
    {
      builder.append("?, ");
    }
    builder.append(" ?, ?)");
    sqlInsertEntry = builder.toString();
  }

  public final EStructuralFeature getFeature()
  {
    return feature;
  }

  public final EClass getContainingClass()
  {
    return containingClass;
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

    if (TRACER.isEnabled())
    {
      TRACER.format("Reading list values for feature {0}.{1} of {2}v{3}", containingClass.getName(), feature.getName(),
          revision.getID(), revision.getVersion());
    }

    PreparedStatement pstmt = null;
    ResultSet resultSet = null;

    try
    {
      String sql = sqlSelectChunksPrefix + sqlOrderByIndex;

      pstmt = accessor.getConnection().prepareStatement(sql);

      setKeyFields(pstmt, revision);

      if (TRACER.isEnabled())
      {
        TRACER.trace(pstmt.toString());
      }

      resultSet = pstmt.executeQuery();

      while ((referenceChunk == CDORevision.UNCHUNKED || --referenceChunk >= 0) && resultSet.next())
      {
        Object value = typeMapping.readValue(resultSet, 1);
        if (TRACER.isEnabled())
        {
          TRACER.format("Read value for index {0} from result set: {1}", list.size(), value);
        }
        list.add(value);
      }

      // TODO Optimize this?
      while (resultSet.next())
      {
        if (TRACER.isEnabled())
        {
          TRACER.format("Additional value for index {0} ignored due to chunking. Setting uninitialized.", list.size());
        }

        list.add(InternalCDORevision.UNINITIALIZED);
      }

      if (TRACER.isEnabled())
      {
        TRACER.format("Reading list values done for feature {0}.{1} of {2}v{3}", containingClass.getName(), feature
            .getName(), revision.getID(), revision.getVersion());
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
    if (TRACER.isEnabled())
    {
      TRACER.format("Reading list chunk values for feature {0}.{1} of {2}v{3}", containingClass.getName(), feature
          .getName(), chunkReader.getRevision().getID(), chunkReader.getRevision().getVersion());
    }

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

      pstmt = chunkReader.getAccessor().getConnection().prepareStatement(sql);

      setKeyFields(pstmt, chunkReader.getRevision());

      if (TRACER.isEnabled())
      {
        TRACER.trace(pstmt.toString());
      }

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

          if (TRACER.isEnabled())
          {
            TRACER.format("Current chunk no. {0} is [start = {1}, size = {2}]", chunkIndex - 1, chunk.getStartIndex(),
                chunkSize);
          }
        }

        if (TRACER.isEnabled())
        {
          TRACER.format("Read value for chunk index {0} from result set: {1}", indexInChunk, value);
        }
        chunk.add(indexInChunk++, value);

        if (indexInChunk == chunkSize)
        {
          if (TRACER.isEnabled())
          {
            TRACER.format("Chunk finished.");
          }

          chunk = null;
          indexInChunk = 0;
        }
      }

      if (TRACER.isEnabled())
      {
        TRACER.format("Reading list chunk values done for feature {0}.{1} of {2}v{3}", containingClass.getName(),
            feature.getName(), chunkReader.getRevision().getID(), chunkReader.getRevision().getVersion());
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
    CDOList values = revision.getList(getFeature());

    // if(values.contains(InternalCDORevision.UNINITIALIZED)) {
    // readUninitializedValues(accessor, values);
    // }

    int idx = 0;
    for (Object element : values)
    {
      writeValue(accessor, revision, idx++, element);
    }
  }

  // private void readUninitializedValues(IDBStoreAccessor accessor, CDORevision revision, MoveableList<Object> values)
  // {
  // CDOID id = revision.getID();
  // int version = revision.getVersion();
  //
  // if (TRACER.isEnabled())
  // {
  // TRACER.format("Reading uninitialized list values for feature {0}.{1} of {2}v{3}", containingClass.getName(),
  // feature.getName(),
  // id, version);
  // }
  //
  // PreparedStatement pstmt = null;
  // ResultSet resultSet = null;
  //
  // try
  // {
  // String sql = sqlSelectChunksPrefix + sqlOrderByIndex;
  //
  // pstmt = accessor.getConnection().prepareStatement(sql);
  //
  // pstmt.setLong(1, CDOIDUtil.getLong(id));
  // pstmt.setInt(2, version);
  // if (TRACER.isEnabled())
  // {
  // TRACER.trace(pstmt.toString());
  // }
  //
  // resultSet = pstmt.executeQuery();
  //
  // int index = 0;
  //      
  // while(resultSet.next()) {
  // if(values.get(index) == InternalCDORevision.UNINITIALIZED) {
  // Object value = typeMapping.readValue(resultSet, 1);
  // if (TRACER.isEnabled())
  // {
  // TRACER.format("Read value for index {0} from result set: {1}", list.size(), value);
  // }
  // }
  // }
  // }
  // catch (SQLException ex)
  // {
  // throw new DBException(ex);
  // }
  // finally
  // {
  // DBUtil.close(resultSet);
  // DBUtil.close(pstmt);
  // }
  // }

  protected final void writeValue(IDBStoreAccessor accessor, CDORevision revision, int idx, Object value)
  {
    PreparedStatement stmt = null;

    if (TRACER.isEnabled())
    {
      TRACER.format("Writing value for feature {0}.{1} index {2} of {3}v{4} : {5}", containingClass.getName(), feature
          .getName(), idx, revision.getID(), revision.getVersion(), value);
    }

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlInsertEntry);

      setKeyFields(stmt, revision);
      int stmtIndex = getKeyFields().length + 1;
      stmt.setInt(stmtIndex++, idx);
      typeMapping.setValue(stmt, stmtIndex++, value);

      CDODBUtil.sqlUpdate(stmt, true);
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
