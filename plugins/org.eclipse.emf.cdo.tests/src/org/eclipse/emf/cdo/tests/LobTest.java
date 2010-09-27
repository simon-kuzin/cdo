/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.tests;

import org.eclipse.emf.cdo.common.model.lob.CDOClob;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.tests.bundle.OM;
import org.eclipse.emf.cdo.tests.model3.File;
import org.eclipse.emf.cdo.transaction.CDOTransaction;

import org.eclipse.net4j.util.io.IOUtil;

import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Eike Stepper
 */
public class LobTest extends AbstractCDOTest
{
  public void testCommitClob() throws Exception
  {
    InputStream inputStream = null;

    try
    {
      inputStream = OM.BUNDLE.getInputStream("copyright.txt");
      CDOClob clob = new CDOClob(new InputStreamReader(inputStream));

      File file = getModel3Factory().createFile();
      file.setName("copyright.txt");
      file.setData(clob);

      CDOSession session = openSession();
      CDOTransaction transaction = session.openTransaction();
      CDOResource resource = transaction.createResource("res");
      resource.getContents().add(file);

      transaction.commit();
    }
    finally
    {
      IOUtil.close(inputStream);
    }
  }
}
