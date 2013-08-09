/*
 * Copyright (c) 2010-2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Victor Roldan Betancort - initial API and implementation
 */
package org.eclipse.emf.cdo.server.internal;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.IStoreFactory;
import org.eclipse.emf.cdo.server.couchbase.ICouchbaseStore;
import org.eclipse.emf.cdo.spi.server.RepositoryConfigurator;
import org.w3c.dom.Element;

public class CouchbaseStoreFactory implements IStoreFactory {
	
	private static final String PROPERTY_URI = "uri";

	private static final String PROPERTY_BUCKET = "bucket";
	
	private static final String PROPERTY_PASSWORD = "password";

	private static final Object PROPERTY_USER = "user";

	public String getStoreType() {
		return ICouchbaseStore.TYPE;
	}

	public IStore createStore(String repositoryName,
			Map<String, String> repositoryProperties, Element storeConfig) {
		Map<String, String> properties = RepositoryConfigurator.getProperties(
				storeConfig, 1);
		String uri = properties.get(PROPERTY_URI);
		String bucket = properties.get(PROPERTY_BUCKET);
		String user = properties.get(PROPERTY_USER);
		String pass = properties.get(PROPERTY_PASSWORD);
		List<URI> uris = new LinkedList<URI>();
		uris.add(URI.create(uri));
		return new CouchbaseStore(uris, bucket, user, pass);
	}

}
