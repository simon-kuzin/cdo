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
package org.eclipse.emf.cdo.server.db.mapping;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.net4j.db.ddl.IDBTable;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import org.eclipse.emf.ecore.EStructuralFeature;

import java.sql.PreparedStatement;
import java.util.Collection;

/**
 * @author Eike Stepper
 * @author Stefan Winkler
 * @since 2.0
 */
public interface IClassMapping
{

  void detachObject(IDBStoreAccessor dbStoreAccessor, CDOID id, long revised, OMMonitor monitor);

  void writeRevision(IDBStoreAccessor dbStoreAccessor, InternalCDORevision revision, OMMonitor monitor);

  boolean readRevision(IDBStoreAccessor dbStoreAccessor, InternalCDORevision revision, int referenceChunk);

  Collection<IDBTable> getDBTables();

  PreparedStatement createObjectIdStatement(IDBStoreAccessor accessor);

  PreparedStatement createResourceQueryStatement(IDBStoreAccessor accessor, CDOID folderId, String name,
      boolean exactMatch, long timeStamp);

  IListMapping getListMapping(EStructuralFeature feature);

}
