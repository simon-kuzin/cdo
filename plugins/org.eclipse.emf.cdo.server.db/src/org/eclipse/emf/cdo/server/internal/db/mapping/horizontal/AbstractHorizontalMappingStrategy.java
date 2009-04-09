/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Stefan Winkler - major refactoring
 */
package org.eclipse.emf.cdo.server.internal.db.mapping.horizontal;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.model.CDOClassifierRef;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.eresource.EresourcePackage;
import org.eclipse.emf.cdo.server.IStoreAccessor.QueryResourcesContext;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.server.db.IObjectTypeCache;
import org.eclipse.emf.cdo.server.db.mapping.IClassMapping;
import org.eclipse.emf.cdo.server.internal.db.mapping.AbstractMappingStrategy;

import org.eclipse.net4j.db.DBException;
import org.eclipse.net4j.db.DBUtil;
import org.eclipse.net4j.db.IDBAdapter;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;

import org.eclipse.emf.ecore.EClass;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * @author Eike Stepper
 * @author Stefan Winkler
 * @since 2.0
 */
public abstract class AbstractHorizontalMappingStrategy extends AbstractMappingStrategy
{
  private IObjectTypeCache objectTypeCache;

  @Override
  public CDOClassifierRef readObjectType(IDBStoreAccessor accessor, CDOID id)
  {
    return objectTypeCache.getObjectType(accessor, id);
  }

  @Override
  public long repairAfterCrash(IDBAdapter dbAdapter, Connection connection)
  {
    return objectTypeCache.getMaxId(connection);
  }

  @Override
  protected Collection<EClass> getClassesWithObjectInfo()
  {
    return getClassMappings().keySet();
  }

  @Override
  protected void doActivate() throws Exception
  {
    super.doActivate();
    if (objectTypeCache == null)
    {
      objectTypeCache = createObjectTypeCache();
      LifecycleUtil.activate(objectTypeCache);
    }
  }

  @Override
  protected void doDeactivate() throws Exception
  {
    LifecycleUtil.deactivate(objectTypeCache);
    objectTypeCache = null;
    super.doDeactivate();
  }

  private IObjectTypeCache createObjectTypeCache()
  {
    ObjectTypeCache cache = new ObjectTypeCache();
    cache.setMappingStrategy(this);
    return cache;
  }

  @Override
  public void queryResources(IDBStoreAccessor dbStoreAccessor, QueryResourcesContext context)
  {
    if (context.getTimeStamp() != CDORevision.UNSPECIFIED_DATE && !hasAuditSupport())
    {
      throw new UnsupportedOperationException("Mapping Strategy does not support audits.");
    }

    EresourcePackage resourcesPackage = EresourcePackage.eINSTANCE;

    if (queryResources(dbStoreAccessor, getClassMapping(resourcesPackage.getCDOResourceFolder()), context))
    {
      queryResources(dbStoreAccessor, getClassMapping(resourcesPackage.getCDOResource()), context);
    }
  }

  /**
   * This is an intermediate implementation. It should be changed after classmappings support a general way to implement
   * queries ...
   * 
   * @return true if result context is not yet full and query should continue false, if result context is full and query
   *         should stop.
   */
  private boolean queryResources(IDBStoreAccessor accessor, IClassMapping classMapping, QueryResourcesContext context)
  {
    PreparedStatement stmt = null;
    ResultSet rset = null;

    CDOID folderID = context.getFolderID();
    String name = context.getName();
    boolean exactMatch = context.exactMatch();
    long timeStamp = context.getTimeStamp();

    try
    {
      stmt = classMapping.createResourceQueryStatement(accessor, folderID, name, exactMatch, timeStamp);
      rset = stmt.executeQuery();

      while (rset.next())
      {
        long longID = rset.getLong(1);
        CDOID id = CDOIDUtil.createLong(longID);
        if (!context.addResource(id))
        {
          // No more results allowed
          return false; // don't continue
        }
      }

      return true; // continue with other results
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
    finally
    {
      DBUtil.close(rset);
      DBUtil.close(stmt);
    }
  }
}
