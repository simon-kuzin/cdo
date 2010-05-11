/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.server.internal.db.mapping;

import org.eclipse.emf.cdo.server.db.ITypeMappingRegistry;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.emf.cdo.server.db.mapping.ITypeMapping;
import org.eclipse.emf.cdo.server.db.mapping.ITypeMappingFactory;
import org.eclipse.emf.cdo.server.internal.db.DBAnnotation;
import org.eclipse.emf.cdo.server.internal.db.bundle.OM;

import org.eclipse.net4j.db.DBType;
import org.eclipse.net4j.db.IDBAdapter;
import org.eclipse.net4j.util.WrappedException;
import org.eclipse.net4j.util.collection.Pair;
import org.eclipse.net4j.util.lifecycle.Lifecycle;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Eike Stepper
 */
public class TypeMappingRegistry extends Lifecycle implements ITypeMappingRegistry
{

  public static final String EXT_POINT_CUSTOM_TYPE_MAPPINGS = "typeMappings"; //$NON-NLS-1$

  @Override
  protected void doActivate() throws Exception
  {
    super.doActivate();

    defaultFeatureMapDBTypes = new HashSet<DBType>();
    typeMappings = new HashMap<String, ITypeMappingFactory>();
    typeMappingByClassifier = new HashMap<Pair<EClassifier, DBType>, String>();
    classifierDefaultMapping = new HashMap<EClassifier, DBType>();

    registerCoreMappings();
    registerCustomMappings();
  }

  private void registerCoreMappings()
  {
    registerTypeMapping(CoreTypeMappings.TMEnum.ID, EcorePackage.eINSTANCE.getEEnum(), DBType.INTEGER,
        new CoreTypeMappings.TMEnum.Factory());

    registerTypeMapping(CoreTypeMappings.TMDate2Timestamp.ID, EcorePackage.eINSTANCE.getEDate(), DBType.TIMESTAMP,
        new CoreTypeMappings.TMDate2Timestamp.Factory());

    registerTypeMapping(CoreTypeMappings.TMDate2Date.ID, EcorePackage.eINSTANCE.getEDate(), DBType.DATE,
        new CoreTypeMappings.TMDate2Date.Factory());

    registerTypeMapping(CoreTypeMappings.TMDate2Time.ID, EcorePackage.eINSTANCE.getEDate(), DBType.TIME,
        new CoreTypeMappings.TMDate2Time.Factory());

    registerTypeMapping(CoreTypeMappings.TMString.ID, EcorePackage.eINSTANCE.getEString(), DBType.VARCHAR,
        new CoreTypeMappings.TMString.Factory());

    registerTypeMapping(CoreTypeMappings.TMString.ID, EcorePackage.eINSTANCE.getEString(), DBType.CLOB,
        new CoreTypeMappings.TMString.Factory());

    registerTypeMapping(CoreTypeMappings.TMBytes.ID, EcorePackage.eINSTANCE.getEByteArray(), DBType.BLOB,
        new CoreTypeMappings.TMBytes.Factory());

    registerTypeMapping(CoreTypeMappings.TMBoolean.ID, EcorePackage.eINSTANCE.getEBoolean(), DBType.BOOLEAN,
        new CoreTypeMappings.TMBoolean.Factory());

    registerTypeMapping(CoreTypeMappings.TMBoolean.ID, EcorePackage.eINSTANCE.getEBooleanObject(), DBType.BOOLEAN,
        new CoreTypeMappings.TMBoolean.Factory());

    registerTypeMapping(CoreTypeMappings.TMByte.ID, EcorePackage.eINSTANCE.getEByte(), DBType.SMALLINT,
        new CoreTypeMappings.TMByte.Factory());

    registerTypeMapping(CoreTypeMappings.TMByte.ID, EcorePackage.eINSTANCE.getEByteObject(), DBType.SMALLINT,
        new CoreTypeMappings.TMByte.Factory());

    registerTypeMapping(CoreTypeMappings.TMCharacter.ID, EcorePackage.eINSTANCE.getEChar(), DBType.CHAR,
        new CoreTypeMappings.TMCharacter.Factory());

    registerTypeMapping(CoreTypeMappings.TMCharacter.ID, EcorePackage.eINSTANCE.getECharacterObject(), DBType.CHAR,
        new CoreTypeMappings.TMCharacter.Factory());

    registerTypeMapping(CoreTypeMappings.TMInteger.ID, EcorePackage.eINSTANCE.getEInt(), DBType.INTEGER,
        new CoreTypeMappings.TMInteger.Factory());

    registerTypeMapping(CoreTypeMappings.TMInteger.ID, EcorePackage.eINSTANCE.getEIntegerObject(), DBType.INTEGER,
        new CoreTypeMappings.TMInteger.Factory());

    registerTypeMapping(CoreTypeMappings.TMLong.ID, EcorePackage.eINSTANCE.getELong(), DBType.BIGINT,
        new CoreTypeMappings.TMLong.Factory());

    registerTypeMapping(CoreTypeMappings.TMLong.ID, EcorePackage.eINSTANCE.getELongObject(), DBType.BIGINT,
        new CoreTypeMappings.TMLong.Factory());

    registerTypeMapping(CoreTypeMappings.TMShort.ID, EcorePackage.eINSTANCE.getEShort(), DBType.SMALLINT,
        new CoreTypeMappings.TMShort.Factory());

    registerTypeMapping(CoreTypeMappings.TMShort.ID, EcorePackage.eINSTANCE.getEShortObject(), DBType.SMALLINT,
        new CoreTypeMappings.TMShort.Factory());

    registerTypeMapping(CoreTypeMappings.TMDouble.ID, EcorePackage.eINSTANCE.getEDouble(), DBType.DOUBLE,
        new CoreTypeMappings.TMDouble.Factory());

    registerTypeMapping(CoreTypeMappings.TMDouble.ID, EcorePackage.eINSTANCE.getEDoubleObject(), DBType.DOUBLE,
        new CoreTypeMappings.TMDouble.Factory());

    registerTypeMapping(CoreTypeMappings.TMFloat.ID, EcorePackage.eINSTANCE.getEFloat(), DBType.FLOAT,
        new CoreTypeMappings.TMFloat.Factory());

    registerTypeMapping(CoreTypeMappings.TMFloat.ID, EcorePackage.eINSTANCE.getEFloatObject(), DBType.FLOAT,
        new CoreTypeMappings.TMFloat.Factory());

    registerTypeMapping(CoreTypeMappings.TMBigDecimal.ID, EcorePackage.eINSTANCE.getEBigDecimal(), DBType.VARCHAR,
        new CoreTypeMappings.TMBigDecimal.Factory());

    registerTypeMapping(CoreTypeMappings.TMBigInteger.ID, EcorePackage.eINSTANCE.getEBigInteger(), DBType.VARCHAR,
        new CoreTypeMappings.TMBigInteger.Factory());

    registerTypeMapping(CoreTypeMappings.TMObject.ID, EcorePackage.eINSTANCE.getEClass(), DBType.BIGINT,
        new CoreTypeMappings.TMObject.Factory());

    registerTypeMapping(CoreTypeMappings.TMCustom.ID, EcorePackage.eINSTANCE.getEDataType(), DBType.VARCHAR,
        new CoreTypeMappings.TMCustom.Factory());

    registerTypeMapping(CoreTypeMappings.TMCustom.ID, EcorePackage.eINSTANCE.getEDataType(), DBType.CLOB,
        new CoreTypeMappings.TMCustom.Factory());
  }

  private void registerCustomMappings()
  {
    IExtensionRegistry registry = Platform.getExtensionRegistry();

    if (registry != null)
    {

      IConfigurationElement[] elements = registry.getConfigurationElementsFor(OM.BUNDLE_ID,
          EXT_POINT_CUSTOM_TYPE_MAPPINGS);
      for (final IConfigurationElement element : elements)
      {
        if ("typeMapping".equals(element.getName())) //$NON-NLS-1$
        {
          String id = element.getAttribute("id"); //$NON-NLS-1$
          String packageUri = element.getAttribute("packageURI"); //$NON-NLS-1$
          String dataTypeName = element.getAttribute("dataTypeName"); //$NON-NLS-1$
          String dbTypeKeyword = element.getAttribute("dbType"); //$NON-NLS-1$
          EPackage ePackage = null;
          EClassifier eClassifier = null;
          DBType dbType = null;

          // XXX Error handling and reporting!
          if ((ePackage = Registry.INSTANCE.getEPackage(packageUri)) != null
              && (eClassifier = ePackage.getEClassifier(dataTypeName)) != null && eClassifier instanceof EDataType
              && (dbType = DBType.getTypeByKeyword(dbTypeKeyword)) != null)
          {
            try
            {
              ITypeMappingFactory typeMappingFactory = (ITypeMappingFactory)element.createExecutableExtension("class"); //$NON-NLS-1$
              registerTypeMapping(id, eClassifier, dbType, typeMappingFactory);
            }
            catch (CoreException ex)
            {
              throw WrappedException.wrap(ex);
            }
          }
        }
      }
    }
  }

  private Set<DBType> defaultFeatureMapDBTypes;

  private Map<EClassifier, DBType> classifierDefaultMapping;

  private Map<Pair<EClassifier, DBType>, String> typeMappingByClassifier;

  private Map<String, ITypeMappingFactory> typeMappings;

  public void registerTypeMapping(String id, EClassifier eClassifier, DBType dbType, ITypeMappingFactory mappingFactory)
  {
    typeMappings.put(id, mappingFactory);
    typeMappingByClassifier.put(new Pair<EClassifier, DBType>(eClassifier, dbType), id);

    // register first dbType for classifier as default
    if (!classifierDefaultMapping.containsKey(eClassifier))
    {
      classifierDefaultMapping.put(eClassifier, dbType);
    }

    defaultFeatureMapDBTypes.add(dbType);
  }

  public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature)
  {
    EClassifier classifier = getEClassifier(feature);
    DBType dbType = getDBType(feature, mappingStrategy.getStore().getDBAdapter());

    String typeMappingID = DBAnnotation.TYPE_MAPPING.getValue(feature);

    ITypeMappingFactory factory = null;

    if (typeMappingID != null)
    {
      // lookup annotated mapping
      factory = typeMappings.get(typeMappingID);
    }

    if (factory == null)
    {
      // try to find suitable mapping by type
      factory = getMappingByType(feature, dbType);
    }

    if (factory == null)
    {
      // XXX i18n
      throw new IllegalStateException("No typeMapping factory found for feature " + feature + " (type "
          + classifier.getName() + ", DB type " + dbType.getClass().getSimpleName() + ")");
    }

    return factory.createTypeMapping(mappingStrategy, feature, dbType);
  }

  private EClassifier getEClassifier(EStructuralFeature feature)
  {
    EClassifier classifier = feature.getEType();
    if (classifier instanceof EEnum)
    {
      return EcorePackage.eINSTANCE.getEEnum();
    }

    if (classifier instanceof EClass)
    {
      return EcorePackage.eINSTANCE.getEClass();
    }

    return classifier;
  }

  private DBType getDBType(EStructuralFeature feature, IDBAdapter dbAdapter)
  {
    String typeKeyword = DBAnnotation.COLUMN_TYPE.getValue(feature);
    if (typeKeyword != null)
    {
      DBType dbType = DBType.getTypeByKeyword(typeKeyword);
      if (dbType == null)
      {
        throw new IllegalArgumentException("Unsupported columnType (" + typeKeyword + ") annotation of feature "
            + feature.getName());
      }
      return dbType;
    }

    // No annotation present - lookup default DB type.
    return getDefaultDBType(getEClassifier(feature), dbAdapter);
  }

  private DBType getDefaultDBType(EClassifier type, IDBAdapter dbAdapter)
  {
    DBType result = classifierDefaultMapping.get(type);

    if (result == null)
    {
      result = DBType.VARCHAR;
    }

    // Give the DBAdapter a chance to override the default type, if it's not supported
    return dbAdapter.adaptType(result);
  }

  private ITypeMappingFactory getMappingByType(EStructuralFeature feature, DBType dbType)
  {
    // First try: lookup specific mapping for the immediate type.
    String factoryId = typeMappingByClassifier.get(new Pair<EClassifier, DBType>(feature.getEType(), dbType));

    if (factoryId == null)
    {
      // Second try: lookup general mapping
      factoryId = typeMappingByClassifier.get(new Pair<EClassifier, DBType>(getEClassifier(feature), dbType));

      if (factoryId == null)
      {
        // Lookup failed. Give up
        return null;
      }
    }

    return typeMappings.get(factoryId);
  }

  public Collection<DBType> getDefaultFeatureMapDBTypes()
  {
    return defaultFeatureMapDBTypes;
  }
}
