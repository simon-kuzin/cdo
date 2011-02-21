/**
 * Copyright (c) 2004 - 2011 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.tests.mongodb;

import org.eclipse.emf.cdo.server.CDOServerBrowser;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.internal.mongodb.MongoDBStore;
import org.eclipse.emf.cdo.server.mongodb.CDOMongoDBUtil;
import org.eclipse.emf.cdo.tests.config.impl.RepositoryConfig;

import org.eclipse.net4j.util.WrappedException;
import org.eclipse.net4j.util.container.IPluginContainer;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;

import java.util.Map;

/**
 * @author Eike Stepper
 */
public class MongoDBStoreRepositoryConfig extends RepositoryConfig
{
  public static final MongoDBStoreRepositoryConfig INSTANCE = new MongoDBStoreRepositoryConfig(false, false);

  public static final MongoDBStoreRepositoryConfig AUDITING = new MongoDBStoreRepositoryConfig(true, false);

  public static final MongoDBStoreRepositoryConfig BRANCHING = new MongoDBStoreRepositoryConfig(true, true);

  private static final long serialVersionUID = 1L;

  private boolean auditing;

  private boolean branching;

  private transient CDOServerBrowser mongoBrowser;

  public MongoDBStoreRepositoryConfig(boolean auditing, boolean branching)
  {
    super("MongoDBStore" + (branching ? " (branching)" : auditing ? " (auditing)" : ""));
    this.auditing = auditing;
    this.branching = branching;
  }

  @Override
  public void setUp() throws Exception
  {
    CDOMongoDBUtil.prepareContainer(IPluginContainer.INSTANCE);

    mongoBrowser = new CDOServerBrowser(MongoDBStore.REPOS);
    mongoBrowser.setPort(7778);
    mongoBrowser.activate();

    super.setUp();
  }

  @Override
  public void tearDown() throws Exception
  {
    mongoBrowser.deactivate();
    super.tearDown();
  }

  @Override
  public IStore createStore(String repoName)
  {
    MongoURI mongoURI = new MongoURI("mongodb://localhost");
    if (!isRestarting())
    {
      dropDatabase(mongoURI, repoName);
    }

    return CDOMongoDBUtil.createStore(mongoURI, repoName);
  }

  protected void dropDatabase(MongoURI mongoURI, String repoName)
  {
    Mongo mongo = null;

    try
    {
      mongo = new Mongo(mongoURI);
      DB db = mongo.getDB(repoName);
      if (!db.getCollectionNames().isEmpty())
      {
        db.dropDatabase();
      }
    }
    catch (Exception ex)
    {
      throw WrappedException.wrap(ex);
    }
    finally
    {
      if (mongo != null)
      {
        mongo.close();
      }
    }
  }

  @Override
  protected void initRepositoryProperties(Map<String, String> props)
  {
    super.initRepositoryProperties(props);
    props.put(IRepository.Props.SUPPORTING_AUDITS, Boolean.toString(auditing));
    props.put(IRepository.Props.SUPPORTING_BRANCHES, Boolean.toString(branching));
  }
}
