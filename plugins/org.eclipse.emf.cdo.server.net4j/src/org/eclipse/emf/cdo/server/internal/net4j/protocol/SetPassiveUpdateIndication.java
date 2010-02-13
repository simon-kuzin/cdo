/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Simon McDuff - bug 230832
 */
package org.eclipse.emf.cdo.server.internal.net4j.protocol;

/**
 * @author Eike Stepper
 */
public class SetPassiveUpdateIndication extends RefreshSessionIndication
{
  public SetPassiveUpdateIndication(CDOServerProtocol protocol)
  {
    super(protocol);
    // TODO: implement SetPassiveUpdateIndication.SetPassiveUpdateIndication(protocol)
    throw new UnsupportedOperationException();
  }

  // private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_PROTOCOL, SetPassiveUpdateIndication.class);
  //
  // public SetPassiveUpdateIndication(CDOServerProtocol protocol)
  // {
  // super(protocol, CDOProtocolConstants.SIGNAL_PASSIVE_UPDATE);
  // }
  //
  // @Override
  // protected void indicating(CDODataInput in) throws IOException
  // {
  // super.indicating(in);
  // boolean passiveUpdateEnabled = in.readBoolean();
  // if (TRACER.isEnabled())
  // {
  //      TRACER.trace("Turning " + (passiveUpdateEnabled ? "on" : "off") + " passive update"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
  // }
  //
  // getSession().setPassiveUpdateEnabled(passiveUpdateEnabled);
  // }
}
