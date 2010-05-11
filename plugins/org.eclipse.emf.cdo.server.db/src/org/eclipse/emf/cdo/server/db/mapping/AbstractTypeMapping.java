/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Stefan Winkler - bug 271444: [DB] Multiple refactorings
 *    Stefan Winkler - bug 275303: [DB] DBStore does not handle BIG_INTEGER and BIG_DECIMAL
 *    Kai Schlamp - bug 282976: [DB] Influence Mappings through EAnnotations
 *    Stefan Winkler - bug 282976: [DB] Influence Mappings through EAnnotations
 *    Stefan Winkler - bug 285270: [DB] Support XSD based models
 */
package org.eclipse.emf.cdo.server.db.mapping;

import org.eclipse.emf.cdo.common.revision.CDORevisionData;
import org.eclipse.emf.cdo.server.internal.db.DBAnnotation;
import org.eclipse.emf.cdo.server.internal.db.MetaDataManager;
import org.eclipse.emf.cdo.server.internal.db.bundle.OM;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.net4j.db.DBType;
import org.eclipse.net4j.db.ddl.IDBField;
import org.eclipse.net4j.db.ddl.IDBTable;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EStructuralFeature;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This is a default implementation for the {@link ITypeMapping} interface which provides default behavor for all common
 * types.
 * 
 * @author Eike Stepper
 */
public abstract class AbstractTypeMapping implements ITypeMapping
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, AbstractTypeMapping.class);

  private IMappingStrategy mappingStrategy;

  private EStructuralFeature feature;

  private IDBField field;

  private DBType dbType;

  /**
   * Create a new type mapping
   */
  public AbstractTypeMapping()
  {
    super();
  }

  /**
   * Create a new type mapping
   * 
   * @param mappingStrategy
   *          the associated mapping strategy.
   * @param feature
   *          the feature to be mapped.
   */
  protected AbstractTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
  {
    this.mappingStrategy = mappingStrategy;
    this.feature = feature;
    dbType = type;
  }

  protected final void setMappingStrategy(IMappingStrategy mappingStrategy)
  {
    this.mappingStrategy = mappingStrategy;
  }

  public final IMappingStrategy getMappingStrategy()
  {
    return mappingStrategy;
  }

  protected final void setFeature(EStructuralFeature feature)
  {
    this.feature = feature;
  }

  public final EStructuralFeature getFeature()
  {
    return feature;
  }

  public final void setValueFromRevision(PreparedStatement stmt, int index, InternalCDORevision revision)
      throws SQLException
  {
    setValue(stmt, index, getRevisionValue(revision));
  }

  public void setDefaultValue(PreparedStatement stmt, int index) throws SQLException
  {
    setValue(stmt, index, getDefaultValue());
  }

  public final void setValue(PreparedStatement stmt, int index, Object value) throws SQLException
  {
    if (value == CDORevisionData.NIL)
    {
      if (TRACER.isEnabled())
      {
        TRACER.format("TypeMapping for {0}: converting Revision.NIL to DB-null", feature.getName()); //$NON-NLS-1$
      }

      stmt.setNull(index, getSqlType());
    }
    else if (value == null)
    {
      if (feature.isMany() || getDefaultValue() == null)
      {
        if (TRACER.isEnabled())
        {
          TRACER.format("TypeMapping for {0}: writing Revision.null as DB.null", feature.getName()); //$NON-NLS-1$
        }

        stmt.setNull(index, getSqlType());
      }
      else
      {
        if (TRACER.isEnabled())
        {
          TRACER.format("TypeMapping for {0}: converting Revision.null to default value", feature.getName()); //$NON-NLS-1$
        }

        setDefaultValue(stmt, index);
      }
    }
    else
    {
      doSetValue(stmt, index, value);
    }
  }

  public final void createDBField(IDBTable table)
  {
    createDBField(table, mappingStrategy.getFieldName(feature));
  }

  public final void createDBField(IDBTable table, String fieldName)
  {
    DBType fieldType = getDBType();
    int fieldLength = getDBLength(fieldType);
    field = table.addField(fieldName, fieldType, fieldLength);
  }

  public final void setDBField(IDBTable table, String fieldName)
  {
    field = table.getField(fieldName);
  }

  public final IDBField getField()
  {
    return field;
  }

  public final void readValueToRevision(ResultSet resultSet, InternalCDORevision revision) throws SQLException
  {
    Object value = readValue(resultSet);
    revision.setValue(getFeature(), value);
  }

  public final Object readValue(ResultSet resultSet) throws SQLException
  {
    Object value = getResultSetValue(resultSet);
    if (resultSet.wasNull())
    {
      if (feature.isMany())
      {
        if (TRACER.isEnabled())
        {
          TRACER.format("TypeMapping for {0}: read db.null - setting Revision.null", feature.getName()); //$NON-NLS-1$
        }

        value = null;
      }
      else
      {
        if (getDefaultValue() == null)
        {
          if (TRACER.isEnabled())
          {
            TRACER.format(
                "TypeMapping for {0}: read db.null - setting Revision.null, because of default", feature.getName()); //$NON-NLS-1$
          }

          value = null;
        }
        else
        {
          if (TRACER.isEnabled())
          {
            TRACER.format("TypeMapping for {0}: read db.null - setting Revision.NIL", feature.getName()); //$NON-NLS-1$
          }

          value = CDORevisionData.NIL;
        }
      }
    }

    return value;
  }

  protected Object getDefaultValue()
  {
    return feature.getDefaultValue();
  }

  protected final Object getRevisionValue(InternalCDORevision revision)
  {
    return revision.getValue(getFeature());
  }

  protected void doSetValue(PreparedStatement stmt, int index, Object value) throws SQLException
  {
    stmt.setObject(index, value, getSqlType());
  }

  /**
   * Returns the SQL type of this TypeMapping. The default implementation considers the type map hold by the meta-data
   * manager (@see {@link MetaDataManager#getDBType(org.eclipse.emf.ecore.EClassifier)} Subclasses may override.
   * 
   * @return The sql type of this TypeMapping.
   */
  protected int getSqlType()
  {
    return getDBType().getCode();
  }

  protected final void setDBType(DBType dbType)
  {
    this.dbType = dbType;
  }

  public DBType getDBType()
  {
    return dbType;
  }

  protected int getDBLength(DBType type)
  {
    String value = DBAnnotation.COLUMN_LENGTH.getValue(feature);
    if (value != null)
    {
      try
      {
        return Integer.parseInt(value);
      }
      catch (NumberFormatException e)
      {
        OM.LOG.error("Illegal columnLength annotation of feature " + feature.getName());
      }
    }

    // TODO: implement DBAdapter.getDBLength
    // mappingStrategy.getStore().getDBAdapter().getDBLength(type);
    // which should then return the correct default field length for the db type
    return type == DBType.VARCHAR ? 32672 : IDBField.DEFAULT;
  }

  protected abstract Object getResultSetValue(ResultSet resultSet) throws SQLException;

}
