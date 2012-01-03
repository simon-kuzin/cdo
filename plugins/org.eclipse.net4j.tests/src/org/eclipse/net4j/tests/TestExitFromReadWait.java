/*
 * Copyright (c) 2004 - 2011 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.net4j.tests;

import org.eclipse.net4j.tests.signal.TestSignalProtocol;
import org.eclipse.net4j.tests.signal.WaitRequest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Egidijus Vaisnora
 */
public class TestExitFromReadWait extends AbstractProtocolTest
{
  private static long WAIT_TIMEOUT_SECONDS = 20;

  public void testExitFromReadWait() throws Exception
  {
    TestSignalProtocol protocol = null;

    try
    {
      startTransport();
      protocol = new TestSignalProtocol(getConnector());
      protocol.setTimeout(TimeUnit.MILLISECONDS.convert(WAIT_TIMEOUT_SECONDS * 2, TimeUnit.SECONDS));

      final CountDownLatch latch = new CountDownLatch(1);

      final TestSignalProtocol protocolNested = protocol;

      new Thread(new Runnable()
      {
        public void run()
        {
          try
          {
            new WaitRequest(protocolNested).send();
          }
          catch (Exception ex)
          {
            ex.printStackTrace();
          }
          finally
          {
            // fails with exceptions
            latch.countDown();
          }
        }
      }).start();

      latch.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      assertEquals("Signal waits for input, but it shouldn't.", 0, latch.getCount());
    }
    finally
    {
      if (protocol != null)
      {
        protocol.close();
      }
    }
  }
}
