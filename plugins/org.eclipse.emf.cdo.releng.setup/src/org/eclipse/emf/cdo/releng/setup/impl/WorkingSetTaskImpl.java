/**
 */
package org.eclipse.emf.cdo.releng.setup.impl;

import org.eclipse.emf.cdo.releng.internal.setup.Activator;
import org.eclipse.emf.cdo.releng.setup.SetupPackage;
import org.eclipse.emf.cdo.releng.setup.SetupTaskContext;
import org.eclipse.emf.cdo.releng.setup.WorkingSetTask;
import org.eclipse.emf.cdo.releng.workingsets.WorkingSetGroup;
import org.eclipse.emf.cdo.releng.workingsets.util.WorkingSetsUtil;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.resource.Resource;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import java.lang.reflect.Method;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Set Working Task</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.eclipse.emf.cdo.releng.setup.impl.WorkingSetTaskImpl#getWorkingSetGroup <em>Working Set Group</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class WorkingSetTaskImpl extends OneTimeSetupTaskImpl implements WorkingSetTask
{
  private static final String PACKAGE_EXPLORER_ID = "org.eclipse.jdt.ui.PackageExplorer";

  /**
   * The cached value of the '{@link #getWorkingSetGroup() <em>Working Set Group</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getWorkingSetGroup()
   * @generated
   * @ordered
   */
  protected WorkingSetGroup workingSetGroup;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected WorkingSetTaskImpl()
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
    return SetupPackage.Literals.WORKING_SET_TASK;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public WorkingSetGroup getWorkingSetGroup()
  {
    if (workingSetGroup != null && workingSetGroup.eIsProxy())
    {
      InternalEObject oldWorkingSetGroup = (InternalEObject)workingSetGroup;
      workingSetGroup = (WorkingSetGroup)eResolveProxy(oldWorkingSetGroup);
      if (workingSetGroup != oldWorkingSetGroup)
      {
        InternalEObject newWorkingSetGroup = (InternalEObject)workingSetGroup;
        NotificationChain msgs = oldWorkingSetGroup.eInverseRemove(this, EOPPOSITE_FEATURE_BASE
            - SetupPackage.WORKING_SET_TASK__WORKING_SET_GROUP, null, null);
        if (newWorkingSetGroup.eInternalContainer() == null)
        {
          msgs = newWorkingSetGroup.eInverseAdd(this, EOPPOSITE_FEATURE_BASE
              - SetupPackage.WORKING_SET_TASK__WORKING_SET_GROUP, null, msgs);
        }
        if (msgs != null)
          msgs.dispatch();
        if (eNotificationRequired())
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, SetupPackage.WORKING_SET_TASK__WORKING_SET_GROUP,
              oldWorkingSetGroup, workingSetGroup));
      }
    }
    return workingSetGroup;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public WorkingSetGroup basicGetWorkingSetGroup()
  {
    return workingSetGroup;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetWorkingSetGroup(WorkingSetGroup newWorkingSetGroup, NotificationChain msgs)
  {
    WorkingSetGroup oldWorkingSetGroup = workingSetGroup;
    workingSetGroup = newWorkingSetGroup;
    if (eNotificationRequired())
    {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET,
          SetupPackage.WORKING_SET_TASK__WORKING_SET_GROUP, oldWorkingSetGroup, newWorkingSetGroup);
      if (msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setWorkingSetGroup(WorkingSetGroup newWorkingSetGroup)
  {
    if (newWorkingSetGroup != workingSetGroup)
    {
      NotificationChain msgs = null;
      if (workingSetGroup != null)
        msgs = ((InternalEObject)workingSetGroup).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
            - SetupPackage.WORKING_SET_TASK__WORKING_SET_GROUP, null, msgs);
      if (newWorkingSetGroup != null)
        msgs = ((InternalEObject)newWorkingSetGroup).eInverseAdd(this, EOPPOSITE_FEATURE_BASE
            - SetupPackage.WORKING_SET_TASK__WORKING_SET_GROUP, null, msgs);
      msgs = basicSetWorkingSetGroup(newWorkingSetGroup, msgs);
      if (msgs != null)
        msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, SetupPackage.WORKING_SET_TASK__WORKING_SET_GROUP,
          newWorkingSetGroup, newWorkingSetGroup));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs)
  {
    switch (featureID)
    {
    case SetupPackage.WORKING_SET_TASK__WORKING_SET_GROUP:
      return basicSetWorkingSetGroup(null, msgs);
    }
    return super.eInverseRemove(otherEnd, featureID, msgs);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType)
  {
    switch (featureID)
    {
    case SetupPackage.WORKING_SET_TASK__WORKING_SET_GROUP:
      if (resolve)
        return getWorkingSetGroup();
      return basicGetWorkingSetGroup();
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public void eSet(int featureID, Object newValue)
  {
    switch (featureID)
    {
    case SetupPackage.WORKING_SET_TASK__WORKING_SET_GROUP:
      setWorkingSetGroup((WorkingSetGroup)newValue);
      return;
    }
    super.eSet(featureID, newValue);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public void eUnset(int featureID)
  {
    switch (featureID)
    {
    case SetupPackage.WORKING_SET_TASK__WORKING_SET_GROUP:
      setWorkingSetGroup((WorkingSetGroup)null);
      return;
    }
    super.eUnset(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public boolean eIsSet(int featureID)
  {
    switch (featureID)
    {
    case SetupPackage.WORKING_SET_TASK__WORKING_SET_GROUP:
      return workingSetGroup != null;
    }
    return super.eIsSet(featureID);
  }

  @Override
  protected void doPerform(SetupTaskContext context) throws Exception
  {
    initPackageExplorer();

    WorkingSetGroup defaultWorkingSetGroup = WorkingSetsUtil.getWorkingSetGroup();
    Resource resource = defaultWorkingSetGroup.eResource();
    resource.getContents().set(0, getWorkingSetGroup());
    resource.save(null);
  }

  private static void initPackageExplorer()
  {
    final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getWorkbenchWindows()[0];
    workbenchWindow.getShell().getDisplay().asyncExec(new Runnable()
    {
      public void run()
      {
        try
        {
          IViewPart view = workbenchWindow.getActivePage().showView(PACKAGE_EXPLORER_ID, null,
              IWorkbenchPage.VIEW_CREATE);
          if (view != null)
          {
            Method method = view.getClass().getMethod("rootModeChanged", int.class);
            method.invoke(view, 2);
          }
        }
        catch (Exception ex)
        {
          Activator.log(ex);
        }
      }
    });
  }
} // SetWorkingTaskImpl
