/*
 * Copyright (c) 2011-2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Simon McDuff - maintenance
 */
package org.eclipse.emf.internal.cdo.view;

import org.eclipse.emf.cdo.CDOState;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.branch.CDOBranchVersion;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.common.revision.CDOList;
import org.eclipse.emf.cdo.common.revision.CDORevisable;
import org.eclipse.emf.cdo.common.revision.CDORevisionFactory;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;
import org.eclipse.emf.cdo.common.revision.CDORevisionUtil;
import org.eclipse.emf.cdo.common.revision.delta.CDOFeatureDelta;
import org.eclipse.emf.cdo.common.revision.delta.CDOOriginSizeProvider;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.common.security.NoPermissionException;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageInfo;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionCache;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionDelta;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionManager;
import org.eclipse.emf.cdo.view.CDOInvalidationPolicy;

import org.eclipse.emf.internal.cdo.view.CDOStateMachine2.Attached.Clean;
import org.eclipse.emf.internal.cdo.view.CDOStateMachine2.Attached.Conflict;
import org.eclipse.emf.internal.cdo.view.CDOStateMachine2.Attached.Dirty;
import org.eclipse.emf.internal.cdo.view.CDOStateMachine2.Attached.New;
import org.eclipse.emf.internal.cdo.view.CDOStateMachine2.Attached.Proxy;
import org.eclipse.emf.internal.cdo.view.CDOStateMachine2.Attached.Undone;
import org.eclipse.emf.internal.cdo.view.CDOStateMachine2.Unattached.Detached;
import org.eclipse.emf.internal.cdo.view.CDOStateMachine2.Unattached.Transient;
import org.eclipse.emf.internal.cdo.view.CDOStateMachine2.Unusable.Invalid;
import org.eclipse.emf.internal.cdo.view.CDOStateMachine2.Unusable.Invalid_Conflict;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EContentsEList;
import org.eclipse.emf.spi.cdo.CDOSessionProtocol.CommitTransactionResult;
import org.eclipse.emf.spi.cdo.FSMUtil;
import org.eclipse.emf.spi.cdo.InternalCDOObject;
import org.eclipse.emf.spi.cdo.InternalCDOSavepoint;
import org.eclipse.emf.spi.cdo.InternalCDOSavepoint.ChangeInfo;
import org.eclipse.emf.spi.cdo.InternalCDOSession;
import org.eclipse.emf.spi.cdo.InternalCDOTransaction;
import org.eclipse.emf.spi.cdo.InternalCDOView;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public final class CDOStateMachine2
{
  public static final CDOStateMachine2 INSTANCE = new CDOStateMachine2();

  private static final State TRANSIENT = new Transient();

  private static final State CLEAN = new Clean();

  private static final State PROXY = new Proxy();

  private static final State CONFLICT = new Conflict();

  private static final State INVALID = new Invalid();

  private static final State INVALID_CONFLICT = new Invalid_Conflict();

  static final ThreadLocal<Boolean> SWITCHING_TARGET = new InheritableThreadLocal<Boolean>();

  private CDOStateMachine2()
  {
  }

  public void attach(InternalCDOObject object, InternalCDOTransaction transaction)
  {
    synchronized (transaction)
    {
      State state = getState(object, transaction, false);
      traceEvent("Attach", object, state);
      state.attach(object, transaction);
    }
  }

  public void detach(InternalCDOObject object)
  {
    synchronized (getMonitor(object))
    {
      // Accumulate objects that need to be detached.
      // In case of errors, we will keep the graph exactly like it was before.
      List<Runnable> runnables = new ArrayList<Runnable>();
      detach(object, runnables);

      for (Runnable runnable : runnables)
      {
        runnable.run();
      }
    }
  }

  private void detach(InternalCDOObject object, List<Runnable> runnables)
  {
    State state = getState(object, null, false);
    traceEvent("Detach", object, state);
    state.detach(object, runnables);
  }

  public InternalCDORevision read(InternalCDOObject object)
  {
    synchronized (getMonitor(object))
    {
      State state = getState(object, null, false);
      traceEvent("Read", object, state);
      return state.read(object);
    }
  }

  public InternalCDORevision readNoLoad(InternalCDOObject object)
  {
    synchronized (getMonitor(object))
    {
      switch (object.cdoState())
      {
      case TRANSIENT:
      case NEW:
      case CONFLICT:
      case INVALID_CONFLICT:
      case INVALID:
      case PROXY:
        return null;
      }

      return object.cdoRevision();
    }
  }

  public InternalCDORevision write(InternalCDOObject object, CDOFeatureDelta featureDelta)
  {
    synchronized (getMonitor(object))
    {
      return writeWithoutViewLock(object, featureDelta);
    }
  }

  private InternalCDORevision writeWithoutViewLock(InternalCDOObject object, CDOFeatureDelta featureDelta)
  {
    State state = getState(object, null, false);
    traceEvent("Write", object, state);
    return state.write(object, featureDelta);
  }

  public void commit(InternalCDOObject object, CommitTransactionResult result)
  {
    synchronized (getMonitor(object))
    {
      State state = getState(object, null, false);
      traceEvent("Commit", object, state);
      state.commit(object, result);
    }
  }

  public void rollback(InternalCDOObject object)
  {
    synchronized (getMonitor(object))
    {
      State state = getState(object, null, true);
      traceEvent("Rollback", object, state);
      state.rollback(object);
    }
  }

  public void invalidate(InternalCDOObject object, CDORevisionKey key)
  {
    synchronized (getMonitor(object))
    {
      State state = getState(object, null, false);
      traceEvent("Invalidate", object, state);
      state.invalidate(object, key);
    }
  }

  public void detachRemote(InternalCDOObject object)
  {
    synchronized (getMonitor(object))
    {
      State state = getState(object, null, false);
      traceEvent("DetachRemote", object, state);
      state.detachRemote(object);
    }
  }

  private void transition(InternalCDOObject object, State state)
  {
    trace("   Transition " + getLabel(object) + " to " + state);
    object.cdoInternalSetState(state.getCDOState());
  }

  private Object getMonitor(InternalCDOObject object)
  {
    InternalCDOView view = object.cdoView();
    if (view != null)
    {
      return view;
    }

    // In TRANSIENT and PREPARED the object is not yet attached to a view
    return object;
  }

  private State getState(InternalCDOObject object, InternalCDOTransaction transaction, boolean remove)
  {
    CDOState cdoState = object.cdoState();
    switch (cdoState)
    {
    case TRANSIENT:
      return getUnattachedState(object, transaction);

    case NEW:
    case DIRTY:
    case PREPARED:
      return getAttachedState(object, remove);

    case CLEAN:
      return CLEAN;

    case PROXY:
      return PROXY;

    case CONFLICT:
      return CONFLICT;

    case INVALID:
      return INVALID;

    case INVALID_CONFLICT:
      return INVALID_CONFLICT;

    default:
      throw new IllegalStateException("Illegal state: " + cdoState);
    }
  }

  private State getUnattachedState(InternalCDOObject object, InternalCDOTransaction transaction)
  {
    if (transaction != null)
    {
      ChangeInfo detachedInfo = transaction.getLastSavepoint().getDetachedInfo(object);
      if (detachedInfo != null)
      {
        return (State)detachedInfo;
      }
    }

    return TRANSIENT;
  }

  private State getAttachedState(InternalCDOObject object, boolean remove)
  {
    InternalCDOSavepoint savepoint = object.cdoView().toTransaction().getLastSavepoint();
    CDOID id = object.cdoID();

    if (remove)
    {
      return (State)savepoint.removeChangeInfo(id);
    }

    return (State)savepoint.getChangeInfo(id);
  }

  private static String getLabel(InternalCDOObject object)
  {
    String label = object.toString();

    int pos = label.indexOf('[');
    if (pos != -1)
    {
      label = label.substring(0, pos);
    }

    return label;
  }

  private static void traceEvent(String event, InternalCDOObject object, State state)
  {
    int xxx;
    // trace(event + " for " + getLabel(object) + " in " + state);
  }

  private static void trace(String message)
  {
    int xxx;
    // System.out.println(message);
  }

  /**
   * @author Eike Stepper
   */
  public static abstract class State
  {
    public static final String FAIL_PREFIX = "Impossible to handle ";

    public abstract CDOState getCDOState();

    public void attach(InternalCDOObject object, InternalCDOTransaction transaction)
    {
      throw fail(object, "Attach");
    }

    public void detach(InternalCDOObject object, List<Runnable> runnables)
    {
      throw fail(object, "Detach");
    }

    public InternalCDORevision read(InternalCDOObject object)
    {
      throw fail(object, "Read");
    }

    public InternalCDORevision write(InternalCDOObject object, CDOFeatureDelta featureDelta)
    {
      throw fail(object, "Write");
    }

    public void commit(InternalCDOObject object, CommitTransactionResult result)
    {
      throw fail(object, "Commit");
    }

    public void rollback(InternalCDOObject object)
    {
      throw fail(object, "Rollback");
    }

    public void invalidate(InternalCDOObject object, CDORevisionKey key)
    {
      throw fail(object, "Invalidate");
    }

    public void detachRemote(InternalCDOObject object)
    {
      throw fail(object, "DetachRemote");
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName().toUpperCase();
    }

    protected void doRemoteDetach(InternalCDOObject object)
    {
      INSTANCE.transition(object, INVALID);

      InternalCDOView view = object.cdoView();
      view.deregisterObject(object);
      object.cdoInternalPostDetach(true);
    }

    protected final void ignore()
    {
      // trace("   IGNORE");
    }

    private IllegalStateException fail(InternalCDOObject object, String event)
    {
      String message = FAIL_PREFIX + event + " for " + getLabel(object) + " in " + this;
      return new IllegalStateException(message);
    }
  }

  /**
   * @author Eike Stepper
   */
  public static abstract class Unattached extends State
  {
    @Override
    public final CDOState getCDOState()
    {
      return CDOState.TRANSIENT;
    }

    @Override
    public void attach(InternalCDOObject object, InternalCDOTransaction transaction)
    {
      // Prepare content tree
      for (Iterator<InternalEObject> it = getPersistentContents(object); it.hasNext();)
      {
        InternalEObject content = it.next();
        Resource.Internal directResource = content.eDirectResource();

        boolean objectIsResource = directResource == object;
        if (objectIsResource || directResource == null)
        {
          InternalCDOObject adapted = FSMUtil.adapt(content, transaction);
          INSTANCE.attach(adapted, transaction);
        }
      }
    }

    @Override
    public void detach(InternalCDOObject object, List<Runnable> runnables)
    {
      ignore();
    }

    @Override
    public final InternalCDORevision read(InternalCDOObject object)
    {
      // Can happen during detach
      return object.cdoRevision();
    }

    @Override
    public final InternalCDORevision write(InternalCDOObject object, CDOFeatureDelta featureDelta)
    {
      // Can happen during detach
      return object.cdoRevision();
    }

    @Override
    public void invalidate(InternalCDOObject object, CDORevisionKey key)
    {
      ignore();
    }

    @Override
    public void detachRemote(InternalCDOObject object)
    {
      ignore();
    }

    protected void registerNewPackage(EClass eClass, InternalCDOSession session)
    {
      checkPackageRegistrationProblems(session, eClass);

      EPackage ePackage = eClass.getEPackage();
      CDOPackageRegistry packageRegistry = session.getPackageRegistry();
      if (!packageRegistry.containsKey(ePackage.getNsURI()))
      {
        packageRegistry.putEPackage(ePackage);
      }
    }

    private void checkPackageRegistrationProblems(InternalCDOSession session, EClass eClass)
    {
      if (session.options().isGeneratedPackageEmulationEnabled())
      {
        // Check that there are no multiple EPackages with the same URI in system. Bug 335004
        String packageURI = eClass.getEPackage().getNsURI();
        Object packageObject = session.getPackageRegistry().get(packageURI);
        if (packageObject instanceof InternalCDOPackageInfo)
        {
          packageObject = ((InternalCDOPackageInfo)packageObject).getEPackage(false);
        }

        if (packageObject instanceof EPackage && packageObject != eClass.getEPackage())
        {
          throw new IllegalStateException(MessageFormat.format(
              "Global EPackage {0} for EClass {1} is different from EPackage found in CDOPackageRegistry", packageURI,
              eClass));
        }
      }
    }

    private Iterator<InternalEObject> getPersistentContents(InternalCDOObject object)
    {
      EStructuralFeature[] features = object.cdoClassInfo().getAllPersistentContainments();
      return new EContentsEList.ResolvingFeatureIteratorImpl<InternalEObject>(object, features);
    }

    /**
     * @author Eike Stepper
     */
    public static final class Transient extends Unattached
    {
      @Override
      public void attach(InternalCDOObject object, InternalCDOTransaction transaction)
      {
        // TODO Permission check needed?
        transaction.handleAttachingObject(object);

        EClass eClass = object.eClass();
        CDOID id = transaction.createIDForNewObject(object.cdoInternalInstance());
        CDOBranchPoint branchPoint = transaction.getBranch().getHead();

        InternalCDOSession session = transaction.getSession();
        registerNewPackage(eClass, session);

        // Create new revision
        CDORevisionFactory revisionFactory = session.getRevisionManager().getFactory();
        InternalCDORevision revision = (InternalCDORevision)revisionFactory.createRevision(eClass);
        revision.setID(id);
        revision.setBranchPoint(branchPoint);

        object.cdoInternalSetView(transaction);
        object.cdoInternalSetRevision(revision);

        transaction.registerObject(object); // Object must have ID
        object.cdoInternalPostAttach(); // Object must have CDOState.TRANSIENT and an empty revision

        New newState = new New(object, null);
        transaction.getLastSavepoint().addChangeInfo(newState);
        INSTANCE.transition(object, newState);
        super.attach(object, transaction);
      }
    }

    /**
     * @author Eike Stepper
     */
    public static final class Detached extends Unattached implements ChangeInfo
    {
      private final InternalCDOObject object;

      private final InternalCDORevision cleanRevision;

      private final InternalCDORevision baseRevision;

      public Detached(InternalCDOObject object, InternalCDORevision cleanRevision, InternalCDORevision baseRevision)
      {
        if (cleanRevision == null)
        {
          throw new IllegalArgumentException("cleanRevision is null");
        }

        this.object = object;
        this.cleanRevision = cleanRevision;
        this.baseRevision = baseRevision;
      }

      public ChangeType getType()
      {
        return ChangeType.DETACHED;
      }

      public CDOID getID()
      {
        return cleanRevision.getID();
      }

      public int getVersion()
      {
        return cleanRevision.getVersion();
      }

      public InternalCDOObject getObject()
      {
        return object;
      }

      public InternalCDORevision getCleanRevision()
      {
        return cleanRevision;
      }

      public InternalCDORevision getBaseRevision()
      {
        return baseRevision;
      }

      public InternalCDORevisionDelta getRevisionDelta()
      {
        return null;
      }

      public ChangeInfo setSavepoint()
      {
        InternalCDORevision newBaseRevision = null; // TODO Compute from instance
        return new Detached(object, cleanRevision, newBaseRevision);
      }

      @Override
      public void attach(InternalCDOObject object, InternalCDOTransaction transaction)
      {
        // TODO Permission check needed?
        transaction.handleAttachingObject(object);

        CDOID id = getID();
        EClass eClass = object.eClass();
        CDOBranchPoint branchPoint = transaction.getBranch().getHead();

        InternalCDOSession session = transaction.getSession();
        registerNewPackage(eClass, session);

        InternalCDORevision cleanRevision = getCleanRevision();

        // Create new revision
        CDORevisionFactory revisionFactory = session.getRevisionManager().getFactory();
        InternalCDORevision revision = (InternalCDORevision)revisionFactory.createRevision(eClass);
        revision.setID(id);
        revision.setVersion(cleanRevision.getVersion());
        revision.setBranchPoint(branchPoint);

        object.cdoInternalSetView(transaction);
        object.cdoInternalSetRevision(revision);

        transaction.registerObject(object); // Object must have ID
        object.cdoInternalPostAttach(); // Object must have CDOState.TRANSIENT and an empty revision

        InternalCDOSavepoint savepoint = transaction.getLastSavepoint();
        savepoint.removeChangeInfo(id);

        State newState;
        InternalCDORevisionDelta revisionDelta = revision.compare(cleanRevision);
        if (revisionDelta.isEmpty())
        {
          if (savepoint.getPreviousSavepoint() != null)
          {
            newState = new Undone(object, cleanRevision, baseRevision);
            savepoint.addChangeInfo((ChangeInfo)newState);
          }
          else
          {
            newState = CLEAN;
            savepoint.removeChangeInfo(id);
          }
        }
        else
        {
          newState = new Dirty(object, cleanRevision, baseRevision, revisionDelta);
          savepoint.addChangeInfo((ChangeInfo)newState);
        }

        INSTANCE.transition(object, newState);
        transaction.updateDirtyState(true);
        super.attach(object, transaction);
      }

      @Override
      protected void registerNewPackage(EClass eClass, InternalCDOSession session)
      {
        // Do nothing
      }

      @Override
      public String toString()
      {
        return super.toString() + "[" + cleanRevision + "]";
      }
    }
  }

  /**
   * @author Eike Stepper
   */
  public static abstract class Attached extends State
  {
    public InternalCDORevision getCleanRevision(InternalCDOObject object)
    {
      // TODO What about states that are no ChangeInfo (i.e. not New or Dirty)?
      return null;
    }

    public InternalCDORevision getBaseRevision()
    {
      // TODO What about states that are no ChangeInfo (i.e. not New or Dirty)?
      return null;
    }

    @Override
    public final void detach(final InternalCDOObject object, List<Runnable> runnables)
    {
      final InternalCDOTransaction transaction = object.cdoView().toTransaction();
      transaction.handleDetachingObject(object);

      runnables.add(new Runnable()
      {
        public void run()
        {
          State newState = doDetach(object, transaction.getLastSavepoint());

          transaction.deregisterObject(object);
          object.cdoInternalSetView(null);
          object.cdoInternalSetID(null);

          INSTANCE.transition(object, newState);
        }
      });

      boolean isResource = object instanceof Resource;

      // Prepare content tree
      for (Iterator<EObject> it = object.eContents().iterator(); it.hasNext();)
      {
        InternalEObject eObject = (InternalEObject)it.next();
        boolean isDirectlyConnected = isResource && eObject.eDirectResource() == object;
        if (isDirectlyConnected || eObject.eDirectResource() == null)
        {
          InternalCDOObject adapted = FSMUtil.adapt(eObject, transaction);
          if (adapted != null)
          {
            INSTANCE.detach(adapted, runnables);
          }
        }
      }
    }

    protected State doDetach(InternalCDOObject object, InternalCDOSavepoint savepoint)
    {
      revisionToInstance(object);

      InternalCDORevision cleanRevision = getCleanRevision(object);
      InternalCDORevision baseRevision = getBaseRevision();

      Detached detached = new Detached(object, cleanRevision, baseRevision);
      savepoint.addChangeInfo(detached);
      return detached;
    }

    private void revisionToInstance(InternalCDOObject object)
    {
      object.cdoInternalSetState(CDOState.TRANSIENT);
    
      try
      {
        object.cdoInternalPostDetach(false); // postDetach() requires the object to be TRANSIENT
      }
      finally
      {
        object.cdoInternalSetState(getCDOState());
      }
    }

    @Override
    public InternalCDORevision read(InternalCDOObject object)
    {
      // Ignore
      return object.cdoRevision();
    }

    protected final InternalCDORevision getWritableRevision(InternalCDOObject object)
    {
      InternalCDORevision cleanRevision = object.cdoRevision();
      if (!cleanRevision.isWritable())
      {
        throw new NoPermissionException(cleanRevision);
      }

      return cleanRevision;
    }

    protected final void doCommit(InternalCDOObject object, CommitTransactionResult result)
    {
      InternalCDOTransaction transaction = object.cdoView().toTransaction();
      InternalCDORevision revision = object.cdoRevision();
      Map<CDOID, CDOID> idMappings = result.getIDMappings();

      // Adjust object
      CDOID oldID = object.cdoID();
      CDOID newID = idMappings.get(oldID);
      if (newID != null)
      {
        revision.setID(newID);
        transaction.remapObject(oldID);
      }

      // Adjust revision
      revision.adjustForCommit(transaction.getBranch(), result.getTimeStamp());
      revision.adjustReferences(result.getReferenceAdjuster());
      // TODO Adjust possible CDOElementProxies!
      revision.freeze();

      InternalCDORevisionManager revisionManager = transaction.getSession().getRevisionManager();
      revisionManager.addRevision(revision);

      transaction.getLastSavepoint().removeChangeInfo(oldID);
      INSTANCE.transition(object, CLEAN);
    }

    /**
     * @author Eike Stepper
     */
    public static final class Proxy extends Attached
    {
      @Override
      public CDOState getCDOState()
      {
        return CDOState.PROXY;
      }

      @Override
      public InternalCDORevision read(InternalCDOObject object)
      {
        load(object, false);
        return object.cdoRevision();
      }

      @Override
      public InternalCDORevision write(InternalCDOObject object, CDOFeatureDelta featureDelta)
      {
        load(object, true);
        return INSTANCE.writeWithoutViewLock(object, featureDelta);
      }

      @Override
      public void invalidate(InternalCDOObject object, CDORevisionKey key)
      {
        ignore();
      }

      @Override
      public void detachRemote(InternalCDOObject object)
      {
        doRemoteDetach(object);
      }

      protected final void load(InternalCDOObject object, boolean forWrite)
      {
        object.cdoInternalPreLoad();

        InternalCDOView view = object.cdoView();
        InternalCDORevision revision = view.getRevision(object.cdoID(), true);
        if (revision == null)
        {
          INSTANCE.detachRemote(object);
          CDOInvalidationPolicy policy = view.options().getInvalidationPolicy();
          policy.handleInvalidObject(object);
        }

        if (forWrite && !revision.isWritable())
        {
          throw new NoPermissionException(revision);
        }

        object.cdoInternalSetRevision(revision);
        INSTANCE.transition(object, CLEAN);
        object.cdoInternalPostLoad();
        view.dispatchLoadNotification(object);
      }
    }

    /**
     * @author Eike Stepper
     */
    public static class Clean extends Attached
    {
      @Override
      public CDOState getCDOState()
      {
        return CDOState.CLEAN;
      }

      @Override
      public InternalCDORevision getCleanRevision(InternalCDOObject object)
      {
        return object.cdoRevision();
      }

      @Override
      public InternalCDORevision write(InternalCDOObject object, final CDOFeatureDelta featureDelta)
      {
        final InternalCDORevision cleanRevision = getWritableRevision(object);

        InternalCDOTransaction transaction = object.cdoView().toTransaction();
        transaction.handleModifyingObject(object, featureDelta);

        InternalCDORevisionDelta revisionDelta = (InternalCDORevisionDelta)CDORevisionUtil.createDelta(cleanRevision);
        boolean mergeIsEmpty = revisionDelta.mergeFeatureDelta(featureDelta, new CDOOriginSizeProvider.Caching()
        {
          @Override
          protected CDOList getList()
          {
            EStructuralFeature feature = featureDelta.getFeature();
            return cleanRevision.getList(feature);
          }
        });

        if (mergeIsEmpty)
        {
          return cleanRevision;
        }

        // Copy revision
        InternalCDORevision revision = cleanRevision.copy();
        featureDelta.apply(revision);
        object.cdoInternalSetRevision(revision);

        Dirty dirty = new Dirty(object, cleanRevision, cleanRevision, revisionDelta);
        transaction.getLastSavepoint().addChangeInfo(dirty);

        INSTANCE.transition(object, dirty);
        transaction.updateDirtyState(false);

        return revision;
      }

      @Override
      public void invalidate(InternalCDOObject object, CDORevisionKey key)
      {
        InternalCDORevision oldRevision = object.cdoRevision();
        InternalCDORevision newRevision = null;

        InternalCDOView view = object.cdoView();
        InternalCDORevisionCache cache = view.getSession().getRevisionManager().getCache();

        if (SWITCHING_TARGET.get() == Boolean.TRUE)
        {
          CDORevisionDelta delta = (CDORevisionDelta)key;
          CDORevisable target = delta.getTarget();
          newRevision = (InternalCDORevision)cache.getRevisionByVersion(delta.getID(), target);
          if (newRevision == null)
          {
            newRevision = oldRevision.copy();
            view.getSession().resolveAllElementProxies(newRevision);
            delta.apply(newRevision);
            newRevision.setBranchPoint(target);
            cache.addRevision(newRevision);
          }

          object.cdoInternalSetRevision(newRevision);
          INSTANCE.transition(object, CLEAN);
          object.cdoInternalPostLoad();
          return;
        }

        if (key == null || key.getVersion() >= oldRevision.getVersion())
        {
          CDORevisionKey newKey = null;
          if (key != null)
          {
            int newVersion = getNewVersion(key);
            newKey = CDORevisionUtil.createRevisionKey(key.getID(), key.getBranch(), newVersion);
          }

          if (newKey != null)
          {
            newRevision = (InternalCDORevision)cache.getRevisionByVersion(newKey.getID(), newKey);
          }

          if (newRevision != null)
          {
            object.cdoInternalSetRevision(newRevision);
            INSTANCE.transition(object, CLEAN);
            object.cdoInternalPostLoad();
          }
          else
          {
            INSTANCE.transition(object, PROXY);

            CDOInvalidationPolicy policy = view.options().getInvalidationPolicy();
            policy.handleInvalidation(object, key);
            object.cdoInternalPostInvalidate();
          }
        }
      }

      @Override
      public void detachRemote(InternalCDOObject object)
      {
        doRemoteDetach(object);
      }

      private static int getNewVersion(CDORevisionKey key)
      {
        if (key instanceof CDORevisionDelta)
        {
          CDORevisionDelta delta = (CDORevisionDelta)key;
          CDORevisable target = delta.getTarget();
          if (target != null && key.getBranch() == target.getBranch())
          {
            return target.getVersion();
          }
        }

        return key.getVersion() + 1;
      }
    }

    /**
     * @author Eike Stepper
     */
    public static final class Undone extends Clean implements ChangeInfo
    {
      private final InternalCDOObject object;

      private final InternalCDORevision cleanRevision;

      private final InternalCDORevision baseRevision;

      public Undone(InternalCDOObject object, InternalCDORevision cleanRevision, InternalCDORevision baseRevision)
      {
        this.object = object;
        this.cleanRevision = cleanRevision;
        this.baseRevision = baseRevision;
      }

      @Override
      public CDOState getCDOState()
      {
        return CDOState.PREPARED;
      }

      public ChangeType getType()
      {
        return ChangeType.UNDONE;
      }

      public CDOID getID()
      {
        return object.cdoID();
      }

      public InternalCDOObject getObject()
      {
        return object;
      }

      @Override
      public InternalCDORevision getCleanRevision(InternalCDOObject object)
      {
        return cleanRevision;
      }

      public InternalCDORevision getCleanRevision()
      {
        return cleanRevision;
      }

      @Override
      public InternalCDORevision getBaseRevision()
      {
        return baseRevision;
      }

      public InternalCDORevisionDelta getRevisionDelta()
      {
        return null;
      }

      public ChangeInfo setSavepoint()
      {
        InternalCDORevision newBaseRevision = object.cdoRevision().copy();
        return new Undone(object, cleanRevision, newBaseRevision);
      }
    }

    /**
     * @author Eike Stepper
     */
    public static final class New extends Attached implements ChangeInfo
    {
      private final InternalCDOObject object;

      private final InternalCDORevision baseRevision;

      public New(InternalCDOObject object, InternalCDORevision baseRevision)
      {
        this.object = object;
        this.baseRevision = baseRevision;
      }

      @Override
      public CDOState getCDOState()
      {
        return CDOState.NEW;
      }

      public ChangeType getType()
      {
        return ChangeType.NEW;
      }

      public CDOID getID()
      {
        return object.cdoID();
      }

      public int getVersion()
      {
        return CDOBranchVersion.UNSPECIFIED_VERSION;
      }

      public InternalCDOObject getObject()
      {
        return object;
      }

      @Override
      public InternalCDORevision getCleanRevision(InternalCDOObject object)
      {
        return null;
      }

      public InternalCDORevision getCleanRevision()
      {
        return null;
      }

      @Override
      public InternalCDORevision getBaseRevision()
      {
        return baseRevision;
      }

      public InternalCDORevisionDelta getRevisionDelta()
      {
        return null;
      }

      public ChangeInfo setSavepoint()
      {
        InternalCDORevision newBaseRevision = object.cdoRevision().copy();
        return new New(object, newBaseRevision);
      }

      @Override
      public InternalCDORevision write(InternalCDOObject object, CDOFeatureDelta featureDelta)
      {
        InternalCDORevision revision = getWritableRevision(object); // Check write permission

        InternalCDOTransaction transaction = object.cdoView().toTransaction();
        transaction.handleModifyingObject(object, featureDelta);

        featureDelta.apply(revision);
        return revision;
      }

      @Override
      public void commit(InternalCDOObject object, CommitTransactionResult result)
      {
        doCommit(object, result);
      }

      @Override
      public String toString()
      {
        return super.toString() + "[" + object.cdoRevision() + "]";
      }

      @Override
      protected State doDetach(InternalCDOObject object, InternalCDOSavepoint savepoint)
      {
        savepoint.removeChangeInfo(object.cdoID());
        return TRANSIENT;
      }
    }

    /**
     * @author Eike Stepper
     */
    public static final class Dirty extends Attached implements ChangeInfo
    {
      private final InternalCDOObject object;

      private final InternalCDORevision cleanRevision;

      private final InternalCDORevision baseRevision;

      private InternalCDORevisionDelta revisionDelta;

      public Dirty(InternalCDOObject object, InternalCDORevision cleanRevision, InternalCDORevision baseRevision,
          InternalCDORevisionDelta revisionDelta)
      {
        this.object = object;
        this.cleanRevision = cleanRevision;
        this.baseRevision = baseRevision;
        this.revisionDelta = revisionDelta;
      }

      @Override
      public CDOState getCDOState()
      {
        return CDOState.DIRTY;
      }

      public ChangeType getType()
      {
        return ChangeType.DIRTY;
      }

      public CDOID getID()
      {
        return cleanRevision.getID();
      }

      public int getVersion()
      {
        return cleanRevision.getVersion();
      }

      public InternalCDOObject getObject()
      {
        return object;
      }

      @Override
      public InternalCDORevision getCleanRevision(InternalCDOObject object)
      {
        return cleanRevision;
      }

      public InternalCDORevision getCleanRevision()
      {
        return cleanRevision;
      }

      @Override
      public InternalCDORevision getBaseRevision()
      {
        return baseRevision;
      }

      public InternalCDORevisionDelta getRevisionDelta()
      {
        return revisionDelta;
      }

      public ChangeInfo setSavepoint()
      {
        InternalCDORevision newBaseRevision = object.cdoRevision().copy();
        return new Dirty(object, cleanRevision, newBaseRevision, revisionDelta);
      }

      @Override
      public InternalCDORevision write(InternalCDOObject object, final CDOFeatureDelta featureDelta)
      {
        InternalCDORevision revision = getWritableRevision(object);

        InternalCDOTransaction transaction = object.cdoView().toTransaction();
        transaction.handleModifyingObject(object, featureDelta);

        final EStructuralFeature feature = featureDelta.getFeature();
        featureDelta.apply(revision);

        if (revision.isUnchunked())
        {
          if (isOnlyFeatureDelta(feature))
          {
            if (isClean(revision, feature))
            {
              undo(object, transaction);
              return cleanRevision;
            }
          }
        }

        boolean mergeIsEmpty = revisionDelta.mergeFeatureDelta(featureDelta, new CDOOriginSizeProvider.Caching()
        {
          @Override
          protected CDOList getList()
          {
            return cleanRevision.getList(feature);
          }
        });

        if (mergeIsEmpty && revisionDelta.isEmpty())
        {
          undo(object, transaction);
          return cleanRevision;
        }

        return revision;
      }

      private boolean isOnlyFeatureDelta(final EStructuralFeature feature)
      {
        Map<EStructuralFeature, CDOFeatureDelta> featureDeltas = revisionDelta.getFeatureDeltaMap();
        return featureDeltas.size() == 1 && featureDeltas.containsKey(feature);
      }

      private boolean isClean(InternalCDORevision revision, EStructuralFeature feature)
      {
        if (feature.isMany())
        {
          CDOList list = revision.getList(feature);
          CDOList cleanList = cleanRevision.getList(feature);

          int size = list.size();
          if (size != cleanList.size())
          {
            return false;
          }

          for (int i = size - 1; i >= 0; --i)
          {
            Object value = list.get(i);
            Object cleanValue = cleanList.get(i);
            if (!CDORevisionUtil.areValuesEqual(value, cleanValue))
            {
              return false;
            }
          }

          return true;
        }

        Object value = revision.getValue(feature);
        Object cleanValue = cleanRevision.getValue(feature);
        return CDORevisionUtil.areValuesEqual(value, cleanValue);
      }

      private void undo(InternalCDOObject object, InternalCDOTransaction transaction)
      {
        object.cdoInternalSetRevision(cleanRevision);

        State newState;
        InternalCDOSavepoint savepoint = transaction.getLastSavepoint();
        if (savepoint.getPreviousSavepoint() != null)
        {
          newState = new Undone(object, cleanRevision, baseRevision);
          savepoint.addChangeInfo((ChangeInfo)newState);
        }
        else
        {
          newState = CLEAN;
          savepoint.removeChangeInfo(getID());
        }

        INSTANCE.transition(object, newState);
        transaction.updateDirtyState(true);
      }

      @Override
      public void invalidate(InternalCDOObject object, CDORevisionKey key)
      {
        InternalCDORevision oldRevision = object.cdoRevision();
        if (key == null || key.getVersion() >= oldRevision.getVersion() - 1)
        {
          INSTANCE.transition(object, CONFLICT);
          InternalCDOTransaction transaction = object.cdoView().toTransaction();
          transaction.setConflict(object);
        }
      }

      @Override
      public void commit(InternalCDOObject object, CommitTransactionResult result)
      {
        doCommit(object, result);
      }

      @Override
      public void detachRemote(InternalCDOObject object)
      {
        INSTANCE.transition(object, INVALID_CONFLICT);

        InternalCDOTransaction transaction = object.cdoView().toTransaction();
        transaction.setConflict(object);
      }

      @Override
      public String toString()
      {
        return super.toString() + "[" + getRevisionDelta() + "]";
      }
    }

    /**
     * @author Eike Stepper
     */
    public static final class Conflict extends Attached
    {
      @Override
      public CDOState getCDOState()
      {
        return CDOState.CONFLICT;
      }

      @Override
      public void invalidate(InternalCDOObject object, CDORevisionKey key)
      {
        ignore();
      }

      @Override
      public void detachRemote(InternalCDOObject object)
      {
        ignore();
      }
    }
  }

  /**
   * @author Eike Stepper
   */
  public static abstract class Unusable extends State
  {
    @Override
    public void detach(InternalCDOObject object, List<Runnable> runnables)
    {
      ignore();
    }

    /**
     * @author Eike Stepper
     */
    public static final class Invalid extends Unusable
    {
      @Override
      public CDOState getCDOState()
      {
        return CDOState.INVALID;
      }

      @Override
      public void invalidate(InternalCDOObject object, CDORevisionKey key)
      {
        ignore();
      }

      @Override
      public void detachRemote(InternalCDOObject object)
      {
        ignore();
      }
    }

    /**
     * @author Eike Stepper
     */
    public static final class Invalid_Conflict extends Unusable
    {
      @Override
      public CDOState getCDOState()
      {
        return CDOState.INVALID_CONFLICT;
      }

      @Override
      public void invalidate(InternalCDOObject object, CDORevisionKey key)
      {
        ignore();
      }

      @Override
      public void rollback(InternalCDOObject object)
      {
        doRemoteDetach(object);
      }

      @Override
      public void detachRemote(InternalCDOObject object)
      {
        ignore();
      }
    }
  }
}
