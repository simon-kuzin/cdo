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
package org.eclipse.net4j.db.dml;

import org.eclipse.net4j.db.DBType;

/**
 * @author Eike Stepper
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IDBParameter
{
  public IDBStatement getStatement();

  /**
   * Returns the zero based position of this parameter within the {@link IDBStatement#getParameters() parameters} list
   * of the containing {@link #getStatement() statement}.
   */
  public int getPosition();

  public DBType getType();
}
