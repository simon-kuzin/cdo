package org.eclipse.emf.cdo.server.internal.commitables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfoHandler;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.spi.common.branch.InternalCDOBranchManager;
import org.eclipse.emf.cdo.spi.common.commit.CDOCommitInfoUtil;
import org.eclipse.emf.cdo.spi.common.commit.InternalCDOCommitInfoManager;
import org.eclipse.emf.cdo.spi.server.InternalRepository;
import org.eclipse.net4j.util.collection.Pair;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import com.couchbase.client.CouchbaseClient;

public class CommitInfoHandler extends JSONHandler {

	public CommitInfoHandler(CouchbaseClient client, IStore store) {
		super(client, store);
	}
	
	protected String getCommitInfoKey(CDOBranch branch, long timeStamp) {
		return getCommitInfoKey(branch.getID(), timeStamp);
	}
	
	protected String getCommitInfoKey(int branch, long timeStamp) {
		return "CDOCommitInfo::Branch-" + branch + "::TimeStamp-" + timeStamp;
	}
	
	protected String getCommitInfoListKey() {
		return "CDOCommitInfo";
	}

	public ICommitable createWriteCommitInfoCommitable(final CDOBranch branch, final long timeStamp,
			final long previousTimeStamp, final String userID, final String comment) {
		return new AbstractCommitable(getClient()) {			
			public void commit(OMMonitor monitor) {
				JSONCommitInfo info = new JSONCommitInfo(branch.getID(), timeStamp, previousTimeStamp, userID, comment);
				doCommit(getCommitInfoKey(branch, timeStamp), toJson(info), null, PersistMethod.ADD);
				String keyListValue = branch.getID() + getCommitInfoSeparator() + timeStamp;
				doCommit(getCommitInfoListKey(), keyListValue, null, PersistMethod.APPEND);
			}
		};
	}
	
	protected String getCommitInfoSeparator() {
		return "#";
	}
	
	public void loadCommitInfos(CDOBranch branch, long startTime, long endTime, CDOCommitInfoHandler handler) {
		List<Pair<Integer, Long>> keyPairs = getBranchAndTimestamp();
		// look for branch matches
		List<Pair<Integer, Long>> matchedBranch = new ArrayList<Pair<Integer, Long>>(); 
		if (branch != null) {
			for (Pair<Integer, Long> keyPair : keyPairs) {				
				if (branch.getID() == keyPair.getElement1()) {
					matchedBranch.add(keyPair);
				}
			}
		} else {
			// branch is not defined, we consider all branch/timestamp combinations
			matchedBranch = keyPairs;
		}

		
		// if not encoded in the endTime, infos should be returned
		// ordered in a descending manner (from latter commits to earlier)
		
		// if endTime < starTime, it means the info count is coded in that parameter
		// it means that we must return "endTime" commit infos
		// Depending on the sign of the number (positive or negative)
		// we should order in a ascending or descending, correspondingly
		
	    long count = CDOCommitInfoUtil.decodeCount(endTime);
	    final boolean descendant;
	    if (count < CDOBranchPoint.UNSPECIFIED_DATE || endTime >= startTime) {
	    	descendant = true;
	    } else {
	    	descendant = false;
	    }
	    count = Math.abs(count);
		List<Pair<Integer, Long>> matchedTimestamp = new ArrayList<Pair<Integer, Long>>();		
		Collections.sort(matchedBranch, new Comparator<Pair<Integer, Long>>() {
			public int compare(Pair<Integer, Long> arg0, Pair<Integer, Long> arg1) {
				if (descendant) {
					return arg0.getElement2() > arg1.getElement2() ? -1 : 1;	
				} else {
					return arg0.getElement2() > arg1.getElement2() ? 1 : -1;
				}
			}
		});
		int currentCount = 0;
		for (Pair<Integer, Long> keyPair : matchedBranch) {			
			if (startTime != CDOBranchPoint.UNSPECIFIED_DATE && keyPair.getElement2() < startTime)
		    {
				continue;
		    }
			if (endTime != CDOBranchPoint.UNSPECIFIED_DATE && endTime > startTime && keyPair.getElement2() > endTime)
		    {
				continue;
		    }
			currentCount++;
			if (currentCount > count) {
				break;
			}
			matchedTimestamp.add(keyPair);
		}
		// Fetch for database actual CommitInfo instances 
		List<JSONCommitInfo> infos = new ArrayList<JSONCommitInfo>();
		for (Pair<Integer, Long> match : matchedTimestamp) {
			String key = getCommitInfoKey(match.getElement1(), match.getElement2());
			Object obj = getValue(key);
			JSONCommitInfo info = fromJson((String)obj, JSONCommitInfo.class);
			infos.add(info);		
		}

	    InternalRepository repository = (InternalRepository)getStore().getRepository();
	    InternalCDOCommitInfoManager commitInfoManager = repository.getCommitInfoManager();
	    InternalCDOBranchManager branchManager = repository.getBranchManager();
	    
		for (JSONCommitInfo info : infos) {
			info.handle(branchManager, commitInfoManager, handler);
		}
		
	}

	private List<Pair<Integer, Long>> getBranchAndTimestamp() {
		List<String> splittedKeys = getMultiValue(getCommitInfoListKey());
		List<Pair<Integer, Long>> keyPairs = new ArrayList<Pair<Integer, Long>>();
		// Parse branch and key
		for (String key : splittedKeys) {
			String[] branchAndTimestamp = key.split(getCommitInfoSeparator());
			keyPairs.add(new Pair<Integer, Long>(Integer.valueOf(branchAndTimestamp[0]), Long.valueOf(branchAndTimestamp[1])));
		}
		return keyPairs;
	}
	
}
