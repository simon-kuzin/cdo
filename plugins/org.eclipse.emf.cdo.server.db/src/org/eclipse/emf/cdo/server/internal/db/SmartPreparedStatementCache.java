package org.eclipse.emf.cdo.server.internal.db;

import org.eclipse.emf.cdo.server.internal.db.bundle.OM;

import org.eclipse.net4j.db.DBException;
import org.eclipse.net4j.util.ImplementationError;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

public class SmartPreparedStatementCache extends AbstractPreparedStatementCache
{
  private Cache cache;

  private HashMap<PreparedStatement, CachedPreparedStatement> checkedOut = new HashMap<PreparedStatement, CachedPreparedStatement>();

  public SmartPreparedStatementCache(int capacity)
  {
    cache = new Cache(capacity);
  }

  public PreparedStatement getPreparedStatement(String sql, PSReuseProbability reuseProbability)
  {
    CachedPreparedStatement cachedStatement = cache.remove(sql);

    if (cachedStatement == null)
    {
      cachedStatement = createCachedPreparedStatement(sql, reuseProbability);
    }

    PreparedStatement result = cachedStatement.getPreparedStatement();
    checkedOut.put(result, cachedStatement);

    return result;
  }

  private CachedPreparedStatement createCachedPreparedStatement(String sql, PSReuseProbability reuseProbability)
  {
    PreparedStatement stmt;
    try
    {
      stmt = getConnection().prepareStatement(sql);
      CachedPreparedStatement result = new CachedPreparedStatement(sql, reuseProbability, stmt);
      return result;
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
  }

  public void releasePreparedStatement(PreparedStatement ps)
  {
    CachedPreparedStatement cachedStatement = checkedOut.remove(ps);
    cache.put(cachedStatement);
  }

  @Override
  protected void doBeforeDeactivate() throws Exception
  {
    if (!checkedOut.isEmpty())
    {
      OM.LOG.warn("Statement leak detected.");
    }
  }

  private class Cache
  {
    private CacheList lists[];

    private HashMap<String, CachedPreparedStatement> lookup;

    private int capacity;

    public Cache(int capacity)
    {
      this.capacity = capacity;

      lookup = new HashMap<String, CachedPreparedStatement>(capacity);

      lists = new CacheList[PSReuseProbability.values().length];
      for (PSReuseProbability prob : PSReuseProbability.values())
      {
        lists[prob.ordinal()] = new CacheList();
      }
    }

    public void put(CachedPreparedStatement cachedStatement)
    {
      // refresh age
      cachedStatement.touch();

      // put into appripriate list
      lists[cachedStatement.getProbability().ordinal()].add(cachedStatement);

      // put into lookup table
      if (lookup.put(cachedStatement.getSql(), cachedStatement) != null)
      {
        throw new ImplementationError(cachedStatement.getSql() + " already in cache.");
      }

      // handle capacity overflow
      if (lookup.size() > capacity)
      {
        evictOne();
      }
    }

    private void evictOne()
    {
      long maxAge = -1;
      int ordinal = -1;

      for (PSReuseProbability prob : PSReuseProbability.values())
      {
        if (!lists[prob.ordinal()].isEmpty())
        {
          long age = lists[prob.ordinal()].tail().getAge();
          if (maxAge < age)
          {
            maxAge = age;
            ordinal = prob.ordinal();
          }
        }
      }

      remove(lists[ordinal].tail().getSql());
    }

    public CachedPreparedStatement remove(String sql)
    {
      CachedPreparedStatement result = lookup.remove(sql);
      if (result == null)
      {
        return null;
      }
      else
      {
        lists[result.getProbability().ordinal()].remove(result);
        return result;
      }
    }

    private class CacheList
    {
      private CachedPreparedStatement first = null;

      private CachedPreparedStatement last = null;

      public CacheList()
      {
      };

      public void add(CachedPreparedStatement s)
      {
        if (first == null)
        {
          first = s;
          last = s;
          s.previous = null;
          s.next = null;
        }
        else
        {
          first.previous = s;
          s.next = first;
          first = s;
        }
      }

      public void remove(CachedPreparedStatement s)
      {
        if (s == first)
        {
          first = s.next;
        }
        if (s.next != null)
        {
          s.next.previous = s.previous;
        }
        if (s == last)
        {
          last = s.previous;
        }
        if (s.previous != null)
        {
          s.previous.next = s.next;
        }

        s.previous = null;
        s.next = null;
      }

      public CachedPreparedStatement tail()
      {
        return last;
      }

      public boolean isEmpty()
      {
        return first == null;
      }
    }
  };

  private class CachedPreparedStatement
  {
    private long timeStamp;

    private String sql;

    private PSReuseProbability probability;

    private PreparedStatement statement;

    // DL fields
    CachedPreparedStatement previous = null;

    CachedPreparedStatement next = null;

    public CachedPreparedStatement(String sql, PSReuseProbability prob, PreparedStatement stmt)
    {
      this.sql = sql;
      probability = prob;
      statement = stmt;
      timeStamp = System.currentTimeMillis();
    }

    public PreparedStatement getPreparedStatement()
    {
      return statement;
    }

    public long getAge()
    {
      long currentTime = System.currentTimeMillis();
      return (currentTime - timeStamp) * probability.ordinal();
    }

    public void touch()
    {
      timeStamp = System.currentTimeMillis();
    }

    public String getSql()
    {
      return sql;
    }

    public PSReuseProbability getProbability()
    {
      return probability;
    }
  }
}
