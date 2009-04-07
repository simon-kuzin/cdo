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
import org.eclipse.emf.cdo.common.model.CDOModelUtil;
import org.eclipse.emf.cdo.common.model.CDOType;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.db.CDODBUtil;
import org.eclipse.emf.cdo.server.db.IDBStore;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.server.db.mapping.IAttributeMapping;
import org.eclipse.emf.cdo.server.db.mapping.IClassMapping;
import org.eclipse.emf.cdo.server.db.mapping.IFeatureMapping;
import org.eclipse.emf.cdo.server.db.mapping.IReferenceMapping;
import org.eclipse.emf.cdo.server.internal.db.CDODBSchema;
import org.eclipse.emf.cdo.server.internal.db.DBStore;
import org.eclipse.emf.cdo.server.internal.db.ToMany;
import org.eclipse.emf.cdo.server.internal.db.bundle.OM;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionDelta;

import org.eclipse.net4j.db.DBException;
import org.eclipse.net4j.db.DBType;
import org.eclipse.net4j.db.DBUtil;
import org.eclipse.net4j.db.IDBAdapter;
import org.eclipse.net4j.db.ddl.IDBField;
import org.eclipse.net4j.db.ddl.IDBIndex;
import org.eclipse.net4j.db.ddl.IDBTable;
import org.eclipse.net4j.util.ImplementationError;
import org.eclipse.net4j.util.om.monitor.OMMonitor;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Eike Stepper TODO: refactor attribute/reference/type mappings
 */
public abstract class ClassMapping implements IClassMapping
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, ClassMapping.class);

  private MappingStrategy mappingStrategy;

  private EClass eClass;

  private IDBTable table;

  private Set<IDBTable> affectedTables = new HashSet<IDBTable>();

  private List<IAttributeMapping> attributeMappings;

  private List<IReferenceMapping> referenceMappings;

  private String sqlSelectAttributesPrefix;

  private String sqlSelectAttributesAffix;

  private String sqlDeleteAttributes;

  private String sqlReviseAttributes;

  private String sqlReviseAttributesByID;

  private String sqlInsertAttributes;

  private String sqlUpdateAttributesAffix;

  private String sqlUpdateAttributesContainmentPart;

  private String sqlUpdateAttributesPrefix;

  private String sqlUpdateAllAttributesPart;

  private String sqlUpdateAllAttributes;

  public ClassMapping(MappingStrategy mappingStrategy, EClass eClass, EStructuralFeature[] features)
  {
    this.mappingStrategy = mappingStrategy;
    this.eClass = eClass;

    String tableName = mappingStrategy.getTableName(eClass);
    table = addTable(tableName);
    initTable(table, hasFullRevisionInfo());

    if (features != null)
    {
      attributeMappings = createAttributeMappings(features);
      referenceMappings = createReferenceMappings(features);

      // // Special handling of CDOResource table
      // CDOResourceClass resourceClass = getResourceClass();
      // if (eClass == resourceClass)
      // {
      // // Create a unique ids to prevent duplicate resource paths
      // for (IAttributeMapping attributeMapping : attributeMappings)
      // {
      // if (attributeMapping.getFeature() == resourceClass.getCDOPathFeature())
      // {
      // IDBField versionField = table.getField(CDODBSchema.ATTRIBUTES_VERSION);
      // IDBField pathField = attributeMapping.getField();
      // pathField.setPrecision(760);// MYSQL key limitation 767
      // pathField.setNotNull(true);
      //
      // // Example: Currently a store can not specify that it does not support non-auditing mode!
      // if (false && !mappingStrategy.getStore().getRepository().isSupportingAudits())
      // {
      // // Create a unique ids to prevent duplicate resource paths
      // table.addIndex(IDBIndex.Type.UNIQUE, versionField, pathField);
      // }
      //
      // break;
      // }
      // }
      // }
    }

    initSqlStrings();
  }

  public MappingStrategy getMappingStrategy()
  {
    return mappingStrategy;
  }

  public EClass getEClass()
  {
    return eClass;
  }

  public IDBTable getTable()
  {
    return table;
  }

  public Set<IDBTable> getAffectedTables()
  {
    return affectedTables;
  }

  protected void initTable(IDBTable table, boolean full)
  {
    IDBField idField = table.addField(CDODBSchema.ATTRIBUTES_ID, DBType.BIGINT, true);
    table.addField(CDODBSchema.ATTRIBUTES_VERSION, DBType.INTEGER, true);
    if (full)
    {
      table.addField(CDODBSchema.ATTRIBUTES_CLASS, DBType.BIGINT, true);
      table.addField(CDODBSchema.ATTRIBUTES_CREATED, DBType.BIGINT, true);
      IDBField revisedField = table.addField(CDODBSchema.ATTRIBUTES_REVISED, DBType.BIGINT, true);
      table.addField(CDODBSchema.ATTRIBUTES_RESOURCE, DBType.BIGINT, true);
      table.addField(CDODBSchema.ATTRIBUTES_CONTAINER, DBType.BIGINT, true);
      table.addField(CDODBSchema.ATTRIBUTES_FEATURE, DBType.INTEGER, true);

      table.addIndex(IDBIndex.Type.NON_UNIQUE, idField, revisedField);
    }
  }

  protected IDBTable addTable(String name)
  {
    IDBTable table = mappingStrategy.getStore().getDBSchema().addTable(name);
    affectedTables.add(table);
    return table;
  }

  protected IDBField addField(EStructuralFeature feature, IDBTable table) throws DBException
  {
    String fieldName = mappingStrategy.getFieldName(feature);
    DBType fieldType = getDBType(feature);
    int fieldLength = getDBLength(feature);

    IDBField field = table.addField(fieldName, fieldType, fieldLength);
    affectedTables.add(table);
    return field;
  }

  protected DBType getDBType(EStructuralFeature feature)
  {
    return DBStore.getDBType(feature.getEType());
  }

  protected int getDBLength(EStructuralFeature feature)
  {
    // TODO make length DB dependent (Oracle only supports 30 chars)
    // Derby: The maximum length for a VARCHAR string is 32,672 characters.
    CDOType type = CDOModelUtil.getType(feature.getEType());
    return type == CDOType.STRING || type == CDOType.CUSTOM ? 32672 : IDBField.DEFAULT;
  }

  protected IDBAdapter getDBAdapter()
  {
    IDBStore store = mappingStrategy.getStore();
    return store.getDBAdapter();
  }

  public IFeatureMapping getFeatureMapping(EStructuralFeature feature)
  {
    if (feature instanceof EReference && mappingStrategy.getToMany() != ToMany.LIKE_ATTRIBUTES)
    {
      return getReferenceMapping(feature);
    }

    return getAttributeMapping(feature);
  }

  public List<IAttributeMapping> getAttributeMappings()
  {
    return attributeMappings;
  }

  public List<IReferenceMapping> getReferenceMappings()
  {
    return referenceMappings;
  }

  public IReferenceMapping getReferenceMapping(EStructuralFeature feature)
  {
    // TODO Optimize this?
    for (IReferenceMapping referenceMapping : referenceMappings)
    {
      if (referenceMapping.getFeature() == feature)
      {
        return referenceMapping;
      }
    }

    return null;
  }

  public IAttributeMapping getAttributeMapping(EStructuralFeature feature)
  {
    // TODO Optimize this?
    for (IAttributeMapping attributeMapping : attributeMappings)
    {
      if (attributeMapping.getFeature() == feature)
      {
        return attributeMapping;
      }
    }

    return null;
  }

  protected List<IAttributeMapping> createAttributeMappings(EStructuralFeature[] features)
  {
    List<IAttributeMapping> attributeMappings = new ArrayList<IAttributeMapping>();
    for (EStructuralFeature feature : features)
    {
      if (feature instanceof EReference)
      {
        if (!feature.isMany())
        {
          attributeMappings.add(createToOneReferenceMapping(feature));
        }
      }
      else
      {
        attributeMappings.add(createAttributeMapping(feature));
      }
    }

    return attributeMappings.isEmpty() ? null : attributeMappings;
  }

  protected List<IReferenceMapping> createReferenceMappings(EStructuralFeature[] features)
  {
    List<IReferenceMapping> referenceMappings = new ArrayList<IReferenceMapping>();
    for (EStructuralFeature feature : features)
    {
      if (feature instanceof EReference && feature.isMany())
      {
        referenceMappings.add(createReferenceMapping(feature));
      }
    }

    return referenceMappings.isEmpty() ? null : referenceMappings;
  }

  /**
   * @deprecated move into extensible and flexible type mapping facility
   */
  @Deprecated
  protected AttributeMapping createAttributeMapping(EStructuralFeature feature)
  {
    CDOType type = CDOModelUtil.getType(feature.getEType());
    if (type == CDOType.BOOLEAN || type == CDOType.BOOLEAN_OBJECT)
    {
      return new AttributeMapping.AMBoolean(this, feature);
    }
    else if (type == CDOType.BYTE || type == CDOType.BYTE_OBJECT)
    {
      return new AttributeMapping.AMByte(this, feature);
    }
    else if (type == CDOType.CHAR || type == CDOType.CHARACTER_OBJECT)
    {
      return new AttributeMapping.AMCharacter(this, feature);
    }
    else if (type == CDOType.DATE)
    {
      return new AttributeMapping.AMDate(this, feature);
    }
    else if (type == CDOType.DOUBLE || type == CDOType.DOUBLE_OBJECT)
    {
      return new AttributeMapping.AMDouble(this, feature);
    }
    else if (type == CDOType.FLOAT || type == CDOType.FLOAT_OBJECT)
    {
      return new AttributeMapping.AMFloat(this, feature);
    }
    else if (type == CDOType.INT || type == CDOType.INTEGER_OBJECT)
    {
      return new AttributeMapping.AMInteger(this, feature);
    }
    else if (type == CDOType.LONG || type == CDOType.LONG_OBJECT)
    {
      return new AttributeMapping.AMLong(this, feature);
    }
    else if (type == CDOType.OBJECT)
    {
      return new AttributeMapping.AMObject(this, feature);
    }
    else if (type == CDOType.SHORT || type == CDOType.SHORT_OBJECT)
    {
      return new AttributeMapping.AMShort(this, feature);
    }
    else if (type == CDOType.ENUM)
    {
      return new AttributeMapping.AMEnum(this, feature);
    }
    else if (type == CDOType.STRING || type == CDOType.CUSTOM)
    {
      return new AttributeMapping.AMString(this, feature);
    }

    throw new ImplementationError("Unrecognized CDOType: " + type);
  }

  protected ToOneReferenceMapping createToOneReferenceMapping(EStructuralFeature feature)
  {
    return new ToOneReferenceMapping(this, feature);
  }

  protected ReferenceMapping createReferenceMapping(EStructuralFeature feature)
  {
    return new ReferenceMapping(this, feature, ToMany.PER_REFERENCE);
  }

  public Object createReferenceMappingKey(EStructuralFeature feature)
  {
    return feature;
  }

  public void writeRevision(IDBStoreAccessor accessor, InternalCDORevision revision, OMMonitor monitor)
  {
    try
    {
      // TODO Better monitoring
      monitor.begin(10);

      if (revision.getVersion() > 1 && hasFullRevisionInfo() && isAuditing())
      {
        writeRevisedRow(accessor, revision);
      }

      monitor.worked();

      if (revision.isResourceFolder() || revision.isResource())
      {
        checkDuplicateResources(accessor, revision);
      }

      monitor.worked();

      // Write attribute table always (even without modeled attributes!)
      writeAttributes(accessor, revision);

      monitor.worked();

      // Write reference tables only if they exist
      if (referenceMappings != null)
      {
        writeReferences(accessor, revision);
      }

      monitor.worked(7);
    }
    finally
    {
      monitor.done();
    }
  }

  private boolean isAuditing()
  {
    return mappingStrategy.getStore().getRevisionTemporality() == IStore.RevisionTemporality.AUDITING;
  }

  protected abstract void checkDuplicateResources(IDBStoreAccessor accessor, CDORevision revision)
      throws IllegalStateException;

  public void detachObject(IDBStoreAccessor accessor, CDOID id, long revised, OMMonitor monitor)
  {
    try
    {
      monitor.begin();
      if (hasFullRevisionInfo())
      {
        if (isAuditing())
        {
          writeRevisedRow(accessor, id, revised);
          monitor.worked(1);
        }
        else
        {
          deleteRevision(accessor, id, monitor.fork(1));
        }
      }

      // TODO Handle !hasFullRevisionInfo() case
    }
    finally
    {
      monitor.done();
    }
  }

  protected void deleteRevision(IDBStoreAccessor accessor, CDOID id, OMMonitor monitor)
  {
    try
    {
      monitor.begin(2);
      deleteAttributes(accessor, id);
      monitor.worked(1);
      deleteReferences(accessor, id);
      monitor.worked(1);
    }
    finally
    {
      monitor.done();
    }
  }

  protected final void writeRevisedRow(IDBStoreAccessor accessor, InternalCDORevision revision)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlReviseAttributes);

      stmt.setLong(1, (revision.getCreated() - 1));
      stmt.setLong(2, CDOIDUtil.getLong(revision.getID()));
      stmt.setInt(3, (revision.getVersion() - 1));

      if (TRACER.isEnabled())
      {
        TRACER.trace(stmt.toString());
      }

      int result = stmt.executeUpdate();
      // only one unrevised row may exist - update count must be 1
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

  protected final void writeRevisedRow(IDBStoreAccessor accessor, CDOID id, long revised)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlReviseAttributesByID);

      stmt.setLong(1, revised);
      stmt.setLong(2, CDOIDUtil.getLong(id));

      if (TRACER.isEnabled())
      {
        TRACER.trace(stmt.toString());
      }

      int result = stmt.executeUpdate();

      // only one unrevised row may exist - update count must be 1
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

  protected final void writeAttributes(IDBStoreAccessor accessor, InternalCDORevision revision)
  {
    if (revision.getVersion() == 1 || isAuditing())
    {
      insertAttributes(accessor, revision);
    }
    else
    {
      updateAttributes(accessor, revision);
    }
  }

  private void insertAttributes(IDBStoreAccessor accessor, InternalCDORevision revision)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlInsertAttributes);

      int col = 1;

      stmt.setLong(col++, CDOIDUtil.getLong(revision.getID()));
      stmt.setInt(col++, revision.getVersion());
      if (hasFullRevisionInfo())
      {
        stmt.setLong(col++, accessor.getStore().getMetaID(revision.getEClass()));
        stmt.setLong(col++, revision.getCreated());
        stmt.setLong(col++, revision.getRevised());
        stmt.setLong(col++, CDOIDUtil.getLong(revision.getResourceID()));
        stmt.setLong(col++, CDODBUtil.getLong((CDOID)revision.getContainerID()));
        stmt.setInt(col++, revision.getContainingFeatureID());
      }

      if (attributeMappings != null)
      {
        for (IAttributeMapping attributeMapping : attributeMappings)
        {
          Object value = attributeMapping.getRevisionValue(revision);

          if (value == null)
          {
            stmt.setNull(col++, attributeMapping.getField().getType().getCode());
          }
          else if (value instanceof java.util.Date)
          {
            // BUG 217255
            stmt.setTimestamp(col++, new Timestamp(((Date)value).getTime()));
          }
          else if (value instanceof EEnumLiteral)
          {
            stmt.setInt(col++, ((EEnumLiteral)value).getValue());
          }
          else
          {
            stmt.setObject(col++, value);
          }
        }
      }

      if (TRACER.isEnabled())
      {
        TRACER.trace(stmt.toString());
      }

      int result = stmt.executeUpdate();
      // INSERT should insert one row
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

  protected final void updateAttributes(IDBStoreAccessor accessor, InternalCDORevision revision)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlUpdateAllAttributes);

      int col = 1;
      stmt.setInt(col++, revision.getVersion());
      stmt.setLong(col++, revision.getCreated());
      stmt.setLong(col++, CDODBUtil.getLong((CDOID)revision.getContainerID()));
      stmt.setInt(col++, revision.getContainingFeatureID());
      stmt.setLong(col++, CDOIDUtil.getLong(revision.getResourceID()));

      if (attributeMappings != null)
      {
        for (IAttributeMapping attributeMapping : attributeMappings)
        {
          Object value = attributeMapping.getRevisionValue(revision);

          if (value == null)
          {
            stmt.setNull(col++, attributeMapping.getField().getType().getCode());
          }
          else if (value instanceof java.util.Date)
          {
            // BUG 217255
            stmt.setTimestamp(col++, new Timestamp(((Date)value).getTime()));
          }
          else if (value instanceof EEnumLiteral)
          {
            stmt.setInt(col++, ((EEnumLiteral)value).getValue());
          }
          else
          {
            stmt.setObject(col++, value);
          }
        }
      }

      stmt.setLong(col++, CDOIDUtil.getLong(revision.getID()));

      if (TRACER.isEnabled())
      {
        TRACER.trace(stmt.toString());
      }

      int result = stmt.executeUpdate();
      // UPDATE should update one row
      if (result != 1)
      {
        throw new IllegalStateException(stmt.toString() + " returned Update count " + result + " (expected: 1)");
      }
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
    finally
    {
      DBUtil.close(stmt);
    }
  }

  protected final void deleteAttributes(IDBStoreAccessor accessor, CDOID id)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlDeleteAttributes);
      stmt.setLong(1, CDOIDUtil.getLong(id));

      if (TRACER.isEnabled())
      {
        TRACER.trace(stmt.toString());
      }

      int result = stmt.executeUpdate();

      // DELETE should delete one row
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

  protected final void deleteReferences(IDBStoreAccessor accessor, CDOID id)
  {
    if (referenceMappings != null)
    {
      for (IReferenceMapping referenceMapping : referenceMappings)
      {
        referenceMapping.deleteReference(accessor, id);
      }
    }
  }

  protected void writeReferences(IDBStoreAccessor accessor, InternalCDORevision revision)
  {
    if (mappingStrategy.getStore().getRevisionTemporality() == IStore.RevisionTemporality.NONE)
    {
      deleteReferences(accessor, revision.getID());
    }

    for (IReferenceMapping referenceMapping : referenceMappings)
    {
      referenceMapping.writeReference(accessor, revision);
    }
  }

  public boolean readRevision(IDBStoreAccessor accessor, InternalCDORevision revision, int referenceChunk)
  {
    String where = mappingStrategy.createWhereClause(CDORevision.UNSPECIFIED_DATE);
    return readRevision(accessor, revision, where, referenceChunk);
  }

  public boolean readRevisionByTime(IDBStoreAccessor accessor, InternalCDORevision revision, long timeStamp,
      int referenceChunk)
  {
    String where = mappingStrategy.createWhereClause(timeStamp);
    return readRevision(accessor, revision, where, referenceChunk);
  }

  public boolean readRevisionByVersion(IDBStoreAccessor accessor, InternalCDORevision revision, int version,
      int referenceChunk)
  {
    String where = CDODBSchema.ATTRIBUTES_VERSION + "=" + version;
    return readRevision(accessor, revision, where, referenceChunk);
  }

  /**
   * Read a revision.
   * 
   * @return <code>true</code> if the revision has been read successfully.<br>
   *         <code>false</code> if the revision does not exist in the DB.
   */
  protected boolean readRevision(IDBStoreAccessor accessor, InternalCDORevision revision, String where,
      int referenceChunk)
  {
    // Read attribute table always (even without modeled attributes!)
    boolean success = readAttributes(accessor, revision, where);

    // Read reference tables only if revision exists and if references exist
    if (success && referenceMappings != null)
    {
      readReferences(accessor, revision, referenceChunk);
    }

    return success;
  }

  private void initSqlStrings()
  {
    // ----------- Select Revision ---------------------------
    StringBuilder builder = new StringBuilder();
    builder.append("SELECT ");
    builder.append(CDODBSchema.ATTRIBUTES_VERSION);
    builder.append(", ");
    builder.append(CDODBSchema.ATTRIBUTES_CREATED);

    if (hasFullRevisionInfo())
    {
      builder.append(", ");
      builder.append(CDODBSchema.ATTRIBUTES_REVISED);
      builder.append(", ");
      builder.append(CDODBSchema.ATTRIBUTES_RESOURCE);
      builder.append(", ");
      builder.append(CDODBSchema.ATTRIBUTES_CONTAINER);
      builder.append(", ");
      builder.append(CDODBSchema.ATTRIBUTES_FEATURE);
    }

    if (attributeMappings != null)
    {
      for (IAttributeMapping attributeMapping : attributeMappings)
      {
        builder.append(", ");
        builder.append(attributeMapping.getField());
      }
    }

    builder.append(" FROM ");
    builder.append(table.getName());
    builder.append(" WHERE ");
    builder.append(CDODBSchema.ATTRIBUTES_ID);
    builder.append("= ? AND (");

    sqlSelectAttributesPrefix = builder.toString();
    sqlSelectAttributesAffix = ")";

    // ----------- Delete Revision ---------------------------
    builder = new StringBuilder("DELETE FROM ");
    builder.append(table.getName());
    builder.append(" WHERE ");
    builder.append(CDODBSchema.ATTRIBUTES_ID);
    builder.append(" = ? ");

    sqlDeleteAttributes = builder.toString();

    // ----------- Update to set revised ---------------------
    builder = new StringBuilder("UPDATE ");
    builder.append(table.getName());
    builder.append(" SET ");
    builder.append(CDODBSchema.ATTRIBUTES_REVISED);
    builder.append(" = ? WHERE ");
    builder.append(CDODBSchema.ATTRIBUTES_ID);
    builder.append(" = ? AND ");
    builder.append(CDODBSchema.ATTRIBUTES_VERSION);
    builder.append(" = ?");
    sqlReviseAttributes = builder.toString();

    // TODO unify both ways to revise revisions!
    // ----------- Update to set revised by ID ----------------
    builder = new StringBuilder("UPDATE ");
    builder.append(getTable().getName());
    builder.append(" SET ");
    builder.append(CDODBSchema.ATTRIBUTES_REVISED);
    builder.append(" = ? WHERE ");
    builder.append(CDODBSchema.ATTRIBUTES_ID);
    builder.append(" = ? AND ");
    builder.append(CDODBSchema.ATTRIBUTES_REVISED);
    builder.append(" = 0");
    sqlReviseAttributesByID = builder.toString();

    // ----------- Insert Attributes -------------------------
    builder = new StringBuilder();
    builder.append("INSERT INTO ");
    builder.append(table.getName());
    builder.append(" VALUES (?, ?, ");
    if (hasFullRevisionInfo())
    {
      builder.append("?, ?, ?, ?, ?, ?");
    }
    if (attributeMappings != null)
    {
      for (int i = 0; i < getAttributeMappings().size(); i++)
      {
        builder.append(", ?");
      }
    }
    builder.append(")");
    sqlInsertAttributes = builder.toString();

    // ------------- Update Attributes -----------------------
    builder = new StringBuilder();
    builder.append("UPDATE ");
    builder.append(table.getName());
    builder.append(" SET ");
    builder.append(CDODBSchema.ATTRIBUTES_VERSION);
    builder.append(" = ?, ");
    builder.append(CDODBSchema.ATTRIBUTES_CREATED);
    builder.append(" = ?");
    sqlUpdateAttributesPrefix = builder.toString();

    builder = new StringBuilder();
    builder.append(", ");
    builder.append(CDODBSchema.ATTRIBUTES_CONTAINER);
    builder.append(" = ?, ");
    builder.append(CDODBSchema.ATTRIBUTES_FEATURE);
    builder.append(" = ?, ");
    builder.append(CDODBSchema.ATTRIBUTES_RESOURCE);
    builder.append(" = ?");
    sqlUpdateAttributesContainmentPart = builder.toString();

    builder = new StringBuilder();
    if (attributeMappings != null)
    {
      for (IAttributeMapping am : attributeMappings)
      {
        builder.append(", ");
        builder.append(am.getField().getName());
        builder.append("= ?");
      }
    }
    sqlUpdateAllAttributesPart = builder.toString();

    builder = new StringBuilder();
    builder.append(" WHERE ");
    builder.append(CDODBSchema.ATTRIBUTES_ID);
    builder.append(" = ?");
    sqlUpdateAttributesAffix = builder.toString();

    sqlUpdateAllAttributes = sqlUpdateAttributesPrefix + sqlUpdateAttributesContainmentPart
        + sqlUpdateAllAttributesPart + sqlUpdateAttributesAffix;

  }

  /**
   * Read the revision's attributes from the DB.
   * 
   * @return <code>true</code> if the revision has been read successfully.<br>
   *         <code>false</code> if the revision does not exist in the DB.
   */
  protected final boolean readAttributes(IDBStoreAccessor accessor, InternalCDORevision revision, String where)
  {
    List<IAttributeMapping> attributeMappings = getAttributeMappings();
    if (attributeMappings == null)
    {
      attributeMappings = Collections.emptyList();
    }

    PreparedStatement pstmt = null;
    ResultSet resultSet = null;
    try
    {
      String sql = sqlSelectAttributesPrefix + where + sqlSelectAttributesAffix;

      // TODO add caching
      pstmt = accessor.getConnection().prepareStatement(sql);
      pstmt.setLong(1, CDOIDUtil.getLong(revision.getID()));
      resultSet = pstmt.executeQuery();

      if (!resultSet.next())
      {
        return false;
      }

      int i = 0;
      if (hasFullRevisionInfo())
      {
        revision.setVersion(resultSet.getInt(++i));
        revision.setCreated(resultSet.getLong(++i));
        revision.setRevised(resultSet.getLong(++i));
        revision.setResourceID(CDOIDUtil.createLong(resultSet.getLong(++i)));
        revision.setContainerID(CDOIDUtil.createLong(resultSet.getLong(++i)));
        revision.setContainingFeatureID(resultSet.getInt(++i));
      }

      if (attributeMappings != null)
      {
        for (IAttributeMapping attributeMapping : attributeMappings)
        {
          attributeMapping.extractValue(resultSet, ++i, revision);
        }
      }

      return true;
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

  protected void readReferences(IDBStoreAccessor accessor, InternalCDORevision revision, int referenceChunk)
  {
    for (IReferenceMapping referenceMapping : referenceMappings)
    {
      referenceMapping.readReference(accessor, revision, referenceChunk);
    }
  }

  public void writeRevisionDelta(IDBStoreAccessor accessor, InternalCDORevisionDelta delta, long created,
      OMMonitor monitor)
  {
    throw new UnsupportedOperationException();
  }
}
