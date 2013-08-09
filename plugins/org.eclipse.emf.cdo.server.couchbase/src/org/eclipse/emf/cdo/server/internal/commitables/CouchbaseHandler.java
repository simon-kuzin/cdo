package org.eclipse.emf.cdo.server.internal.commitables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.couchbase.client.CouchbaseClient;

public class CouchbaseHandler {
	
	private CouchbaseClient client;
	
	public CouchbaseHandler(CouchbaseClient client) {
		this.client = client;
	}

	protected CouchbaseClient getClient() {
		return client;
	}
	
	protected Object getValue(String key) {
		return getClient().get(key);
	}
	
	@SuppressWarnings("unchecked")
	protected List<String> getMultiValue(String key) {
		String keys = (String)getValue(key);
		// FIXME there should not be empty values committed, investigate this
		if (keys == null || "".equals(keys)) { 
			return Collections.EMPTY_LIST;
		}
		String[] splittedValues = keys.split(getSeparator());
		List<String> result = new ArrayList<String>();
		for (String value : splittedValues) {
			result.add(value);
		}
		return result;
	}
	
	protected String getSeparator() {
		return ";";
	}

	
}
