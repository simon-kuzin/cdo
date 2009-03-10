/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Victor Roldan Betancort - maintenance
 */
package org.eclipse.emf.cdo.internal.ui.dialogs;

import org.eclipse.emf.cdo.common.model.CDOPackageInfo;
import org.eclipse.emf.cdo.internal.ui.SharedIcons;
import org.eclipse.emf.cdo.internal.ui.actions.RegisterFilesystemPackagesAction;
import org.eclipse.emf.cdo.internal.ui.actions.RegisterGeneratedPackagesAction;
import org.eclipse.emf.cdo.internal.ui.actions.RegisterWorkspacePackagesAction;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.ui.CDOItemProvider;

import org.eclipse.net4j.util.ObjectUtil;
import org.eclipse.net4j.util.ui.UIUtil;

import org.eclipse.emf.ecore.EPackage;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbenchPage;

import javax.swing.text.AbstractDocument.Content;

import java.util.List;

/**
 * @author Eike Stepper
 */
public class PackageManagerDialog extends TitleAreaDialog
{
  private static final int REGISTER_GENERATED_PACKAGES_ID = IDialogConstants.CLIENT_ID + 1;

  private static final int REGISTER_WORKSPACE_PACKAGES_ID = IDialogConstants.CLIENT_ID + 2;

  private static final int REGISTER_FILESYSTEM_PACKAGES_ID = IDialogConstants.CLIENT_ID + 3;

  private static final String TITLE = "CDO Package Manager";

  private IWorkbenchPage page;

  private CDOSession session;

  private TableViewer viewer;

  public PackageManagerDialog(IWorkbenchPage page, CDOSession session)
  {
    super(new Shell(page.getWorkbenchWindow().getShell()));
    this.page = page;
    this.session = session;
    setShellStyle(getShellStyle() | SWT.APPLICATION_MODAL | SWT.MAX | SWT.TITLE | SWT.RESIZE);
  }

  @Override
  protected void configureShell(Shell newShell)
  {
    super.configureShell(newShell);
    newShell.setText(TITLE);
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    Composite composite = (Composite)super.createDialogArea(parent);
    setTitle(CDOItemProvider.getSessionLabel(session));
    setTitleImage(SharedIcons.getImage(SharedIcons.WIZBAN_PACKAGE_MANAGER));

    viewer = new TableViewer(composite, SWT.NONE);
    Table table = viewer.getTable();

    table.setHeaderVisible(true);
    table.setLayoutData(UIUtil.createGridData());
    addColumn(table, "Package", 400, SWT.LEFT);
    addColumn(table, "State", 80, SWT.CENTER);
    addColumn(table, "Type", 80, SWT.CENTER);
    addColumn(table, "Original Type", 80, SWT.CENTER);

    viewer.setContentProvider(new EPackageContentProvider());
    viewer.setLabelProvider(new EPackageLabelProvider());
    viewer.setInput(session);

    return composite;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent)
  {
    createButton(parent, REGISTER_GENERATED_PACKAGES_ID, "Generated...", false);
    createButton(parent, REGISTER_WORKSPACE_PACKAGES_ID, "Workspace...", false);
    createButton(parent, REGISTER_FILESYSTEM_PACKAGES_ID, "Filesystem...", false);
    createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, false);
  }

  @Override
  protected void buttonPressed(int buttonId)
  {
    switch (buttonId)
    {
    case REGISTER_GENERATED_PACKAGES_ID:
      new RegisterGeneratedPackagesAction(page, session)
      {
        @Override
        protected void postRegistration(List<EPackage> ePackages)
        {
          refreshViewer();
        }
      }.run();
      break;

    case REGISTER_WORKSPACE_PACKAGES_ID:
      new RegisterWorkspacePackagesAction(page, session)
      {
        @Override
        protected void postRegistration(List<EPackage> ePackages)
        {
          refreshViewer();
        }
      }.run();
      break;

    case REGISTER_FILESYSTEM_PACKAGES_ID:
      new RegisterFilesystemPackagesAction(page, session)
      {
        @Override
        protected void postRegistration(List<EPackage> ePackages)
        {
          refreshViewer();
        }
      }.run();
      break;

    case IDialogConstants.CLOSE_ID:
      close();
      break;
    }
  }

  private void addColumn(Table table, String title, int width, int alignment)
  {
    TableColumn column = new TableColumn(table, alignment);
    column.setText(title);
    column.setWidth(width);
  }

  protected Image getContentIcon(Content content)
  {
    return null;
  }

  protected void refreshViewer()
  {
    page.getWorkbenchWindow().getShell().getDisplay().syncExec(new Runnable()
    {
      public void run()
      {
        try
        {
          viewer.refresh();
        }
        catch (RuntimeException ignore)
        {
        }
      }
    });
  }

  /**
   * @author Eike Stepper
   */
  public class EPackageLabelProvider extends BaseLabelProvider implements ITableLabelProvider
  {
    public EPackageLabelProvider()
    {
    }

    public String getColumnText(Object element, int columnIndex)
    {
      if (element instanceof EPackage)
      {
        CDOPackageInfo packageInfo = session.getPackageRegistry().getPackageInfo((EPackage)element);
        switch (columnIndex)
        {
        case 0:
          return packageInfo.getPackageURI();

        case 1:
          return packageInfo.getPackageUnit().getState().toString();

        case 2:
          return packageInfo.getPackageUnit().getType().toString();

        case 3:
          return packageInfo.getPackageUnit().getOriginalType().toString();
        }
      }

      return element.toString();
    }

    public Image getColumnImage(Object element, int columnIndex)
    {
      if (element instanceof Content)
      {
        Content content = (Content)element;
        if (columnIndex == 0)
        {
          return getContentIcon(content);
        }
      }

      return null;
    }
  }

  /**
   * @author Eike Stepper
   */
  public static class EPackageContentProvider implements IStructuredContentProvider
  {
    private static final Object[] NO_ELEMENTS = {};

    private CDOSession session;

    public EPackageContentProvider()
    {
    }

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
      if (newInput instanceof CDOSession)
      {
        if (!ObjectUtil.equals(session, newInput))
        {
          session = (CDOSession)newInput;
        }
      }
    }

    public Object[] getElements(Object inputElement)
    {
      if (inputElement != session)
      {
        return NO_ELEMENTS;
      }

      return session.getPackageRegistry().values().toArray();
    }
  }
}
