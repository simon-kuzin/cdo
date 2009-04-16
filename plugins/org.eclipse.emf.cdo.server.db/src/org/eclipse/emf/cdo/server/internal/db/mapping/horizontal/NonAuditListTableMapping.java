/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
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
import org.eclipse.emf.cdo.server.db.CDODBUtil;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.server.db.mapping.IListMapping;
import org.eclipse.emf.cdo.server.db.mapping.IListMappingDeltaSupport;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.emf.cdo.server.internal.db.CDODBSchema;

import org.eclipse.net4j.db.DBException;
import org.eclipse.net4j.db.DBType;
import org.eclipse.net4j.db.DBUtil;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author Eike Stepper
 * @author Stefan Winkler
 * @since 2.0
 */
public class NonAuditListTableMapping extends AbstractListTableMapping implements IListMapping,
    IListMappingDeltaSupport
{
  private static final FieldInfo[] KEY_FIELDS = { new FieldInfo(CDODBSchema.FEATURE_REVISION_ID, DBType.BIGINT) };

  private static final int TEMP_INDEX = -1;

  private static final int UNBOUNDED_MOVE = -1;

  private String sqlClear;

  private String sqlUpdateValue;

  private String sqlUpdateIndex;

  private String sqlInsertValue;

  private String sqlDeleteItem;

  private String sqlMoveDownWithLimit;

  private String sqlMoveDown;

  private String sqlMoveUpWithLimit;

  private String sqlMoveUp;

  public NonAuditListTableMapping(IMappingStrategy mappingStrategy, EClass eClass, EStructuralFeature feature)
  {
    super(mappingStrategy, eClass, feature);

    initSqlStrings();
  }

  private void initSqlStrings()
  {
    // ----------- clear list -------------------------
    StringBuilder builder = new StringBuilder();

    builder.append("DELETE FROM ");
    builder.append(getTable().getName());
    builder.append(" WHERE ");
    builder.append(CDODBSchema.FEATURE_REVISION_ID);
    builder.append(" = ? ");

    sqlClear = builder.toString();

    builder.append(" AND ");
    builder.append(CDODBSchema.FEATURE_IDX);
    builder.append(" = ? ");

    sqlDeleteItem = builder.toString();

    // ----------- update one item --------------------
    builder = new StringBuilder();
    builder.append("UPDATE ");
    builder.append(getTable().getName());
    builder.append(" SET ");
    builder.append(CDODBSchema.FEATURE_TARGET);
    builder.append(" = ? ");
    builder.append(" WHERE ");
    builder.append(CDODBSchema.FEATURE_REVISION_ID);
    builder.append(" = ? AND ");
    builder.append(CDODBSchema.FEATURE_IDX);
    builder.append(" = ? ");
    sqlUpdateValue = builder.toString();

    // ----------- insert one item --------------------
    builder = new StringBuilder();
    builder.append("INSERT INTO ");
    builder.append(getTable().getName());
    builder.append(" VALUES(?, ?, ?) ");
    sqlInsertValue = builder.toString();

    // ----------- update one item index --------------
    builder = new StringBuilder();
    builder.append("UPDATE ");
    builder.append(getTable().getName());
    builder.append(" SET ");
    builder.append(CDODBSchema.FEATURE_IDX);
    builder.append(" = ? ");
    builder.append(" WHERE ");
    builder.append(CDODBSchema.FEATURE_REVISION_ID);
    builder.append(" = ? AND ");
    builder.append(CDODBSchema.FEATURE_IDX);
    builder.append(" = ? ");
    sqlUpdateIndex = builder.toString();

    // ----------- move down --------------
    builder = new StringBuilder();
    builder.append("UPDATE ");
    builder.append(getTable().getName());
    builder.append(" SET ");
    builder.append(CDODBSchema.FEATURE_IDX);
    builder.append(" = ");
    builder.append(CDODBSchema.FEATURE_IDX);
    builder.append("-1 WHERE ");
    builder.append(CDODBSchema.FEATURE_REVISION_ID);
    builder.append("= ? AND ");
    builder.append(CDODBSchema.FEATURE_IDX);
    builder.append(" > ? ");
    sqlMoveDown = builder.toString();

    builder.append(" AND ");
    builder.append(CDODBSchema.FEATURE_IDX);
    builder.append(" <= ?");
    sqlMoveDownWithLimit = builder.toString();

    // ----------- move up --------------
    builder = new StringBuilder();
    builder.append("UPDATE ");
    builder.append(getTable().getName());
    builder.append(" SET ");
    builder.append(CDODBSchema.FEATURE_IDX);
    builder.append(" = ");
    builder.append(CDODBSchema.FEATURE_IDX);
    builder.append("+1 WHERE ");
    builder.append(CDODBSchema.FEATURE_REVISION_ID);
    builder.append("= ? AND ");
    builder.append(CDODBSchema.FEATURE_IDX);
    builder.append(" >= ? ");
    sqlMoveUp = builder.toString();

    builder.append(" AND ");
    builder.append(CDODBSchema.FEATURE_IDX);
    builder.append(" < ?");
    sqlMoveUpWithLimit = builder.toString();

  }

  @Override
  protected FieldInfo[] getKeyFields()
  {
    return KEY_FIELDS;
  }

  @Override
  protected void setKeyFields(PreparedStatement stmt, CDORevision revision) throws SQLException
  {
    stmt.setLong(1, CDOIDUtil.getLong(revision.getID()));
  }

  public void objectRevised(IDBStoreAccessor accessor, CDOID id, long revised)
  {
    clearList(accessor, id);
  }

  public void clearList(IDBStoreAccessor accessor, CDOID id)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlClear);
      stmt.setLong(1, CDOIDUtil.getLong(id));
      CDODBUtil.sqlUpdate(stmt, false);
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

  public void insertListItem(IDBStoreAccessor accessor, CDOID id, int newVersion, int index, Object value)
  {
    move1up(accessor, id, index, UNBOUNDED_MOVE);
    insertValue(accessor, id, index, value);
  }

  private void insertValue(IDBStoreAccessor accessor, CDOID id, int index, Object value)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlInsertValue);
      stmt.setLong(1, CDOIDUtil.getLong(id));
      stmt.setInt(2, index);
      getTypeMapping().setValue(stmt, 3, value);

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

  public void moveListItem(IDBStoreAccessor accessor, CDOID id, int newVersion, int oldPosition, int newPosition)
  {
    if (oldPosition == newPosition)
    {
      return;
    }

    // move element away temporarily
    updateOneIndex(accessor, id, oldPosition, TEMP_INDEX);

    // move elements in between
    if (oldPosition < newPosition)
    {
      move1down(accessor, id, oldPosition, newPosition);
    }
    else
    {
      // oldPosition > newPosition -- equal case is handled above
      move1up(accessor, id, newPosition, oldPosition);
    }

    // move temporary element to new position
    updateOneIndex(accessor, id, TEMP_INDEX, newPosition);
  }

  private void updateOneIndex(IDBStoreAccessor accessor, CDOID id, int oldIndex, int newIndex)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlUpdateIndex);
      stmt.setInt(1, newIndex);
      stmt.setLong(2, CDOIDUtil.getLong(id));
      stmt.setInt(3, oldIndex);
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

  public void removeListItem(IDBStoreAccessor accessor, CDOID id, int newVersion, int index)
  {
    deleteItem(accessor, id, index);
    move1down(accessor, id, index, UNBOUNDED_MOVE);
  }

  /**
   * Move references downwards to close a gap at position <code>index</code>. Only indexes starting with
   * <code>index + 1</code> and ending with <code>upperIndex</code> are moved down.
   */
  private void move1down(IDBStoreAccessor accessor, CDOID id, int index, int upperIndex)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(
          upperIndex == UNBOUNDED_MOVE ? sqlMoveDown : sqlMoveDownWithLimit);

      stmt.setLong(1, CDOIDUtil.getLong(id));
      stmt.setInt(2, index);
      if (upperIndex != UNBOUNDED_MOVE)
      {
        stmt.setInt(3, upperIndex);
      }

      CDODBUtil.sqlUpdate(stmt, false);
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
  private void move1up(IDBStoreAccessor accessor, CDOID id, int index, int upperIndex)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(upperIndex == UNBOUNDED_MOVE ? sqlMoveUp : sqlMoveUpWithLimit);
      stmt.setLong(1, CDOIDUtil.getLong(id));
      stmt.setInt(2, index);
      if (upperIndex != UNBOUNDED_MOVE)
      {
        stmt.setInt(3, upperIndex);
      }

      CDODBUtil.sqlUpdate(stmt, false);
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

  private void deleteItem(IDBStoreAccessor accessor, CDOID id, int index)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlDeleteItem);
      stmt.setLong(1, CDOIDUtil.getLong(id));
      stmt.setInt(2, index);
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

  public void setListItem(IDBStoreAccessor accessor, CDOID id, int newVersion, int index, Object value)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlUpdateValue);
      getTypeMapping().setValue(stmt, 1, value);
      stmt.setLong(2, CDOIDUtil.getLong(id));
      stmt.setInt(3, index);
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
