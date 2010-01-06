/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.common.revision;

import org.eclipse.emf.cdo.common.id.CDOID;

import org.eclipse.emf.ecore.EClass;

import java.util.Collection;
import java.util.List;

/**
 * @author Eike Stepper
 * @since 3.0
 */
public interface CDORevisionManager
{
  /**
   * @return The type of an object if a revision for that object is in the revision cache, <code>null</code> otherwise.
   */
  public EClass getObjectType(CDOID id);

  public boolean containsRevision(CDOID id, long timeStamp);

  public boolean containsRevisionByVersion(CDOID id, int version);

  public List<CDORevision> getRevisions(Collection<CDOID> ids, long timeStamp, int referenceChunk,
  int prefetchDepth, boolean loadOnDemand);

  public CDORevision getRevision(CDOID id, long timeStamp, int referenceChunk, int prefetchDepth,
      boolean loadOnDemand);

  public CDORevision getRevisionByVersion(CDOID id, int version, int referenceChunk, int prefetchDepth,
      boolean loadOnDemand);
}
