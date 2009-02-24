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
package org.eclipse.emf.cdo.internal.common.model;

import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.model.InternalEModelElement;

import org.eclipse.net4j.util.om.trace.ContextTracer;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public abstract class EModelElementImpl implements InternalEModelElement
{
  private static final ContextTracer MODEL_TRACER = new ContextTracer(OM.DEBUG_MODEL, EModelElementImpl.class);

  private String name;

  private Object clientInfo;

  private Object serverInfo;

  protected EModelElementImpl(String name)
  {
    this.name = name;
  }

  protected EModelElementImpl()
  {
  }

  public void read(CDODataInput in) throws IOException
  {
    name = in.readString();
  }

  public void write(CDODataOutput out) throws IOException
  {
    out.writeString(name);
  }

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public Object getClientInfo()
  {
    return clientInfo;
  }

  public void setClientInfo(Object clientInfo)
  {
    if (MODEL_TRACER.isEnabled())
    {
      MODEL_TRACER.format("Setting client info: {0} --> {1}", this, clientInfo);
    }

    this.clientInfo = clientInfo;
  }

  public Object getServerInfo()
  {
    return serverInfo;
  }

  public void setServerInfo(Object serverInfo)
  {
    if (MODEL_TRACER.isEnabled())
    {
      MODEL_TRACER.format("Setting server info: {0} --> {1}", this, serverInfo);
    }

    this.serverInfo = serverInfo;
  }
}
