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
import org.eclipse.emf.internal.cdo.util.GenUtil;
import org.eclipse.emf.internal.cdo.util.ModelUtil;

import org.eclipse.net4j.util.ImplementationError;
import org.eclipse.net4j.util.ReflectUtil;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EAttributeImpl;
import org.eclipse.emf.ecore.impl.EClassImpl;
import org.eclipse.emf.ecore.impl.EDataTypeImpl;
import org.eclipse.emf.ecore.impl.EReferenceImpl;
import org.eclipse.emf.ecore.impl.EStructuralFeatureImpl;
import org.eclipse.emf.ecore.impl.ETypedElementImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.InternalEList;

import java.lang.reflect.Field;
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
      cdoView().toTransaction().registerRevisionDelta(delta);
    }
  }

  public void cdoInternalPostLoad()
  {
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

  @Override
  public NotificationChain eSetResource(Resource.Internal resource, NotificationChain notifications)
  {
    if (resource.getClass() == CDOResourceImpl.class)
    {
      this.resource = (CDOResourceImpl)resource;
    }

    return super.eSetResource(resource, notifications);
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

  private void instanceToRevisionContainment(CDOViewImpl view) throws ImplementationError
  {
    EObject container = instance.eContainer();
    if (container != null)
    {
      if (container instanceof CDOResource)
      {
        revision.setResourceID(((CDOResource)container).cdoID());
        revision.setContainerID(CDOID.NULL);
        revision.setContainingFeatureID(0);
      }
      else
      {
        revision.setResourceID(CDOID.NULL);
        // TODO is as CDOIDProvider call ok here?
        CDOID containerID = view.provideCDOID(container);
        if (containerID.isNull())
        {
          throw new ImplementationError("containerID.isNull()");
        }

        int containerFeatureID = instance.eContainerFeatureID();// container???
        revision.setContainerID(containerID);
        revision.setContainingFeatureID(containerFeatureID);
      }
    }
  }

  private void instanceToRevisionFeature(CDOViewImpl view, CDOFeature feature, CDOPackageRegistry packageRegistry)
      throws ImplementationError
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
    // Not supported anymore
    // Object containerID = revision.getContainerID();
    // if (containerID.isNull())
    // {
    // CDOID resourceID = revision.getResourceID();
    // Resource.Internal resource = (Resource.Internal)view.getObject(resourceID);
    // transferResourceToInstance(resource);
    // }
    // else
    // {
    // int containingFeatureID = revision.getContainingFeatureID();
    // InternalEObject container = convertPotentialID(view, containerID);
    // ((BasicEObjectImpl)instance).eBasicSetContainer(container, containingFeatureID, null);
    // }
  }

  // private void transferRevisionToInstanceResource(Resource.Internal resource)
  // {
  // Method method = ReflectUtil.getMethod(BasicEObjectImpl.class, "eSetDirectResource", Resource.Internal.class);
  //
  // try
  // {
  // ReflectUtil.invokeMethod(method, instance, resource);
  // }
  // catch (InvocationTargetException ex)
  // {
  // throw WrappedException.wrap(ex);
  // }
  // }

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
              element = getEObjectFromPotentialID(view, element);
              instanceList.basicAdd(element, null);
            }
          }
          else
          {
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
        value = getEObjectFromPotentialID(view, value);
      }

      setInstanceValue(instance, feature, value);
    }
  }

  private InternalEObject getEObjectFromPotentialID(CDOViewImpl view, Object potentialID)
  {
    if (potentialID instanceof CDOID)
    {
      CDOID id = (CDOID)potentialID;
      if (id.isNull())
      {
        return null;
      }

      potentialID = createProxy(view, id);
    }

    if (potentialID instanceof InternalCDOObject)
    {
      potentialID = ((InternalCDOObject)potentialID).cdoInternalInstance();
    }

    if (potentialID instanceof InternalEObject)
    {
      return (InternalEObject)potentialID;
    }

    throw new ImplementationError();
  }

  private InternalCDOObject createProxy(CDOViewImpl view, CDOID id)
  {
    return view.getObject(id, false);
  }

  private Object getInstanceValue(InternalEObject instance, CDOFeature feature, CDOPackageRegistry packageRegistry)
  {
    EStructuralFeature eFeature = ModelUtil.getEFeature(feature, packageRegistry);
    return instance.eGet(eFeature);
  }

  private void setInstanceValue(InternalEObject instance, CDOFeature feature, Object value)
  {
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
}
