package org.eclipse.emf.cdo.server.internal.commitables;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.spy.memcached.CASValue;
import net.spy.memcached.internal.OperationFuture;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.server.internal.couchbase.bundle.OM;

import com.couchbase.client.CouchbaseClient;

public abstract class AbstractCommitable extends CouchbaseHandler implements ICommitable {
	
	private StoreMetaHandler storeMetaHandler;
	
	protected enum PersistMethod {
		ADD, DELETE, SET, APPEND, APPEND_IF_NOT_CONTAINED, DELETE_APPENDED
	}
	
	private CDOID cdoid = CDOID.NULL;
	
	public AbstractCommitable(CouchbaseClient client) {
		super(client);
		storeMetaHandler = new StoreMetaHandler(client);
	}
	
	public CDOID getCDOID() {
		return cdoid;
	}
	
	protected void setCDOID(CDOID id) {
		cdoid = id;
	}
	
	protected void doCommit(String key, Object value, CDOID id, PersistMethod persistMethod) {
	    Long storedLastCDOID = storeMetaHandler.getLastCDOID();
	    long lastCDOID = storedLastCDOID == null? -1 : storedLastCDOID;
	    boolean newLastCDOIDFound = false;
    	long commitableID = CDOIDUtil.getLong(id);
    	if (commitableID > lastCDOID) {
    		lastCDOID = commitableID;
    		newLastCDOIDFound = true;
    	}
    	// write actual data
    	switch (persistMethod) {
			case DELETE:
				getClient().delete(key);
				break;					
			case ADD:
				getClient().add(key, 0, value);
				break;
			case SET:
				getClient().set(key, 0, value);
				break;
			case DELETE_APPENDED:
				List<String> multivalue = getMultiValue(key);
				Iterator<String> it = multivalue.iterator();
				String valueString = value.toString();
				while (it.hasNext()) {
					String fetchedValue = it.next();
					if (fetchedValue.equals(valueString)) {
						it.remove();
					}
				}
				StringBuilder builder = new StringBuilder();
				for (String fetchedValue : multivalue) {
					builder.append(getSeparator() + fetchedValue);
				}
				if (builder.length() > 0) {
					builder.deleteCharAt(0);
				}
				doCommit(key, builder.toString(), id, PersistMethod.SET);
				break;
			case APPEND_IF_NOT_CONTAINED:
				List<String> valueList = getMultiValue(key);
				String valueString2 = value.toString();
				for(String valueFromList : valueList) {
					if (valueFromList.equals(valueString2)) {
						return;
					}
				}
				doCommit(key, value, id, PersistMethod.APPEND);
				break;
			case APPEND:
				CASValue<Object> cas = getClient().gets(key);
				if (cas == null || "".equals(cas.getValue())) { //FIXME why sometimes value is empty?
					doCommit(key, value.toString(), id, PersistMethod.SET);
				} else {
					doAppend(key, value, cas);
				}				
				break;
		}
	    if (newLastCDOIDFound) {
	    	storeMetaHandler.setLastCDOID(lastCDOID);
	    }
	}

	private void doAppend(String key, Object value, CASValue<Object> cas) {
		for(;;) {
			OperationFuture<Boolean> future = getClient().append(cas.getCas(), key, getSeparator() + value.toString());
			try {
				if (future.get()) {
					break;
				}
			} catch (InterruptedException e) {
				OM.LOG.error(e);
			} catch (ExecutionException e) {
				OM.LOG.error(e);
			}
		}
	}

}
