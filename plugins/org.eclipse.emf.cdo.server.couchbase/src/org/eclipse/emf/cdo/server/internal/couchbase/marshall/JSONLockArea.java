/*
 * Copyright (c) 2011-2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Caspar De Groot - initial API and implementation
 */
package org.eclipse.emf.cdo.server.internal.couchbase.marshall;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchManager;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.lock.CDOLockUtil;
import org.eclipse.emf.cdo.common.lock.IDurableLockingManager.LockArea;
import org.eclipse.emf.cdo.common.lock.IDurableLockingManager.LockGrade;
import org.eclipse.emf.cdo.server.IStore;

/**
 * @author Victor Roldan Betancort TODO Verbatim copy of DB4OLockArea. Refactor
 */
public class JSONLockArea {
	
	private String id;

	private String userID;

	private long timestamp;

	private int branchID;

	private boolean readOnly;

	private List<JSONLockEntry> lockEntries = new LinkedList<JSONLockEntry>();

	public static JSONLockArea getJSONLockArea(LockArea lockArea) {
		JSONLockArea jsonLockArea = new JSONLockArea();

		jsonLockArea.setId(lockArea.getDurableLockingID());
		jsonLockArea.setUserID(lockArea.getUserID());
		jsonLockArea.setTimestamp(lockArea.getTimeStamp());
		jsonLockArea.setBranchID(lockArea.getBranch().getID());
		jsonLockArea.setReadOnly(lockArea.isReadOnly());

		List<JSONLockEntry> newList = JSONLockEntry.getPrimitiveLockEntries(jsonLockArea, lockArea.getLocks());
		jsonLockArea.setLockEntries(newList);

		return jsonLockArea;
	}

	public static LockArea getLockArea(IStore store, JSONLockArea jsonLockArea) {
		// Reconstruct the branchpoint
		//
		CDOBranchManager branchManager = store.getRepository().getBranchManager();
		CDOBranch branch = branchManager.getBranch(jsonLockArea.getBranchID());
		CDOBranchPoint branchpoint = branch.getPoint(jsonLockArea.getTimestamp());

		// Reconstruct the lockMap
		//
		Map<CDOID, LockGrade> lockMap = CDOIDUtil.createMap();
		for (JSONLockEntry entry : jsonLockArea.getLockEntries()) {
			CDOID cdoid = CDOIDUtil.createLong(entry.getCdoID());
			LockGrade lockGrade = LockGrade.get(entry.getLockGrade());
			lockMap.put(cdoid, lockGrade);
		}

		return CDOLockUtil.createLockArea(jsonLockArea.getId(),
				jsonLockArea.getUserID(), branchpoint,
				jsonLockArea.isReadOnly(), lockMap);
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getUserID() {
		return userID;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public int getBranchID() {
		return branchID;
	}

	public List<JSONLockEntry> getLockEntries() {
		return lockEntries;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void setBranchID(int branchID) {
		this.branchID = branchID;
	}

	private void setLockEntries(List<JSONLockEntry> lockEntries) {
		this.lockEntries = lockEntries;
	}
}
