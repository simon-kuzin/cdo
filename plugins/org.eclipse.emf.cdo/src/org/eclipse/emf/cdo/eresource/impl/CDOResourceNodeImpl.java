/**
 * <copyright>
 * </copyright>
 *
 * $Id: CDOResourceNodeImpl.java,v 1.1.2.1 2008-10-14 20:39:30 estepper Exp $
 */
package org.eclipse.emf.cdo.eresource.impl;

import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.eresource.CDOResourceNode;
import org.eclipse.emf.cdo.eresource.EresourcePackage;

import org.eclipse.emf.internal.cdo.CDOObjectImpl;

import org.eclipse.emf.ecore.EClass;

/**
 * <!-- begin-user-doc --> An implementation of the model object '<em><b>CDO Resource Node</b></em>'. <!-- end-user-doc
 * -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.emf.cdo.eresource.impl.CDOResourceNodeImpl#getFolder <em>Folder</em>}</li>
 * <li>{@link org.eclipse.emf.cdo.eresource.impl.CDOResourceNodeImpl#getName <em>Name</em>}</li>
 * <li>{@link org.eclipse.emf.cdo.eresource.impl.CDOResourceNodeImpl#getPath <em>Path</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 * @since 2.0
 */
public abstract class CDOResourceNodeImpl extends CDOObjectImpl implements CDOResourceNode
{
  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected CDOResourceNodeImpl()
  {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass()
  {
    return EresourcePackage.Literals.CDO_RESOURCE_NODE;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected int eStaticFeatureCount()
  {
    return 0;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public CDOResourceFolder getFolder()
  {
    return (CDOResourceFolder)eGet(EresourcePackage.Literals.CDO_RESOURCE_NODE__FOLDER, true);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setFolder(CDOResourceFolder newFolder)
  {
    eSet(EresourcePackage.Literals.CDO_RESOURCE_NODE__FOLDER, newFolder);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getName()
  {
    return (String)eGet(EresourcePackage.Literals.CDO_RESOURCE_NODE__NAME, true);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setName(String newName)
  {
    eSet(EresourcePackage.Literals.CDO_RESOURCE_NODE__NAME, newName);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated NOT
   */
  public String getPath()
  {
    CDOResourceFolder folder = getFolder();
    if (folder == null)
    {
      return "/" + getName();
    }

    return folder.getPath() + "/" + getName();
  }
} // CDOResourceNodeImpl
