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
package org.eclipse.emf.cdo.server.db.mapping;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.model.CDOClassifierRef;
import org.eclipse.emf.cdo.server.IStoreAccessor.QueryResourcesContext;
import org.eclipse.emf.cdo.server.db.IDBStore;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;

import org.eclipse.net4j.db.IDBAdapter;
import org.eclipse.net4j.util.collection.CloseableIterator;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.sql.Connection;
import java.util.Map;

/**
 * @author Eike Stepper
 * @author Stefan Winkler
 * @since 2.0
 */
public interface IMappingStrategy
{
  /**
   * Name of the integer property that configures the maximum length for table names. A value of zero indicates the
   * value of the {@link IDBAdapter#getMaxTableNameLength() db adapter} to be used.
   */
  public static final String PROP_MAX_TABLE_NAME_LENGTH = "maxTableNameLength";

  /**
   * Name of the integer property that configures the maximum length for column names. A value of zero indicates the
   * value of the {@link IDBAdapter#getMaxFieldNameLength() db adapter} to be used.
   */
  public static final String PROP_MAX_FIELD_NAME_LENGTH = "maxFieldNameLength";

  /**
   * Name of the String property that specifies a common prefix for table names.
   */
  public static final String PROP_TABLE_NAME_PREFIX = "tableNamePrefix";

  /**
   * Name of the boolean property that configures whether the table names are made of simple class names or of qualified
   * class names.
   */
  public static final String PROP_QUALIFIED_NAMES = "qualifiedNames";

  /**
   * Name of the boolean property that configures whether table names and column names are always suffixed with the
   * internal DBID or only in cases where generated names violate the naming constraints of the underlying backend.
   */
  public static final String PROP_FORCE_NAMES_WITH_ID = "forceNamesWithID";

  public static final String PROP_TO_MANY_REFERENCE_MAPPING = "toManyReferenceMapping";

  public static final String PROP_TO_ONE_REFERENCE_MAPPING = "toOneReferenceMapping";

  public IDBStore getStore();

  public void setStore(IDBStore dbStore);

  public ITypeMapping createValueMapping(EStructuralFeature feature);

  public IListMapping createListMapping(EClass containingClass, EStructuralFeature feature);

  public String getTableName(ENamedElement eClass);

  public String getTableName(EClass containingClass, EStructuralFeature feature);

  public String getFieldName(EStructuralFeature feature);

  public void createMapping(Connection connection, InternalCDOPackageUnit[] packageUnits, OMMonitor monitor);

  public IClassMapping getClassMapping(EClass eClass);

  public IListMapping getListMapping(EStructuralFeature feature);

  public boolean hasDeltaSupport();

  public void queryResources(IDBStoreAccessor dbStoreAccessor, QueryResourcesContext context);

  public boolean hasAuditSupport();

  public CDOClassifierRef readObjectType(IDBStoreAccessor dbStoreAccessor, CDOID id);

  public CloseableIterator<CDOID> readObjectIDs(IDBStoreAccessor dbStoreAccessor);

  public long repairAfterCrash(IDBAdapter dbAdapter, Connection connection);

  public void setProperties(Map<String, String> properties);

}
