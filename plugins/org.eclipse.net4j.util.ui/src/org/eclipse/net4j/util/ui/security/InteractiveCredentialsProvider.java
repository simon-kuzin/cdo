/*
 * Copyright (c) 2008, 2011-2013 Eike Stepper (Berlin, Germany), CEA LIST, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Christian W. Damus (CEA LIST) - 399306
 */
package org.eclipse.net4j.util.ui.security;

import org.eclipse.net4j.util.security.IPasswordCredentials;
import org.eclipse.net4j.util.security.IPasswordCredentialsProvider2;
import org.eclipse.net4j.util.security.IPasswordCredentialsUpdate;
import org.eclipse.net4j.util.security.IPasswordCredentialsUpdateProvider;
import org.eclipse.net4j.util.ui.UIUtil;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * @author Eike Stepper
 * @since 2.0
 */
public class InteractiveCredentialsProvider implements IPasswordCredentialsProvider2,
    IPasswordCredentialsUpdateProvider
{
  public InteractiveCredentialsProvider()
  {
  }

  public boolean isInteractive()
  {
    return true;
  }

  public IPasswordCredentials getCredentials()
  {
    return getCredentials(null);
  }

  /**
   * @since 3.3
   */
  public IPasswordCredentials getCredentials(final String realm)
  {
    final IPasswordCredentials[] credentials = new IPasswordCredentials[1];
    final Display display = UIUtil.getDisplay();
    display.syncExec(new Runnable()
    {
      public void run()
      {
        Shell shell;

        try
        {
          IWorkbenchWindow window = UIUtil.getActiveWorkbenchWindow();
          shell = window.getShell();
        }
        catch (Exception ex)
        {
          shell = new Shell(display);
        }

        CredentialsDialog dialog = new CredentialsDialog(shell, realm);
        if (dialog.open() == CredentialsDialog.OK)
        {
          credentials[0] = dialog.getCredentials();
        }
      }
    });

    return credentials[0];
  }

  /**
   * @since 3.4
   */
  public IPasswordCredentialsUpdate getCredentialsUpdate()
  {
    return getCredentialsUpdate(null);
  }

  /**
   * @since 3.4
   */
  public IPasswordCredentialsUpdate getCredentialsUpdate(final String realm)
  {
    final IPasswordCredentialsUpdate[] update = { null };
    final Display display = UIUtil.getDisplay();
    display.syncExec(new Runnable()
    {
      public void run()
      {
        Shell shell;

        try
        {
          IWorkbenchWindow window = UIUtil.getActiveWorkbenchWindow();
          shell = window.getShell();
        }
        catch (Exception ex)
        {
          shell = new Shell(display);
        }

        CredentialsUpdateDialog dialog = new CredentialsUpdateDialog(shell, realm);
        if (dialog.open() == Window.OK)
        {
          update[0] = dialog.getCredentials();
        }
      }
    });

    return update[0];
  }
}
