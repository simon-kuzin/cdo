/**
 */
package org.eclipse.emf.cdo.releng.setup;

import org.eclipse.emf.cdo.releng.workingsets.WorkingSetGroup;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Set Working Task</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.eclipse.emf.cdo.releng.setup.WorkingSetTask#getWorkingSetGroup <em>Working Set Group</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.eclipse.emf.cdo.releng.setup.SetupPackage#getWorkingSetTask()
 * @model
 * @generated
 */
public interface WorkingSetTask extends OneTimeSetupTask
{
  /**
   * Returns the value of the '<em><b>Working Set Group</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Working Set Group</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Working Set Group</em>' containment reference.
   * @see #setWorkingSetGroup(WorkingSetGroup)
   * @see org.eclipse.emf.cdo.releng.setup.SetupPackage#getWorkingSetTask_WorkingSetGroup()
   * @model containment="true" resolveProxies="true" required="true"
   * @generated
   */
  WorkingSetGroup getWorkingSetGroup();

  /**
   * Sets the value of the '{@link org.eclipse.emf.cdo.releng.setup.WorkingSetTask#getWorkingSetGroup <em>Working Set Group</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Working Set Group</em>' containment reference.
   * @see #getWorkingSetGroup()
   * @generated
   */
  void setWorkingSetGroup(WorkingSetGroup value);

} // SetWorkingTask
