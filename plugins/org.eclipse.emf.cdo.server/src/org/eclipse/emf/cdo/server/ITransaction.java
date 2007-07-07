/***************************************************************************
 * Copyright (c) 2004 - 2007 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo.server;

import org.eclipse.emf.cdo.internal.protocol.revision.CDORevisionImpl;
import org.eclipse.emf.cdo.protocol.CDOID;

import org.eclipse.net4j.util.store.IStoreTransaction;

/**
 * @author Eike Stepper
 */
public interface ITransaction extends IStoreTransaction
{
  public void registerResource(CDOID id, String path);

  public CDOID getResourceID(String path);

  public String getResourcePath(CDOID id);

  public void addRevision(CDORevisionImpl revision);

  public CDORevisionImpl getRevision(CDOID id);

  public CDORevisionImpl getRevision(CDOID id, long timeStamp);
}
