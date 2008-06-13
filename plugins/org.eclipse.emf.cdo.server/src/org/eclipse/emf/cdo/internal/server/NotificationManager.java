/***************************************************************************
 * Copyright (c) 2004 - 2008 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Simon McDuff - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo.internal.server;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.server.INotificationManager;
import org.eclipse.emf.cdo.server.IStoreWriter.CommitContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Simon McDuff
 */
public class NotificationManager implements INotificationManager 
{
  private Repository repository = null;

  public NotificationManager(Repository repository)
  {
    this.repository = repository;
  }

  public void notifyInvalidation(Session session, CommitContext commitContext)
  {
    CDORevisionDelta[] dirtyObjectDeltas = commitContext.getDirtyObjectDeltas();

    int modifications = dirtyObjectDeltas.length;

    if (modifications > 0)
    {
      List<CDOID> dirtyIDs = new ArrayList<CDOID>(modifications);
      List<CDORevisionDelta> deltas = new ArrayList<CDORevisionDelta>(modifications);

      for (int i = 0; i < modifications; i++)
      {
        dirtyIDs.add(dirtyObjectDeltas[i].getID());
        deltas.add(dirtyObjectDeltas[i]);
      }

      SessionManager sessionManager = (SessionManager)repository.getSessionManager();
      
      sessionManager.notifyInvalidation(commitContext.getTimeStamp(), dirtyIDs, deltas, session);
    }
    
  }
}
