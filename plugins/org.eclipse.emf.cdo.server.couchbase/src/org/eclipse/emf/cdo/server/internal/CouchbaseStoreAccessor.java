package org.eclipse.emf.cdo.server.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchHandler;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.branch.CDOBranchVersion;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfoHandler;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.lob.CDOLobHandler;
import org.eclipse.emf.cdo.common.lock.IDurableLockingManager.LockArea.Handler;
import org.eclipse.emf.cdo.common.protocol.CDODataInput;
import org.eclipse.emf.cdo.common.protocol.CDODataOutput;
import org.eclipse.emf.cdo.common.revision.CDORevisionCacheAdder;
import org.eclipse.emf.cdo.common.revision.CDORevisionHandler;
import org.eclipse.emf.cdo.common.util.CDOQueryInfo;
import org.eclipse.emf.cdo.server.IQueryHandler;
import org.eclipse.emf.cdo.server.ISession;
import org.eclipse.emf.cdo.server.IStoreAccessor.DurableLocking;
import org.eclipse.emf.cdo.server.IStoreAccessor.Raw;
import org.eclipse.emf.cdo.server.IStoreChunkReader;
import org.eclipse.emf.cdo.server.ITransaction;
import org.eclipse.emf.cdo.server.internal.commitables.CommitInfoHandler;
import org.eclipse.emf.cdo.server.internal.commitables.ICommitable;
import org.eclipse.emf.cdo.server.internal.commitables.LobHandler;
import org.eclipse.emf.cdo.server.internal.commitables.LockingHandler;
import org.eclipse.emf.cdo.server.internal.commitables.PackageUnitHandler;
import org.eclipse.emf.cdo.server.internal.commitables.ResourceHandler;
import org.eclipse.emf.cdo.server.internal.commitables.RevisionHandler;
import org.eclipse.emf.cdo.spi.common.commit.CDOChangeSetSegment;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionDelta;
import org.eclipse.emf.cdo.spi.server.LongIDStoreAccessor;
import org.eclipse.emf.cdo.spi.server.Store;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.net4j.util.collection.Pair;
import org.eclipse.net4j.util.concurrent.IRWLockManager.LockType;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import com.couchbase.client.CouchbaseClient;

public class CouchbaseStoreAccessor extends LongIDStoreAccessor implements Raw,
		DurableLocking {

	private List<ICommitable> transaction = new ArrayList<ICommitable>();
	
	private CouchbaseClient client;
	
	private RevisionHandler revisionHandler;
	
	private ResourceHandler resourceHandler;
	
	private PackageUnitHandler packageUnitHandler;
	
	private CommitInfoHandler commitInfoHandler;
	
	private LobHandler lobHandler;
	
	private LockingHandler lockingHandler;
	
	protected CouchbaseStoreAccessor(Store store, ISession session) {
		super(store, session);
	}

	public CouchbaseStoreAccessor(Store store, ITransaction transaction) {
		super(store, transaction);
	}

	private void initHandlers() {
		revisionHandler = new RevisionHandler(getClient(), getStore());
		resourceHandler = new ResourceHandler(getClient(), getStore());
		packageUnitHandler = new PackageUnitHandler(getClient(), getStore());
		commitInfoHandler = new CommitInfoHandler(getClient(), getStore());
		lobHandler = new LobHandler(getClient(), getStore());
		lockingHandler = new LockingHandler(getClient(), getStore());
	}

	@Override
	public CouchbaseStore getStore() {
		return (CouchbaseStore) super.getStore();
	}

	@Override
	protected void doActivate() throws Exception {
		super.doActivate();
		if (client == null) {
			client = getStore().openClient();
		}
		initHandlers();
	}
	
	@Override
	protected void doDeactivate() throws Exception {
		super.doDeactivate();
		if (getClient() != null) {
			getClient().shutdown(10, TimeUnit.SECONDS);
		}
	}

	private CouchbaseClient getClient() {
		return client;
	}
	
	public IStoreChunkReader createChunkReader(InternalCDORevision revision, EStructuralFeature feature) {
		throw new UnsupportedOperationException("not implemented");
	}

	public Collection<InternalCDOPackageUnit> readPackageUnits() {
		return packageUnitHandler.readPackageUnits();
	}

	public EPackage[] loadPackageUnit(InternalCDOPackageUnit packageUnit) {
		return packageUnitHandler.loadPackageUnit(packageUnit);
	}

	public InternalCDORevision readRevision(CDOID id, CDOBranchPoint branchPoint, int listChunk, CDORevisionCacheAdder cache) {
		return revisionHandler.readRevision(id, branchPoint);
	}

	public InternalCDORevision readRevisionByVersion(CDOID id, CDOBranchVersion branchVersion, int listChunk, CDORevisionCacheAdder cache) {
		throw new UnsupportedOperationException("not implemented");
	}

	public void handleRevisions(EClass eClass, CDOBranch branch, long timeStamp, boolean exactTime, CDORevisionHandler handler) {
		revisionHandler.handleRevisions(eClass, branch, timeStamp, exactTime, handler);
	}

	public Set<CDOID> readChangeSet(OMMonitor monitor, CDOChangeSetSegment... segments) {
		throw new UnsupportedOperationException("not implemented");
	}

	public void queryResources(QueryResourcesContext context) {
		resourceHandler.queryResources(context);
	}

	public void queryXRefs(QueryXRefsContext context) {
		revisionHandler.queryXRefs(context);
	}

	public void queryLobs(List<byte[]> ids) {
	    for (Iterator<byte[]> it = ids.iterator(); it.hasNext();)
	    {	     
	      if (lobHandler.readLob(it.next()) == null)
	      {
	        it.remove();
	      }
	    }
	}

	public void loadLob(byte[] id, OutputStream out) throws IOException {
		lobHandler.loadLob(id, out);
	}

	public void handleLobs(long fromTime, long toTime, CDOLobHandler handler) throws IOException {
		lobHandler.handleLobs(fromTime, toTime, handler);
	}

	public void writePackageUnits(InternalCDOPackageUnit[] packageUnits, OMMonitor monitor) {
		for (InternalCDOPackageUnit packageUnit : packageUnits) {
			addToTransaction(packageUnitHandler.createPackageUnitCommitable(packageUnit));
		}
	}

	public IQueryHandler getQueryHandler(CDOQueryInfo info) {
		return null;
	}

	public Pair<Integer, Long> createBranch(int branchID, BranchInfo branchInfo) {
		throw new UnsupportedOperationException("not implemented");
	}

	public BranchInfo loadBranch(int branchID) {
		throw new UnsupportedOperationException("not implemented");
	}

	public SubBranchInfo[] loadSubBranches(int branchID) {
		throw new UnsupportedOperationException("not implemented");
	}

	public int loadBranches(int startID, int endID, CDOBranchHandler branchHandler) {
		throw new UnsupportedOperationException("not implemented");
	}

	public void loadCommitInfos(CDOBranch branch, long startTime, long endTime, CDOCommitInfoHandler handler) {
		commitInfoHandler.loadCommitInfos(branch, startTime, endTime, handler);
	}

	public LockArea createLockArea(String userID, CDOBranchPoint branchPoint, boolean readOnly, Map<CDOID, LockGrade> locks) throws LockAreaAlreadyExistsException {
		return lockingHandler.createLockArea(userID, branchPoint, readOnly, locks);
	}

	public LockArea getLockArea(String durableLockingID) throws LockAreaNotFoundException {
		return lockingHandler.getLockArea(durableLockingID);
	}

	public void getLockAreas(String userIDPrefix, Handler handler) {
		lockingHandler.getLockAreas(userIDPrefix, handler);
	}

	public void deleteLockArea(String durableLockingID) {
		lockingHandler.deleteLockArea(durableLockingID);
	}

	public void lock(String durableLockingID, LockType type, Collection<? extends Object> objectsToLock) {
		lockingHandler.lock(durableLockingID, type, objectsToLock);
	}

	public void unlock(String durableLockingID, LockType type, Collection<? extends Object> objectsToUnlock) {
		lockingHandler.unlock(durableLockingID, type, objectsToUnlock);
	}

	public void unlock(String durableLockingID) {
		lockingHandler.unlock(durableLockingID);
	}

	public void rawExport(CDODataOutput out, int fromBranchID, int toBranchID, long fromCommitTime, long toCommitTime) throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	public void rawImport(CDODataInput in, int fromBranchID, int toBranchID, long fromCommitTime, long toCommitTime, OMMonitor monitor) throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	public void rawStore(InternalCDOPackageUnit[] packageUnits, OMMonitor monitor) {
		throw new UnsupportedOperationException("not implemented");
	}

	public void rawStore(InternalCDORevision revision, OMMonitor monitor) {
		throw new UnsupportedOperationException("not implemented");
	}

	public void rawStore(byte[] id, long size, InputStream inputStream) throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	public void rawStore(byte[] id, long size, Reader reader) throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	public void rawStore(CDOBranch branch, long timeStamp, long previousTimeStamp, String userID, String comment, OMMonitor monitor) {
		throw new UnsupportedOperationException("not implemented");
	}

	public void rawDelete(CDOID id, int version, CDOBranch branch, EClass eClass, OMMonitor monitor) {
		throw new UnsupportedOperationException("not implemented");
	}

	public void rawCommit(double commitWork, OMMonitor monitor) {
		throw new UnsupportedOperationException("not implemented");
	}

	private void addToTransaction(ICommitable commitable) {
		transaction.add(commitable);
	}

	@Override
	protected void writeCommitInfo(CDOBranch branch, long timeStamp, long previousTimeStamp, String userID, String comment, OMMonitor monitor) {
		addToTransaction(commitInfoHandler.createWriteCommitInfoCommitable(branch, timeStamp, previousTimeStamp, userID, comment));
	}

	@Override
	protected void writeRevisions(InternalCDORevision[] revisions, CDOBranch branch, OMMonitor monitor) {
		 monitor.begin(revisions.length);
		 try
		 {
		     for (InternalCDORevision revision : revisions)
		     {
		    	 writeRevision(revision, monitor.fork());
		     }
		 }
		 finally
		 {
			 monitor.done();
		 }
	}

	protected void writeRevision(InternalCDORevision revision, OMMonitor monitor) {
		monitor.begin(10);
		try
		{
			addToTransaction(revisionHandler.createRevisionWriteCommitable(revision));
		} finally {
			monitor.done();
		}
	}

	@Override
	protected void writeRevisionDeltas(InternalCDORevisionDelta[] revisionDeltas, CDOBranch branch, long created, OMMonitor monitor) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	protected void detachObjects(CDOID[] detachedObjects, CDOBranch branch, long timeStamp, OMMonitor monitor) {
	    monitor.begin(detachedObjects.length);

	    try
	    {
	      for (CDOID id : detachedObjects)
	      {
	    	addToTransaction(revisionHandler.createRevisionDetachCommitable(id, branch));
	        monitor.worked();
	      }
	    }
	    finally
	    {
	      monitor.done();
	    }		
	}

	@Override
	protected void writeBlob(byte[] id, long size, InputStream inputStream) throws IOException {
		addToTransaction(lobHandler.createWriteBlobCommitable(id, size, inputStream));
	}

	@Override
	protected void writeClob(byte[] id, long size, Reader reader) throws IOException {
		addToTransaction(lobHandler.createWriteClobCommitable(id, size, reader));
	}

	@Override
	protected void doCommit(OMMonitor monitor) {
		monitor.begin(transaction.size());
		try {
	 		for (ICommitable commitable : transaction) {
	 			commitable.commit(monitor.fork());
	 		}
	 		transaction.clear();
		}	    
		finally
	    {
			monitor.done();
		}	
	}

	@Override
	protected void doRollback(CommitContext commitContext) {
		transaction.clear();
	}
}
