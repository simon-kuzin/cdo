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

import org.eclipse.emf.cdo.server.IStoreChunkReader.Chunk;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.server.db.IDBStoreChunkReader;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.net4j.db.ddl.IDBTable;

import java.util.Collection;
import java.util.List;

/**
 * @author Eike Stepper
 * @author Stefan Winkler
 * @since 2.0
 */
public interface IListMapping
{

  void writeValues(IDBStoreAccessor accessor, InternalCDORevision revision);

  void readValues(IDBStoreAccessor accessor, InternalCDORevision revision, int referenceChunk);

  void readChunks(IDBStoreChunkReader dbStoreChunkReader, List<Chunk> chunks, String string);

  Collection<IDBTable> getDBTables();
}
