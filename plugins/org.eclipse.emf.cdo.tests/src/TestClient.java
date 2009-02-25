import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.net4j.CDOSession;
import org.eclipse.emf.cdo.net4j.CDOSessionConfiguration;
import org.eclipse.emf.cdo.tests.model1.Model1Package;
import org.eclipse.emf.cdo.util.CDOUtil;

import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.connector.IConnector;
import org.eclipse.net4j.tcp.TCPUtil;
import org.eclipse.net4j.util.container.ContainerUtil;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.eclipse.net4j.util.io.IOUtil;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.eclipse.net4j.util.om.OMPlatform;
import org.eclipse.net4j.util.om.log.PrintLogHandler;
import org.eclipse.net4j.util.om.trace.PrintTraceHandler;

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

/**
 * @author Eike Stepper
 */
public class TestClient
{
  public static void main(String[] args) throws Exception
  {
    PrintTraceHandler.CONSOLE.setShortContext(true);
    OMPlatform.INSTANCE.addTraceHandler(PrintTraceHandler.CONSOLE);
    OMPlatform.INSTANCE.addLogHandler(PrintLogHandler.CONSOLE);
    OMPlatform.INSTANCE.setDebugging(true);

    IManagedContainer container = ContainerUtil.createContainer();
    Net4jUtil.prepareContainer(container);
    TCPUtil.prepareContainer(container);
    CDOUtil.prepareContainer(container);
    LifecycleUtil.activate(container);

    IConnector connector = (IConnector)container.getElement("org.eclipse.net4j.connectors", "tcp", "localhost");

    CDOSessionConfiguration sessionConfiguration = CDONet4jUtil.createSessionConfiguration();
    sessionConfiguration.setRepositoryName(TestServer.REPOSITORY_NAME);
    sessionConfiguration.setConnector(connector);
    CDOSession session = sessionConfiguration.openSession();
    session.getPackageRegistry().putEPackage(Model1Package.eINSTANCE);

    System.out.println("Press any key to shutdown");
    while (IOUtil.IN().read() == -1)
    {
      Thread.sleep(200);
    }

    LifecycleUtil.deactivate(session);
    LifecycleUtil.deactivate(connector);
    LifecycleUtil.deactivate(container);
  }
}
