/*
 * Copyright (c) 2011, 2012 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Caspar De Groot - initial API and implementation
 */
package org.eclipse.emf.cdo.server.internal.couchbase.marshall;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.lock.IDurableLockingManager.LockGrade;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Victor Roldan Betancort
 * TODO Verbatim copy of DB4OLockArea. Refactor
 */
public class JSONLockEntry
{
  private long cdoID;

  private int lockGrade;

  public JSONLockEntry(long longCdoID, int intLockGrade)
  {
    cdoID = longCdoID;
    lockGrade = intLockGrade;
  }

  public static List<JSONLockEntry> getPrimitiveLockEntries(JSONLockArea jsonLockArea, Map<CDOID, LockGrade> locks)
  {
    List<JSONLockEntry> newList = new LinkedList<JSONLockEntry>();

    for (Entry<CDOID, LockGrade> entry : locks.entrySet())
    {
      CDOID cdoid = entry.getKey();
      long longCdoID = CDOIDUtil.getLong(cdoid);

      LockGrade lockGrade = entry.getValue();
      int intLockGrade = lockGrade.getValue();

      JSONLockEntry lockEntry = getEntry(jsonLockArea.getLockEntries(), longCdoID);
      if (lockEntry == null)
      {
        lockEntry = new JSONLockEntry(longCdoID, intLockGrade);
      }
      else
      {
        lockEntry.setLockGrade(intLockGrade);
      }

      newList.add(lockEntry);
    }

    return newList;
  }

  private void setLockGrade(int lockGrade)
  {
    this.lockGrade = lockGrade;
  }

  // TODO (CD) Avoid linear search
  private static JSONLockEntry getEntry(List<JSONLockEntry> entries, long targetID)
  {
    for (JSONLockEntry entry : entries)
    {
      if (entry.cdoID == targetID)
      {
        return entry;
      }
    }

    return null;
  }

  public long getCdoID()
  {
    return cdoID;
  }

  public int getLockGrade()
  {
    return lockGrade;
  }
}
