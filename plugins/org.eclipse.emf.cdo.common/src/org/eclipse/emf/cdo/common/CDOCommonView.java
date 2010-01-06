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
package org.eclipse.emf.cdo.common;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.revision.CDORevision;

import org.eclipse.net4j.util.collection.Closeable;

/**
 * @author Eike Stepper
 * @since 2.0
 */
public interface CDOCommonView extends CDOBranchPoint, Closeable
{
  public int getViewID();

  /**
   * @since 3.0
   */
  public boolean isReadOnly();

  public CDOCommonSession getSession();

  /**
   * Returns the ID of the {@link CDOBranch branch} this view is currently referring to.
   * 
   * @since 3.0
   */
  public int getBranchID();

  /**
   * Returns the point in (repository) time this view is currently referring to. {@link CDOObject Objects} provided by
   * this view are {@link CDORevision#isValid(long) valid} at this time. The special value {@link #UNSPECIFIED_DATE}
   * denotes a "floating view" that always shows the latest state of the repository. Any other positive value denotes a
   * historical view that shows the state of the repository at that time.
   * 
   * @see #isHistorical()
   */
  public long getTimeStamp();
}
