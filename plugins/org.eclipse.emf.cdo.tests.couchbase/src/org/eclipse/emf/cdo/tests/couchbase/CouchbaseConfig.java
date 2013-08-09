/*
 * Copyright (c) 2011, 2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Victor Roldan Betancort - initial API and implementation
 */
package org.eclipse.emf.cdo.tests.couchbase;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.cdo.common.CDOCommonRepository.IDGenerationLocation;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.couchbase.ICouchbaseStore;
import org.eclipse.emf.cdo.server.internal.CouchbaseStore;
import org.eclipse.emf.cdo.tests.config.impl.RepositoryConfig;

import com.couchbase.client.clustermanager.BucketType;

/**
 * @author Victor Roldan Betancort
 */
public class CouchbaseConfig extends RepositoryConfig {
	private static final long serialVersionUID = 1L;

	private static final String HOST = "localhost";

	private static final String PORT = "8091";

	private static final String USER = "Administrator";

	private static final String PASS = "scaverok";

	private static final URI COUCHBASE_URI = URI.create("http://" + HOST + ":" + PORT + "/pools");

	private static Map<String, Boolean> bucketCleaned = new HashMap<String, Boolean>();
	
	private static boolean firstRun = true;
	
	private transient CouchbaseUtil couchbaseUtil;

	private BucketType bucketType;
	
	public CouchbaseConfig(BucketType bucketType) {
		super(ICouchbaseStore.TYPE, false, false, IDGenerationLocation.STORE);
		this.bucketType = bucketType;
	}

	@Override
	public void initCapabilities(Set<String> capabilities) {
		super.initCapabilities(capabilities);
	}

	@Override
	protected String getStoreName() {
		return "Couchbase";
	}

	public IStore createStore(String repoName) {
		initBucket(repoName);
		List<URI> uris = new LinkedList<URI>();
		uris.add(COUCHBASE_URI);
		return new CouchbaseStore(uris, repoName, USER, PASS);
	}

	// buckets get cleaned only once for the whole test-suite
	// (cleaning takes too much)
	private void initBucket(String repoName) {
		if (firstRun) {
			try {
				getCouchbaseUtil().deleteAllBuckets();
			} catch (Exception e) {
				e.printStackTrace();
			}
			firstRun = false;
		}
		Boolean bucketIsClean = bucketCleaned.get(repoName);
		if (bucketIsClean == null) {
			bucketIsClean = false;
		}
		if (!bucketIsClean || needsCleanRepos()) {
			try {
				getCouchbaseUtil().cleanBucket(repoName);
				bucketCleaned.put(repoName, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private CouchbaseUtil getCouchbaseUtil() {
		if (couchbaseUtil == null) {
			couchbaseUtil = new CouchbaseUtil(bucketType, HOST, PORT, USER, PASS);
		}
		return couchbaseUtil;
	}

	
	@Override
	protected void deactivateRepositories() {
		super.deactivateRepositories();
		if (couchbaseUtil != null) {
			couchbaseUtil.clear();
		}
		couchbaseUtil = null;	
	}
	
	
}
