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
package org.eclipse.emf.cdo.server.db;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.emf.cdo.server.internal.db.DBStore;
import org.eclipse.emf.cdo.server.internal.db.bundle.OM;
import org.eclipse.emf.cdo.server.internal.db.mapping.horizontal.HorizontalAuditMappingStrategy;
import org.eclipse.emf.cdo.server.internal.db.mapping.horizontal.HorizontalNonAuditMappingStrategy;

import org.eclipse.net4j.db.DBUtil;
import org.eclipse.net4j.db.IDBAdapter;
import org.eclipse.net4j.db.IDBConnectionProvider;
import org.eclipse.net4j.util.ObjectUtil;
import org.eclipse.net4j.util.WrappedException;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Eike Stepper
 */
public final class CDODBUtil
{
  /**
   * @since 2.0
   */
  public static final String EXT_POINT_MAPPING_STRATEGIES = "mappingStrategies";

  private CDODBUtil()
  {
  }

  /**
   * @since 2.0
   */
  public static IDBStore createStore(IMappingStrategy mappingStrategy, IDBAdapter dbAdapter,
      IDBConnectionProvider dbConnectionProvider)
  {
    DBStore store = new DBStore();
    store.setMappingStrategy(mappingStrategy);
    store.setDBAdapter(dbAdapter);
    store.setDbConnectionProvider(dbConnectionProvider);
    mappingStrategy.setStore(store);
    return store;
  }

  /**
   * @since 2.0
   */
  public static IMappingStrategy createHorizontalMappingStrategy()
  {
    return new HorizontalAuditMappingStrategy();
  }

  /**
   * @since 2.0
   */
  public static IMappingStrategy createHorizontalNonAuditMappingStrategy()
  {
    return new HorizontalNonAuditMappingStrategy();
  }

  /**
   * Can only be used when Eclipse is running. In standalone scenarios create the mapping strategy instance by directly
   * calling the constructor of the mapping strategy class.
   * 
   * @see #createHorizontalMappingStrategy()
   * @since 2.0
   */
  public static IMappingStrategy createMappingStrategy(String type)
  {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IConfigurationElement[] elements = registry.getConfigurationElementsFor(OM.BUNDLE_ID, EXT_POINT_MAPPING_STRATEGIES);
    for (final IConfigurationElement element : elements)
    {
      if ("mappingStrategy".equals(element.getName()))
      {
        String typeAttr = element.getAttribute("type");
        if (ObjectUtil.equals(typeAttr, type))
        {
          try
          {
            return (IMappingStrategy)element.createExecutableExtension("class");
          }
          catch (CoreException ex)
          {
            throw WrappedException.wrap(ex);
          }
        }
      }
    }

    return null;
  }

  /**
   * Get the long value of a CDOID (by delegating to {@link CDOIDUtil#getLong(org.eclipse.emf.cdo.common.id.CDOID)}) In
   * addition, provide a check for external IDs which are not supported by the DBStore
   * 
   * @param id
   *          the ID to convert to long
   * @return the long value of the ID
   * @throws IllegalArgumentException
   *           if the ID is not convertibla
   * @since 2.0
   */
  public static long getLong(CDOID id)
  {
    if (id != null && id.getType() == CDOID.Type.EXTERNAL_OBJECT)
    {
      throw new IllegalArgumentException("DBStore does not support external references.");
    }

    return CDOIDUtil.getLong(id);
  }

  /**
   * @since 2.0
   */
  public static int sqlUpdate(PreparedStatement stmt, boolean exactlyOne) throws SQLException
  {
    DBUtil.trace(stmt.toString());
    int result = stmt.executeUpdate();

    // basic check of update result
    if (exactlyOne && result != 1)
    {
      throw new IllegalStateException(stmt.toString() + " returned Update count " + result + " (expected: 1)");
    }
    else if (result == Statement.EXECUTE_FAILED)
    {
      throw new IllegalStateException(stmt.toString() + " returned EXECUTE_FAILED.");
    }

    return result;
  }

  // public static CDODBStoreManager getStoreManager(IDBAdapter dbAdapter,
  // DataSource dataSource)
  // {
  // CDODBStoreManager storeManager = new CDODBStoreManager(dbAdapter,
  // dataSource);
  // storeManager.initDatabase();
  // return storeManager;
  // }
  //
  // public static CDODBStoreManager getStoreManager()
  // {
  // Properties properties = OM.BUNDLE.getConfigProperties();
  // String adapterName = properties.getProperty("store.adapterName", "derby-embedded");
  // IDBAdapter dbAdapter = DBUtil.getDBAdapter(adapterName);
  // DataSource dataSource = DBUtil.createDataSource(properties, "datasource");
  // return getStoreManager(dbAdapter, dataSource);
  // }

  /**
   * For debugging purposes ONLY!
   * 
   * @since 2.0
   */
  public static void sqlDump(Connection conn, String sql)
  {
    ContextTracer TRACER = new ContextTracer(OM.DEBUG, CDODBUtil.class);
    ResultSet rs = null;
    try
    {
      TRACER.format("Dumping output of {0}", sql);
      rs = conn.createStatement().executeQuery(sql);
      int numCol = rs.getMetaData().getColumnCount();

      StringBuilder row = new StringBuilder();
      for (int c = 1; c <= numCol; c++)
      {
        row.append(String.format("%10s | ", rs.getMetaData().getColumnLabel(c)));
      }

      TRACER.trace(row.toString());

      row = new StringBuilder();
      for (int c = 1; c <= numCol; c++)
      {
        row.append("-----------+--");
      }

      TRACER.trace(row.toString());

      while (rs.next())
      {
        row = new StringBuilder();
        for (int c = 1; c <= numCol; c++)
        {
          row.append(String.format("%10s | ", rs.getString(c)));
        }

        TRACER.trace(row.toString());
      }

      row = new StringBuilder();
      for (int c = 1; c <= numCol; c++)
      {
        row.append("-----------+-");
      }

      TRACER.trace(row.toString());
    }
    catch (SQLException ex)
    {
      // NOP
    }
    finally
    {
      if (rs != null)
      {
        try
        {
          rs.close();
        }
        catch (SQLException ex)
        {
          // NOP
        }
      }
    }
  }

  /**
   * For debugging purposes ONLY!
   * 
   * @since 2.0
   */
  public static void sqlDump(IDBConnectionProvider connectionProvider, String sql)
  {
    Connection connection = connectionProvider.getConnection();
    try
    {
      sqlDump(connection, sql);
    }
    finally
    {
      DBUtil.close(connection);
    }
  }

}
