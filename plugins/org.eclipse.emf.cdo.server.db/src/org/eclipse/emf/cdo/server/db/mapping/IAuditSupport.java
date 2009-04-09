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

import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

/**
 * @author Eike Stepper
 * @author Stefan Winkler
 * @since 2.0
 */
public interface IAuditSupport
{

  boolean readRevisionByVersion(IDBStoreAccessor dbStoreAccessor, InternalCDORevision revision, int version,
      int referenceChunk);

  boolean readRevisionByTime(IDBStoreAccessor dbStoreAccessor, InternalCDORevision revision, long timeStamp,
      int referenceChunk);

}
