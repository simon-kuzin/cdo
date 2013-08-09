package org.eclipse.emf.cdo.server.internal.commitables;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.lock.CDOLockUtil;
import org.eclipse.emf.cdo.common.lock.IDurableLockingManager.LockArea;
import org.eclipse.emf.cdo.common.lock.IDurableLockingManager.LockArea.Handler;
import org.eclipse.emf.cdo.common.lock.IDurableLockingManager.LockAreaNotFoundException;
import org.eclipse.emf.cdo.common.lock.IDurableLockingManager.LockGrade;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.internal.couchbase.marshall.JSONLockArea;
import org.eclipse.emf.cdo.spi.server.InternalLockManager;
import org.eclipse.net4j.util.concurrent.IRWLockManager.LockType;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import com.couchbase.client.CouchbaseClient;

public class LockingHandler extends JSONHandler {

	public LockingHandler(CouchbaseClient client, IStore store) {
		super(client, store);
	}

	public LockArea getLockArea(String durableLockingID) throws LockAreaNotFoundException
	{
		JSONLockArea primitive = readJSONLockArea(durableLockingID);
		if (primitive != null) {
			return JSONLockArea.getLockArea(getStore(), primitive);	
		}
	    throw new LockAreaNotFoundException(durableLockingID);
	}
	
	public LockArea createLockArea(String userID, CDOBranchPoint branchPoint, boolean readOnly, Map<CDOID, LockGrade> locks)
	{
		String durableLockingID = getNextDurableLockingID();
		LockArea lockArea = CDOLockUtil.createLockArea(durableLockingID, userID, branchPoint, readOnly, locks);
		storeLockArea(lockArea);
		return lockArea;
	}

	public void lock(String durableLockingID, LockType type, Collection<? extends Object> objectsToLock)
	{
	    // TODO Refactor. Next chunk of code copied verbatim from MEMStore.lock
	    LockArea area = getLockArea(durableLockingID);
	    Map<CDOID, LockGrade> locks = area.getLocks();

	    InternalLockManager lockManager = (InternalLockManager)getStore().getRepository().getLockingManager();
	    for (Object objectToLock : objectsToLock)
	    {
	      CDOID id = lockManager.getLockKeyID(objectToLock);
	      LockGrade grade = locks.get(id);
	      if (grade != null)
	      {
	        grade = grade.getUpdated(type, true);
	      }
	      else
	      {
	        grade = LockGrade.get(type);
	      }

	      locks.put(id, grade);
	    }

	    storeLockArea(area);
	}
	
	public void unlock(String durableLockingID, LockType type, Collection<? extends Object> objectsToUnlock) {
		// TODO (CD) Refactor? Next chunk of code copied verbatim from MEMStore.lock
		LockArea area = getLockArea(durableLockingID);
		Map<CDOID, LockGrade> locks = area.getLocks();
		InternalLockManager lockManager = (InternalLockManager) getStore() .getRepository().getLockingManager();
		for (Object objectToUnlock : objectsToUnlock) {
			CDOID id = lockManager.getLockKeyID(objectToUnlock);
			LockGrade grade = locks.get(id);
			if (grade != null) {
				grade = grade.getUpdated(type, false);
				if (grade == LockGrade.NONE) {
					locks.remove(id);
				} else {
					locks.put(id, grade);
				}
			}
		}

		storeLockArea(area);
	}

	public void unlock(String durableLockingID) {
		LockArea area = getLockArea(durableLockingID);
		Map<CDOID, LockGrade> locks = area.getLocks();
		locks.clear();
		storeLockArea(area);
	}
	
	// FIXME if user id is a prefix, and not the full userID, this method will fail
	public void getLockAreas(String userIDPrefix, Handler handler) {
		// the framework requests with userIDPrefix == "" to
		// get all the persisted locks		
		List<String> lockIds = userIDPrefix.length() == 0 ? getMultiValue(getAllLocksKey()) : getMultiValue(getLockUserKey(userIDPrefix));
		for (String lockId : lockIds) {
			System.out.println();
			LockArea lockArea = getLockArea(lockId);
			handler.handleLockArea(lockArea);
		}
	}

	public void deleteLockArea(final String durableLockingID) {
		new AbstractCommitable(getClient()) {
			public void commit(OMMonitor monitor) {
				LockArea lockArea = getLockArea(durableLockingID);
				doCommit(durableLockingID, null, null, PersistMethod.DELETE);
				doCommit(getLockUserKey(lockArea), durableLockingID, null, PersistMethod.DELETE_APPENDED);	
				doCommit(getAllLocksKey(), durableLockingID, null, PersistMethod.DELETE_APPENDED);
			}
		}.commit(null);
	}
	
	private void storeLockArea(final LockArea area)
	{
		new AbstractCommitable(getClient()) {
			public void commit(OMMonitor monitor) {
				JSONLockArea jsonLockArea = JSONLockArea.getJSONLockArea(area);				
				doCommit(jsonLockArea.getId(), toJson(jsonLockArea), null, PersistMethod.SET);
				doCommit(getLockUserKey(area), jsonLockArea.getId(), null, PersistMethod.APPEND_IF_NOT_CONTAINED);
				doCommit(getAllLocksKey(), jsonLockArea.getId(), null, PersistMethod.APPEND_IF_NOT_CONTAINED);
			}
		}.commit(null);
	}

	private String getLockUserKey(final LockArea area) {
		return getLockUserKey(area.getUserID());
	}
	
	private String getLockUserKey(final String userName) {
		return "LockUser::" + userName;
	}
	
	private String getAllLocksKey() {
		return "AllLocks";
	}

	private JSONLockArea readJSONLockArea(String durableLockingID) throws LockAreaNotFoundException
	{
		String jsonString = (String)getValue(durableLockingID);
		if (jsonString != null) {
			return fromJson(jsonString, JSONLockArea.class);
		}
		return null;
	}
	
	// TODO: Refactor -- this was copied verbatim from DurableLockingManager
	private String getNextDurableLockingID() {
		for (;;) {
			String durableLockingID = CDOLockUtil.createDurableLockingID();
			try {
				getLockArea(durableLockingID); // Check uniqueness
				// Not unique; try once more...
			} catch (LockAreaNotFoundException ex) {
				return durableLockingID;
			}
		}
	}
}
