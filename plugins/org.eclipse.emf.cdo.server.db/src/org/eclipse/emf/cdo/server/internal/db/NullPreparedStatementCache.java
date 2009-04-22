package org.eclipse.emf.cdo.server.internal.db;

import org.eclipse.emf.cdo.server.db.IPreparedStatementCache;
import org.eclipse.emf.cdo.server.internal.db.bundle.OM;

import org.eclipse.net4j.db.DBException;
import org.eclipse.net4j.db.DBUtil;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;

public class NullPreparedStatementCache extends AbstractPreparedStatementCache implements IPreparedStatementCache
{
  HashSet<PreparedStatement> allocatedStatements = new HashSet<PreparedStatement>();

  public PreparedStatement getPreparedStatement(String sql, PSReuseProbability reuseProbability)
  {
    try
    {
      PreparedStatement result = getConnection().prepareStatement(sql);
      allocatedStatements.add(result);
      return result;
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
  }

  public void releasePreparedStatement(PreparedStatement ps)
  {
    allocatedStatements.remove(ps);
    DBUtil.close(ps);
  }

  @Override
  protected void doBeforeDeactivate() throws Exception
  {
    if (!allocatedStatements.isEmpty())
    {
      OM.LOG.warn("Possible Leak Detected:");
      for (PreparedStatement ps : allocatedStatements)
      {
        OM.LOG.warn("- " + ps.toString());
      }
      assert false;
    }
  }
}
