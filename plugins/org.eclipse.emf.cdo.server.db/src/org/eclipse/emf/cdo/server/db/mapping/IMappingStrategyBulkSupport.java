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
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.net4j.util.om.monitor.OMMonitor;

import org.eclipse.emf.ecore.EClass;

import java.util.List;
import java.util.Map;

/**
 * @author Eike Stepper
 * @since 4.4
 */
public interface IMappingStrategyBulkSupport extends IMappingStrategy
{
  public boolean hasBulkSupport();

  public void writeBulkRevisions(IDBStoreAccessor accessor, Map<EClass, List<InternalCDORevision>> revisionsPerClass,
      Map<CDOID, EClass> newObjectTypes, CDOBranch branch, long timeStamp, OMMonitor monitor);
}
