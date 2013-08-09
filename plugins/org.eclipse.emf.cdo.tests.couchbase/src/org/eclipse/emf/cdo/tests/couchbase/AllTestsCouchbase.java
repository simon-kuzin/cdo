/*
 * Copyright (c) 2011-2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Victor Roldan Betancort - initial API and implementation
 */
package org.eclipse.emf.cdo.tests.couchbase;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.emf.cdo.tests.AllConfigs;
import org.eclipse.emf.cdo.tests.bugzilla.Bugzilla_261218_Test;
import org.eclipse.emf.cdo.tests.bugzilla.Bugzilla_303466_Test;
import org.eclipse.emf.cdo.tests.bugzilla.Bugzilla_324585_Test;
import org.eclipse.emf.cdo.tests.bugzilla.Bugzilla_411927_Test;
import org.eclipse.emf.cdo.tests.config.IScenario;
import org.eclipse.emf.cdo.tests.config.impl.ConfigTest;

import com.couchbase.client.clustermanager.BucketType;

/**
 * Some comments on setting up the tests:
 * <p>
 * check the server does have memory free for new bucket creation.
 * If no memory is available, bucket creation fails silently
 * <p>
 * increase bucket number limit
 * http://www.couchbase.com/docs/couchbase-manual-2.0/couchbase-admin-restapi-max-buckets.html
 * curl -X POST -u admin:password -d maxBucketCount=20 http://ip_address:8091/internalSettings
 * 
 * @author Victor Roldan Betancort
 */
public class AllTestsCouchbase extends AllConfigs
{
  public static Test suite()
  {
    return new AllTestsCouchbase().getTestSuite();
  }

  @Override
  protected void initConfigSuites(TestSuite parent)
  {
    addScenario(parent, COMBINED, new CouchbaseConfig(BucketType.COUCHBASE), JVM, NATIVE);
  }

  @Override
  protected void initTestClasses(List<Class<? extends ConfigTest>> testClasses, IScenario scenario)
  {
    super.initTestClasses(testClasses, scenario);

    // Added here testcases to skip
    // takes too much
    testClasses.remove(Bugzilla_261218_Test.class);
    testClasses.remove(Bugzilla_324585_Test.class);
    testClasses.remove(Bugzilla_411927_Test.class);

    // this test-case uses files that cannot be found because 
    // are no longer where is expected (due to git)
    // (if manually added the file, it passes)
    testClasses.remove(Bugzilla_303466_Test.class);
  }
}
