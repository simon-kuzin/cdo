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
package org.eclipse.emf.cdo.internal.server.protocol;

import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;
import org.eclipse.emf.cdo.internal.server.Repository;
import org.eclipse.emf.cdo.internal.server.Session;
import org.eclipse.emf.cdo.internal.server.SessionManager;
import org.eclipse.emf.cdo.internal.server.bundle.OM;
import org.eclipse.emf.cdo.server.IRepositoryProvider;
import org.eclipse.emf.cdo.server.RepositoryNotFoundException;
import org.eclipse.emf.cdo.server.SessionCreationException;

import org.eclipse.net4j.util.om.trace.ContextTracer;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public class OpenSessionIndication extends RepositoryTimeIndication
{
  private static final ContextTracer PROTOCOL_TRACER = new ContextTracer(OM.DEBUG_PROTOCOL, OpenSessionIndication.class);

  private String repositoryName;

  private boolean passiveUpdateEnabled;

  private Repository repository;

  private Session session;

  public OpenSessionIndication(CDOServerProtocol protocol)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_OPEN_SESSION);
  }

  @Override
  protected Repository getRepository()
  {
    return repository;
  }

  @Override
  protected Session getSession()
  {
    return session;
  }

  @Override
  protected void indicating(CDODataInput in) throws IOException
  {
    super.indicating(in);
    repositoryName = in.readString();
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Read repositoryName: {0}", repositoryName);
    }

    passiveUpdateEnabled = in.readBoolean();
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Read passiveUpdateEnabled: {0}", passiveUpdateEnabled);
    }
  }

  @Override
  protected void responding(CDODataOutput out) throws IOException
  {
    try
    {
      CDOServerProtocol protocol = getProtocol();
      IRepositoryProvider repositoryProvider = protocol.getRepositoryProvider();
      repository = (Repository)repositoryProvider.getRepository(repositoryName);
      if (repository == null)
      {
        throw new RepositoryNotFoundException(repositoryName);
      }

      SessionManager sessionManager = repository.getSessionManager();
      session = sessionManager.openSession(protocol);
      session.setPassiveUpdateEnabled(passiveUpdateEnabled);

      // Adjust the infra structure (was IRepositoryProvider)
      protocol.setInfraStructure(session);
      if (PROTOCOL_TRACER.isEnabled())
      {
        PROTOCOL_TRACER.format("Writing sessionID: {0}", session.getSessionID());
      }

      out.writeInt(session.getSessionID());
      if (PROTOCOL_TRACER.isEnabled())
      {
        PROTOCOL_TRACER.format("Writing repositoryUUID: {0}", repository.getUUID());
      }

      out.writeString(repository.getUUID());
      out.writeLong(repository.getCreationTime());
      out.writeBoolean(repository.isSupportingAudits());
      repository.getStore().getCDOIDLibraryDescriptor().write(out);

      CDOPackageUnit[] packageUnits = repository.getPackageRegistry().getPackageUnits();
      sendPackageUnits(out, packageUnits);
    }
    catch (RepositoryNotFoundException ex)
    {
      if (PROTOCOL_TRACER.isEnabled())
      {
        PROTOCOL_TRACER.format("Repository {0} not found", repositoryName);
      }

      out.writeInt(CDOProtocolConstants.ERROR_REPOSITORY_NOT_FOUND);
    }
    catch (SessionCreationException ex)
    {
      if (PROTOCOL_TRACER.isEnabled())
      {
        PROTOCOL_TRACER.format("Failed to open session for repository {0}", repositoryName);
      }

      out.writeInt(CDOProtocolConstants.ERROR_NO_SESSION);
      return;
    }

    super.responding(out);
  }

  private static void sendPackageUnits(CDODataOutput out, CDOPackageUnit[] packageUnits) throws IOException
  {
    int size = packageUnits.length - 2; // TODO Do not expect 2 system package units
    out.writeInt(size);
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Writing {0} package units", size);
    }

    for (CDOPackageUnit packageUnit : packageUnits)
    {
      if (!packageUnit.isSystem())
      {
        out.writeCDOPackageUnit(packageUnit, false);
        out.writeBoolean(packageUnit.getState() == CDOPackageUnit.State.NEW);
        out.writeBoolean(packageUnit.isDynamic());
      }
    }
  }
}
