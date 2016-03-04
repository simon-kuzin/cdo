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

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.net4j.util.om.monitor.OMMonitor;

import java.sql.SQLException;
import java.util.List;

/**
 * An extension interface for {@link IClassMapping class mappings} that support <i>bulk inserts</i>.
 *
 * @author Eike Stepper
 * @since 4.4
 */
public interface IClassMappingBulkSupport extends IClassMapping
{
  public void writeBulkRevisions(IDBStoreAccessor accessor, List<InternalCDORevision> revisions, CDOBranch branch,
      long timeStamp, OMMonitor monitor) throws SQLException;
}
