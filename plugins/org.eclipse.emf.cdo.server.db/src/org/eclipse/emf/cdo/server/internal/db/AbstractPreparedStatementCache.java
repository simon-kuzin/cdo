package org.eclipse.emf.cdo.server.internal.db;

import org.eclipse.emf.cdo.server.db.IPreparedStatementCache;

import org.eclipse.net4j.util.lifecycle.Lifecycle;

import java.sql.Connection;

public abstract class AbstractPreparedStatementCache extends Lifecycle implements IPreparedStatementCache
{
  private Connection connection = null;

  public final Connection getConnection()
  {
    return connection;
  }

  public final void setConnection(Connection connection)
  {
    checkInactive();
    this.connection = connection;
  }

  @Override
  protected void doBeforeActivate()
  {
    checkNull(connection, "Must have valid connection to start.");
  }
}
