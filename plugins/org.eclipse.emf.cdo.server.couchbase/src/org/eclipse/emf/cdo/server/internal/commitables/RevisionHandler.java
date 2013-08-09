package org.eclipse.emf.cdo.server.internal.commitables;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.revision.CDORevisionHandler;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.IStoreAccessor.QueryXRefsContext;
import org.eclipse.emf.cdo.server.internal.couchbase.marshall.JSONRevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.net4j.util.ObjectUtil;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import com.couchbase.client.CouchbaseClient;

public class RevisionHandler extends JSONHandler {
	
	private ResourceHandler resourceHandler;
	
	public RevisionHandler(CouchbaseClient client, IStore store) {
		super(client, store);
		resourceHandler = new ResourceHandler(getClient(), getStore());
	}
	
	public ICommitable createRevisionWriteCommitable(final InternalCDORevision revision) {
		return new AbstractCommitable(getClient()) {
			public void commit(OMMonitor monitor) {
				resourceHandler.createStoreResourceCommitable(revision).commit(monitor);
				JSONRevision rev = JSONRevision.getJSONRevision(revision);
				doCommit(getRevisionByIdAndBranchKey(revision), toJson(rev), revision.getID(), PersistMethod.SET);
				//FIXME modeling this information as lists wont scale well, and introduces overhead on writting, and this info is not requested usually
				// Investigate Couchbase queries
				doCommit(getRevisionByEClassKey(revision), CDOIDUtil.getLong(revision.getID()), revision.getID(), PersistMethod.APPEND_IF_NOT_CONTAINED);
				doCommit(getRevisionByEClassAndBranchKey(revision), CDOIDUtil.getLong(revision.getID()), revision.getID(), PersistMethod.APPEND_IF_NOT_CONTAINED);
				doCommit(getAllRevisionInBranchKey(revision.getBranch()), CDOIDUtil.getLong(revision.getID()), revision.getID(), PersistMethod.APPEND_IF_NOT_CONTAINED);
			}
		};
	}
	
	public ICommitable createRevisionDetachCommitable(final CDOID id, final CDOBranch branch) {
		return new AbstractCommitable(getClient()) {
			public void commit(OMMonitor monitor) {				
				InternalCDORevision revision = readRevision(id, branch);
				resourceHandler.createDetachResourceCommitable(revision).commit(monitor);
				doCommit(getRevisionByIdAndBranchKey(revision), null, id, PersistMethod.DELETE);
				doCommit(getRevisionByEClassKey(revision), CDOIDUtil.getLong(id), id, PersistMethod.DELETE_APPENDED);
				doCommit(getRevisionByEClassAndBranchKey(revision), CDOIDUtil.getLong(id), id, PersistMethod.DELETE_APPENDED);
				doCommit(getAllRevisionInBranchKey(revision.getBranch()), CDOIDUtil.getLong(revision.getID()), revision.getID(), PersistMethod.DELETE_APPENDED);
			}
		};
	}
	
	public InternalCDORevision readRevision(CDOID id, CDOBranchPoint branchPoint) {
		return readRevision(id, branchPoint.getBranch());
	}
	
	public InternalCDORevision readRevision(CDOID id, CDOBranch branch) {
		String key = getRevisionByIdAndBranchKey(id, branch);
		String jsonString = (String)getValue(key);
		if (jsonString == null) {
			return null;
		}
		JSONRevision revision = fromJson(jsonString, JSONRevision.class);
		return JSONRevision.getCDORevision(getStore(), revision);
	}
	
	public List<CDOID> readAllRevisionsInBranch(CDOBranch branch) {
		return getCDOIDList(getMultiValue(getAllRevisionInBranchKey(branch)));		
	}
	
	public List<CDOID> readAllRevisionsOfEClassInBranch(EClass eClass, CDOBranch branch) {
		return getCDOIDList(getMultiValue(getRevisionByEClassAndBranchKey(eClass, branch)));
	}
	
	public void handleRevisions(EClass eClass, CDOBranch branch, long timeStamp, boolean exactTime, CDORevisionHandler handler) {
		for (CDOID id : eClass != null ? readAllRevisionsOfEClassInBranch(eClass, branch) : readAllRevisionsInBranch(branch)) {
			InternalCDORevision revision = readRevision(id, branch);
		    if (timeStamp != CDOBranchPoint.INVALID_DATE)
		    {
		      if (exactTime)
		      {
		        if (timeStamp != CDOBranchPoint.UNSPECIFIED_DATE && revision.getTimeStamp() != timeStamp)
		        {
		          continue;
		        }
		      }
		      else
		      {
		        if (!revision.isValid(timeStamp))
		        {
		          continue;
		        }
		      }
		    }
		    handler.handleRevision(revision);
		}
	}
	
	private List<CDOID> getCDOIDList(List<String> cdoids) {
		List<CDOID> ids = new ArrayList<CDOID>();
		for (String id : cdoids) {
			CDOID cdoid = CDOIDUtil.createLong(Long.parseLong(id));
			ids.add(cdoid);
		}
		return ids;
	}

	protected String getRevisionByIdAndBranchKey(InternalCDORevision revision) {
		return getRevisionByIdAndBranchKey(revision.getID(), revision.getBranch());
	}
	
	protected String getRevisionByIdAndBranchKey(CDOID id, CDOBranch branch) {
		return "CDORevision::" + branch.getName() + "::" + CDOIDUtil.getLong(id);
	}
	
	protected String getRevisionByEClassKey(InternalCDORevision revision) {
		return getRevisionByEClassKey(revision.getEClass()); 
	}

	protected String getRevisionByEClassKey(EClass eClass) {
		return "RevisionByEClass::" + eClass.getEPackage().getNsURI() + "::" + eClass.getName(); 
	}
	
	protected String getRevisionByEClassAndBranchKey(InternalCDORevision revision) {
		return getRevisionByEClassAndBranchKey(revision.getEClass(), revision.getBranch());
	}
	
	protected String getRevisionByEClassAndBranchKey(EClass eClass, CDOBranch branch) {
		return "RevisionByEClassAndBranch::" + eClass.getEPackage().getNsURI() 
				+ "::" + eClass.getName() + branch.getName(); 
	}
		
	protected String getAllRevisionInBranchKey(CDOBranch branch) {
		return "AllCDORevision::" + branch.getName();
	}

	public void queryXRefs(QueryXRefsContext context) {
	    final CDOBranch branch = context.getBranch();
	    for (final CDOID target : context.getTargetObjects().keySet())
	    {
	      for (final EClass eClass : context.getSourceCandidates().keySet())
	      {
	        final List<EReference> eReferences = context.getSourceCandidates().get(eClass);
	        for (EReference eReference : eReferences) {
	        	List<String> byEClassIds = getMultiValue(getRevisionByEClassKey(eClass));
	        	for (String id : byEClassIds) {
	        		CDOID source = CDOIDUtil.createLong(Long.parseLong(id));
	        		InternalCDORevision revision = readRevision(source, branch);
	        		Object obj = revision.getValue(eReference);
	        		if (obj instanceof CDOID) {
	        			if (ObjectUtil.equals(obj, target)) {
	        				context.addXRef(target, source, eReference, 0);
	        			}
	        		} else if (obj instanceof List<?>) {
	        			int index = 0;
	        			for (Object objFromList :(List<?>)obj) {	        				
	        				if (ObjectUtil.equals(objFromList, target)) {
		        				context.addXRef(target, source, eReference, index);
		        			} 
	        				++index;
	        			}
	        		}
	        	}
	        }
	      }
	    }
	}
	
}
