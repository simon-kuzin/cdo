/*
 * Copyright (c) 2011-2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Stefan Winkler - 271444: [DB] Multiple refactorings bug 271444
 */
package org.eclipse.emf.cdo.server.internal.db;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;

import org.eclipse.net4j.util.om.monitor.OMMonitor;

import org.eclipse.emf.ecore.EClass;

import java.util.Map;
import java.util.Set;

/**
 * @author Eike Stepper
 * @since 4.0
 */
public interface IObjectTypeMapperBulkSupport extends IObjectTypeMapper
{
  public boolean hasBulkSupport();

  public Set<CDOID> putObjectTypes(IDBStoreAccessor accessor, Map<CDOID, EClass> newObjectTypes, long timeStamp,
      OMMonitor monitor);
}
