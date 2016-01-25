/*
 * Copyright (c) 2016 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.internal.server;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevisionHandler;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.IStoreAccessor.UnitSupport;
import org.eclipse.emf.cdo.server.IUnit;
import org.eclipse.emf.cdo.server.IUnitManager;
import org.eclipse.emf.cdo.server.IView;
import org.eclipse.emf.cdo.spi.server.InternalRepository;

import org.eclipse.net4j.util.container.Container;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * @author Eike Stepper
 */
public final class UnitManager extends Container<IUnit> implements IUnitManager
{
  private final InternalRepository repository;

  private final Map<CDOID, IUnit> units = new HashMap<CDOID, IUnit>();

  private final ReentrantReadWriteLock managerLock = new ReentrantReadWriteLock();

  public UnitManager(InternalRepository repository)
  {
    this.repository = repository;
  }

  public final IRepository getRepository()
  {
    return repository;
  }

  public boolean isUnit(CDOID rootID)
  {
    ReadLock readLock = managerLock.readLock();
    readLock.lock();

    try
    {
      return units.containsKey(rootID);
    }
    finally
    {
      readLock.unlock();
    }
  }

  public IUnit createUnit(CDOID rootID, IView view, CDORevisionHandler revisionHandler)
  {
    WriteLock writeLock = managerLock.writeLock();
    writeLock.lock();

    try
    {
      Unit unit;
      if (units.containsKey(rootID))
      {
        return null;
      }

      int xxx; // TODO Check that units are not nested.

      unit = new Unit(rootID);
      units.put(rootID, unit);

      // Acquire unit write lock early here, release it in Unit.init()
      unit.unitLock.writeLock().lock();

      writeLock.unlock();
      writeLock = null;

      unit.init(view, revisionHandler);
      fireElementAddedEvent(unit);
      return unit;
    }
    finally
    {
      if (writeLock != null)
      {
        writeLock.unlock();
      }
    }
  }

  public IUnit getUnit(CDOID rootID)
  {
    ReadLock readLock = managerLock.readLock();
    readLock.lock();

    try
    {
      return units.get(rootID);
    }
    finally
    {
      readLock.unlock();
    }
  }

  public IUnit[] getUnits()
  {
    return getElements();
  }

  public IUnit[] getElements()
  {
    ReadLock readLock = managerLock.readLock();
    readLock.lock();

    try
    {
      return units.values().toArray(new IUnit[units.size()]);
    }
    finally
    {
      readLock.unlock();
    }
  }

  @Override
  protected void doActivate() throws Exception
  {
    super.doActivate();

    UnitSupport unitSupport = (UnitSupport)repository.getStore().getReader(null);

    try
    {
      List<CDOID> roots = unitSupport.readUnitRoots();
      for (CDOID root : roots)
      {
        IUnit unit = new Unit(root);
        units.put(root, unit);
      }
    }
    finally
    {
      unitSupport.release();
    }
  }

  @Override
  protected void doDeactivate() throws Exception
  {
    units.clear();
    super.doDeactivate();
  }

  /**
   * @author Eike Stepper
   */
  public final class Unit implements IUnit
  {
    private final CDOID rootID;

    private final Set<IView> views = new HashSet<IView>();

    private final ReentrantReadWriteLock unitLock = new ReentrantReadWriteLock();

    public Unit(CDOID rootID)
    {
      this.rootID = rootID;
    }

    public IUnitManager getManager()
    {
      return UnitManager.this;
    }

    public CDOID getRootID()
    {
      return rootID;
    }

    public void init(IView view, CDORevisionHandler revisionHandler)
    {
      // Write lock has been acquired by UnitManager.createUnit()

      try
      {
        UnitSupport unitSupport = (UnitSupport)repository.getStore().getWriter(null);

        try
        {
          unitSupport.initUnit(view, rootID, revisionHandler);
        }
        finally
        {
          unitSupport.release();
        }
      }
      finally
      {
        // Write lock has been acquired by UnitManager.createUnit()
        unitLock.writeLock().unlock();
      }
    }

    public boolean isOpen()
    {
      ReadLock readLock = unitLock.readLock();
      readLock.lock();

      try
      {
        return !views.isEmpty();
      }
      finally
      {
        readLock.unlock();
      }
    }

    public void open(IView view, final CDORevisionHandler revisionHandler)
    {
      ReadLock readLock = unitLock.readLock();
      readLock.lock();

      try
      {
        views.add(view);

        UnitSupport unitSupport = (UnitSupport)repository.getStore().getReader(null);

        try
        {
          unitSupport.readUnit(view, rootID, revisionHandler);
        }
        finally
        {
          unitSupport.release();
        }
      }
      finally
      {
        readLock.unlock();
      }
    }

    public void close(IView view)
    {
      ReadLock readLock = unitLock.readLock();
      readLock.lock();

      try
      {
        views.remove(view);
      }
      finally
      {
        readLock.unlock();
      }
    }

    @Override
    public String toString()
    {
      return "Unit[" + rootID + "]";
    }
  }
}
