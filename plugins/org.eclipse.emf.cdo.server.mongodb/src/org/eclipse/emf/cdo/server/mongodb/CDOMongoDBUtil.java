/*
 * Copyright (c) 2004 - 2011 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Stefan Winkler - 271444: [DB] Multiple refactorings
 *    Stefan Winkler - 249610: [DB] Support external references (Implementation)
 */
package org.eclipse.emf.cdo.server.mongodb;

import org.eclipse.emf.cdo.server.internal.mongodb.MongoDBBrowserPage;
import org.eclipse.emf.cdo.server.internal.mongodb.MongoDBStore;

import org.eclipse.net4j.util.container.IManagedContainer;

import com.mongodb.MongoURI;

/**
 * @author Eike Stepper
 */
public final class CDOMongoDBUtil
{
  private CDOMongoDBUtil()
  {
  }

  public static void prepareContainer(IManagedContainer container)
  {
    container.registerFactory(new MongoDBBrowserPage.Factory());
  }

  public static IMongoDBStore createStore(String uri, String dbName)
  {
    MongoURI mongoURI = new MongoURI(uri);
    MongoDBStore store = new MongoDBStore();
    store.setMongoURI(mongoURI);
    store.setDBName(dbName);
    return store;
  }
}
