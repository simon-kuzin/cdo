/*
 * Copyright (c) 2004 - 2011 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Martin Fluegge - initial API and implementation
 */
package org.eclipse.emf.cdo.dawn.graphiti.editors;

import org.eclipse.emf.cdo.dawn.editors.IDawnEditor;
import org.eclipse.emf.cdo.dawn.editors.IDawnEditorSupport;
import org.eclipse.emf.cdo.dawn.util.connection.CDOConnectionUtil;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.view.CDOView;

import org.eclipse.emf.common.ui.URIEditorInput;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.transaction.TransactionalEditingDomain;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.graphiti.ui.editor.DiagramEditorInput;
import org.eclipse.graphiti.ui.internal.services.GraphitiUiInternal;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

/**
 * @author Martin Fluegge
 */
/*
 * TODO remove this suppress warning as soon as I have found a way to workaround the problem that the Graphiti editor
 * which is extended is internal
 */
@SuppressWarnings("restriction")
public class DawnGraphitiDiagramEditor extends DiagramEditor implements IDawnEditor
{
  public static final String ID = "org.eclipse.emf.cdo.dawn.graphiti.editor";

  private IDawnEditorSupport dawnEditorSupport;

  public DawnGraphitiDiagramEditor()
  {
    dawnEditorSupport = new DawnGraphitiEditorSupport(this);
  }

  @Override
  public void init(IEditorSite site, IEditorInput input) throws PartInitException
  {
    if (input instanceof URIEditorInput)
    {
      CDOConnectionUtil.instance.getCurrentSession();
      final URIEditorInput uriInput = (URIEditorInput)input;
      final TransactionalEditingDomain domain = DawnGraphitiDiagramEditorFactory.createResourceSetAndEditingDomain();
      URI diagramFileUri = uriInput.getURI();
      if (diagramFileUri != null)
      {
        // the file's first base node has to be a diagram

        URI diagramUri = GraphitiUiInternal.getEmfService().mapDiagramFileUriToDiagramUri(diagramFileUri);
        input = new DawnGraphitiEditorInput(diagramUri, domain, null, true);
      }
    }

    super.init(site, input);
  }

  @Override
  public void setInput(IEditorInput input)
  {
    super.setInput(input);
    DiagramEditorInput iDawnEditorInput = (DiagramEditorInput)input;

    Resource eResource = iDawnEditorInput.getDiagram().eResource();

    /**
     * TODO check if this can be always done this way and if the view can be canceled from the DawnEditorInput or if
     * there is a better way to put in the view to the editor input.
     */
    if (eResource instanceof CDOResource)
    {
      dawnEditorSupport.setView(((CDOResource)eResource).cdoView());
    }
  }

  @Override
  protected void initializeGraphicalViewer()
  {
    super.initializeGraphicalViewer();
    dawnEditorSupport.registerListeners();
  }

  public CDOView getView()
  {
    return dawnEditorSupport.getView();
  }

  public IDawnEditorSupport getDawnEditorSupport()
  {
    return dawnEditorSupport;
  }

  public String getContributorID()
  {
    return ID;
  }

  @Override
  public boolean isDirty()
  {
    // return super.isDirty() || dawnEditorSupport.isDirty();
    return dawnEditorSupport.isDirty();
  }

  public void setDirty()
  {
    dawnEditorSupport.setDirty(true);
  }

  @Override
  public void doSave(IProgressMonitor monitor)
  {
    dawnEditorSupport.setDirty(false);
    super.doSave(monitor);
  }

  @Override
  public void dispose()
  {
    try
    {
      super.dispose();
    }
    finally
    {
      dawnEditorSupport.close();
    }
  }
}