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
package org.eclipse.emf.cdo.releng.setup;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Eclipse Version</b></em>'.
 * @extends SetupTaskContainer
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.eclipse.emf.cdo.releng.setup.EclipseVersion#getConfiguration <em>Configuration</em>}</li>
 *   <li>{@link org.eclipse.emf.cdo.releng.setup.EclipseVersion#getVersion <em>Version</em>}</li>
 *   <li>{@link org.eclipse.emf.cdo.releng.setup.EclipseVersion#getInstallTasks <em>Install Tasks</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.eclipse.emf.cdo.releng.setup.SetupPackage#getEclipseVersion()
 * @model
 * @generated
 */
public interface EclipseVersion extends EObject, SetupTaskContainer
{
  /**
   * Returns the value of the '<em><b>Configuration</b></em>' container reference.
   * It is bidirectional and its opposite is '{@link org.eclipse.emf.cdo.releng.setup.Configuration#getEclipseVersions <em>Eclipse Versions</em>}'.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Configuration</em>' container reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Configuration</em>' container reference.
   * @see #setConfiguration(Configuration)
   * @see org.eclipse.emf.cdo.releng.setup.SetupPackage#getEclipseVersion_Configuration()
   * @see org.eclipse.emf.cdo.releng.setup.Configuration#getEclipseVersions
   * @model opposite="eclipseVersions" transient="false"
   * @generated
   */
  Configuration getConfiguration();

  /**
   * Sets the value of the '{@link org.eclipse.emf.cdo.releng.setup.EclipseVersion#getConfiguration <em>Configuration</em>}' container reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Configuration</em>' container reference.
   * @see #getConfiguration()
   * @generated
   */
  void setConfiguration(Configuration value);

  /**
   * Returns the value of the '<em><b>Version</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Version</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Version</em>' attribute.
   * @see #setVersion(String)
   * @see org.eclipse.emf.cdo.releng.setup.SetupPackage#getEclipseVersion_Version()
   * @model
   * @generated
   */
  String getVersion();

  /**
   * Sets the value of the '{@link org.eclipse.emf.cdo.releng.setup.EclipseVersion#getVersion <em>Version</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Version</em>' attribute.
   * @see #getVersion()
   * @generated
   */
  void setVersion(String value);

  /**
   * Returns the value of the '<em><b>Install Tasks</b></em>' containment reference list.
   * The list contents are of type {@link org.eclipse.emf.cdo.releng.setup.InstallTask}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Install Tasks</em>' containment reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Install Tasks</em>' containment reference list.
   * @see org.eclipse.emf.cdo.releng.setup.SetupPackage#getEclipseVersion_InstallTasks()
   * @model containment="true" resolveProxies="true" required="true"
   * @generated
   */
  EList<InstallTask> getInstallTasks();

} // EclipseVersion
