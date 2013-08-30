/*
 * Copyright (c) 2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.releng.setup.ide;

/**
 * @author Eike Stepper
 */
@Deprecated
public final class WorkingSets
{
  // private static final SetupContext CONTEXT = Activator.getDefault();
  //
  // private static final IWorkbench WORKBENCH = PlatformUI.getWorkbench();
  //
  // private static final String PACKAGE_EXPLORER_ID = "org.eclipse.jdt.ui.PackageExplorer";
  //
  // public static void init() throws IOException
  // {
  // WorkingSetGroup workingSetGroup = CONTEXT.getSetup().getBranch().getProject().getWorkingSetGroup();
  // if (workingSetGroup != null)
  // {
  // initPackageExplorer();
  //
  // WorkingSetGroup defaultWorkingSetGroup = WorkingSetsUtil.getWorkingSetGroup();
  // Resource resource = defaultWorkingSetGroup.eResource();
  // resource.getContents().set(0, workingSetGroup);
  // resource.save(null);
  // }
  // }
  //
  // private static void initPackageExplorer()
  // {
  // final IWorkbenchWindow workbenchWindow = WORKBENCH.getWorkbenchWindows()[0];
  // workbenchWindow.getShell().getDisplay().asyncExec(new Runnable()
  // {
  // public void run()
  // {
  // try
  // {
  // IViewPart view = workbenchWindow.getActivePage().showView(PACKAGE_EXPLORER_ID, null,
  // IWorkbenchPage.VIEW_CREATE);
  // if (view != null)
  // {
  // Method method = view.getClass().getMethod("rootModeChanged", int.class);
  // method.invoke(view, 2);
  // }
  // }
  // catch (Exception ex)
  // {
  // Activator.log(ex);
  // }
  // }
  // });
  // }
}
