/***************************************************************************
 * Copyright (c) 2004 - 2008 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Victor Roldan Betancort - http://bugs.eclipse.org/208689
 **************************************************************************/
package org.eclipse.emf.cdo.server.internal.db;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDObject;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.model.CDOClassRef;
import org.eclipse.emf.cdo.server.db.IDBStoreReader;

import org.eclipse.net4j.db.DBException;
import org.eclipse.net4j.db.DBUtil;
import org.eclipse.net4j.util.collection.CloseableIterator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

/**
 * @author Eike Stepper
 */
public abstract class ObjectIDIterator implements CloseableIterator<CDOID>
{
  private MappingStrategy mappingStrategy;

  private IDBStoreReader storeReader;

  private boolean withTypes;

  private ResultSet currentResultSet;

  private CDOID nextID;

  private boolean closed;

  /**
   * Creates an iterator over all objects in a store. It is important to {@link #dispose()} of this iterator after usage
   * to properly close internal result sets.
   */
  public ObjectIDIterator(MappingStrategy mappingStrategy, IDBStoreReader storeReader, boolean withTypes)
  {
    this.mappingStrategy = mappingStrategy;
    this.storeReader = storeReader;
    this.withTypes = withTypes;
  }

  public void close()
  {
    DBUtil.close(currentResultSet);
    nextID = null;
    closed = true;
  }

  public boolean isClosed()
  {
    return closed;
  }

  public MappingStrategy getMappingStrategy()
  {
    return mappingStrategy;
  }

  public IDBStoreReader getStoreReader()
  {
    return storeReader;
  }

  public boolean isWithTypes()
  {
    return withTypes;
  }

  public boolean hasNext()
  {
    if (closed)
    {
      return false;
    }

    nextID = null;
    for (;;)
    {
      if (currentResultSet == null)
      {
        currentResultSet = getNextResultSet();
        if (currentResultSet == null)
        {
          return false;
        }
      }

      try
      {
        if (currentResultSet.next())
        {
          long id = currentResultSet.getLong(1);
          nextID = CDOIDUtil.createLong(id);
          if (withTypes && nextID instanceof CDOIDObject)
          {
            int classID = currentResultSet.getInt(2);
            CDOClassRef classRef = mappingStrategy.getClassRef(storeReader, classID);
            nextID = ((CDOIDObject)nextID).asLegacy(classRef);
          }

          return true;
        }

        DBUtil.close(currentResultSet);
        currentResultSet = null;
        return false;
      }
      catch (SQLException ex)
      {
        throw new DBException(ex);
      }
    }
  }

  public CDOID next()
  {
    if (nextID == null)
    {
      throw new NoSuchElementException();
    }

    return nextID;
  }

  public void remove()
  {
    throw new UnsupportedOperationException();
  }

  protected abstract ResultSet getNextResultSet();
}
