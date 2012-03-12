/*
 * Copyright (c) 2004 - 2012 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.tests.db;

import org.eclipse.emf.cdo.common.CDOCommonRepository.IDGenerationLocation;
import org.eclipse.emf.cdo.tests.db.bundle.OM;

import org.eclipse.net4j.db.DBUtil;
import org.eclipse.net4j.db.IDBAdapter;
import org.eclipse.net4j.db.postgresql.PostgreSQLAdapter;
import org.eclipse.net4j.util.io.IOUtil;

import org.postgresql.jdbc3.Jdbc3SimpleDataSource;

import javax.sql.DataSource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Victor Roldan Betancort
 */
public class PostgresqlConfig extends DBConfig
{
  private static final String HOSTNAME = "majua02";

  private static final String USER_NAME = "cdo";

  private static final String PASSWORD = USER_NAME;

  public static final String DB_ADAPTER_NAME = "Postgresql";

  private static final long serialVersionUID = 1L;

  private transient Jdbc3SimpleDataSource dataSource;

  private transient Jdbc3SimpleDataSource setupDataSource;

  private String currentRepositoryName = "cdodb1";

  public PostgresqlConfig(boolean supportingAudits, boolean supportingBranches,
      IDGenerationLocation idGenerationLocation)
  {
    super(DB_ADAPTER_NAME, supportingAudits, supportingBranches, false, false, idGenerationLocation);
  }

  @Override
  protected String getDBAdapterName()
  {
    return DB_ADAPTER_NAME;
  }

  @Override
  protected IDBAdapter createDBAdapter()
  {
    return new PostgreSQLAdapter();
  }

  @Override
  protected DataSource createDataSource(String repoName)
  {
    currentRepositoryName = repoName;

    dataSource = new Jdbc3SimpleDataSource();
    dataSource.setServerName(HOSTNAME);
    dataSource.setDatabaseName(currentRepositoryName);
    dataSource.setUser(USER_NAME);
    dataSource.setPassword(PASSWORD);

    try
    {
      dataSource.setLogWriter(new PrintWriter(System.err));
    }
    catch (SQLException ex)
    {
      OM.LOG.warn(ex.getMessage());
    }
    if (!isRestarting())
    {
      dropDatabase();
    }

    return dataSource;
  }

  @Override
  protected void deactivateRepositories()
  {
    super.deactivateRepositories();
    dataSource = null;
    setupDataSource = null;
    dropDatabase();
  }

  private void dropDatabase()
  {
    Connection connection = null;

    try
    {
      connection = getSetupDataSource().getConnection();
      DBUtil.dropAllTables(connection, currentRepositoryName);
    }
    catch (SQLException ignore)
    {
      IOUtil.ERR().println(ignore);
    }
    finally
    {
      DBUtil.close(connection);
    }
  }

  private DataSource getSetupDataSource()
  {
    setupDataSource = new Jdbc3SimpleDataSource();
    setupDataSource.setServerName(HOSTNAME);
    setupDataSource.setDatabaseName(currentRepositoryName);
    setupDataSource.setUser(USER_NAME);
    setupDataSource.setPassword(PASSWORD);
    return setupDataSource;
  }
}
