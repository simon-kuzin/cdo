/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Simon McDuff - maintenance
 */
package org.eclipse.emf.internal.cdo;

import org.eclipse.emf.cdo.CDOState;
import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDTemp;
import org.eclipse.emf.cdo.common.model.EMFUtil;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.CDORevisionFactory;
import org.eclipse.emf.cdo.common.revision.delta.CDOFeatureDelta;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDeltaUtil;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionManager;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.InvalidObjectException;
import org.eclipse.emf.cdo.view.CDOView;

import org.eclipse.emf.internal.cdo.bundle.OM;
import org.eclipse.emf.internal.cdo.util.FSMUtil;

import org.eclipse.net4j.util.collection.Pair;
import org.eclipse.net4j.util.fsm.FiniteStateMachine;
import org.eclipse.net4j.util.fsm.ITransition;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EStoreEObjectImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.spi.cdo.InternalCDOObject;
import org.eclipse.emf.spi.cdo.InternalCDOTransaction;
import org.eclipse.emf.spi.cdo.InternalCDOView;
import org.eclipse.emf.spi.cdo.CDOSessionProtocol.CommitTransactionResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Eike Stepper
 */
public final class CDOStateMachine extends FiniteStateMachine<CDOState, CDOEvent, InternalCDOObject>
{
  // @Singleton
  public static final CDOStateMachine INSTANCE = new CDOStateMachine();

  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_STATEMACHINE, CDOStateMachine.class);

  private InternalCDOObject lastTracedObject;

  private CDOState lastTracedState;

  private CDOEvent lastTracedEvent;

  @SuppressWarnings("unchecked")
  private CDOStateMachine()
  {
    super(CDOState.class, CDOEvent.class);

    init(CDOState.TRANSIENT, CDOEvent.PREPARE, new PrepareTransition());
    init(CDOState.TRANSIENT, CDOEvent.ATTACH, FAIL);
    init(CDOState.TRANSIENT, CDOEvent.DETACH, IGNORE);
    init(CDOState.TRANSIENT, CDOEvent.READ, IGNORE);
    init(CDOState.TRANSIENT, CDOEvent.WRITE, IGNORE);
    init(CDOState.TRANSIENT, CDOEvent.INVALIDATE, FAIL);
    init(CDOState.TRANSIENT, CDOEvent.DETACH_REMOTE, FAIL);
    init(CDOState.TRANSIENT, CDOEvent.RELOAD, IGNORE);
    init(CDOState.TRANSIENT, CDOEvent.COMMIT, FAIL);
    init(CDOState.TRANSIENT, CDOEvent.ROLLBACK, FAIL);
    init(CDOState.TRANSIENT, CDOEvent.REATTACH, new ReattachTransition());

    init(CDOState.PREPARED, CDOEvent.PREPARE, FAIL);
    init(CDOState.PREPARED, CDOEvent.ATTACH, new AttachTransition());
    init(CDOState.PREPARED, CDOEvent.DETACH, FAIL);
    init(CDOState.PREPARED, CDOEvent.READ, IGNORE);
    init(CDOState.PREPARED, CDOEvent.WRITE, FAIL);
    init(CDOState.PREPARED, CDOEvent.INVALIDATE, FAIL);
    init(CDOState.PREPARED, CDOEvent.DETACH_REMOTE, FAIL);
    init(CDOState.PREPARED, CDOEvent.RELOAD, FAIL);
    init(CDOState.PREPARED, CDOEvent.COMMIT, FAIL);
    init(CDOState.PREPARED, CDOEvent.ROLLBACK, FAIL);
    init(CDOState.PREPARED, CDOEvent.REATTACH, FAIL);

    init(CDOState.NEW, CDOEvent.PREPARE, FAIL);
    init(CDOState.NEW, CDOEvent.ATTACH, FAIL);
    init(CDOState.NEW, CDOEvent.DETACH, new DetachTransition());
    init(CDOState.NEW, CDOEvent.READ, IGNORE);
    init(CDOState.NEW, CDOEvent.WRITE, new WriteNewTransition());
    init(CDOState.NEW, CDOEvent.INVALIDATE, FAIL);
    init(CDOState.NEW, CDOEvent.DETACH_REMOTE, FAIL);
    init(CDOState.NEW, CDOEvent.RELOAD, FAIL);
    init(CDOState.NEW, CDOEvent.COMMIT, new CommitTransition(false));
    init(CDOState.NEW, CDOEvent.ROLLBACK, FAIL);
    init(CDOState.NEW, CDOEvent.REATTACH, FAIL);

    init(CDOState.CLEAN, CDOEvent.PREPARE, FAIL);
    init(CDOState.CLEAN, CDOEvent.ATTACH, FAIL);
    init(CDOState.CLEAN, CDOEvent.DETACH, new DetachTransition());
    init(CDOState.CLEAN, CDOEvent.READ, IGNORE);
    init(CDOState.CLEAN, CDOEvent.WRITE, new WriteTransition());
    init(CDOState.CLEAN, CDOEvent.INVALIDATE, new InvalidateTransition());
    init(CDOState.CLEAN, CDOEvent.DETACH_REMOTE, DetachRemoteTransition.INSTANCE);
    init(CDOState.CLEAN, CDOEvent.RELOAD, new ReloadTransition());
    init(CDOState.CLEAN, CDOEvent.COMMIT, FAIL);
    init(CDOState.CLEAN, CDOEvent.ROLLBACK, FAIL);
    init(CDOState.CLEAN, CDOEvent.REATTACH, FAIL);

    init(CDOState.DIRTY, CDOEvent.PREPARE, FAIL);
    init(CDOState.DIRTY, CDOEvent.ATTACH, FAIL);
    init(CDOState.DIRTY, CDOEvent.DETACH, new DetachTransition());
    init(CDOState.DIRTY, CDOEvent.READ, IGNORE);
    init(CDOState.DIRTY, CDOEvent.WRITE, new RewriteTransition());
    init(CDOState.DIRTY, CDOEvent.INVALIDATE, new ConflictTransition());
    init(CDOState.DIRTY, CDOEvent.DETACH_REMOTE, new InvalidConflictTransition());
    init(CDOState.DIRTY, CDOEvent.RELOAD, new ReloadTransition());
    init(CDOState.DIRTY, CDOEvent.COMMIT, new CommitTransition(true));
    init(CDOState.DIRTY, CDOEvent.ROLLBACK, new RollbackTransition());
    init(CDOState.DIRTY, CDOEvent.REATTACH, FAIL);

    init(CDOState.PROXY, CDOEvent.PREPARE, FAIL);
    init(CDOState.PROXY, CDOEvent.ATTACH, FAIL);
    init(CDOState.PROXY, CDOEvent.DETACH, new DetachTransition());
    init(CDOState.PROXY, CDOEvent.READ, new LoadTransition(false));
    init(CDOState.PROXY, CDOEvent.WRITE, new LoadTransition(true));
    init(CDOState.PROXY, CDOEvent.INVALIDATE, IGNORE);
    init(CDOState.PROXY, CDOEvent.DETACH_REMOTE, DetachRemoteTransition.INSTANCE);
    init(CDOState.PROXY, CDOEvent.RELOAD, new ReloadTransition());
    init(CDOState.PROXY, CDOEvent.COMMIT, FAIL);
    init(CDOState.PROXY, CDOEvent.ROLLBACK, FAIL);
    init(CDOState.PROXY, CDOEvent.REATTACH, FAIL);

    init(CDOState.CONFLICT, CDOEvent.PREPARE, FAIL);
    init(CDOState.CONFLICT, CDOEvent.ATTACH, IGNORE);
    init(CDOState.CONFLICT, CDOEvent.DETACH, new DetachTransition());
    init(CDOState.CONFLICT, CDOEvent.READ, IGNORE);
    init(CDOState.CONFLICT, CDOEvent.WRITE, new RewriteTransition());
    init(CDOState.CONFLICT, CDOEvent.INVALIDATE, IGNORE);
    init(CDOState.CONFLICT, CDOEvent.DETACH_REMOTE, IGNORE);
    init(CDOState.CONFLICT, CDOEvent.RELOAD, FAIL);
    init(CDOState.CONFLICT, CDOEvent.COMMIT, IGNORE);
    init(CDOState.CONFLICT, CDOEvent.ROLLBACK, new RollbackTransition());
    init(CDOState.CONFLICT, CDOEvent.REATTACH, FAIL);

    init(CDOState.INVALID, CDOEvent.PREPARE, InvalidTransition.INSTANCE);
    init(CDOState.INVALID, CDOEvent.ATTACH, InvalidTransition.INSTANCE);
    init(CDOState.INVALID, CDOEvent.DETACH, InvalidTransition.INSTANCE);
    init(CDOState.INVALID, CDOEvent.READ, InvalidTransition.INSTANCE);
    init(CDOState.INVALID, CDOEvent.WRITE, InvalidTransition.INSTANCE);
    init(CDOState.INVALID, CDOEvent.INVALIDATE, IGNORE);
    init(CDOState.INVALID, CDOEvent.DETACH_REMOTE, IGNORE);
    init(CDOState.INVALID, CDOEvent.RELOAD, InvalidTransition.INSTANCE);
    init(CDOState.INVALID, CDOEvent.COMMIT, InvalidTransition.INSTANCE);
    init(CDOState.INVALID, CDOEvent.ROLLBACK, InvalidTransition.INSTANCE);
    init(CDOState.INVALID, CDOEvent.REATTACH, FAIL);

    init(CDOState.INVALID_CONFLICT, CDOEvent.PREPARE, InvalidTransition.INSTANCE);
    init(CDOState.INVALID_CONFLICT, CDOEvent.ATTACH, InvalidTransition.INSTANCE);
    init(CDOState.INVALID_CONFLICT, CDOEvent.DETACH, InvalidTransition.INSTANCE);
    init(CDOState.INVALID_CONFLICT, CDOEvent.READ, IGNORE);
    init(CDOState.INVALID_CONFLICT, CDOEvent.WRITE, IGNORE);
    init(CDOState.INVALID_CONFLICT, CDOEvent.INVALIDATE, IGNORE);
    init(CDOState.INVALID_CONFLICT, CDOEvent.DETACH_REMOTE, IGNORE);
    init(CDOState.INVALID_CONFLICT, CDOEvent.RELOAD, InvalidTransition.INSTANCE);
    init(CDOState.INVALID_CONFLICT, CDOEvent.COMMIT, InvalidTransition.INSTANCE);
    init(CDOState.INVALID_CONFLICT, CDOEvent.ROLLBACK, DetachRemoteTransition.INSTANCE);
    init(CDOState.INVALID_CONFLICT, CDOEvent.REATTACH, FAIL);
  }

  /**
   * The object is already attached in EMF world. It contains all the information needed to know where it will be
   * connected.
   * 
   * @since 2.0
   */
  public void attach(InternalCDOObject object, InternalCDOTransaction transaction)
  {
    ReentrantLock lock = lockView(transaction);

    try
    {
      List<InternalCDOObject> contents = new ArrayList<InternalCDOObject>();
      prepare(object, new Pair<InternalCDOTransaction, List<InternalCDOObject>>(transaction, contents));

      attachOrReattach(object, transaction);
      for (InternalCDOObject content : contents)
      {
        attachOrReattach(content, transaction);
      }
    }
    finally
    {
      unlockView(lock);
    }
  }

  private void attachOrReattach(InternalCDOObject object, InternalCDOTransaction transaction)
  {
    // Bug 283985 (Re-attachment)
    if (transaction.getFormerRevisions().containsKey(object))
    {
      reattachObject(object, transaction);
    }
    else
    {
      attachObject(object);
    }
  }

  /**
   * Phase 1: TRANSIENT --> PREPARED
   */
  private void prepare(InternalCDOObject object,
      Pair<InternalCDOTransaction, List<InternalCDOObject>> transactionAndContents)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("PREPARE: {0} --> {1}", object, transactionAndContents.getElement1()); //$NON-NLS-1$
    }

    process(object, CDOEvent.PREPARE, transactionAndContents);
  }

  /**
   * Phase 2: PREPARED --> NEW
   */
  private void attachObject(InternalCDOObject object)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("ATTACH: {0}", object); //$NON-NLS-1$
    }

    process(object, CDOEvent.ATTACH, null);
  }

  private void reattachObject(InternalCDOObject object, InternalCDOTransaction transaction)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("REATTACH: {0}", object);
    }

    process(object, CDOEvent.REATTACH, transaction);
  }

  /**
   * @since 2.0
   */
  public void detach(InternalCDOObject object)
  {
    ReentrantLock lock = lockView(object.cdoView());

    try
    {
      if (TRACER.isEnabled())
      {
        trace(object, CDOEvent.DETACH);
      }

      List<InternalCDOObject> objectsToDetach = new ArrayList<InternalCDOObject>();
      InternalCDOTransaction transaction = (InternalCDOTransaction)object.cdoView();

      // Accumulate objects that needs to be detached
      // If we have an error, we will keep the graph exactly like it was before.
      process(object, CDOEvent.DETACH, objectsToDetach);

      // postDetach requires the object to be TRANSIENT
      for (InternalCDOObject content : objectsToDetach)
      {
        CDOState oldState = content.cdoInternalSetState(CDOState.TRANSIENT);
        content.cdoInternalPostDetach(false);
        content.cdoInternalSetState(oldState);
      }

      // detachObject needs to know the state before we change the object to TRANSIENT
      for (InternalCDOObject content : objectsToDetach)
      {
        transaction.detachObject(content);
        content.cdoInternalSetState(CDOState.TRANSIENT);

        content.cdoInternalSetView(null);
        content.cdoInternalSetID(null);
        content.cdoInternalSetRevision(null);
      }
    }
    finally
    {
      unlockView(lock);
    }
  }

  /**
   * @since 2.0
   */
  public InternalCDORevision read(InternalCDOObject object)
  {
    ReentrantLock lock = lockView(object.cdoView());

    try
    {
      if (TRACER.isEnabled())
      {
        trace(object, CDOEvent.READ);
      }

      process(object, CDOEvent.READ, null);

      return object.cdoRevision();
    }
    finally
    {
      unlockView(lock);
    }
  }

  /**
   * @since 2.0
   */
  public InternalCDORevision readNoLoad(InternalCDOObject object)
  {
    ReentrantLock lock = lockView(object.cdoView());

    try
    {
      switch (object.cdoState())
      {
      case TRANSIENT:
      case PREPARED:
      case NEW:
      case CONFLICT:
      case INVALID_CONFLICT:
      case INVALID:
      case PROXY:
        return null;
      }

      return object.cdoRevision();
    }
    finally
    {
      unlockView(lock);
    }
  }

  /**
   * @since 2.0
   */
  public void write(InternalCDOObject object)
  {
    write(object, null);
  }

  /**
   * @since 2.0
   */
  public void write(InternalCDOObject object, CDOFeatureDelta featureDelta)
  {
    ReentrantLock lock = lockView(object.cdoView());

    try
    {
      writeWithoutViewLock(object, featureDelta);
    }
    finally
    {
      unlockView(lock);
    }
  }

  private void writeWithoutViewLock(InternalCDOObject object, CDOFeatureDelta featureDelta)
  {
    if (TRACER.isEnabled())
    {
      trace(object, CDOEvent.WRITE);
    }

    process(object, CDOEvent.WRITE, featureDelta);
  }

  /**
   * @since 2.0
   */
  public void reload(InternalCDOObject... objects)
  {
    // InternalCDOView view = null;
    // Map<CDOID, InternalCDOObject> ids = new HashMap<CDOID, InternalCDOObject>();
    // List<InternalCDORevision> revisions = new ArrayList<InternalCDORevision>();
    // List<InternalCDORevision> revised = new ArrayList<InternalCDORevision>();
    // // Detect the view
    // for (InternalCDOObject object : objects)
    // {
    // if (view == null)
    // {
    // view = object.cdoView();
    // break;
    // }
    // }
    //
    // if (view != null)
    // {
    // ReentrantLock lock = lockView(view);
    //
    // try
    // {
    // for (InternalCDOObject object : objects)
    // {
    // CDOState state = object.cdoState();
    // if (state != CDOState.TRANSIENT && state != CDOState.PREPARED && state != CDOState.NEW
    // && state != CDOState.CONFLICT && state != CDOState.INVALID_CONFLICT && state != CDOState.INVALID)
    // {
    // InternalCDORevision revision = object.cdoRevision();
    // // Revision is null for proxy state
    // if (revision != null)
    // {
    // if (revision.isCurrent())
    // {
    // revisions.add(revision);
    // }
    // else
    // {
    // revised.add(revision);
    // }
    // }
    //
    // ids.put(object.cdoID(), object);
    // }
    // }
    //
    // InternalCDOSession session = view.getSession();
    // revisions = session.getSessionProtocol().verifyRevisions(revisions);
    //
    // revisions.addAll(revised);
    // for (InternalCDORevision revision : revisions)
    // {
    // InternalCDOObject object = ids.get(revision.getID());
    // if (TRACER.isEnabled())
    // {
    // trace(object, CDOEvent.RELOAD);
    // }
    //
    // process(object, CDOEvent.RELOAD, null);
    // }
    // }
    // finally
    // {
    // unlockView(lock);
    // }
    // }
  }

  /**
   * @since 2.0
   */
  public void invalidate(InternalCDOObject object, int version)
  {
    ReentrantLock lock = lockView(object.cdoView());

    try
    {
      if (TRACER.isEnabled())
      {
        trace(object, CDOEvent.INVALIDATE);
      }

      process(object, CDOEvent.INVALIDATE, version);
    }
    finally
    {
      unlockView(lock);
    }
  }

  /**
   * @since 2.0
   */
  public void detachRemote(InternalCDOObject object)
  {
    ReentrantLock lock = lockView(object.cdoView());

    try
    {
      if (TRACER.isEnabled())
      {
        trace(object, CDOEvent.DETACH_REMOTE);
      }

      process(object, CDOEvent.DETACH_REMOTE, CDORevision.UNSPECIFIED_VERSION);
    }
    finally
    {
      unlockView(lock);
    }
  }

  /**
   * @since 2.0
   */
  public void commit(InternalCDOObject object, CommitTransactionResult result)
  {
    ReentrantLock lock = lockView(object.cdoView());

    try
    {
      if (TRACER.isEnabled())
      {
        trace(object, CDOEvent.COMMIT);
      }

      process(object, CDOEvent.COMMIT, result);
    }
    finally
    {
      unlockView(lock);
    }
  }

  /**
   * @since 2.0
   */
  public void rollback(InternalCDOObject object)
  {
    ReentrantLock lock = lockView(object.cdoView());

    try
    {
      if (TRACER.isEnabled())
      {
        trace(object, CDOEvent.ROLLBACK);
      }

      process(object, CDOEvent.ROLLBACK, null);
    }
    finally
    {
      unlockView(lock);
    }
  }

  @Override
  protected CDOState getState(InternalCDOObject object)
  {
    return object.cdoState();
  }

  @Override
  protected void setState(InternalCDOObject object, CDOState state)
  {
    object.cdoInternalSetState(state);
  }

  private ReentrantLock lockView(InternalCDOView view)
  {
    if (view == null)
    {
      return null;
    }

    ReentrantLock stateLock = view.getStateLock();
    stateLock.lock();
    return stateLock;
  }

  private void unlockView(ReentrantLock stateLock)
  {
    if (stateLock != null)
    {
      stateLock.unlock();
    }
  }

  /**
   * Removes clutter from the trace log
   */
  private void trace(InternalCDOObject object, CDOEvent event)
  {
    CDOState state = object.cdoState();
    if (lastTracedObject != object || lastTracedState != state || lastTracedEvent != event)
    {
      TRACER.format("{0}: {1}", event, object.getClass().getName()); //$NON-NLS-1$
      lastTracedObject = object;
      lastTracedState = state;
      lastTracedEvent = event;
    }
  }

  @SuppressWarnings("unused")
  private void testAttach(InternalCDOObject object)
  {
    process(object, CDOEvent.ATTACH, null);
  }

  @SuppressWarnings("unused")
  private void testReload(InternalCDOObject object)
  {
    process(object, CDOEvent.RELOAD, null);
  }

  /**
   * Prepares a tree of transient objects to be subsequently {@link AttachTransition attached} to a CDOView.
   * <p>
   * Execution is recursive and includes:
   * <ol>
   * <li>Assignment of a new {@link CDOIDTemp}
   * <li>Assignment of a new {@link CDORevision}
   * <li>Bidirectional association with the {@link CDOView}
   * <li>Registration with the {@link CDOTransaction}
   * <li>Changing state to {@link CDOState#PREPARED PREPARED}
   * </ol>
   * 
   * @see AttachTransition
   * @author Eike Stepper
   */
  private final class PrepareTransition implements
      ITransition<CDOState, CDOEvent, InternalCDOObject, Pair<InternalCDOTransaction, List<InternalCDOObject>>>
  {
    public void execute(InternalCDOObject object, CDOState state, CDOEvent event,
        Pair<InternalCDOTransaction, List<InternalCDOObject>> transactionAndContents)
    {
      InternalCDOTransaction transaction = transactionAndContents.getElement1();
      List<InternalCDOObject> contents = transactionAndContents.getElement2();

      Map<InternalCDOObject, InternalCDORevision> formerRevisionMap = transaction.getFormerRevisions();
      boolean reattaching = formerRevisionMap.containsKey(object);

      if (!reattaching)
      {
        // Prepare object
        CDOID id = transaction.getNextTemporaryID();
        object.cdoInternalSetID(id);
        object.cdoInternalSetView(transaction);
        changeState(object, CDOState.PREPARED);

        // Create new revision
        EClass eClass = object.eClass();
        CDORevisionFactory factory = transaction.getSession().getRevisionManager().getFactory();
        InternalCDORevision revision = (InternalCDORevision)factory.createRevision(eClass);
        revision.setID(id);
        revision.setVersion(-1);

        object.cdoInternalSetRevision(revision);

        // Register object
        transaction.registerObject(object);
        transaction.registerNew(object);
      }

      // Prepare content tree
      for (Iterator<InternalCDOObject> it = getProperContents(object, transaction); it.hasNext();)
      {
        InternalCDOObject content = it.next();
        contents.add(content);
        INSTANCE.process(content, CDOEvent.PREPARE, transactionAndContents);
      }
    }

    private Iterator<InternalCDOObject> getProperContents(final InternalCDOObject object,
        final CDOTransaction transaction)
    {
      final boolean isResource = object instanceof Resource;
      final Iterator<EObject> delegate = object.eContents().iterator();

      return new Iterator<InternalCDOObject>()
      {
        private Object next;

        public boolean hasNext()
        {
          while (delegate.hasNext())
          {
            InternalEObject eObject = (InternalEObject)delegate.next();
            EStructuralFeature eContainingFeature = eObject.eContainingFeature();
            if (isResource || eObject.eDirectResource() == null
                && (eContainingFeature == null || EMFUtil.isPersistent(eContainingFeature)))
            {
              next = FSMUtil.adapt(eObject, transaction);
              if (next instanceof InternalCDOObject)
              {
                return true;
              }
            }
          }

          return false;
        }

        public InternalCDOObject next()
        {
          return (InternalCDOObject)next;
        }

        public void remove()
        {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  /**
   * Attaches a tree of {@link PrepareTransition prepared} objects to a CDOView.
   * <p>
   * Execution is recursive and includes:
   * <ol>
   * <li>Calling {@link InternalCDOObject#cdoInternalPostAttach()},<br>
   * which includes for {@link CDOObjectImpl}:
   * <ol>
   * <li>Population of the CDORevision with the current values in
   * {@link EStoreEObjectImpl#eSetting(org.eclipse.emf.ecore.EStructuralFeature) eSettings}
   * <li>Unsetting {@link EStoreEObjectImpl#eSetting(org.eclipse.emf.ecore.EStructuralFeature) eSettings}
   * </ol>
   * <li>Changing state to {@link CDOState#NEW NEW}
   * </ol>
   * 
   * @see PrepareTransition
   * @author Eike Stepper
   */
  private final class AttachTransition implements ITransition<CDOState, CDOEvent, InternalCDOObject, Object>
  {
    public void execute(InternalCDOObject object, CDOState state, CDOEvent event, Object NULL)
    {
      object.cdoInternalPostAttach();
      changeState(object, CDOState.NEW);
    }
  }

  /**
   * Bug 283985 (Re-attachment)
   * 
   * @author Caspar De Groot
   */
  private final class ReattachTransition implements
      ITransition<CDOState, CDOEvent, InternalCDOObject, InternalCDOTransaction>
  {
    public void execute(InternalCDOObject object, CDOState state, CDOEvent event, InternalCDOTransaction transaction)
    {
      InternalCDORevisionManager revisionManager = transaction.getSession().getRevisionManager();
      InternalCDORevision formerRevision = transaction.getFormerRevisions().get(object);
      CDOID id = formerRevision.getID();

      object.cdoInternalSetID(id);
      object.cdoInternalSetView(transaction);

      // Construct a new revision if the old one is not transactional
      InternalCDORevision revision;
      EClass eClass = object.eClass();
      if (!formerRevision.isTransactional())
      {
        CDORevisionFactory factory = revisionManager.getFactory();
        revision = (InternalCDORevision)factory.createRevision(eClass);
        revision.setID(id);
        revision.setVersion(formerRevision.getVersion());
        revision.setTransactional(true);
      }
      else
      {
        // This branch only gets taken if the object that is being re-attached,
        // was already DIRTY when it was first detached. In this case, the revision
        // is already transactional; we clear it before repopulating it.
        //
        revision = formerRevision;
        for (int i = 0; i < eClass.getFeatureCount(); i++)
        {
          EStructuralFeature eFeature = object.cdoInternalDynamicFeature(i);
          if (!eFeature.isTransient())
          {
            revision.clear(eFeature);
          }
        }
      }

      // Populate the revision based on the values in the CDOObject
      object.cdoInternalSetRevision(revision);
      object.cdoInternalPostAttach();

      // Compute a revision delta and register it with the tx
      CDOBranch branch = transaction.getBranch();
      int version = revision.getVersion() - 1;
      CDORevision originalRevision = revisionManager.getRevisionByVersion(id, branch, version, -1, true);
      CDORevisionDelta revisionDelta = CDORevisionDeltaUtil.create(originalRevision, revision);
      transaction.registerRevisionDelta(revisionDelta);
      transaction.registerDirty(object, null);
      changeState(object, CDOState.DIRTY);

      // Add the object to the set of reattached objects
      transaction.getLastSavepoint().getReattachedObjects().put(id, object);
    }
  }

  /**
   * @author Eike Stepper
   */
  private final class DetachTransition implements
      ITransition<CDOState, CDOEvent, InternalCDOObject, List<InternalCDOObject>>
  {
    public void execute(InternalCDOObject object, CDOState state, CDOEvent event,
        List<InternalCDOObject> objectsToDetach)
    {
      InternalCDOTransaction transaction = (InternalCDOTransaction)object.cdoView();
      objectsToDetach.add(object);
      boolean isResource = object instanceof Resource;

      // Prepare content tree
      for (Iterator<EObject> it = object.eContents().iterator(); it.hasNext();)
      {
        InternalEObject eObject = (InternalEObject)it.next();
        boolean isDirectlyConnected = isResource && eObject.eDirectResource() == object;
        if (isDirectlyConnected || eObject.eDirectResource() == null)
        {
          InternalCDOObject content = FSMUtil.adapt(eObject, transaction);
          if (content != null)
          {
            INSTANCE.process(content, CDOEvent.DETACH, objectsToDetach);
          }
        }
      }
    }
  }

  /**
   * @author Eike Stepper
   */
  final private class CommitTransition implements
      ITransition<CDOState, CDOEvent, InternalCDOObject, CommitTransactionResult>
  {
    public CommitTransition(boolean useDeltas)
    {
    }

    public void execute(InternalCDOObject object, CDOState state, CDOEvent event, CommitTransactionResult data)
    {
      InternalCDOView view = object.cdoView();
      InternalCDORevision revision = object.cdoRevision();
      Map<CDOIDTemp, CDOID> idMappings = data.getIDMappings();

      // Adjust object
      CDOID id, oldID;
      oldID = id = object.cdoID();
      CDOID newID = idMappings.get(id);
      if (newID != null)
      {
        object.cdoInternalSetID(newID);
        view.remapObject(oldID);
        id = newID;
      }

      // Adjust revision
      revision.setID(id);
      revision.setTransactional(false);
      revision.setBranchPoint(data.getBranchPoint());

      // if (useDeltas)
      // {
      // // Cannot use that yet, since we need to change adjust index for list.
      // // TODO Simon Implement a way to adjust indexes as fast as possible.
      // RevisionAdjuster revisionAdjuster = new RevisionAdjuster(data.getReferenceAdjuster());
      // CDORevisionDelta delta = data.getCommitContext().getRevisionDeltas().get(oldID);
      // revisionAdjuster.adjustRevision(revision, delta);
      // }
      // else
      {
        revision.adjustReferences(data.getReferenceAdjuster());
      }

      InternalCDORevisionManager revisionManager = view.getSession().getRevisionManager();
      revisionManager.getCache().addRevision(revision);
      changeState(object, CDOState.CLEAN);
    }
  }

  /**
   * @author Eike Stepper
   */
  private final class RollbackTransition implements ITransition<CDOState, CDOEvent, InternalCDOObject, Object>
  {
    public void execute(InternalCDOObject object, CDOState state, CDOEvent event, Object NULL)
    {
      changeState(object, CDOState.PROXY);
    }
  }

  /**
   * @author Eike Stepper
   */
  private final class WriteTransition implements ITransition<CDOState, CDOEvent, InternalCDOObject, Object>
  {
    public void execute(InternalCDOObject object, CDOState state, CDOEvent event, Object featureDelta)
    {
      // Copy revision
      InternalCDORevision revision = (InternalCDORevision)object.cdoRevision().copy();
      revision.setTransactional(true);
      object.cdoInternalSetRevision(revision);

      InternalCDOView view = object.cdoView();
      InternalCDOTransaction transaction = view.toTransaction();
      transaction.registerDirty(object, (CDOFeatureDelta)featureDelta);
      changeState(object, CDOState.DIRTY);
    }
  }

  /**
   * @author Simon McDuff
   */
  private final class WriteNewTransition implements ITransition<CDOState, CDOEvent, InternalCDOObject, Object>
  {
    public void execute(InternalCDOObject object, CDOState state, CDOEvent event, Object featureDelta)
    {
      InternalCDOView view = object.cdoView();
      InternalCDOTransaction transaction = view.toTransaction();
      transaction.registerFeatureDelta(object, (CDOFeatureDelta)featureDelta);
    }
  }

  /**
   * @author Simon McDuff
   */
  private final class RewriteTransition implements ITransition<CDOState, CDOEvent, InternalCDOObject, Object>
  {
    public void execute(InternalCDOObject object, CDOState state, CDOEvent event, Object featureDelta)
    {
      InternalCDOView view = object.cdoView();
      InternalCDOTransaction transaction = view.toTransaction();
      transaction.registerFeatureDelta(object, (CDOFeatureDelta)featureDelta);
    }
  }

  /**
   * @author Eike Stepper
   */
  private final class ReloadTransition implements ITransition<CDOState, CDOEvent, InternalCDOObject, Object>
  {
    public void execute(InternalCDOObject object, CDOState state, CDOEvent event, Object NULL)
    {
      changeState(object, CDOState.PROXY);
    }
  }

  /**
   * @author Simon McDuff
   */
  static private class DetachRemoteTransition implements ITransition<CDOState, CDOEvent, InternalCDOObject, Object>
  {
    static DetachRemoteTransition INSTANCE = new DetachRemoteTransition();

    public void execute(InternalCDOObject object, CDOState state, CDOEvent event, Object NULL)
    {
      InternalCDOView view = object.cdoView();
      view.deregisterObject(object);

      object.cdoInternalSetState(CDOState.INVALID);
      object.cdoInternalPostDetach(true);
    }
  }

  /**
   * @author Eike Stepper
   */
  private class InvalidateTransition implements ITransition<CDOState, CDOEvent, InternalCDOObject, Integer>
  {
    public void execute(InternalCDOObject object, CDOState state, CDOEvent event, Integer version)
    {
      InternalCDORevision revision = object.cdoRevision();
      if (version == CDORevision.UNSPECIFIED_VERSION || revision.getVersion() <= version)
      {
        changeState(object, CDOState.PROXY);
        object.cdoInternalSetRevision(null);
        object.cdoInternalPostInvalidate();
      }
    }
  }

  /**
   * @author Eike Stepper
   * @since 2.0
   */
  private class ConflictTransition extends InvalidateTransition
  {
    @Override
    public void execute(InternalCDOObject object, CDOState state, CDOEvent event, Integer version)
    {
      InternalCDORevision revision = object.cdoRevision();
      if (version == 0 || revision.getVersion() <= version + 1)
      {
        InternalCDOView view = object.cdoView();
        InternalCDOTransaction transaction = view.toTransaction();
        transaction.setConflict(object);
        changeState(object, CDOState.CONFLICT);
      }
    }
  }

  /**
   * @author Simon McDuff
   */
  private final class InvalidConflictTransition extends ConflictTransition
  {
    @Override
    public void execute(InternalCDOObject object, CDOState state, CDOEvent event, Integer version)
    {
      InternalCDOView view = object.cdoView();
      InternalCDOTransaction transaction = view.toTransaction();
      transaction.setConflict(object);
      changeState(object, CDOState.INVALID_CONFLICT);
    }
  }

  /**
   * @author Eike Stepper
   */
  private final class LoadTransition implements ITransition<CDOState, CDOEvent, InternalCDOObject, Object>
  {
    private boolean forWrite;

    public LoadTransition(boolean forWrite)
    {
      this.forWrite = forWrite;
    }

    public void execute(InternalCDOObject object, CDOState state, CDOEvent event, Object delta)
    {
      object.cdoInternalPreLoad();

      CDOID id = object.cdoID();
      InternalCDOView view = object.cdoView();
      InternalCDORevision revision = view.getRevision(id, true);
      FSMUtil.validate(object, revision);
      object.cdoInternalSetRevision(revision);
      changeState(object, CDOState.CLEAN);
      object.cdoInternalPostLoad();

      if (forWrite)
      {
        INSTANCE.writeWithoutViewLock(object, (CDOFeatureDelta)delta);
      }
    }
  }

  /**
   * @author Simon McDuff
   */
  private static final class InvalidTransition implements ITransition<CDOState, CDOEvent, InternalCDOObject, Object>
  {
    public static final InvalidTransition INSTANCE = new InvalidTransition();

    public void execute(InternalCDOObject object, CDOState state, CDOEvent event, Object NULL)
    {
      throw new InvalidObjectException(object.cdoID());
    }
  }
}

/**
 * @author Eike Stepper
 */
enum CDOEvent
{
  PREPARE, ATTACH, DETACH, READ, WRITE, INVALIDATE, DETACH_REMOTE, RELOAD, COMMIT, ROLLBACK,

  // Bug 283985 (Re-attachment)
  REATTACH
}
