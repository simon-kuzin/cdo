package org.eclipse.emf.cdo.server.internal.commitables;

import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.internal.couchbase.marshall.JSONRevision;
import org.eclipse.emf.cdo.server.internal.couchbase.marshall.JSONRevisionTypeAdapter;

import com.couchbase.client.CouchbaseClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class JSONHandler extends StoreHandler {

	private Gson gson;
	
	private void initGson() {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(JSONRevision.class, new JSONRevisionTypeAdapter(getStore()));
		gson = builder.serializeNulls().create();
	}
	
	public JSONHandler(CouchbaseClient client, IStore store) {
		super(client, store);
		initGson();
	}

	protected <TYPE> TYPE fromJson(String jsonString, Class<TYPE> clazz) {
		return gson.fromJson(jsonString, clazz);
	}
	
	protected String toJson(Object elemenToJson) {
		return gson.toJson(elemenToJson);
	}
	
}
