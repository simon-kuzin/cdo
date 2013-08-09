package org.eclipse.emf.cdo.server.internal.commitables;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.net4j.util.om.monitor.OMMonitor;

import com.couchbase.client.CouchbaseClient;

public class StoreMetaHandler extends CouchbaseHandler {

	public StoreMetaHandler(CouchbaseClient client) {
		super(client);
	}

	public Long getLastCDOID() {
		return (Long)getValue(getLastCDOIDKey());
	}
	
	public Boolean isFirstStart() {
		Boolean result = (Boolean)getValue(getIsFirstStartKey());
		if (result != null) {
			return result;
		}
		return true;
	}
	
	public void setLastCDOID(final Long lastCDOID) {
		new AbstractCommitable(getClient()) {
			public void commit(OMMonitor monitor) {
				doCommit(getLastCDOIDKey(), lastCDOID, null, PersistMethod.SET);
			}
		}.commit(null);
	}
	
	public void setIsFirstStart(final Boolean firstStart) {
		new AbstractCommitable(getClient()) {
			public void commit(OMMonitor monitor) {
				doCommit(getIsFirstStartKey(), firstStart, null, PersistMethod.SET);
			}
		}.commit(null);
	}
	
	public Long getCreationTime() {
		Long value = (Long)getValue(getCreationTimeKey());
		if (value != null) {
			return value;
		}
		value = System.currentTimeMillis();
		setCreationTime(value);
		return value;
	}
	
	public void setCreationTime(final long creationTime) {
		new AbstractCommitable(getClient()) {
			public void commit(OMMonitor monitor) {
				doCommit(getCreationTimeKey(), creationTime, null, PersistMethod.SET);
			}
		}.commit(null);
	}
	
	public Long getLastCommitTime() {
		Long value = (Long)getValue(getLastCommitTimeKey());
		if (value != null) {
			return value;
		}
		return 0l;
	}
	
	public void setLastCommitTime(final long commitTime) {
		new AbstractCommitable(getClient()) {
			public void commit(OMMonitor monitor) {
				doCommit(getLastCommitTimeKey(), commitTime, null, PersistMethod.SET);
			}
		}.commit(null);
	}

	public Map<String, String> getPersistentProperties(Set<String> names) {
	    
		if (names == null || names.isEmpty())
	    {
		  List<String> propertyKeys = getMultiValue(getAllPropertyKey());
	      Map<String, String> result = new HashMap<String, String>();
	      for (String propertyKey : propertyKeys) {
	    	  result.put(propertyKey, (String)getValue(getPropertyKey(propertyKey)));
	      }
	      return result;
	    }

	    Map<String, String> result = new HashMap<String, String>();
	    for (String key : names)
	    {
	      String value = (String)getValue(getPropertyKey(key));
	      if (value != null)
	      {
	        result.put(key, value);
	      }
	    }

	    return result;
	}

	public void setPersistentProperties(final Map<String, String> properties) {
		new AbstractCommitable(getClient()) {
			public void commit(OMMonitor monitor) {
				for (String key : properties.keySet()) {
					doCommit(getPropertyKey(key), properties.get(key), null, PersistMethod.SET);
					doCommit(getAllPropertyKey(), key, null, PersistMethod.APPEND_IF_NOT_CONTAINED);
				}				
			}
		}.commit(null);

	}

	public void removePersistentProperties(final Set<String> names) {
		new AbstractCommitable(getClient()) {
			public void commit(OMMonitor monitor) {
				for (String key : names) {
					doCommit(getPropertyKey(key), null, null, PersistMethod.DELETE);
					doCommit(getAllPropertyKey(), key, null, PersistMethod.DELETE_APPENDED);
				}				
			}
		}.commit(null);
	}

	private String getLastCDOIDKey() {
		return "lastCDOID";
	}

	private String getIsFirstStartKey() {
		return "isFirstStart";
	}

	private String getCreationTimeKey() {
		return "creationTime";
	}

	private String getLastCommitTimeKey() {
		return "lastCommitTime";
	}

	private String getAllPropertyKey() {
		return "AllStoreProperties";
	}

	private String getPropertyKey(String key) {
		return "StoreProperty::" + key;
	}
}
