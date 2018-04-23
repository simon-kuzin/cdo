/*
 * Copyright (c) 2008-2012, 2015, 2016 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.eresource.impl;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDOList;
import org.eclipse.emf.cdo.eresource.CDOBinaryResource;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.eresource.CDOResourceNode;
import org.eclipse.emf.cdo.eresource.CDOTextResource;
import org.eclipse.emf.cdo.eresource.EresourcePackage;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.util.CDOURIUtil;

import org.eclipse.net4j.util.ObjectUtil;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.spi.cdo.FSMUtil;
import org.eclipse.emf.spi.cdo.InternalCDOTransaction;
import org.eclipse.emf.spi.cdo.InternalCDOView;

import java.io.IOException;
import java.util.Map;

/**
 * <!-- begin-user-doc --> An implementation of the model object '<em><b>CDO Resource Folder</b></em>'.
 *
 * @since 2.0
 * @noextend This interface is not intended to be extended by clients. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.emf.cdo.eresource.impl.CDOResourceFolderImpl#getNodes <em>Nodes</em>}</li>
 * </ul>
 *
 * @generated
 */
public class CDOResourceFolderImpl extends CDOResourceNodeImpl implements CDOResourceFolder
{
  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected CDOResourceFolderImpl()
  {
    super();
  }

  /**
   * @ADDED
   */
  public boolean isRoot()
  {
    return false;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  protected EClass eStaticClass()
  {
    return EresourcePackage.Literals.CDO_RESOURCE_FOLDER;
  }

  /**
   * @since 4.5
   */
  @Override
  public void recacheURIs()
  {
    InternalCDORevision revision = cdoRevision(false);
    if (revision != null)
    {
      CDOList list;
      boolean bypassPermissionChecks = revision.bypassPermissionChecks(true);

      try
      {
        list = revision.getListOrNull(EresourcePackage.Literals.CDO_RESOURCE_FOLDER__NODES);
      }
      finally
      {
        revision.bypassPermissionChecks(bypassPermissionChecks);
      }

      if (list != null)
      {
        InternalCDOView view = cdoView();

        for (Object value : list)
        {
          if (value instanceof CDOID)
          {
            CDOID id = (CDOID)value;
            value = view.getObject(id, false);
          }

          if (value instanceof CDOResourceNodeImpl)
          {
            CDOResourceNodeImpl child = (CDOResourceNodeImpl)value;
            child.recacheURIs();
          }
        }
      }
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @SuppressWarnings("unchecked")
  public EList<CDOResourceNode> getNodes()
  {
    return (EList<CDOResourceNode>)eGet(EresourcePackage.Literals.CDO_RESOURCE_FOLDER__NODES, true);
  }

  /**
   * @ADDED
   * @since 4.4
   */
  public CDOResourceNode getNode(String name)
  {
    for (CDOResourceNode resourceNode : getNodes())
    {
      if (ObjectUtil.equals(resourceNode.getName(), name))
      {
        return resourceNode;
      }
    }

    return null;
  }

  /**
   * <!-- begin-user-doc -->
   * @since 4.0
   * <!-- end-user-doc -->
   * @generated NOT
   */
  public CDOResourceFolder addResourceFolder(String name)
  {
    return cdoView().toTransaction().createResourceFolder(getPath() + CDOURIUtil.SEGMENT_SEPARATOR + name);
  }

  /**
   * <!-- begin-user-doc -->
   * @since 4.0
   * <!-- end-user-doc -->
   * @generated NOT
   */
  public CDOResource addResource(String name)
  {
    InternalCDOTransaction transaction = cdoView().toTransaction();
    return transaction.createResource(getPath() + CDOURIUtil.SEGMENT_SEPARATOR + name);
  }

  /**
   * <!-- begin-user-doc -->
   * @since 4.2
   * <!-- end-user-doc -->
   * @generated
   */
  public CDOTextResource addTextResource(String name)
  {
    // TODO: implement this method
    // Ensure that you remove @generated or mark it @generated NOT
    throw new UnsupportedOperationException();
  }

  /**
   * <!-- begin-user-doc -->
   * @since 4.2
   * <!-- end-user-doc -->
   * @generated
   */
  public CDOBinaryResource addBinaryResource(String name)
  {
    // TODO: implement this method
    // Ensure that you remove @generated or mark it @generated NOT
    throw new UnsupportedOperationException();
  }

  /**
   * @ADDED
   */
  public void delete(Map<?, ?> options) throws IOException
  {
    if (!FSMUtil.isTransient(this))
    {
      if (getFolder() == null)
      {
        InternalCDOView view = cdoView();
        view.getRootResource().getContents().remove(this);
      }
      else
      {
        basicSetFolder(null, false);
      }
    }
  }
} // CDOResourceFolderImpl
