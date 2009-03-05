/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.server.internal.db;

import org.eclipse.net4j.db.DBType;
import org.eclipse.net4j.db.ddl.IDBField;
import org.eclipse.net4j.db.ddl.IDBIndex;
import org.eclipse.net4j.db.ddl.IDBTable;
import org.eclipse.net4j.spi.db.DBSchema;

/**
 * @author Eike Stepper
 */
public class CDODBSchema extends DBSchema
{
  public static final CDODBSchema INSTANCE = new CDODBSchema();

  /**
   * DBTable cdo_repository
   */
  public static final IDBTable REPOSITORY = INSTANCE.addTable("cdo_repository");

  public static final IDBField REPOSITORY_CREATED = //
  REPOSITORY.addField("created", DBType.BIGINT);

  public static final IDBField REPOSITORY_STARTS = //
  REPOSITORY.addField("starts", DBType.BIGINT);

  public static final IDBField REPOSITORY_STARTED = //
  REPOSITORY.addField("started", DBType.BIGINT);

  public static final IDBField REPOSITORY_STOPPED = //
  REPOSITORY.addField("stopped", DBType.BIGINT);

  public static final IDBField REPOSITORY_NEXT_CDOID = //
  REPOSITORY.addField("next_cdoid", DBType.BIGINT);

  public static final IDBField REPOSITORY_NEXT_METAID = //
  REPOSITORY.addField("next_metaid", DBType.BIGINT);

  /**
   * DBTable cdo_package_units
   */
  public static final IDBTable PACKAGE_UNITS = INSTANCE.addTable("cdo_package_units");

  public static final IDBField PACKAGE_UNITS_ID = //
  PACKAGE_UNITS.addField("id", DBType.VARCHAR, 255);

  public static final IDBField PACKAGE_UNITS_ORIGINAL_TYPE = //
  PACKAGE_UNITS.addField("original_type", DBType.INTEGER);

  public static final IDBField PACKAGE_UNITS_TIME_STAMP = //
  PACKAGE_UNITS.addField("time_stamp", DBType.BIGINT);

  public static final IDBField PACKAGE_UNITS_PACKAGE_DATA = //
  PACKAGE_UNITS.addField("package_data", DBType.BLOB);

  public static final IDBIndex INDEX_PACKAGE_UNITS_PK = //
  PACKAGE_UNITS.addIndex(IDBIndex.Type.PRIMARY_KEY, PACKAGE_UNITS_ID);

  /**
   * DBTable cdo_packages
   */
  public static final IDBTable PACKAGE_INFOS = INSTANCE.addTable("cdo_package_infos");

  public static final IDBField PACKAGE_INFOS_ID = //
  PACKAGE_INFOS.addField("id", DBType.INTEGER);

  public static final IDBField PACKAGE_INFOS_URI = //
  PACKAGE_INFOS.addField("uri", DBType.VARCHAR, 255);

  public static final IDBField PACKAGE_INFOS_PARENT = //
  PACKAGE_INFOS.addField("parent", DBType.VARCHAR, 255);

  public static final IDBField PACKAGE_INFOS_UNIT = //
  PACKAGE_INFOS.addField("unit", DBType.VARCHAR, 255);

  public static final IDBField PACKAGE_INFOS_META_LB = //
  PACKAGE_INFOS.addField("meta_lb", DBType.BIGINT);

  public static final IDBField PACKAGE_INFOS_META_UB = //
  PACKAGE_INFOS.addField("meta_ub", DBType.BIGINT);

  public static final IDBField PACKAGE_INFOS_CLASSIFIER_LB = //
  PACKAGE_INFOS.addField("classifier_lb", DBType.INTEGER);

  public static final IDBField PACKAGE_INFOS_CLASSIFIER_UB = //
  PACKAGE_INFOS.addField("classifier_ub", DBType.INTEGER);

  public static final IDBField PACKAGE_INFOS_FEATURE_LB = //
  PACKAGE_INFOS.addField("feature_lb", DBType.INTEGER);

  public static final IDBField PACKAGE_INFOS_FEATURE_UB = //
  PACKAGE_INFOS.addField("feature_ub", DBType.INTEGER);

  public static final IDBIndex INDEX_PACKAGE_INFOS_PK = //
  PACKAGE_INFOS.addIndex(IDBIndex.Type.PRIMARY_KEY, PACKAGE_INFOS_ID);

  public static final IDBIndex INDEX_PACKAGE_INFOS_URI = //
  PACKAGE_INFOS.addIndex(IDBIndex.Type.UNIQUE, PACKAGE_INFOS_URI);

  public static final IDBIndex INDEX_PACKAGE_INFOS_PARENT = //
  PACKAGE_INFOS.addIndex(IDBIndex.Type.NON_UNIQUE, PACKAGE_INFOS_PARENT);

  public static final IDBIndex INDEX_PACKAGE_INFOS_UNIT = //
  PACKAGE_INFOS.addIndex(IDBIndex.Type.NON_UNIQUE, PACKAGE_INFOS_UNIT);

  // /**
  // * DBTable cdo_classes
  // */
  // public static final IDBTable CLASSES = INSTANCE.addTable("cdo_classes");
  //
  // public static final IDBField CLASSES_ID = //
  // CLASSES.addField("id", DBType.INTEGER);
  //
  // public static final IDBField CLASSES_PACKAGE = //
  // CLASSES.addField("package", DBType.INTEGER);
  //
  // public static final IDBField CLASSES_CLASSIFIER = //
  // CLASSES.addField("classifier", DBType.INTEGER);
  //
  // public static final IDBField CLASSES_NAME = //
  // CLASSES.addField("name", DBType.VARCHAR, 255);
  //
  // public static final IDBField CLASSES_ABSTRACT = //
  // CLASSES.addField("abstract", DBType.BOOLEAN);
  //
  // public static final IDBIndex INDEX_CLASSES_PK = //
  // CLASSES.addIndex(IDBIndex.Type.PRIMARY_KEY, CLASSES_ID);
  //
  // public static final IDBIndex INDEX_CLASSES_PACKAGE = //
  // CLASSES.addIndex(IDBIndex.Type.NON_UNIQUE, CLASSES_PACKAGE);
  //
  // /**
  // * DBTable cdo_supertypes
  // */
  // public static final IDBTable SUPERTYPES = INSTANCE.addTable("cdo_supertypes");
  //
  // public static final IDBField SUPERTYPES_TYPE = //
  // SUPERTYPES.addField("type_id", DBType.INTEGER);
  //
  // public static final IDBField SUPERTYPES_SUPERTYPE_PACKAGE = //
  // SUPERTYPES.addField("supertype_package", DBType.VARCHAR, 255);
  //
  // public static final IDBField SUPERTYPES_SUPERTYPE_CLASSIFIER = //
  // SUPERTYPES.addField("supertype_classifier", DBType.INTEGER);
  //
  // public static final IDBIndex INDEX_SUPERTYPES_PK = //
  // SUPERTYPES.addIndex(IDBIndex.Type.PRIMARY_KEY, SUPERTYPES_TYPE);
  //
  // /**
  // * DBTable cdo_features
  // */
  // public static final IDBTable FEATURES = INSTANCE.addTable("cdo_features");
  //
  // public static final IDBField FEATURES_ID = //
  // FEATURES.addField("id", DBType.INTEGER);
  //
  // public static final IDBField FEATURES_CLASS = //
  // FEATURES.addField("class", DBType.INTEGER);
  //
  // public static final IDBField FEATURES_FEATURE = //
  // FEATURES.addField("feature", DBType.INTEGER);
  //
  // public static final IDBField FEATURES_NAME = //
  // FEATURES.addField("name", DBType.VARCHAR, 255);
  //
  // public static final IDBField FEATURES_TYPE = //
  // FEATURES.addField("type", DBType.INTEGER);
  //
  // public static final IDBField FEATURES_REFERENCE_PACKAGE = //
  // FEATURES.addField("reference_package", DBType.VARCHAR, 255);
  //
  // public static final IDBField FEATURES_REFERENCE_CLASSIFIER = //
  // FEATURES.addField("reference_classifier", DBType.INTEGER);
  //
  // public static final IDBField FEATURES_MANY = //
  // FEATURES.addField("many", DBType.BOOLEAN);
  //
  // public static final IDBField FEATURES_CONTAINMENT = //
  // FEATURES.addField("containment", DBType.BOOLEAN);
  //
  // public static final IDBField FEATURES_INDEX = //
  // FEATURES.addField("idx", DBType.INTEGER);
  //
  // public static final IDBIndex INDEX_FEATURES_PK = //
  // FEATURES.addIndex(IDBIndex.Type.PRIMARY_KEY, FEATURES_ID);
  //
  // public static final IDBIndex INDEX_FEATURES_CLASS = //
  // FEATURES.addIndex(IDBIndex.Type.NON_UNIQUE, FEATURES_CLASS);

  /**
   * Name of object table
   */
  public static final String CDO_OBJECTS = "cdo_objects";

  /**
   * Field names of attribute tables
   */
  public static final String ATTRIBUTES_ID = "cdo_id";

  public static final String ATTRIBUTES_VERSION = "cdo_version";

  public static final String ATTRIBUTES_CLASS = "cdo_class";

  public static final String ATTRIBUTES_CREATED = "cdo_created";

  public static final String ATTRIBUTES_REVISED = "cdo_revised";

  public static final String ATTRIBUTES_RESOURCE = "cdo_resource";

  public static final String ATTRIBUTES_CONTAINER = "cdo_container";

  public static final String ATTRIBUTES_FEATURE = "cdo_feature";

  /**
   * Field names of reference tables
   */
  public static final String REFERENCES_FEATURE = "cdo_feature";

  public static final String REFERENCES_SOURCE = "cdo_source";

  public static final String REFERENCES_VERSION = "cdo_version";

  public static final String REFERENCES_IDX = "cdo_idx";

  public static final String REFERENCES_TARGET = "cdo_target";

  private CDODBSchema()
  {
    super("CDO");
  }

  static
  {
    INSTANCE.lock();
  }
}
