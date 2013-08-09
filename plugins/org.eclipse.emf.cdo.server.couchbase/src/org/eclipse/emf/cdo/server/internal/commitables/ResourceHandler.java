package org.eclipse.emf.cdo.server.internal.commitables;

import java.util.List;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.eresource.EresourcePackage;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.IStoreAccessor;
import org.eclipse.emf.cdo.server.IStoreAccessor.QueryResourcesContext;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import com.couchbase.client.CouchbaseClient;

public class ResourceHandler extends JSONHandler {

	public ResourceHandler(CouchbaseClient client, IStore store) {
		super(client, store);
	}
	
	/**
	 * We are persisting here:
	 * - Association folder + name = resourceID
	 * - Association resourceID = parentFolderID
	 * - Association resourceID = resourceName
	 * - List of all resources
	 */
	public ICommitable createStoreResourceCommitable(final InternalCDORevision revision) {
		return new AbstractCommitable(getClient()) {			
			public void commit(OMMonitor monitor) {
				if (EresourcePackage.eINSTANCE.getCDOResourceNode().isSuperTypeOf(revision.getEClass())) {
					String currentName = (String)revision.get(EresourcePackage.eINSTANCE.getCDOResourceNode_Name(), 0);				
					Long resourceID = (Long)getValue(getResourceIDKey(revision));
					if (resourceID == null || resourceID == CDOIDUtil.getLong(revision.getID())) {						
						Long formerFolder = (Long)getValue(getResourceFolderKey(revision));						
						Long currentFolder = CDOIDUtil.getLong((CDOID)revision.getContainerID());						
						formerFolder = formerFolder != null ? formerFolder : currentFolder;
						String formerName = (String)getValue(getResourceNameKey(revision));						
						if (currentFolder != formerFolder || formerName != currentName) { // this updates data in case path changed
							doCommit(getAllResourceKey(), getResourceIDKey(formerFolder, formerName), revision.getID(), PersistMethod.DELETE_APPENDED);
							doCommit(getResourceIDKey(formerFolder, formerName), null, null, PersistMethod.DELETE);
						}
						doCommit(getResourceIDKey(revision), getResourceID(revision), revision.getID(), PersistMethod.SET);
						doCommit(getResourceFolderKey(revision), CDOIDUtil.getLong((CDOID)revision.getContainerID()), revision.getID(), PersistMethod.SET);
						doCommit(getResourceNameKey(revision), currentName == null? "" : currentName, revision.getID(), PersistMethod.SET);
						// FIXME APPEND_IF_NOT_CONTAINED is highly inefficient
						doCommit(getAllResourceKey(), getResourceIDKey(revision), revision.getID(), PersistMethod.APPEND_IF_NOT_CONTAINED);
					} else {
						throw new RuntimeException("Duplicate Resource " + currentName);
					}
				}	
			}
		};
	}

	public ICommitable createDetachResourceCommitable(final InternalCDORevision revision) {
		return new AbstractCommitable(getClient()) {			
			public void commit(OMMonitor monitor) {
				if (EresourcePackage.eINSTANCE.getCDOResourceNode().isSuperTypeOf(revision.getEClass())) {
					doCommit(getResourceFolderKey(revision), null, null, PersistMethod.DELETE);
					doCommit(getResourceNameKey(revision), null, null, PersistMethod.DELETE);
					doCommit(getResourceIDKey(revision), getResourceID(revision), revision.getID(), PersistMethod.DELETE);
					doCommit(getAllResourceKey(), getResourceIDKey(revision), revision.getID(), PersistMethod.DELETE_APPENDED);
				}	
			}
		};
	}
	
	protected String getResourceIDKey(InternalCDORevision revision) {
		String name = (String)revision.get(EresourcePackage.eINSTANCE.getCDOResourceNode_Name(), 0);
		return getResourceIDKey(CDOIDUtil.getLong((CDOID)revision.getContainerID()), name);
	}
	
	protected String getResourceIDKey(long containerID, String resourceName) {
		return "CDOResources::" + containerID + "::" + resourceName;
	}
	
	protected String getAllResourceKey() {
		return "AllCDOResources";
	}
	
	protected String getResourceFolderKey(InternalCDORevision revision) {		
		return getResourceFolderKey(CDOIDUtil.getLong(revision.getID()));
	}
	
	protected String getResourceNameKey(InternalCDORevision revision) {		
		return getResourceNameKey(CDOIDUtil.getLong(revision.getID()));
	}

	protected String getResourceFolderKey(long resourceID) {
		return "CDOResourceFolder::" + resourceID;
	}
	
	protected String getResourceNameKey(long resourceID) {
		return "CDOResourceName::" + resourceID;
	}
	
	protected Long getResourceID(InternalCDORevision revision) {
		return CDOIDUtil.getLong(revision.getID());
	}	
	
	protected String getResourceKey(IStoreAccessor.QueryResourcesContext context) {
		final long folderID = CDOIDUtil.getLong(context.getFolderID());
	    final String name = context.getName();
	    return getResourceIDKey(folderID, name);
	}
	
	private String getNameFromID(String resourceID) {
		String[] splitted = resourceID.split("::");
		return splitted[2];
	}

	private Long getFolderFromID(String resourceID) {
		String[] splitted = resourceID.split("::");
		return Long.parseLong(splitted[1]);
	}

	private void addExistingResourceToContext(QueryResourcesContext context,
			String resourceID) {
		Long result = (Long)getValue(resourceID);
		if (result != null) {
			CDOID id = CDOIDUtil.createLong(result);
		    context.addResource(id);	
		}
	}

	public void queryResources(QueryResourcesContext context) {
	    final boolean exactMatch = context.exactMatch();
	    if (exactMatch) {
			Long result = (Long)getValue(getResourceKey(context));
		    if (result != null) {
		    	CDOID id = CDOIDUtil.createLong(result);
			    context.addResource(id);	
		    }
	    } else {
	    	// FIXME This is not efficient, maybe implement with Couchbase queries?
	    	List<String> resourceIDs = getMultiValue(getAllResourceKey());
			CDOID folderID = context.getFolderID();
	    	for (String resourceID : resourceIDs) {	    		
	    		if (getNameFromID(resourceID).startsWith(context.getName())) {
	    			if (folderID != null && folderID != CDOID.NULL) {
	    				if (CDOIDUtil.getLong(folderID) == getFolderFromID(resourceID)) {
	    					addExistingResourceToContext(context, resourceID);	
	    				}
	    			} else {
	    				// if no folder restriction is not defined in the context, we check all resources
		    			addExistingResourceToContext(context, resourceID);		
	    			}
	    		}
	    	}
	    }
	}
}
