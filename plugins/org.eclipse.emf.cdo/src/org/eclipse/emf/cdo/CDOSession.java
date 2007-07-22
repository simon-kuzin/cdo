/***************************************************************************
 * Copyright (c) 2004-2007 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo;

import org.eclipse.emf.cdo.protocol.model.CDOPackageManager;
import org.eclipse.emf.cdo.util.CDOPackageRegistry;

import org.eclipse.net4j.IChannel;
import org.eclipse.net4j.util.container.IContainer;

import org.eclipse.emf.ecore.resource.ResourceSet;

/**
 * @author Eike Stepper
 */
public interface CDOSession extends IContainer<CDOView>
{
  public int getSessionID();

  public IChannel getChannel();

  public boolean isOpen();

  public String getRepositoryName();

  public String getRepositoryUUID();

  public CDOPackageRegistry getPackageRegistry();

  public CDOPackageManager getPackageManager();

  public CDORevisionManager getRevisionManager();

  public CDOView[] getViews();

  public CDOTransaction openTransaction(ResourceSet resourceSet);

  public CDOTransaction openTransaction();

  public CDOView openView(ResourceSet resourceSet);

  public CDOView openView();

  public CDOAudit openAudit(ResourceSet resourceSet, long timeStamp);

  public CDOAudit openAudit(long timeStamp);

  public void close();
}
