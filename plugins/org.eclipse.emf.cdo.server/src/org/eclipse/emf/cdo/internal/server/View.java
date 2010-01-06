/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Simon McDuff - bug 233490
 */
package org.eclipse.emf.cdo.internal.server;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.spi.server.InternalRepository;
import org.eclipse.emf.cdo.spi.server.InternalSession;
import org.eclipse.emf.cdo.spi.server.InternalView;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Eike Stepper
 */
public class View implements InternalView
{
  private InternalSession session;

  private int viewID;

  private int branchID;

  private long timeStamp;

  private InternalRepository repository;

  private Set<CDOID> changeSubscriptionIDs = new HashSet<CDOID>();

  /**
   * @since 2.0
   */
  public View(InternalSession session, int viewID, int branchID, long timeStamp)
  {
    this.session = session;
    repository = session.getManager().getRepository();

    this.viewID = viewID;
    this.branchID = branchID;
    setTimeStamp(timeStamp);
  }

  public InternalSession getSession()
  {
    return session;
  }

  public int getViewID()
  {
    return viewID;
  }

  public int getBranchID()
  {
    return branchID;
  }

  public long getTimeStamp()
  {
    return timeStamp;
  }

  public boolean isReadOnly()
  {
    return true;
  }

  public boolean isHistorical()
  {
    return timeStamp != UNSPECIFIED_DATE;
  }

  /**
   * @since 2.0
   */
  public InternalRepository getRepository()
  {
    checkOpen();
    return repository;
  }

  public boolean[] changeTarget(int branchID, long timeStamp, List<CDOID> invalidObjects)
  {
    checkOpen();
    setTimeStamp(timeStamp);
    List<CDORevision> revisions = repository.getRevisionManager().getRevisions(invalidObjects, timeStamp,
        0, CDORevision.DEPTH_NONE, false);
    boolean[] existanceFlags = new boolean[revisions.size()];
    for (int i = 0; i < existanceFlags.length; i++)
    {
      existanceFlags[i] = revisions.get(i) != null;
    }

    return existanceFlags;
  }

  private void setTimeStamp(long timeStamp)
  {
    repository.validateTimeStamp(timeStamp);
    this.timeStamp = timeStamp;
  }

  /**
   * @since 2.0
   */
  public synchronized void subscribe(CDOID id)
  {
    checkOpen();
    changeSubscriptionIDs.add(id);
  }

  /**
   * @since 2.0
   */
  public synchronized void unsubscribe(CDOID id)
  {
    checkOpen();
    changeSubscriptionIDs.remove(id);
  }

  /**
   * @since 2.0
   */
  public synchronized boolean hasSubscription(CDOID id)
  {
    checkOpen();
    return changeSubscriptionIDs.contains(id);
  }

  /**
   * @since 2.0
   */
  public synchronized void clearChangeSubscription()
  {
    checkOpen();
    changeSubscriptionIDs.clear();
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("View[{0}]", viewID); //$NON-NLS-1$
  }

  /**
   * @since 2.0
   */
  public void close()
  {
    if (!isClosed())
    {
      session.viewClosed(this);
    }
  }

  /**
   * @since 2.0
   */
  public void doClose()
  {
    clearChangeSubscription();
    session = null;
    repository = null;
    changeSubscriptionIDs = null;
  }

  /**
   * @since 2.0
   */
  public boolean isClosed()
  {
    return session == null;
  }

  private void checkOpen()
  {
    if (isClosed())
    {
      throw new IllegalStateException("View closed"); //$NON-NLS-1$
    }
  }
}
