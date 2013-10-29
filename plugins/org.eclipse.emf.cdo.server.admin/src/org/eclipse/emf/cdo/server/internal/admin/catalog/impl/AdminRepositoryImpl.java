/*
 * Copyright (c) 2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christian W. Damus (CEA LIST) - initial API and implementation
 */
package org.eclipse.emf.cdo.server.internal.admin.catalog.impl;

import org.eclipse.emf.cdo.common.lob.CDOClob;
import org.eclipse.emf.cdo.server.internal.admin.catalog.AdminRepository;
import org.eclipse.emf.cdo.server.internal.admin.catalog.CatalogPackage;

import org.eclipse.emf.internal.cdo.CDOObjectImpl;

import org.eclipse.emf.ecore.EClass;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Admin Repository</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.eclipse.emf.cdo.server.internal.admin.catalog.impl.AdminRepositoryImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.emf.cdo.server.internal.admin.catalog.impl.AdminRepositoryImpl#getConfigXML <em>Config XML</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class AdminRepositoryImpl extends CDOObjectImpl implements AdminRepository
{
  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected AdminRepositoryImpl()
  {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  protected EClass eStaticClass()
  {
    return CatalogPackage.Literals.ADMIN_REPOSITORY;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  protected int eStaticFeatureCount()
  {
    return 0;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getName()
  {
    return (String)eGet(CatalogPackage.Literals.ADMIN_REPOSITORY__NAME, true);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setName(String newName)
  {
    eSet(CatalogPackage.Literals.ADMIN_REPOSITORY__NAME, newName);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public CDOClob getConfigXML()
  {
    return (CDOClob)eGet(CatalogPackage.Literals.ADMIN_REPOSITORY__CONFIG_XML, true);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setConfigXML(CDOClob newConfigXML)
  {
    eSet(CatalogPackage.Literals.ADMIN_REPOSITORY__CONFIG_XML, newConfigXML);
  }

} // AdminRepositoryImpl
