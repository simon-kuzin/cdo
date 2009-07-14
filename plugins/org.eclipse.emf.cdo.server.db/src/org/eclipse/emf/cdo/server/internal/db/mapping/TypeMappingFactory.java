/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Winkler - initial API and implementation
 *    Eike Stepper - maintenance
 */
package org.eclipse.emf.cdo.server.internal.db.mapping;

import org.eclipse.emf.cdo.common.model.CDOModelUtil;
import org.eclipse.emf.cdo.common.model.CDOType;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.emf.cdo.server.db.mapping.ITypeMapping;
import org.eclipse.emf.cdo.server.internal.db.DBAnnotation;

import org.eclipse.net4j.db.DBType;
import org.eclipse.net4j.util.collection.Pair;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Stefan Winkler
 */
public enum TypeMappingFactory
{
  BOOLEAN_MAPPING
  {
    @Override
    public ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      return new TypeMapping.TMBoolean(mappingStrategy, feature, type);
    }
  },

  BYTE_MAPPING
  {
    @Override
    public ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      return new TypeMapping.TMByte(mappingStrategy, feature, type);
    }
  },

  CHARACTER_MAPPING
  {
    @Override
    public ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      return new TypeMapping.TMCharacter(mappingStrategy, feature, type);
    }
  },

  DATE_MAPPING
  {
    @Override
    public ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      return new TypeMapping.TMDate(mappingStrategy, feature, type);
    }
  },

  DOUBLE_MAPPING
  {
    @Override
    public ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      return new TypeMapping.TMDouble(mappingStrategy, feature, type);
    }
  },

  FLOAT_MAPPING
  {
    @Override
    public ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      return new TypeMapping.TMFloat(mappingStrategy, feature, type);
    }
  },

  INT_MAPPING
  {
    @Override
    public ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      return new TypeMapping.TMInteger(mappingStrategy, feature, type);
    }
  },

  LONG_MAPPING
  {
    @Override
    public ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      return new TypeMapping.TMLong(mappingStrategy, feature, type);
    }
  },

  OBJECT_MAPPING
  {
    @Override
    public ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      return new TypeMapping.TMObject(mappingStrategy, feature, type);
    }
  },

  SHORT_MAPPING
  {
    @Override
    public ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      return new TypeMapping.TMShort(mappingStrategy, feature, type);
    }
  },

  ENUM_MAPPING
  {
    @Override
    public ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      return new TypeMapping.TMEnum(mappingStrategy, feature, type);
    }
  },

  STRING_MAPPING
  {
    @Override
    public ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      return new TypeMapping.TMString(mappingStrategy, feature, type);
    }
  },

  BIG_INT_MAPPING
  {
    @Override
    public ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      return new TypeMapping.TMBigInteger(mappingStrategy, feature, type);
    }
  },

  BIG_DECIMAL_MAPPING
  {
    @Override
    public ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      return new TypeMapping.TMBigDecimal(mappingStrategy, feature, type);
    }
  },

  BYTES_MAPPING
  {
    @Override
    public ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      return new TypeMapping.TMBytes(mappingStrategy, feature, type);
    }
  };

  private static Map<EClassifier, DBType> defaultTypeMap = new HashMap<EClassifier, DBType>();

  private static Map<Pair<CDOType, DBType>, TypeMappingFactory> mappingTable = new HashMap<Pair<CDOType, DBType>, TypeMappingFactory>();

  static
  {
    /* --- initialize default types --- */
    defaultTypeMap.put(EcorePackage.eINSTANCE.getEDate(), DBType.TIMESTAMP);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getEString(), DBType.VARCHAR);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getEByteArray(), DBType.BLOB);

    defaultTypeMap.put(EcorePackage.eINSTANCE.getEBoolean(), DBType.BOOLEAN);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getEByte(), DBType.SMALLINT);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getEChar(), DBType.CHAR);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getEDouble(), DBType.DOUBLE);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getEFloat(), DBType.FLOAT);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getEInt(), DBType.INTEGER);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getELong(), DBType.BIGINT);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getEShort(), DBType.SMALLINT);

    defaultTypeMap.put(EcorePackage.eINSTANCE.getEBooleanObject(), DBType.BOOLEAN);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getEByteObject(), DBType.SMALLINT);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getECharacterObject(), DBType.CHAR);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getEDoubleObject(), DBType.DOUBLE);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getEFloatObject(), DBType.FLOAT);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getEIntegerObject(), DBType.INTEGER);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getELongObject(), DBType.BIGINT);
    defaultTypeMap.put(EcorePackage.eINSTANCE.getEShortObject(), DBType.SMALLINT);

    /* --- register type mappings --- */
    mappingTable.put(new Pair<CDOType, DBType>(CDOType.BIG_INTEGER, DBType.VARCHAR), BIG_INT_MAPPING);
    mappingTable.put(new Pair<CDOType, DBType>(CDOType.BIG_DECIMAL, DBType.VARCHAR), BIG_DECIMAL_MAPPING);

    mappingTable.put(new Pair<CDOType, DBType>(CDOType.BOOLEAN, DBType.BOOLEAN), BOOLEAN_MAPPING);
    mappingTable.put(new Pair<CDOType, DBType>(CDOType.BOOLEAN_OBJECT, DBType.BOOLEAN), BOOLEAN_MAPPING);

    mappingTable.put(new Pair<CDOType, DBType>(CDOType.BYTE, DBType.SMALLINT), BYTE_MAPPING);
    mappingTable.put(new Pair<CDOType, DBType>(CDOType.BYTE_OBJECT, DBType.SMALLINT), BYTE_MAPPING);

    mappingTable.put(new Pair<CDOType, DBType>(CDOType.BYTE_ARRAY, DBType.BLOB), BYTES_MAPPING);

    mappingTable.put(new Pair<CDOType, DBType>(CDOType.CHAR, DBType.CHAR), CHARACTER_MAPPING);
    mappingTable.put(new Pair<CDOType, DBType>(CDOType.CHARACTER_OBJECT, DBType.CHAR), CHARACTER_MAPPING);

    mappingTable.put(new Pair<CDOType, DBType>(CDOType.DATE, DBType.TIMESTAMP), DATE_MAPPING);

    mappingTable.put(new Pair<CDOType, DBType>(CDOType.DOUBLE, DBType.DOUBLE), DOUBLE_MAPPING);
    mappingTable.put(new Pair<CDOType, DBType>(CDOType.DOUBLE_OBJECT, DBType.DOUBLE), DOUBLE_MAPPING);

    mappingTable.put(new Pair<CDOType, DBType>(CDOType.ENUM, DBType.INTEGER), ENUM_MAPPING);

    mappingTable.put(new Pair<CDOType, DBType>(CDOType.FLOAT, DBType.FLOAT), FLOAT_MAPPING);
    mappingTable.put(new Pair<CDOType, DBType>(CDOType.FLOAT_OBJECT, DBType.FLOAT), FLOAT_MAPPING);

    mappingTable.put(new Pair<CDOType, DBType>(CDOType.INT, DBType.INTEGER), INT_MAPPING);
    mappingTable.put(new Pair<CDOType, DBType>(CDOType.INTEGER_OBJECT, DBType.INTEGER), INT_MAPPING);

    mappingTable.put(new Pair<CDOType, DBType>(CDOType.LONG, DBType.BIGINT), LONG_MAPPING);
    mappingTable.put(new Pair<CDOType, DBType>(CDOType.LONG_OBJECT, DBType.BIGINT), LONG_MAPPING);

    mappingTable.put(new Pair<CDOType, DBType>(CDOType.OBJECT, DBType.BIGINT), OBJECT_MAPPING);

    mappingTable.put(new Pair<CDOType, DBType>(CDOType.SHORT, DBType.SMALLINT), SHORT_MAPPING);
    mappingTable.put(new Pair<CDOType, DBType>(CDOType.SHORT_OBJECT, DBType.SMALLINT), SHORT_MAPPING);

    mappingTable.put(new Pair<CDOType, DBType>(CDOType.STRING, DBType.VARCHAR), STRING_MAPPING);
    mappingTable.put(new Pair<CDOType, DBType>(CDOType.STRING, DBType.CLOB), STRING_MAPPING);

    // preliminary
    mappingTable.put(new Pair<CDOType, DBType>(CDOType.FEATURE_MAP_ENTRY, DBType.VARCHAR), STRING_MAPPING);
  }

  protected abstract ITypeMapping doCreateTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature,
      DBType type);

  public static ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature)
  {
    CDOType cdoType = CDOModelUtil.getType(feature);
    DBType dbType = getDBType(feature);

    TypeMappingFactory concreteFactory = mappingTable.get(new Pair<CDOType, DBType>(cdoType, dbType));
    if (concreteFactory == null)
    {
      throw new IllegalArgumentException("No suitable mapping found from EMF type " + cdoType.getName()
          + " to DB type " + dbType.getClass().getSimpleName());
    }

    return concreteFactory.doCreateTypeMapping(mappingStrategy, feature, dbType);
  }

  private static DBType getDBType(EStructuralFeature feature)
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

    // no annotation present - lookup default DB type.
    return getDefaultDBType(feature.getEType());
  }

  private static DBType getDefaultDBType(EClassifier type)
  {
    if (type instanceof EClass)
    {
      return DBType.BIGINT;
    }

    if (type instanceof EEnum)
    {
      return DBType.INTEGER;
    }

    DBType dbType = defaultTypeMap.get(type);
    if (dbType != null)
    {
      return dbType;
    }

    return DBType.VARCHAR;
  }
}
