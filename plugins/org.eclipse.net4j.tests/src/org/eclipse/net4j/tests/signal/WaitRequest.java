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
package org.eclipse.net4j.tests.signal;

import org.eclipse.net4j.channel.IChannel;
import org.eclipse.net4j.signal.RequestWithConfirmation;
import org.eclipse.net4j.util.io.ExtendedDataInputStream;
import org.eclipse.net4j.util.io.ExtendedDataOutputStream;

import java.util.concurrent.CountDownLatch;

/**
 * @author Egidijus Vaisnora
 */
public class WaitRequest extends RequestWithConfirmation<Integer>
{
  private static CountDownLatch writelatch;

  public WaitRequest(TestSignalProtocol protocol)
  {
    super(protocol, TestSignalProtocol.SIGNAL_WITH_WAIT);
  }

  public static CountDownLatch getWriteSemaphore()
  {
    return writelatch;
  }

  @Override
  protected void requesting(ExtendedDataOutputStream out) throws Exception
  {
    writelatch = new CountDownLatch(1);
    out.writeInt(100);
  }

  @Override
  protected Integer confirming(ExtendedDataInputStream in) throws Exception
  {
    final IChannel channel = getProtocol().getChannel();

    new Thread(new Runnable()
    {
      public void run()
      {
        channel.close();
        writelatch.countDown();
      }
    }).start();

    int readValue = in.readInt();
    return readValue;
  }
}
