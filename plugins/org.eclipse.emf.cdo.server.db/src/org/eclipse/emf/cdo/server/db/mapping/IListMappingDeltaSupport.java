/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others. 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Stefan Winkler - 271444: [DB] Multiple refactorings https://bugs.eclipse.org/bugs/show_bug.cgi?id=271444  
 */
package org.eclipse.emf.cdo.server.db.mapping;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;

/**
 * Interface to complement {@link IListMapping} in order to provide list delta processing support.
 * 
 * @author Eike Stepper
 * @author Stefan Winkler
 * @since 2.0
 */
public interface IListMappingDeltaSupport
{
  /**
   * Insert a list item at a specified position.
   * 
   * @param accessor
   *          the accessor to use
   * @param id
   *          the id of the revision to insert the value
   * @param newVersion
   *          the new version of the revision after the value is inserted.
   * @param index
   *          the position at which the value should be inserted.
   * @param value
   *          the value to insert.
   */
  void insertListItem(IDBStoreAccessor accessor, CDOID id, int newVersion, int index, Object value);

  /**
   * Set a value at a specified position to the given value.
   * 
   * @param accessor
   *          the accessor to use
   * @param id
   *          the id of the revision to set the value
   * @param newVersion
   *          the new version of the revision after the value is set.
   * @param index
   *          the position at which the value should be set.
   * @param value
   *          the value to be set.
   */
  void setListItem(IDBStoreAccessor accessor, CDOID id, int newVersion, int index, Object value);

  /**
   * Move a list item from one position to another. Indices between both positions are updated so that the list remains
   * consistent.
   * 
   * @param accessor
   *          the accessor to use
   * @param id
   *          the id of the revision in which to move the item
   * @param newVersion
   *          the new version of the revision after the item is moved.
   * @param oldPosition
   *          the old position of the item.
   * @param newPosition
   *          the new position of the item.
   */
  void moveListItem(IDBStoreAccessor accessor, CDOID id, int newVersion, int oldPosition, int newPosition);

  /**
   * Remove a list item from a specified a position.
   * 
   * @param accessor
   *          the accessor to use
   * @param id
   *          the id of the revision from which to remove the item
   * @param newVersion
   *          the new version of the revision after the item is removed.
   * @param index
   *          the index of the item to be removed.
   */
  void removeListItem(IDBStoreAccessor accessor, CDOID id, int newVersion, int index);

  /**
   * Clear a list of a given revision.
   * 
   * @param accessor
   *          the accessor to use
   * @param id
   *          the id of the revision from which to remove all items
   */
  void clearList(IDBStoreAccessor accessor, CDOID id);
}
