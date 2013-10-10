/*
 * Copyright (c) 2004-2013 Eike Stepper (Berlin, Germany), CEA LIST, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Christian W. Damus (CEA LIST) - initial API and implementation
 */
package org.eclipse.emf.cdo.internal.security.ui.editor;

import org.eclipse.emf.cdo.internal.security.ui.actions.SelectionListenerAction;
import org.eclipse.emf.cdo.internal.security.ui.messages.Messages;
import org.eclipse.emf.cdo.internal.security.ui.util.ActionBarsHelper;
import org.eclipse.emf.cdo.internal.security.ui.util.ObjectExistsConverter;
import org.eclipse.emf.cdo.internal.security.ui.util.SecurityModelUtil;
import org.eclipse.emf.cdo.internal.security.ui.util.TableLabelProvider;
import org.eclipse.emf.cdo.security.Directory;
import org.eclipse.emf.cdo.security.Realm;
import org.eclipse.emf.cdo.security.SecurityFactory;
import org.eclipse.emf.cdo.security.SecurityPackage;
import org.eclipse.emf.cdo.ui.shared.SharedIcons;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.command.CommandActionDelegate;
import org.eclipse.emf.edit.command.CommandParameter;
import org.eclipse.emf.edit.command.CreateChildCommand;
import org.eclipse.emf.edit.command.DeleteCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryContentProvider;
import org.eclipse.emf.edit.ui.provider.ExtendedImageRegistry;

import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import java.util.Collections;

/**
 * 
 */
public abstract class TableSection<T extends EObject> extends AbstractSectionPart<Directory>
{

  private final Class<T> elementType;

  private final EClass elementEClass;

  private TableViewer viewer;

  public TableSection(Class<T> elementType, EClass elementEClass, EditingDomain domain, AdapterFactory adapterFactory)
  {
    super(Directory.class, SecurityPackage.Literals.DIRECTORY, domain, adapterFactory);

    this.elementType = elementType;
    this.elementEClass = elementEClass;
  }

  @Override
  protected void createContents(Composite parent, FormToolkit toolkit)
  {
    parent.setLayout(new GridLayout());
    Table table = toolkit.createTable(parent, SWT.H_SCROLL | SWT.V_SCROLL);
    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    viewer = new TableViewer(table);

    viewer.setContentProvider(new AdapterFactoryContentProvider(getAdapterFactory()));
    viewer.setLabelProvider(new TableLabelProvider(getAdapterFactory()));
    addFilters(viewer);
    forwardSelection(viewer);

    getContext().bindValue(ViewersObservables.observeInput(viewer), getValue());

    configureDragSupport(viewer);
  }

  @Override
  public void setFocus()
  {
    if (viewer != null)
    {
      viewer.getControl().setFocus();
    }
    else
    {
      super.setFocus();
    }
  }

  protected void addFilters(TableViewer viewer)
  {
    viewer.addFilter(createTypeFilter(elementEClass));
    SecurityModelUtil.applyDefaultFilters(viewer, elementEClass);
  }

  protected ViewerFilter createTypeFilter(final EClassifier type)
  {
    return new ViewerFilter()
    {

      @Override
      public boolean select(Viewer viewer, Object parentElement, Object element)
      {
        return type.isInstance(element);
      }
    };
  }

  @Override
  public boolean setFormInput(Object input)
  {
    if (elementType.isInstance(input))
    {
      viewer.setSelection(new StructuredSelection(input), true);
      return true;
    }
    else if (input instanceof Directory && input == getDirectory(((Directory)input).getRealm()))
    {
      // it's my directory
      boolean result = super.setFormInput(input);

      checkForUnsupportedModelContent();

      return result;
    }
    else if (input instanceof Realm)
    {
      return setFormInput(getDirectory((Realm)input));
    }

    return false;
  }

  protected Directory getDirectory(Realm realm)
  {
    return SecurityModelUtil.getDirectory(realm, elementEClass);
  }

  @Override
  protected void createActionToolbar(Section section, FormToolkit toolkit)
  {
    ToolBarManager mgr = new ToolBarManager(SWT.FLAT);
    ToolBar toolbar = mgr.createControl(section);
    toolbar.setCursor(section.getDisplay().getSystemCursor(SWT.CURSOR_HAND));

    mgr.add(createAddNewAction());

    IAction deleteAction = createDeleteAction();
    mgr.add(deleteAction);
    if (deleteAction instanceof ISelectionChangedListener)
    {
      ISelectionChangedListener scl = (ISelectionChangedListener)deleteAction;
      viewer.addSelectionChangedListener(scl);
      scl.selectionChanged(new SelectionChangedEvent(viewer, viewer.getSelection()));
    }

    mgr.update(true);
    section.setTextClient(toolbar);

    new ActionBarsHelper(getEditorActionBars()).addGlobalAction(ActionFactory.DELETE.getId(), deleteAction).install(
        viewer);
  }

  protected IAction createAddNewAction()
  {
    Command dummy = createCreateNewCommand();
    ImageDescriptor image = null;

    if (dummy instanceof CommandActionDelegate)
    {
      image = ExtendedImageRegistry.getInstance().getImageDescriptor(((CommandActionDelegate)dummy).getImage());
    }

    IAction result = new Action(dummy.getLabel(), image)
    {
      @Override
      public void run()
      {
        final Command command = createCreateNewCommand();
        if (command.canExecute())
        {
          getEditingDomain().getCommandStack().execute(command);
          viewer.getControl().getDisplay().asyncExec(new Runnable()
          {

            public void run()
            {
              viewer.getControl().setFocus();
              viewer.setSelection(new StructuredSelection(command.getResult().toArray()));
            }
          });
        }
      }
    };

    getContext().bindValue(PojoObservables.observeValue(getContext().getValidationRealm(), result, "enabled"), //$NON-NLS-1$
        getValue(), null, ObjectExistsConverter.createUpdateValueStrategy());

    return result;
  }

  protected Command createCreateNewCommand()
  {
    Object input = viewer.getInput();
    Directory parent = input instanceof Directory ? (Directory)input : SecurityFactory.eINSTANCE.createDirectory();
    Object child = EcoreUtil.create(elementEClass);
    CommandParameter param = new CommandParameter(parent, SecurityPackage.Literals.DIRECTORY__ITEMS, child);
    return CreateChildCommand.create(getEditingDomain(), parent, param, Collections.singleton(parent));
  }

  protected IAction createDeleteAction()
  {
    Command dummy = createDeleteCommand(EcoreUtil.create(elementEClass));

    return new SelectionListenerAction(dummy.getLabel(), SharedIcons.getDescriptor("etool16/delete.gif")) //$NON-NLS-1$
    {
      @Override
      public void run()
      {
        Command delete = createDeleteCommand(getSelectedObject());
        if (delete.canExecute())
        {
          getEditingDomain().getCommandStack().execute(delete);
        }
      }

      @Override
      protected boolean updateSelection(IStructuredSelection selection)
      {
        return super.updateSelection(selection) && SecurityModelUtil.isEditable(getInput());
      }
    };
  }

  protected Command createDeleteCommand(EObject toDelete)
  {
    return DeleteCommand.create(getEditingDomain(), toDelete);
  }

  private void forwardSelection(StructuredViewer viewer)
  {
    viewer.addSelectionChangedListener(new ISelectionChangedListener()
    {

      public void selectionChanged(SelectionChangedEvent event)
      {
        IManagedForm form = getManagedForm();
        if (form != null)
        {
          form.fireSelectionChanged(TableSection.this, event.getSelection());
        }
      }
    });
  }

  protected void configureDragSupport(final TableViewer viewer)
  {
    viewer.addDragSupport(DND.DROP_LINK | DND.DROP_MOVE | DND.DROP_COPY,
        new Transfer[] { LocalSelectionTransfer.getTransfer() }, new DragSourceAdapter()
        {
          private long lastDragTime;

          @Override
          public void dragStart(DragSourceEvent event)
          {
            lastDragTime = System.currentTimeMillis();
            LocalSelectionTransfer.getTransfer().setSelection(viewer.getSelection());
            LocalSelectionTransfer.getTransfer().setSelectionSetTime(lastDragTime);
          }

          @Override
          public void dragFinished(DragSourceEvent event)
          {
            if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == lastDragTime)
            {
              LocalSelectionTransfer.getTransfer().setSelection(null);
            }
          }
        });
  }

  protected void checkForUnsupportedModelContent()
  {
    if (getInput() == null)
    {
      getManagedForm().getMessageManager().addMessage(this, Messages.TableSection_2, null, IStatus.WARNING,
          viewer.getControl());
    }
    else
    {
      // anything not matching filters?
      if (viewer.getTable().getItemCount() < getInput().getItems().size())
      {
        getManagedForm().getMessageManager().addMessage(this, Messages.TableSection_3, null, IStatus.WARNING,
            viewer.getControl());
      }
    }
  }
}
