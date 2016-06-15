/*
 * Copyright (c) 2011, 2012 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Victor Roldan Betancort - initial API and implementation
 */
package org.eclipse.emf.cdo.util;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.CDOState;
import org.eclipse.emf.cdo.view.CDOObjectHandler;
import org.eclipse.emf.cdo.view.CDOView;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.spi.cdo.FSMUtil;
import org.eclipse.emf.spi.cdo.InternalCDOView;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

/**
 * A scalable {@link EContentAdapter content adapter} that uses CDO mechansims to attach itself to {@link CDOObject
 * objects} when they are lazily loaded.
 *
 * @author Victor Roldan Betancort
 * @since 4.0
 */
public class CDOLazyContentAdapter extends AdapterImpl
{
  private CDOObjectHandler handler = new CleanObjectHandler();

  private Set<WeakReference<CDOObject>> adaptedObjects = new HashSet<WeakReference<CDOObject>>();

  private WeakReference<Notifier> adaptedRoot;

  @Override
  public Notifier getTarget()
  {
    return null;
  }

  @Override
  public void setTarget(Notifier target)
  {
    if (isConnectedObject(target))
    {
      if (adaptedRoot == null)
      {
        adaptedRoot = new WeakReference<Notifier>(CDOUtil.getCDOObject(target));
      }

      if (target instanceof Resource)
      {
        addCleanObjectHandler(target);
      }
    }
  }

  @Override
  public void notifyChanged(Notification notification)
  {
  }

  protected void setTarget(EObject target)
  {
    if (isConnectedObject(target))
    {
      if (adaptedRoot == null)
      {
        adaptedRoot = new WeakReference<Notifier>(CDOUtil.getCDOObject(target));
      }

      if (target instanceof Resource)
      {
        addCleanObjectHandler(target);
      }
    }
  }

  /**
   * EContentAdapter removes adapter from all contained EObjects. In this case, we remove this adapter from all lazily
   * loaded objects
   */
  @Override
  public void unsetTarget(Notifier target)
  {
    if (isConnectedObject(target))
    {
      if (target instanceof Resource)
      {
        InternalCDOView view = getCDOView(target);
        if (view != null)
        {
          // Remove adapter from all adapted objects
          for (WeakReference<CDOObject> weakReference : adaptedObjects)
          {
            CDOObject object = weakReference.get();
            if (object != null)
            {
              removeAdapter(object);
            }
          }
        }

        target.eAdapters().remove(this);
        removeCleanObjectHandler(target);
      }
    }
  }

  private void addCleanObjectHandler(EObject target)
  {
    InternalCDOView view = getCDOView(target);
    if (view != null)
    {
      CDOObjectHandler[] handlers = view.getObjectHandlers();
      for (CDOObjectHandler handler : handlers)
      {
        if (handler.equals(this.handler))
        {
          return;
        }
      }

      view.addObjectHandler(handler);

      // Adapt already loaded objects
      for (CDOObject cdoObject : view.getObjectsList())
      {
        if (isContained(cdoObject))
        {
          addAdapter(cdoObject);
        }
      }
    }
  }

  private void removeCleanObjectHandler(EObject target)
  {
    InternalCDOView view = getCDOView(target);
    if (view != null)
    {
      CDOObjectHandler[] handlers = view.getObjectHandlers();
      for (CDOObjectHandler handler : handlers)
      {
        if (handler.equals(this.handler))
        {
          view.removeObjectHandler(handler);
          break;
        }
      }
    }
  }

  protected void addAdapter(Notifier notifier)
  {
    if (isConnectedObject(notifier) && !isAlreadyAdapted(notifier))
    {
      adaptedObjects.add(new WeakReference<CDOObject>(CDOUtil.getCDOObject((EObject)notifier)));
    }
  }

  protected void removeAdapter(Notifier notifier)
  {
    notifier.eAdapters().remove(this);
  }

  private boolean isAlreadyAdapted(Notifier notifier)
  {
    return notifier.eAdapters().contains(this);
  }

  /**
   * Checks if the argument is contained in the object graph of the root element
   */
  private boolean isContained(CDOObject object)
  {
    if (adaptedRoot == null)
    {
      return false;
    }

    Notifier root = adaptedRoot.get();
    return isContained(object, root);
  }

  private boolean isContained(CDOObject object, Notifier root)
  {
    if (object == null || root == null)
    {
      return false;
    }

    if (root instanceof Resource)
    {
      return root == (object instanceof Resource ? object : object.cdoResource());
    }

    if (root instanceof ResourceSet)
    {
      ResourceSet resourceSet = (ResourceSet)root;
      for (Resource resource : resourceSet.getResources())
      {
        if (isContained(object, resource))
        {
          return true;
        }
      }

      return false;
    }

    return EcoreUtil.isAncestor((EObject)root, object);
  }

  private static InternalCDOView getCDOView(EObject target)
  {
    CDOObject object = CDOUtil.getCDOObject(target);
    if (object != null)
    {
      return (InternalCDOView)object.cdoView();
    }

    return null;
  }

  private static boolean isConnectedObject(Notifier target)
  {
    if (target instanceof EObject)
    {
      CDOObject object = CDOUtil.getCDOObject((EObject)target);
      if (object != null)
      {
        return !FSMUtil.isTransient(object);
      }
    }

    return false;
  }

  /**
   * @author Victor Roldan Betancort
   */
  private final class CleanObjectHandler implements CDOObjectHandler
  {
    public void objectStateChanged(CDOView view, CDOObject object, CDOState oldState, CDOState newState)
    {
      if (newState == CDOState.CLEAN || newState == CDOState.NEW)
      {
        if (isConnectedObject(object) && !isAlreadyAdapted(object) && isContained(object))
        {
          addAdapter(object);
        }
      }
    }
  }
}
