/**
 */
package org.eclipse.emf.cdo.releng.setup.impl;

import org.eclipse.emf.cdo.releng.internal.setup.Activator;
import org.eclipse.emf.cdo.releng.setup.InstallableUnit;
import org.eclipse.emf.cdo.releng.setup.P2Repository;
import org.eclipse.emf.cdo.releng.setup.P2Task;
import org.eclipse.emf.cdo.releng.setup.SetupPackage;
import org.eclipse.emf.cdo.releng.setup.SetupTaskContext;
import org.eclipse.emf.cdo.releng.setup.SetupTaskScope;
import org.eclipse.emf.cdo.releng.setup.util.FileUtil;
import org.eclipse.emf.cdo.releng.setup.util.log.ProgressLogMonitor;

import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EObjectContainmentWithInverseEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.director.app.DirectorApplication;
import org.eclipse.equinox.internal.p2.director.app.ILog;

import java.util.Collection;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Install Task</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.eclipse.emf.cdo.releng.setup.impl.P2TaskImpl#getP2Repositories <em>P2 Repositories</em>}</li>
 *   <li>{@link org.eclipse.emf.cdo.releng.setup.impl.P2TaskImpl#getInstallableUnits <em>Installable Units</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class P2TaskImpl extends InstallTaskImpl implements P2Task
{
  /**
   * The cached value of the '{@link #getP2Repositories() <em>P2 Repositories</em>}' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getP2Repositories()
   * @generated
   * @ordered
   */
  protected EList<P2Repository> p2Repositories;

  /**
   * The cached value of the '{@link #getInstallableUnits() <em>Installable Units</em>}' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getInstallableUnits()
   * @generated
   * @ordered
   */
  protected EList<InstallableUnit> installableUnits;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected P2TaskImpl()
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
    return SetupPackage.Literals.P2_TASK;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EList<InstallableUnit> getInstallableUnits()
  {
    if (installableUnits == null)
    {
      installableUnits = new EObjectContainmentWithInverseEList.Resolving<InstallableUnit>(InstallableUnit.class, this,
          SetupPackage.P2_TASK__INSTALLABLE_UNITS, SetupPackage.INSTALLABLE_UNIT__P2_TASK);
    }
    return installableUnits;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EList<P2Repository> getP2Repositories()
  {
    if (p2Repositories == null)
    {
      p2Repositories = new EObjectContainmentWithInverseEList.Resolving<P2Repository>(P2Repository.class, this,
          SetupPackage.P2_TASK__P2_REPOSITORIES, SetupPackage.P2_REPOSITORY__P2_TASK);
    }
    return p2Repositories;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @SuppressWarnings("unchecked")
  @Override
  public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs)
  {
    switch (featureID)
    {
    case SetupPackage.P2_TASK__P2_REPOSITORIES:
      return ((InternalEList<InternalEObject>)(InternalEList<?>)getP2Repositories()).basicAdd(otherEnd, msgs);
    case SetupPackage.P2_TASK__INSTALLABLE_UNITS:
      return ((InternalEList<InternalEObject>)(InternalEList<?>)getInstallableUnits()).basicAdd(otherEnd, msgs);
    }
    return super.eInverseAdd(otherEnd, featureID, msgs);
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
    case SetupPackage.P2_TASK__P2_REPOSITORIES:
      return ((InternalEList<?>)getP2Repositories()).basicRemove(otherEnd, msgs);
    case SetupPackage.P2_TASK__INSTALLABLE_UNITS:
      return ((InternalEList<?>)getInstallableUnits()).basicRemove(otherEnd, msgs);
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
    case SetupPackage.P2_TASK__P2_REPOSITORIES:
      return getP2Repositories();
    case SetupPackage.P2_TASK__INSTALLABLE_UNITS:
      return getInstallableUnits();
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @SuppressWarnings("unchecked")
  @Override
  public void eSet(int featureID, Object newValue)
  {
    switch (featureID)
    {
    case SetupPackage.P2_TASK__P2_REPOSITORIES:
      getP2Repositories().clear();
      getP2Repositories().addAll((Collection<? extends P2Repository>)newValue);
      return;
    case SetupPackage.P2_TASK__INSTALLABLE_UNITS:
      getInstallableUnits().clear();
      getInstallableUnits().addAll((Collection<? extends InstallableUnit>)newValue);
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
    case SetupPackage.P2_TASK__P2_REPOSITORIES:
      getP2Repositories().clear();
      return;
    case SetupPackage.P2_TASK__INSTALLABLE_UNITS:
      getInstallableUnits().clear();
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
    case SetupPackage.P2_TASK__P2_REPOSITORIES:
      return p2Repositories != null && !p2Repositories.isEmpty();
    case SetupPackage.P2_TASK__INSTALLABLE_UNITS:
      return installableUnits != null && !installableUnits.isEmpty();
    }
    return super.eIsSet(featureID);
  }

  public boolean isNeeded(SetupTaskContext context) throws Exception
  {
    if (Activator.SETUP_IDE)
    {
      // TODO Do more detailed analysis
      return false;
    }

    return true;
  }

  @Override
  protected void doPerform(SetupTaskContext context) throws Exception
  {
    if (Activator.SETUP_IDE)
    {
      // TODO Install into the running IDE
      return;
    }

    callDirectorApp(context);
  }

  private void callDirectorApp(final SetupTaskContext context) throws Exception
  {
    if (getScope() == SetupTaskScope.CONFIGURATION)
    {
      FileUtil.delete(context.getP2ProfileDir(), new ProgressLogMonitor(context));
    }

    String destination = context.getEclipseDir().toString();
    String bundlePool = context.getP2PoolDir().toString();
    String bundleAgent = context.getP2AgentDir().toString();

    String os = Platform.getOS();
    String ws = Platform.getWS();
    String arch = Platform.getOSArch();

    EList<P2Repository> p2Repositories = getP2Repositories();
    EList<InstallableUnit> installableUnits = getInstallableUnits();

    context.log("Calling director to install " + installableUnits.size()
        + (installableUnits.size() == 1 ? " unit" : " units") + " from " + p2Repositories.size()
        + (p2Repositories.size() == 1 ? " repository" : " repositories") + " to " + destination);

    String repositories = makeList(p2Repositories, SetupPackage.Literals.P2_REPOSITORY__URL);
    String ius = makeList(installableUnits, SetupPackage.Literals.INSTALLABLE_UNIT__ID);

    String[] args = { "-destination", destination, "-repository", repositories, "-installIU", ius, "-profile",
        context.getP2ProfileName(), "-profileProperties", "org.eclipse.update.install.features=true", "-bundlepool",
        bundlePool, "-shared", bundleAgent, "-p2.os", os, "-p2.ws", ws, "-p2.arch", arch };

    DirectorApplication app = new DirectorApplication();
    app.setLog(new ILog()
    {
      public void log(String message)
      {
        if (context.isCancelled())
        {
          throw new OperationCanceledException();
        }

        context.log(message);
      }

      public void log(IStatus status)
      {
        log(status.getMessage());
      }

      public void close()
      {
      }
    });

    app.run(args);
  }

  private String makeList(EList<? extends EObject> objects, EAttribute attribute)
  {
    StringBuilder builder = new StringBuilder();
    for (EObject object : objects)
    {
      if (builder.length() > 0)
      {
        builder.append(',');
      }

      Object value = object.eGet(attribute);
      builder.append(value);
    }

    return builder.toString();
  }

} // InstallTaskImpl
