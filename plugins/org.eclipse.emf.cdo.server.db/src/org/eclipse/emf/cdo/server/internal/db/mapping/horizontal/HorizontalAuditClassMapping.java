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
import org.eclipse.emf.cdo.common.model.CDOModelUtil;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.eresource.EresourcePackage;
import org.eclipse.emf.cdo.server.db.CDODBUtil;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.server.db.IMetaDataManager;
import org.eclipse.emf.cdo.server.db.mapping.IAuditSupport;
import org.eclipse.emf.cdo.server.db.mapping.IClassMapping;
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
import org.eclipse.net4j.db.ddl.IDBIndex;
import org.eclipse.net4j.db.ddl.IDBTable;
import org.eclipse.net4j.util.ImplementationError;
import org.eclipse.net4j.util.om.monitor.OMMonitor;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * TODO use async monitors
 * 
 * @author Eike Stepper
 * @author Stefan Winkler
 * @since 2.0
 */
public class HorizontalAuditClassMapping implements IClassMapping, IAuditSupport
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, HorizontalAuditClassMapping.class);

  private EClass eClass;

  private IDBTable table;

  private IMappingStrategy mappingStrategy;

  private List<ITypeMapping> singleValueFeatureMappings;

  private List<IListMapping> manyValueFeatureMappings;

  private String sqlReviseAttributes;

  private String sqlInsertAttributes;

  private String sqlSelectCurrentAttributes;

  private String sqlSelectAttributesByTime;

  private String sqlSelectAttributesByVersion;

  private String sqlSelectAllObjectIds;

  public HorizontalAuditClassMapping(IMappingStrategy mappingStrategy, EClass eClass)
  {
    this.mappingStrategy = mappingStrategy;
    this.eClass = eClass;

    initTable();
    initFeatures();
    initSqlStrings();
  }

  private void initTable()
  {
    String name = mappingStrategy.getTableName(eClass);
    table = mappingStrategy.getStore().getDBSchema().addTable(name);

    IDBField idField = table.addField(CDODBSchema.ATTRIBUTES_ID, DBType.BIGINT, true);
    table.addField(CDODBSchema.ATTRIBUTES_VERSION, DBType.INTEGER, true);
    table.addField(CDODBSchema.ATTRIBUTES_CLASS, DBType.BIGINT, true);
    table.addField(CDODBSchema.ATTRIBUTES_CREATED, DBType.BIGINT, true);
    IDBField revisedField = table.addField(CDODBSchema.ATTRIBUTES_REVISED, DBType.BIGINT, true);
    table.addField(CDODBSchema.ATTRIBUTES_RESOURCE, DBType.BIGINT, true);
    table.addField(CDODBSchema.ATTRIBUTES_CONTAINER, DBType.BIGINT, true);
    table.addField(CDODBSchema.ATTRIBUTES_FEATURE, DBType.INTEGER, true);

    table.addIndex(IDBIndex.Type.NON_UNIQUE, idField, revisedField);
  }

  private void initFeatures()
  {
    EStructuralFeature[] features = CDOModelUtil.getAllPersistentFeatures(eClass);

    if (features == null)
    {
      singleValueFeatureMappings = Collections.emptyList();
      manyValueFeatureMappings = Collections.emptyList();
    }
    else
    {
      singleValueFeatureMappings = createSingleMappings(features);
      manyValueFeatureMappings = createManyMappings(features);
    }
  }

  private List<ITypeMapping> createSingleMappings(EStructuralFeature[] features)
  {
    List<ITypeMapping> mappings = new ArrayList<ITypeMapping>();
    for (EStructuralFeature feature : features)
    {
      if (!feature.isMany())
      {
        ITypeMapping mapping = mappingStrategy.createValueMapping(feature);
        mapping.createDBField(getTable());
        mappings.add(mapping);
      }
    }

    return mappings;
  }

  private List<IListMapping> createManyMappings(EStructuralFeature[] features)
  {
    List<IListMapping> referenceMappings = new ArrayList<IListMapping>();
    for (EStructuralFeature feature : features)
    {
      if (feature.isMany())
      {
        referenceMappings.add(mappingStrategy.createListMapping(eClass, feature));
      }
    }

    return referenceMappings;
  }

  private void initSqlStrings()
  {
    // ----------- Select Revision ---------------------------
    StringBuilder builder = new StringBuilder();

    builder.append("SELECT ");
    builder.append(CDODBSchema.ATTRIBUTES_VERSION);
    builder.append(", ");
    builder.append(CDODBSchema.ATTRIBUTES_CREATED);
    builder.append(", ");
    builder.append(CDODBSchema.ATTRIBUTES_REVISED);
    builder.append(", ");
    builder.append(CDODBSchema.ATTRIBUTES_RESOURCE);
    builder.append(", ");
    builder.append(CDODBSchema.ATTRIBUTES_CONTAINER);
    builder.append(", ");
    builder.append(CDODBSchema.ATTRIBUTES_FEATURE);

    for (ITypeMapping singleMapping : singleValueFeatureMappings)
    {
      builder.append(", ");
      builder.append(singleMapping.getField().getName());
    }

    builder.append(" FROM ");
    builder.append(table.getName());
    builder.append(" WHERE ");
    builder.append(CDODBSchema.ATTRIBUTES_ID);
    builder.append("= ? AND (");

    String sqlSelectAttributesPrefix = builder.toString();

    builder.append(CDODBSchema.ATTRIBUTES_REVISED);
    builder.append(" = 0 )");

    sqlSelectCurrentAttributes = builder.toString();

    builder = new StringBuilder(sqlSelectAttributesPrefix);

    builder.append(CDODBSchema.ATTRIBUTES_REVISED);
    builder.append(" = 0 OR ");
    builder.append(CDODBSchema.ATTRIBUTES_REVISED);
    builder.append(" >= ?)");

    sqlSelectAttributesByTime = builder.toString();

    builder = new StringBuilder(sqlSelectAttributesPrefix);

    builder.append(CDODBSchema.ATTRIBUTES_VERSION);
    builder.append(" = ?)");

    sqlSelectAttributesByVersion = builder.toString();

    // ----------- Insert Attributes -------------------------
    builder = new StringBuilder();
    builder.append("INSERT INTO ");
    builder.append(table.getName());
    builder.append(" VALUES (?, ?, ");
    builder.append("?, ?, ?, ?, ?, ?");
    for (int i = 0; i < getSingleValueFeatureMappings().size(); i++)
    {
      builder.append(", ?");
    }
    builder.append(")");
    sqlInsertAttributes = builder.toString();

    // ----------- Update to set revised ----------------
    builder = new StringBuilder("UPDATE ");
    builder.append(getTable().getName());
    builder.append(" SET ");
    builder.append(CDODBSchema.ATTRIBUTES_REVISED);
    builder.append(" = ? WHERE ");
    builder.append(CDODBSchema.ATTRIBUTES_ID);
    builder.append(" = ? AND ");
    builder.append(CDODBSchema.ATTRIBUTES_REVISED);
    builder.append(" = 0");
    sqlReviseAttributes = builder.toString();

    // ----------- Select all unrevised Object IDs ------
    builder = new StringBuilder("SELECT ");
    builder.append(CDODBSchema.ATTRIBUTES_ID);
    builder.append(" FROM ");
    builder.append(getTable().getName());
    builder.append(" WHERE ");
    builder.append(CDODBSchema.ATTRIBUTES_REVISED);
    builder.append(" = 0");
    sqlSelectAllObjectIds = builder.toString();
  }

  public void detachObject(IDBStoreAccessor accessor, CDOID id, long revised, OMMonitor monitor)
  {
    try
    {
      monitor.begin();
      writeRevisedRow(accessor, id, revised);
      monitor.worked(1);
    }
    finally
    {
      monitor.done();
    }
  }

  private void checkDuplicateResources(IDBStoreAccessor accessor, CDORevision revision) throws IllegalStateException
  {
    CDOID folderID = (CDOID)revision.data().getContainerID();
    String name = (String)revision.data().get(EresourcePackage.eINSTANCE.getCDOResourceNode_Name(), 0);

    CDOID existingID = accessor.readResourceID(folderID, name, revision.getCreated());
    if (existingID != null && !existingID.equals(revision.getID()))
    {
      throw new IllegalStateException("Duplicate resource or folder: " + name + " in folder " + folderID);
    }
  }

  public void writeRevision(IDBStoreAccessor accessor, InternalCDORevision revision, OMMonitor monitor)
  {
    try
    {
      monitor.begin(10);

      if (revision.getVersion() > 1)
      {
        writeRevisedRow(accessor, revision.getID(), revision.getCreated() - 1);
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
      if (manyValueFeatureMappings != null)
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

  protected final void writeAttributes(IDBStoreAccessor accessor, InternalCDORevision revision)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlInsertAttributes);

      int col = 1;

      stmt.setLong(col++, CDOIDUtil.getLong(revision.getID()));
      stmt.setInt(col++, revision.getVersion());
      stmt.setLong(col++, accessor.getStore().getMetaDataManager().getMetaID(revision.getEClass()));
      stmt.setLong(col++, revision.getCreated());
      stmt.setLong(col++, revision.getRevised());
      stmt.setLong(col++, CDOIDUtil.getLong(revision.getResourceID()));
      stmt.setLong(col++, CDODBUtil.getLong((CDOID)revision.getContainerID()));
      stmt.setInt(col++, revision.getContainingFeatureID());

      for (ITypeMapping singleMapping : singleValueFeatureMappings)
      {
        singleMapping.setValueFromRevision(stmt, col++, revision);
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

  protected final void writeRevisedRow(IDBStoreAccessor accessor, CDOID id, long revised)
  {
    PreparedStatement stmt = null;

    try
    {
      stmt = accessor.getConnection().prepareStatement(sqlReviseAttributes);

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

  protected void writeReferences(IDBStoreAccessor accessor, InternalCDORevision revision)
  {
    for (IListMapping manyMapping : manyValueFeatureMappings)
    {
      manyMapping.writeValues(accessor, revision);
    }
  }

  public boolean readRevision(IDBStoreAccessor accessor, InternalCDORevision revision, int referenceChunk)
  {
    PreparedStatement pstmt = null;
    try
    {
      // TODO add caching
      pstmt = accessor.getConnection().prepareStatement(sqlSelectCurrentAttributes);
      pstmt.setLong(1, CDOIDUtil.getLong(revision.getID()));

      // Read singleval-attribute table always (even without modeled attributes!)
      boolean success = readSingleValuesFromStatement(pstmt, revision);

      // Read multival tables only if revision exists
      if (success)
      {
        readMultiValues(accessor, revision, referenceChunk);
      }

      return success;
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
    finally
    {
      DBUtil.close(pstmt);
    }
  }

  public boolean readRevisionByTime(IDBStoreAccessor accessor, InternalCDORevision revision, long timeStamp,
      int referenceChunk)
  {
    PreparedStatement pstmt = null;
    try
    {
      // TODO add caching
      pstmt = accessor.getConnection().prepareStatement(sqlSelectAttributesByTime);
      pstmt.setLong(1, CDOIDUtil.getLong(revision.getID()));
      pstmt.setLong(2, timeStamp);
      // Read singleval-attribute table always (even without modeled attributes!)
      boolean success = readSingleValuesFromStatement(pstmt, revision);

      // Read multival tables only if revision exists
      if (success)
      {
        readMultiValues(accessor, revision, referenceChunk);
      }

      return success;
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
    finally
    {
      DBUtil.close(pstmt);
    }
  }

  public boolean readRevisionByVersion(IDBStoreAccessor accessor, InternalCDORevision revision, int version,
      int referenceChunk)
  {
    PreparedStatement pstmt = null;
    try
    {
      // TODO add caching
      pstmt = accessor.getConnection().prepareStatement(sqlSelectAttributesByVersion);
      pstmt.setLong(1, CDOIDUtil.getLong(revision.getID()));
      pstmt.setInt(2, version);
      // Read singleval-attribute table always (even without modeled attributes!)
      boolean success = readSingleValuesFromStatement(pstmt, revision);

      // Read multival tables only if revision exists
      if (success)
      {
        readMultiValues(accessor, revision, referenceChunk);
      }

      return success;
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
    finally
    {
      DBUtil.close(pstmt);
    }
  }

  /**
   * Read the revision's attributes from the DB.
   * 
   * @return <code>true</code> if the revision has been read successfully.<br>
   *         <code>false</code> if the revision does not exist in the DB.
   */
  protected final boolean readSingleValuesFromStatement(PreparedStatement pstmt, InternalCDORevision revision)
  {
    ResultSet resultSet = null;
    try
    {
      resultSet = pstmt.executeQuery();
      if (!resultSet.next())
      {
        return false;
      }

      int i = 1;
      revision.setVersion(resultSet.getInt(i++));
      revision.setCreated(resultSet.getLong(i++));
      revision.setRevised(resultSet.getLong(i++));
      revision.setResourceID(CDOIDUtil.createLong(resultSet.getLong(i++)));

      // TODO add mapping for external container CDOIDs here ->
      revision.setContainerID(CDOIDUtil.createLong(resultSet.getLong(i++)));
      revision.setContainingFeatureID(resultSet.getInt(i++));

      for (ITypeMapping singleMapping : singleValueFeatureMappings)
      {
        singleMapping.readValueToRevision(resultSet, i++, revision);
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
    }
  }

  protected void readMultiValues(IDBStoreAccessor accessor, InternalCDORevision revision, int referenceChunk)
  {
    for (IListMapping manyMapping : manyValueFeatureMappings)
    {
      manyMapping.readValues(accessor, revision, referenceChunk);
    }
  }

  protected final IMetaDataManager getMetaDataManager()
  {
    return getMappingStrategy().getStore().getMetaDataManager();
  }

  protected final IMappingStrategy getMappingStrategy()
  {
    return mappingStrategy;
  }

  protected final List<ITypeMapping> getSingleValueFeatureMappings()
  {
    return singleValueFeatureMappings;
  }

  protected final ITypeMapping getSingleValueFeatureMapping(EStructuralFeature feature)
  {
    for (ITypeMapping mapping : singleValueFeatureMappings)
    {
      if (mapping.getFeature() == feature)
      {
        return mapping;
      }
    }
    return null;
  }

  protected final IDBTable getTable()
  {
    return table;
  }

  public Collection<IDBTable> getDBTables()
  {
    return Arrays.asList(table);
  }

  public PreparedStatement createResourceQueryStatement(IDBStoreAccessor accessor, CDOID folderId, String name,
      boolean exactMatch, long timeStamp)
  {
    EStructuralFeature nameFeature = EresourcePackage.eINSTANCE.getCDOResourceNode_Name();

    ITypeMapping nameValueMapping = getSingleValueFeatureMapping(nameFeature);
    if (nameValueMapping == null)
    {
      throw new ImplementationError(nameFeature + " not found in ClassMapping " + this);
    }

    StringBuilder builder = new StringBuilder();
    builder.append("SELECT ");
    builder.append(CDODBSchema.ATTRIBUTES_ID);
    builder.append(" FROM ");
    builder.append(getTable().getName());
    builder.append(" WHERE ");
    builder.append(CDODBSchema.ATTRIBUTES_CONTAINER);
    builder.append("= ? AND ");
    builder.append(nameValueMapping.getField().getName());
    builder.append(exactMatch ? " = ? AND (" : " LIKE ? AND (");
    builder.append(CDODBSchema.ATTRIBUTES_REVISED);

    if (timeStamp == CDORevision.UNSPECIFIED_DATE)
    {
      builder.append(" = 0 )");
    }
    else
    {
      builder.append(" = 0 OR ");
      builder.append(CDODBSchema.ATTRIBUTES_REVISED);
      builder.append(" >= ?)");
    }

    PreparedStatement pstmt = null;
    try
    {
      pstmt = accessor.getConnection().prepareStatement(builder.toString());
      pstmt.setLong(1, CDOIDUtil.getLong(folderId));
      nameValueMapping.setValue(pstmt, 2, name);
      if (timeStamp != CDORevision.UNSPECIFIED_DATE)
      {
        pstmt.setLong(3, timeStamp);
      }

      return pstmt;
    }
    catch (SQLException ex)
    {
      DBUtil.close(pstmt); // only close on error
      throw new DBException(ex);
    }
  }

  public PreparedStatement createObjectIdStatement(IDBStoreAccessor accessor)
  {
    try
    {
      return accessor.getConnection().prepareStatement(sqlSelectAllObjectIds);
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
  }
}
