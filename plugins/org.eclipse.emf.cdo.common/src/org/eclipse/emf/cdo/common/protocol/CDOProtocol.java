/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.common.protocol;

import org.eclipse.emf.cdo.common.CDOCommonSession;
import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionDelta;

import java.util.Map;
import java.util.Set;

/**
 * @author Eike Stepper
 * @since 2.0
 */
public interface CDOProtocol
{
  public CDOCommonSession getSession();

  /**
   * @author Eike Stepper
   */
  public interface SyncRevisionsContext
  {
    public Set<InternalCDORevision> getUnrevisedRevisions();

    public Map<CDOBranch, Set<InternalCDORevision>> getPointerRevisions();

    public void reviseRevision(CDORevisionKey revisionKey, long revised);

    public void addRevision(InternalCDORevision revision);

    public void addRevisionDelta(InternalCDORevisionDelta revisionDelta);
  }
}
