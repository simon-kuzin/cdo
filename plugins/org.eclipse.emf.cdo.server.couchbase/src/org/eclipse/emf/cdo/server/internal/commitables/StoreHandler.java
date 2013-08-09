package org.eclipse.emf.cdo.server.internal.commitables;

import org.eclipse.emf.cdo.server.IStore;

import com.couchbase.client.CouchbaseClient;

public class StoreHandler extends CouchbaseHandler {
		
	private IStore store;
	
	public StoreHandler(CouchbaseClient client, IStore store) {
		super(client);
		this.store = store;
	}
	
	protected IStore getStore() {
		return store;
	}
}
