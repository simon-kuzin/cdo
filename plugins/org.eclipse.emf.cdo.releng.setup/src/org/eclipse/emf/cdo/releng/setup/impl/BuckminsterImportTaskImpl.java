/**
 */
package org.eclipse.emf.cdo.releng.setup.impl;

import org.eclipse.emf.cdo.releng.setup.BuckminsterImportTask;
import org.eclipse.emf.cdo.releng.setup.SetupPackage;
import org.eclipse.emf.cdo.releng.setup.SetupTaskContext;
import org.eclipse.emf.cdo.releng.setup.util.log.ProgressLogMonitor;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.buckminster.core.CorePlugin;
import org.eclipse.buckminster.core.materializer.MaterializationContext;
import org.eclipse.buckminster.core.materializer.MaterializationJob;
import org.eclipse.buckminster.core.metadata.model.BillOfMaterials;
import org.eclipse.buckminster.core.mspec.builder.MaterializationSpecBuilder;
import org.eclipse.buckminster.core.mspec.model.MaterializationSpec;
import org.eclipse.buckminster.core.parser.IParser;
import org.eclipse.buckminster.core.query.model.ComponentQuery;
import org.eclipse.buckminster.core.resolver.IResolver;
import org.eclipse.buckminster.core.resolver.MainResolver;
import org.eclipse.buckminster.core.resolver.ResolutionContext;
import org.eclipse.buckminster.download.DownloadManager;
import org.eclipse.buckminster.runtime.MonitorUtils;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Buckminster Import Task</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.eclipse.emf.cdo.releng.setup.impl.BuckminsterImportTaskImpl#getMspec <em>Mspec</em>}</li>
 *   <li>{@link org.eclipse.emf.cdo.releng.setup.impl.BuckminsterImportTaskImpl#getTargetPlatform <em>Target Platform</em>}</li>
 *   <li>{@link org.eclipse.emf.cdo.releng.setup.impl.BuckminsterImportTaskImpl#getBundlePool <em>Bundle Pool</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class BuckminsterImportTaskImpl extends OneTimeSetupTaskImpl implements BuckminsterImportTask
{
  /**
   * The default value of the '{@link #getMspec() <em>Mspec</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getMspec()
   * @generated
   * @ordered
   */
  protected static final String MSPEC_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getMspec() <em>Mspec</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getMspec()
   * @generated
   * @ordered
   */
  protected String mspec = MSPEC_EDEFAULT;

  /**
   * The default value of the '{@link #getTargetPlatform() <em>Target Platform</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getTargetPlatform()
   * @generated
   * @ordered
   */
  protected static final String TARGET_PLATFORM_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getTargetPlatform() <em>Target Platform</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getTargetPlatform()
   * @generated
   * @ordered
   */
  protected String targetPlatform = TARGET_PLATFORM_EDEFAULT;

  /**
   * The default value of the '{@link #getBundlePool() <em>Bundle Pool</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getBundlePool()
   * @generated
   * @ordered
   */
  protected static final String BUNDLE_POOL_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getBundlePool() <em>Bundle Pool</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getBundlePool()
   * @generated
   * @ordered
   */
  protected String bundlePool = BUNDLE_POOL_EDEFAULT;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected BuckminsterImportTaskImpl()
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
    return SetupPackage.Literals.BUCKMINSTER_IMPORT_TASK;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getMspec()
  {
    return mspec;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setMspec(String newMspec)
  {
    String oldMspec = mspec;
    mspec = newMspec;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, SetupPackage.BUCKMINSTER_IMPORT_TASK__MSPEC, oldMspec,
          mspec));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getTargetPlatform()
  {
    return targetPlatform;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setTargetPlatform(String newTargetPlatform)
  {
    String oldTargetPlatform = targetPlatform;
    targetPlatform = newTargetPlatform;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, SetupPackage.BUCKMINSTER_IMPORT_TASK__TARGET_PLATFORM,
          oldTargetPlatform, targetPlatform));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getBundlePool()
  {
    return bundlePool;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setBundlePool(String newBundlePool)
  {
    String oldBundlePool = bundlePool;
    bundlePool = newBundlePool;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, SetupPackage.BUCKMINSTER_IMPORT_TASK__BUNDLE_POOL,
          oldBundlePool, bundlePool));
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
    case SetupPackage.BUCKMINSTER_IMPORT_TASK__MSPEC:
      return getMspec();
    case SetupPackage.BUCKMINSTER_IMPORT_TASK__TARGET_PLATFORM:
      return getTargetPlatform();
    case SetupPackage.BUCKMINSTER_IMPORT_TASK__BUNDLE_POOL:
      return getBundlePool();
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
    case SetupPackage.BUCKMINSTER_IMPORT_TASK__MSPEC:
      setMspec((String)newValue);
      return;
    case SetupPackage.BUCKMINSTER_IMPORT_TASK__TARGET_PLATFORM:
      setTargetPlatform((String)newValue);
      return;
    case SetupPackage.BUCKMINSTER_IMPORT_TASK__BUNDLE_POOL:
      setBundlePool((String)newValue);
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
    case SetupPackage.BUCKMINSTER_IMPORT_TASK__MSPEC:
      setMspec(MSPEC_EDEFAULT);
      return;
    case SetupPackage.BUCKMINSTER_IMPORT_TASK__TARGET_PLATFORM:
      setTargetPlatform(TARGET_PLATFORM_EDEFAULT);
      return;
    case SetupPackage.BUCKMINSTER_IMPORT_TASK__BUNDLE_POOL:
      setBundlePool(BUNDLE_POOL_EDEFAULT);
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
    case SetupPackage.BUCKMINSTER_IMPORT_TASK__MSPEC:
      return MSPEC_EDEFAULT == null ? mspec != null : !MSPEC_EDEFAULT.equals(mspec);
    case SetupPackage.BUCKMINSTER_IMPORT_TASK__TARGET_PLATFORM:
      return TARGET_PLATFORM_EDEFAULT == null ? targetPlatform != null : !TARGET_PLATFORM_EDEFAULT
          .equals(targetPlatform);
    case SetupPackage.BUCKMINSTER_IMPORT_TASK__BUNDLE_POOL:
      return BUNDLE_POOL_EDEFAULT == null ? bundlePool != null : !BUNDLE_POOL_EDEFAULT.equals(bundlePool);
    }
    return super.eIsSet(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public String toString()
  {
    if (eIsProxy())
      return super.toString();

    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (mspec: ");
    result.append(mspec);
    result.append(", targetPlatform: ");
    result.append(targetPlatform);
    result.append(", bundlePool: ");
    result.append(bundlePool);
    result.append(')');
    return result.toString();
  }

  @Override
  protected void doPerform(SetupTaskContext context) throws Exception
  {
    IProgressMonitor monitor = new ProgressLogMonitor(context);
    monitor.beginTask(null, 80);

    try
    {
      URL mSpecURL = new URL(context.expandString(getMspec()));
      MaterializationSpec mspec = getMSpec(mSpecURL, monitor); // 20 ticks
      ComponentQuery cquery = getCQuery(mspec.getResolvedURL(), monitor); // 20 ticks

      IResolver resolver = new MainResolver(new ResolutionContext(mspec, cquery));
      resolver.getContext().setContinueOnError(true);

      BillOfMaterials bom = resolver.resolve(MonitorUtils.subMonitor(monitor, 40));

      MaterializationSpecBuilder mspecBuilder = new MaterializationSpecBuilder();
      mspecBuilder.initFrom(mspec);
      mspecBuilder.setName(bom.getViewName());

      bom.addMaterializationNodes(mspecBuilder);

      ResolutionContext resolutionContext = new ResolutionContext(bom.getQuery());
      MaterializationContext materializationContext = new MaterializationContext(bom, mspec, resolutionContext);

      MaterializationJob job = new MaterializationJob(materializationContext);
      job.schedule();
    }
    finally
    {
      monitor.done();
    }
  }

  private MaterializationSpec getMSpec(URL mspecURL, IProgressMonitor monitor) throws Exception
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DownloadManager.readInto(mspecURL, null, baos, MonitorUtils.subMonitor(monitor, 20));

    IParser<MaterializationSpec> parser = CorePlugin.getDefault().getParserFactory().getMaterializationSpecParser(true);
    return parser.parse(mspecURL.toString(), new ByteArrayInputStream(baos.toByteArray()));
  }

  private ComponentQuery getCQuery(URL cqueryURL, IProgressMonitor monitor) throws Exception
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DownloadManager.readInto(cqueryURL, null, baos, MonitorUtils.subMonitor(monitor, 20));

    return ComponentQuery.fromStream(cqueryURL, null, new ByteArrayInputStream(baos.toByteArray()), true);
  }

} // BuckminsterImportTaskImpl
