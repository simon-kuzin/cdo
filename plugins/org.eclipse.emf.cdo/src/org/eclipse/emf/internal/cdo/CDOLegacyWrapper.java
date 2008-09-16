/***************************************************************************
 * Copyright (c) 2004 - 2008 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.internal.cdo;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.CDOState;
import org.eclipse.emf.cdo.common.CDOProtocolConstants;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.model.CDOClass;
import org.eclipse.emf.cdo.common.model.CDOFeature;
import org.eclipse.emf.cdo.common.model.CDOType;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.eresource.impl.CDOResourceImpl;
import org.eclipse.emf.cdo.spi.common.InternalCDORevision;
import org.eclipse.emf.cdo.util.CDOPackageRegistry;

import org.eclipse.emf.internal.cdo.bundle.OM;
import org.eclipse.emf.internal.cdo.util.FSMUtil;
import org.eclipse.emf.internal.cdo.util.GenUtil;
import org.eclipse.emf.internal.cdo.util.ModelUtil;

import org.eclipse.net4j.util.ImplementationError;
import org.eclipse.net4j.util.ReflectUtil;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EAttributeImpl;
import org.eclipse.emf.ecore.impl.EClassImpl;
import org.eclipse.emf.ecore.impl.EDataTypeImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.impl.EReferenceImpl;
import org.eclipse.emf.ecore.impl.EStructuralFeatureImpl;
import org.eclipse.emf.ecore.impl.ETypedElementImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.InternalEList;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.List;

/**
 * @author Eike Stepper
 * @since 2.0
 */
public final class CDOLegacyWrapper extends CDOObjectWrapper implements InternalEObject.EReadListener,
    InternalEObject.EWriteListener
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_OBJECT, CDOLegacyWrapper.class);

  private static final Method cdoIDMethod = ReflectUtil.getMethod(CDOObject.class, "cdoID");

  private static final Method cdoStateMethod = ReflectUtil.getMethod(CDOObject.class, "cdoState");

  private static final Method eIsProxyMethod = ReflectUtil.getMethod(EObject.class, "eIsProxy");

  private static final Method eProxyURIMethod = ReflectUtil.getMethod(InternalEObject.class, "eProxyURI");

  private static final Method eSetDirectResourceMethod = ReflectUtil.getMethod(EObjectImpl.class, "eSetDirectResource",
      Resource.Internal.class);

  private static final Method eBasicSetContainerMethod = ReflectUtil.getMethod(EObjectImpl.class, "eBasicSetContainer",
      InternalEObject.class, int.class);

  private CDOState state;

  private CDOResourceImpl resource;

  private InternalCDORevision revision;

  public CDOLegacyWrapper(InternalEObject instance)
  {
    this.instance = instance;
    state = CDOState.TRANSIENT;
  }

  public CDOClass cdoClass()
  {
    return CDOObjectImpl.getCDOClass(this);
  }

  public CDOState cdoState()
  {
    return state;
  }

  public InternalCDORevision cdoRevision()
  {
    return revision;
  }

  public CDOResourceImpl cdoResource()
  {
    return resource;
  }

  public void cdoReload()
  {
    CDOStateMachine.INSTANCE.reload(this);
  }

  public CDOState cdoInternalSetState(CDOState state)
  {
    if (this.state != state)
    {
      if (TRACER.isEnabled())
      {
        TRACER.format("Setting state {0} for {1}", state, this);
      }

      CDOState tmp = this.state;
      this.state = state;
      adjustEProxy();
      return tmp;
    }

    // TODO Detect duplicate cdoInternalSetState() calls
    return null;
  }

  public void cdoInternalSetRevision(CDORevision revision)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Setting revision: {0}", revision);
    }

    this.revision = (InternalCDORevision)revision;
  }

  public void cdoInternalSetResource(CDOResource resource)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Setting resource: {0}", resource);
    }

    this.resource = (CDOResourceImpl)resource;
    // if (resource != null)
    // {
    // transferResourceToInstance(resource);
    // }
  }

  public void cdoInternalPostAttach()
  {
    // TODO Avoid if no adapters in list (eBasicAdapters?)
    // TODO LEGACY Clarify how to intercept adapter addition in the legacy instance
    for (Adapter adapter : eAdapters())
    {
      view.subscribe(this, adapter);
    }
  }

  public void cdoInternalPostDetach()
  {
    // Do nothing
  }

  public void cdoInternalPreCommit()
  {
    instanceToRevision();
    if (cdoState() == CDOState.DIRTY) // NEW is handled in PrepareTransition
    {
      CDORevisionManagerImpl revisionManager = (CDORevisionManagerImpl)revision.getRevisionResolver();
      InternalCDORevision originRevision = revisionManager.getRevisionByVersion(revision.getID(),
          CDORevision.UNCHUNKED, revision.getVersion() - 1, false);
      CDORevisionDelta delta = revision.compare(originRevision);

      // TODO LEGACY Consider to gather the deltas on the fly with noremal EMF change notifications
      cdoView().toTransaction().registerRevisionDelta(delta);
    }
  }

  public void cdoInternalPostLoad()
  {
    // TODO Consider not remebering the revisin after copying it to the instance (spare 1/2 of the space)
    revisionToInstance();
  }

  public void handleRead(InternalEObject object, int featureID)
  {
    CDOStateMachine.INSTANCE.read(this);
  }

  public void handleWrite(InternalEObject object, int featureID)
  {
    CDOStateMachine.INSTANCE.write(this);
  }

  private void instanceToRevision()
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Transfering instance to revision: {0} --> {1}", instance, revision);
    }

    CDOViewImpl view = cdoView();
    if (view == null)
    {
      throw new ImplementationError("view == null");
    }

    // Handle containment
    instanceToRevisionContainment(view);

    // Handle values
    CDOPackageRegistry packageRegistry = cdoView().getSession().getPackageRegistry();
    CDOClass cdoClass = revision.getCDOClass();
    for (CDOFeature feature : cdoClass.getAllFeatures())
    {
      instanceToRevisionFeature(view, feature, packageRegistry);
    }
  }

  private void instanceToRevisionContainment(CDOViewImpl view)
  {
    CDOResource resource = (CDOResource)getInstanceResource(instance);
    revision.setResourceID(resource == null ? CDOID.NULL : resource.cdoID());

    InternalEObject eContainer = getInstanceContainer(instance);
    if (eContainer == null)
    {
      revision.setContainerID(CDOID.NULL);
      revision.setContainingFeatureID(0);
    }
    else
    {
      CDOObject cdoContainer = FSMUtil.adapt(eContainer, view);
      revision.setContainerID(cdoContainer.cdoID());
      revision.setContainingFeatureID(getInstanceContainerFeatureID(instance));
    }
  }

  private void instanceToRevisionFeature(CDOViewImpl view, CDOFeature feature, CDOPackageRegistry packageRegistry)
  {
    Object instanceValue = getInstanceValue(instance, feature, packageRegistry);
    if (feature.isMany())
    {
      List<Object> revisionList = revision.getList(feature); // TODO lazy?
      revisionList.clear();

      if (instanceValue != null)
      {
        if (instanceValue instanceof InternalEList)
        {
          InternalEList<?> instanceList = (InternalEList<?>)instanceValue;
          if (!instanceList.isEmpty())
          {
            for (Iterator<?> it = instanceList.basicIterator(); it.hasNext();)
            {
              Object instanceElement = it.next();
              if (instanceElement != null && feature.isReference())
              {
                instanceElement = view.convertObjectToID(instanceElement);
              }

              revisionList.add(instanceElement);
            }
          }
        }
        else
        {
          throw new ImplementationError("Not an InternalEList: " + instanceValue.getClass().getName());
        }
      }
    }
    else
    {
      if (instanceValue != null && feature.isReference())
      {
        instanceValue = view.convertObjectToID(instanceValue);
      }

      revision.setValue(feature, instanceValue);
    }
  }

  /**
   * TODO Simon: Fix this whole mess ;-)
   */
  private void revisionToInstance()
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Transfering revision to instance: {0} --> {1}", revision, instance);
    }

    CDOViewImpl view = cdoView();
    if (view == null)
    {
      throw new ImplementationError("view == null");
    }

    boolean deliver = instance.eDeliver();
    if (deliver)
    {
      instance.eSetDeliver(false);
    }

    try
    {
      // Handle containment
      revisionToInstanceContainment(view);

      // Handle values
      CDOPackageRegistry packageRegistry = cdoView().getSession().getPackageRegistry();
      CDOClass cdoClass = revision.getCDOClass();
      for (CDOFeature feature : cdoClass.getAllFeatures())
      {
        revisionToInstanceFeature(view, feature, packageRegistry);
      }
    }
    finally
    {
      if (deliver)
      {
        instance.eSetDeliver(true);
      }
    }
  }

  private void revisionToInstanceContainment(CDOViewImpl view)
  {
    CDOID resourceID = revision.getResourceID();
    InternalEObject resource = getEObjectFromPotentialID(view, null, resourceID);
    setInstanceResource((Resource.Internal)resource);

    Object containerID = revision.getContainerID();
    InternalEObject container = getEObjectFromPotentialID(view, null, containerID);
    setInstanceContainer(container, revision.getContainingFeatureID());
  }

  @SuppressWarnings("unchecked")
  private void revisionToInstanceFeature(CDOViewImpl view, CDOFeature feature, CDOPackageRegistry packageRegistry)
  {
    Object value = revision.getValue(feature);
    if (feature.isMany())
    {
      InternalEList<Object> instanceList = (InternalEList<Object>)getInstanceValue(instance, feature, packageRegistry);
      if (instanceList != null)
      {
        clearEList(instanceList);
        if (value != null)
        {
          List<?> revisionList = (List<?>)value;
          if (feature.isReference())
          {
            for (Object element : revisionList)
            {
              element = getEObjectFromPotentialID(view, feature, element);
              instanceList.basicAdd(element, null);
            }
          }
          else
          {
            // TODO Is this only for multi-valued attributes??
            for (Object element : revisionList)
            {
              instanceList.basicAdd(element, null);
            }
          }
        }
      }
    }
    else
    {
      if (feature.isReference())
      {
        value = getEObjectFromPotentialID(view, feature, value);
      }

      setInstanceValue(instance, feature, value);
    }
  }

  /**
   * @param feature
   *          in case that a proxy has to be created the feature that will determine the interface type of the proxy and
   *          that will be used later to resolve the proxy. <code>null</code> indicates that proxy creation will be
   *          avoided!
   */
  private InternalEObject getEObjectFromPotentialID(CDOViewImpl view, CDOFeature feature, Object potentialID)
  {
    if (potentialID instanceof CDOID)
    {
      CDOID id = (CDOID)potentialID;
      if (id.isNull())
      {
        return null;
      }

      boolean loadOnDemand = feature == null;
      potentialID = view.getObject(id, loadOnDemand);
      if (potentialID == null && !loadOnDemand)
      {
        return createProxy(view, feature, id);
      }
    }

    if (potentialID instanceof InternalCDOObject)
    {
      return ((InternalCDOObject)potentialID).cdoInternalInstance();
    }

    return (InternalEObject)potentialID;
  }

  /**
   * Creates and returns a <em>proxy</em> object. The usage of a proxy object is strongly limited. The only guarantee
   * that can be made is that the following methods are callable and will behave in the expected way:
   * <ul>
   * <li>{@link CDOObject#cdoID()} will return the {@link CDOID} of the target object
   * <li>{@link CDOObject#cdoState()} will return {@link CDOState#PROXY PROXY}
   * <li>{@link InternalEObject#eIsProxy()} will return <code>true</code>
   * <li>{@link InternalEObject#eProxyURI()} will return the EMF proxy URI of the target object
   * </ul>
   * Calling any other method on the proxy object will result in an {@link UnsupportedOperationException} being thrown
   * at runtime. Note also that the proxy object might even not be cast to the concrete type of the target object. The
   * proxy can only guaranteed to be of <em>any</em> concrete subtype of the declared type of the given feature.
   * <p>
   * TODO {@link InternalEObject#eResolveProxy(InternalEObject) 
   */
  private InternalEObject createProxy(CDOViewImpl view, CDOFeature feature, CDOID id)
  {
    CDOPackageRegistry packageRegistry = view.getSession().getPackageRegistry();
    EStructuralFeature eFeature = ModelUtil.getEFeature(feature, packageRegistry);
    EClassifier eType = eFeature.getEType();
    Class<?> instanceClass = eType.getInstanceClass();

    Class<?>[] interfaces = { instanceClass, InternalCDOObject.class };
    ClassLoader classLoader = CDOLegacyWrapper.class.getClassLoader();
    LegacyProxyInvocationHandler handler = new LegacyProxyInvocationHandler(id);
    return (InternalEObject)Proxy.newProxyInstance(classLoader, interfaces, handler);
  }

  private Resource.Internal getInstanceResource(InternalEObject instance)
  {
    return instance.eDirectResource();
  }

  private InternalEObject getInstanceContainer(InternalEObject instance)
  {
    return instance.eInternalContainer();
  }

  private int getInstanceContainerFeatureID(InternalEObject instance)
  {
    return instance.eContainerFeatureID();
  }

  private Object getInstanceValue(InternalEObject instance, CDOFeature feature, CDOPackageRegistry packageRegistry)
  {
    EStructuralFeature eFeature = ModelUtil.getEFeature(feature, packageRegistry);
    return instance.eGet(eFeature);
  }

  private void setInstanceResource(Resource.Internal resource)
  {
    ReflectUtil.invokeMethod(eSetDirectResourceMethod, instance, resource);
  }

  private void setInstanceContainer(InternalEObject container, int containerFeatureID)
  {
    ReflectUtil.invokeMethod(eBasicSetContainerMethod, instance, container, containerFeatureID);
  }

  /**
   * TODO Ed: Help to fix whole mess (avoid inverse updates)
   */
  private void setInstanceValue(InternalEObject instance, CDOFeature feature, Object value)
  {
    // TODO Consider EStoreEObjectImpl based objects as well!
    // TODO Don't use Java reflection
    Class<?> instanceClass = instance.getClass();
    String featureName = feature.getName();
    String fieldName = featureName;// TODO safeName()
    Field field = ReflectUtil.getField(instanceClass, fieldName);
    if (field == null && feature.getType() == CDOType.BOOLEAN)
    {
      if (instanceClass.isAssignableFrom(EAttributeImpl.class) || instanceClass.isAssignableFrom(EClassImpl.class)
          || instanceClass.isAssignableFrom(EDataTypeImpl.class)
          || instanceClass.isAssignableFrom(EReferenceImpl.class)
          || instanceClass.isAssignableFrom(EStructuralFeatureImpl.class)
          || instanceClass.isAssignableFrom(ETypedElementImpl.class))
      {
        // *******************************************
        // ID_EFLAG = 1 << 15;
        // *******************************************
        // ABSTRACT_EFLAG = 1 << 8;
        // INTERFACE_EFLAG = 1 << 9;
        // *******************************************
        // SERIALIZABLE_EFLAG = 1 << 8;
        // *******************************************
        // CONTAINMENT_EFLAG = 1 << 15;
        // RESOLVE_PROXIES_EFLAG = 1 << 16;
        // *******************************************
        // CHANGEABLE_EFLAG = 1 << 10;
        // VOLATILE_EFLAG = 1 << 11;
        // TRANSIENT_EFLAG = 1 << 12;
        // UNSETTABLE_EFLAG = 1 << 13;
        // DERIVED_EFLAG = 1 << 14;
        // *******************************************
        // ORDERED_EFLAG = 1 << 8;
        // UNIQUE_EFLAG = 1 << 9;
        // *******************************************

        String flagName = GenUtil.getFeatureUpperName(featureName) + "_EFLAG";
        int flagsMask = getEFlagMask(instanceClass, flagName);

        field = ReflectUtil.getField(instanceClass, "eFlags");
        int flags = (Integer)ReflectUtil.getValue(field, instance);
        boolean on = (Boolean)value;
        if (on)
        {
          flags |= flagsMask; // Add EFlag
        }
        else
        {
          flags &= ~flagsMask; // Remove EFlag
        }

        ReflectUtil.setValue(field, instance, flags);
        return;
      }
    }

    if (field == null)
    {
      throw new ImplementationError("Field not found: " + fieldName);
    }

    ReflectUtil.setValue(field, instance, value);
  }

  private void adjustEProxy()
  {
    // Setting eProxyURI is necessary to prevent content adapters from
    // loading the whole content tree.
    // TODO Does not have the desired effect ;-( see CDOEditor.createModel()
    if (state == CDOState.PROXY)
    {
      if (!instance.eIsProxy())
      {
        URI uri = URI.createURI(CDOProtocolConstants.PROTOCOL_NAME + ":proxy#" + id);
        if (TRACER.isEnabled())
        {
          TRACER.format("Setting proxyURI {0} for {1}", uri, instance);
        }

        instance.eSetProxyURI(uri);
      }
    }
    else
    {
      if (instance.eIsProxy())
      {
        if (TRACER.isEnabled())
        {
          TRACER.format("Unsetting proxyURI for {0}", instance);
        }

        instance.eSetProxyURI(null);
      }
    }
  }

  /**
   * TODO Ed: Fix whole mess ;-)
   */
  private void clearEList(InternalEList<Object> list)
  {
    while (!list.isEmpty())
    {
      Object toBeRemoved = list.basicGet(0);
      list.basicRemove(toBeRemoved, null);
    }
  }

  private static int getEFlagMask(Class<?> instanceClass, String flagName)
  {
    Field field = ReflectUtil.getField(instanceClass, flagName);
    if (!field.isAccessible())
    {
      field.setAccessible(true);
    }

    try
    {
      return (Integer)field.get(null);
    }
    catch (IllegalAccessException ex)
    {
      throw new ImplementationError(ex);
    }
  }

  /**
   * @author Eike Stepper
   */
  private final class LegacyProxyInvocationHandler implements InvocationHandler
  {
    private CDOID id;

    private LegacyProxyInvocationHandler(CDOID id)
    {
      this.id = id;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
      if (method.equals(cdoIDMethod))
      {
        return id;
      }

      if (method.equals(cdoStateMethod))
      {
        return CDOState.PROXY;
      }

      if (method.equals(eIsProxyMethod))
      {
        return true;
      }

      if (method.equals(eProxyURIMethod))
      {
        // Use the resource of the container because it's guaranteed to be in the same CDOView as the resource
        // of the target!
        Resource resource = eResource();

        // TODO Consider using a "fake" Resource implementation. See Resource.getEObject(...)
        return resource.getURI().appendFragment(id.toURIFragment());
      }

      throw new UnsupportedOperationException(method.getName());
    }
  }
}
