/*
 * Copyright (c) 2016 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.server.db.mapping;

import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.server.db.IIDHandler;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.net4j.db.BatchedStatement;
import org.eclipse.net4j.db.IDBPreparedStatement;
import org.eclipse.net4j.util.collection.MoveableList;

import java.sql.SQLException;

/**
 * Interface to complement {@link IListMapping} in order to provide bulk insert support.
 *
 * @author Eike Stepper
 * @since 4.4
 */
public interface IListMappingBulkSupport extends IListMapping
{
  public IDBPreparedStatement getInsertEntryStatement(IDBStoreAccessor accessor) throws SQLException;

  public void writeBulkValues(BatchedStatement stmt, IIDHandler idHandler, InternalCDORevision revision,
      MoveableList<Object> list) throws SQLException;
}
