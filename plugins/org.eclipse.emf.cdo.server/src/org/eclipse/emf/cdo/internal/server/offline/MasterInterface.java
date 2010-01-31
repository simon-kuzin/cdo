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
package org.eclipse.emf.cdo.internal.server.offline;

import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.session.CDOSessionConfiguration;

import org.eclipse.net4j.util.lifecycle.Lifecycle;

/**
 * @author Eike Stepper
 */
public class MasterInterface extends Lifecycle
{
  private CDOSessionConfiguration sessionConfiguration;

  private CDOSession session;

  // private State state = State.OFFLINE;

  public MasterInterface()
  {
  }

  public CDOSessionConfiguration getSessionConfiguration()
  {
    return sessionConfiguration;
  }

  public void setSessionConfiguration(CDOSessionConfiguration sessionConfiguration)
  {
    checkInactive();
    this.sessionConfiguration = sessionConfiguration;
  }

  @Override
  protected void doBeforeActivate() throws Exception
  {
    super.doBeforeActivate();
    checkState(sessionConfiguration, "sessionConfiguration");
  }

  @Override
  protected void doActivate() throws Exception
  {
    super.doActivate();
    connect();
  }

  @Override
  protected void doDeactivate() throws Exception
  {
    disconnect();
    super.doDeactivate();
  }

  private void connect()
  {
    session = sessionConfiguration.openSession();
  }

  private void disconnect()
  {
  }

  /**
   * @author Eike Stepper
   */
  public static enum State
  {
    OFFLINE, SYNCING, ONLINE
  }
}
