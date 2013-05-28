/*
 * Copyright (c) 2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.tests.bugzilla;

import org.eclipse.emf.cdo.common.revision.CDORevisionCache;
import org.eclipse.emf.cdo.common.revision.CDORevisionUtil;
import org.eclipse.emf.cdo.common.util.CDOCommonUtil;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.tests.AbstractCDOTest;
import org.eclipse.emf.cdo.tests.config.IConfig;
import org.eclipse.emf.cdo.tests.config.impl.ConfigTest.Skips;
import org.eclipse.emf.cdo.tests.config.impl.RepositoryConfig;
import org.eclipse.emf.cdo.tests.model1.Category;
import org.eclipse.emf.cdo.tests.model1.Company;
import org.eclipse.emf.cdo.transaction.CDOTransaction;

import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.eclipse.net4j.util.om.OMPlatform;

import org.eclipse.emf.common.util.EList;

/**
 * Bug 396743: [DB] List size column mismatching the row entries.
 *
 * @author Eike Stepper
 */
// This test tries to produce the problem until it occurs. Runs forever if the problem is fixed.
@Skips(IConfig.CAPABILITY_ALL)
public class Bugzilla_396743_Test extends AbstractCDOTest
{
  private static final boolean REUSE_TRANSACTIONS = true;

  /**
   * @author Eike Stepper
   */
  private abstract class ClientThread extends Thread
  {
    private String indent;

    public ClientThread(int id)
    {
      super("TestClient-" + id);
      indent = "                                                                                                                  "
          .substring(0, id * 40);
    }

    @Override
    public void run()
    {
      try
      {
        CDOSession session = openSession();
        ((org.eclipse.emf.cdo.net4j.CDONet4jSession)session).options().setCommitTimeout(100000000);

        CDOTransaction transaction = null;
        Company company = null;

        if (REUSE_TRANSACTIONS)
        {
          transaction = session.openTransaction();
          company = getCompany(transaction);
        }

        while (!CDOCommonUtil.STOP_CLIENTS)
        {
          if (!REUSE_TRANSACTIONS)
          {
            transaction = session.openTransaction();
            company = getCompany(transaction);
          }

          try
          {
            int count;
            synchronized (transaction)
            {
              modifyModel(company);
              transaction.commit();

              count = company.getCategories().size();
            }

            println("Committed " + count);
            sleep(3); // Give other threads a chance
          }
          catch (Exception ex)
          {
            String message = ex.getMessage();
            if (message.indexOf("Attempt by Transaction") != -1
                || message.indexOf("This transaction has conflicts") != -1)
            {
              println("FAILED");
            }
            else
            {
              println(message);
              ex.printStackTrace();
            }

            if (REUSE_TRANSACTIONS)
            {
              transaction.rollback();
            }
          }
          finally
          {
            if (!REUSE_TRANSACTIONS)
            {
              LifecycleUtil.deactivate(transaction);
              transaction = null;
              company = null;
            }
          }
        }

        println("ENDE");
      }
      catch (Throwable t)
      {
        t.printStackTrace();
      }
    }

    private void println(String string)
    {
      // if (string.length() > 100)
      // {
      // string = string.substring(0, 99);
      // }

      System.out.println(indent + string);
    }

    private Company getCompany(CDOTransaction transaction)
    {
      CDOResource resource = transaction.getResource(getResourcePath("/test1"));
      return (Company)resource.getContents().get(0);
    }

    protected abstract void modifyModel(Company company);
  }

  /**
   * @author Eike Stepper
   */
  private final class Adder extends ClientThread
  {
    public Adder(int id)
    {
      super(id);
    }

    @Override
    protected void modifyModel(Company company)
    {
      Category category = getModel1Factory().createCategory();
      company.getCategories().add(category);
    }
  }

  /**
   * @author Eike Stepper
   */
  private final class Deleter extends ClientThread
  {
    public Deleter(int id)
    {
      super(id);
    }

    @Override
    protected void modifyModel(Company company)
    {
      EList<Category> categories = company.getCategories();
      categories.remove(Math.random() * categories.size());
    }
  }

  @Override
  protected void doSetUp() throws Exception
  {
    super.doSetUp();
    OMPlatform.INSTANCE.setDebugging(false);

    getRepositoryConfig().getTestProperties().put(RepositoryConfig.PROP_TEST_REVISION_MANAGER,
        CDORevisionUtil.createRevisionManager(CDORevisionCache.NOOP));

    // new Thread("CC-Trigger")
    // {
    // @Override
    // public void run()
    // {
    // ConcurrencyUtil.sleep(3000);
    //
    // for (;;)
    // {
    // ConcurrencyUtil.sleep(1000);
    //
    // try
    // {
    // clearCache(getRepository().getRevisionManager());
    // System.out.println("CACHE CLEARED");
    // }
    // catch (Exception ex)
    // {
    // ex.printStackTrace();
    // }
    // }
    // }
    // }.start();
  }

  public void testWrongListSizeAdditions() throws Exception
  {
    {
      CDOSession session = openSession();
      CDOTransaction transaction = session.openTransaction();
      CDOResource resource = transaction.createResource(getResourcePath("/test1"));
      resource.getContents().add(getModel1Factory().createCompany());
      transaction.commit();
      session.close();
    }

    Adder thread1 = new Adder(1);
    Adder thread2 = new Adder(2);

    thread1.start();
    thread2.start();

    thread1.join();
    thread2.join();
  }

  public void _testWrongListSizeAdditionsAndDeletions() throws Exception
  {
    {
      CDOSession session = openSession();
      CDOTransaction transaction = session.openTransaction();
      CDOResource resource = transaction.createResource(getResourcePath("/test1"));
      resource.getContents().add(getModel1Factory().createCompany());
      transaction.commit();
      session.close();
    }

    Adder thread1 = new Adder(1);
    Adder thread2 = new Adder(2);
    Deleter thread3 = new Deleter(3);

    thread1.start();
    thread2.start();
    thread3.start();

    thread1.join();
    thread2.join();
  }
}
