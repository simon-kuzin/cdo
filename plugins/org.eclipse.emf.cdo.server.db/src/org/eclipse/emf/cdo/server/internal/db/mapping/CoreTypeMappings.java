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
package org.eclipse.emf.cdo.server.internal.db.mapping;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevisionData;
import org.eclipse.emf.cdo.server.IStoreAccessor;
import org.eclipse.emf.cdo.server.IStoreAccessor.CommitContext;
import org.eclipse.emf.cdo.server.StoreThreadLocal;
import org.eclipse.emf.cdo.server.db.CDODBUtil;
import org.eclipse.emf.cdo.server.db.IDBStore;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.server.db.IExternalReferenceManager;
import org.eclipse.emf.cdo.server.db.mapping.AbstractTypeMapping;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.emf.cdo.server.db.mapping.ITypeMapping;
import org.eclipse.emf.cdo.server.db.mapping.ITypeMappingFactory;

import org.eclipse.net4j.db.DBType;

import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 * This is a default implementation for the {@link ITypeMapping} interface which provides default behavor for all common
 * types.
 * 
 * @author Eike Stepper
 */
public class CoreTypeMappings
{
  public static final String ID_PREFIX = "org.eclipse.emf.cdo.db.CoreTypeMappings.";

  /**
   * @author Eike Stepper
   */
  public static class TMEnum extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "Enum";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMEnum(mappingStrategy, feature, dbType);
      }
    }

    public TMEnum(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    public Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      // see Bug 271941
      return resultSet.getInt(getField().getName());
      // EEnum type = (EEnum)getFeature().getEType();
      // int value = resultSet.getInt(column);
      // return type.getEEnumLiteral(value);
    }

    @Override
    protected Object getDefaultValue()
    {
      EEnum eenum = (EEnum)getFeature().getEType();

      String defaultValueLiteral = getFeature().getDefaultValueLiteral();
      if (defaultValueLiteral != null)
      {
        EEnumLiteral literal = eenum.getEEnumLiteralByLiteral(defaultValueLiteral);
        return literal.getValue();
      }

      Enumerator enumerator = (Enumerator)eenum.getDefaultValue();
      return enumerator.getValue();
    }
  }

  /**
   * @author Eike Stepper
   */
  public static class TMString extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "String";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMString(mappingStrategy, feature, dbType);
      }
    }

    public TMString(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    public Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      return resultSet.getString(getField().getName());
    }

  }

  /**
   * @author Eike Stepper
   */
  public static class TMShort extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "Short";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMShort(mappingStrategy, feature, dbType);
      }
    }

    public TMShort(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    public Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      return resultSet.getShort(getField().getName());
    }

  }

  /**
   * @author Eike Stepper <br>
   */
  public static class TMObject extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "Object";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMObject(mappingStrategy, feature, dbType);
      }
    }

    public TMObject(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    public Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      long id = resultSet.getLong(getField().getName());
      if (resultSet.wasNull())
      {
        return getFeature().isUnsettable() ? CDORevisionData.NIL : null;
      }

      IExternalReferenceManager externalRefs = getMappingStrategy().getStore().getExternalReferenceManager();
      return CDODBUtil.convertLongToCDOID(externalRefs, getAccessor(), id);
    }

    @Override
    protected void doSetValue(PreparedStatement stmt, int index, Object value) throws SQLException
    {
      IDBStore store = getMappingStrategy().getStore();
      IExternalReferenceManager externalReferenceManager = store.getExternalReferenceManager();
      CommitContext commitContext = StoreThreadLocal.getCommitContext();
      long commitTime = commitContext.getBranchPoint().getTimeStamp();
      long id = CDODBUtil.convertCDOIDToLong(externalReferenceManager, getAccessor(), (CDOID)value, commitTime);
      super.doSetValue(stmt, index, id);
    }

    private IDBStoreAccessor getAccessor()
    {
      IStoreAccessor accessor = StoreThreadLocal.getAccessor();
      if (accessor == null)
      {
        throw new IllegalStateException("Can only be called from within a valid IDBStoreAccessor context");
      }

      return (IDBStoreAccessor)accessor;
    }

  }

  /**
   * @author Eike Stepper
   */
  public static class TMLong extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "Long";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMLong(mappingStrategy, feature, dbType);
      }
    }

    public TMLong(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    public Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      return resultSet.getLong(getField().getName());
    }

  }

  /**
   * @author Eike Stepper
   */
  public static class TMInteger extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "Integer";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMInteger(mappingStrategy, feature, dbType);
      }
    }

    public TMInteger(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    public Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      return resultSet.getInt(getField().getName());
    }

  }

  /**
   * @author Eike Stepper
   */
  public static class TMFloat extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "Float";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMFloat(mappingStrategy, feature, dbType);
      }
    }

    public TMFloat(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    public Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      return resultSet.getFloat(getField().getName());
    }

  }

  /**
   * @author Eike Stepper
   */
  public static class TMDouble extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "Double";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMDouble(mappingStrategy, feature, dbType);
      }
    }

    public TMDouble(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    public Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      return resultSet.getDouble(getField().getName());
    }

  }

  /**
   * @author Eike Stepper
   */
  public static class TMDate2Timestamp extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "Timestamp";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMDate2Timestamp(mappingStrategy, feature, dbType);
      }
    }

    public TMDate2Timestamp(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    public Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      return resultSet.getTimestamp(getField().getName());
    }

    @Override
    protected void doSetValue(PreparedStatement stmt, int index, Object value) throws SQLException
    {
      stmt.setTimestamp(index, new Timestamp(((Date)value).getTime()));
    }
  }

  /**
   * @author Heiko Ahlig
   */
  public static class TMDate2Date extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "Date";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMDate2Date(mappingStrategy, feature, dbType);
      }
    }

    public TMDate2Date(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    public Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      return resultSet.getDate(getField().getName(), Calendar.getInstance());
    }

  }

  /**
   * @author Heiko Ahlig
   */
  public static class TMDate2Time extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "Time";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMDate2Time(mappingStrategy, feature, dbType);
      }
    }

    public TMDate2Time(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    public Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      return resultSet.getTime(getField().getName(), Calendar.getInstance());
    }

  }

  /**
   * @author Eike Stepper
   */
  public static class TMCharacter extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "Character";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMCharacter(mappingStrategy, feature, dbType);
      }
    }

    public TMCharacter(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    public Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      String str = resultSet.getString(getField().getName());
      if (resultSet.wasNull())
      {
        return getFeature().isUnsettable() ? CDORevisionData.NIL : null;
      }

      return str.charAt(0);
    }

    @Override
    protected void doSetValue(PreparedStatement stmt, int index, Object value) throws SQLException
    {
      stmt.setString(index, ((Character)value).toString());
    }

  }

  /**
   * @author Eike Stepper
   */
  public static class TMByte extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "Byte";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMByte(mappingStrategy, feature, dbType);
      }
    }

    public TMByte(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    public Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      return resultSet.getByte(getField().getName());
    }

  }

  /**
   * @author Eike Stepper
   */
  public static class TMBytes extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "ByteArray";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMBytes(mappingStrategy, feature, dbType);
      }
    }

    public TMBytes(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    public Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      return resultSet.getBytes(getField().getName());
    }

  }

  /**
   * @author Eike Stepper
   */
  public static class TMBoolean extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "Boolean";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMBoolean(mappingStrategy, feature, dbType);
      }
    }

    public TMBoolean(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    public Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      return resultSet.getBoolean(getField().getName());
    }

  }

  /**
   * @author Stefan Winkler
   */
  public static class TMBigInteger extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "BigInteger";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMBigInteger(mappingStrategy, feature, dbType);
      }
    }

    public TMBigInteger(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    protected Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      String val = resultSet.getString(getField().getName());

      if (resultSet.wasNull())
      {
        return getFeature().isUnsettable() ? CDORevisionData.NIL : null;
      }

      return new BigInteger(val);
    }

    @Override
    protected void doSetValue(PreparedStatement stmt, int index, Object value) throws SQLException
    {
      stmt.setString(index, ((BigInteger)value).toString());
    }

  }

  /**
   * @author Stefan Winkler
   */
  public static class TMBigDecimal extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "BigDecimal";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMBigDecimal(mappingStrategy, feature, dbType);
      }
    }

    public TMBigDecimal(IMappingStrategy strategy, EStructuralFeature feature, DBType type)
    {
      super(strategy, feature, type);
    }

    @Override
    protected Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      String val = resultSet.getString(getField().getName());

      if (resultSet.wasNull())
      {
        return getFeature().isUnsettable() ? CDORevisionData.NIL : null;
      }

      return new BigDecimal(val);
    }

    @Override
    protected void doSetValue(PreparedStatement stmt, int index, Object value) throws SQLException
    {
      stmt.setString(index, ((BigDecimal)value).toPlainString());
    }

  }

  /**
   * @author Stefan Winkler
   */
  public static class TMCustom extends AbstractTypeMapping
  {
    public static final String ID = ID_PREFIX + "Custom";

    public static class Factory implements ITypeMappingFactory
    {
      public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType)
      {
        return new TMCustom(mappingStrategy, feature, dbType);
      }
    }

    public TMCustom(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType type)
    {
      super(mappingStrategy, feature, type);
    }

    @Override
    protected Object getResultSetValue(ResultSet resultSet) throws SQLException
    {
      String val = resultSet.getString(getField().getName());
      if (resultSet.wasNull())
      {
        return getFeature().isUnsettable() ? CDORevisionData.NIL : null;
      }

      return val;
    }
  }
}
