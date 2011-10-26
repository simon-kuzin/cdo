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
package org.eclipse.internal.net4j.buffer;

import org.eclipse.net4j.buffer.BufferState;
import org.eclipse.net4j.buffer.IBufferProvider;
import org.eclipse.net4j.util.IErrorHandler;

import org.eclipse.spi.net4j.InternalBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Buffer indicating end-of-stream.
 * 
 * @author Egidijus Vaisnora
 */
public class EOSBuffer implements InternalBuffer
{
  private static final String ERROR_MESSAGE = "This buffer is end of stream buffer and cannot participate in IO operations";

  private short shannelID;

  private IErrorHandler errorHandler;

  public EOSBuffer(short shannelID)
  {
    this.shannelID = shannelID;
  }

  public IBufferProvider getBufferProvider()
  {
    throw new UnsupportedOperationException();
  }

  public void setBufferProvider(IBufferProvider bufferProvider)
  {
    throw new UnsupportedOperationException();
  }

  public IErrorHandler getErrorHandler()
  {
    return errorHandler;
  }

  public void setErrorHandler(IErrorHandler errorHandler)
  {
    this.errorHandler = errorHandler;
  }

  public boolean isEOS()
  {
    return true;
  }

  public void setEOS(boolean eos)
  {
    // Do nothing
  }

  public short getCapacity()
  {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  public short getChannelID()
  {
    return shannelID;
  }

  public BufferState getState()
  {
    return BufferState.GETTING;
  }

  public ByteBuffer startGetting(SocketChannel socketChannel) throws IllegalStateException, IOException
  {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  public ByteBuffer startPutting(short channelID) throws IllegalStateException
  {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  public boolean write(SocketChannel socketChannel) throws IllegalStateException, IOException
  {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  public void flip() throws IllegalStateException
  {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  public ByteBuffer getByteBuffer() throws IllegalStateException
  {
    return null;
  }

  public void release()
  {
    // Do nothing
  }

  public void clear()
  {
    // Do nothing
  }

  public String formatContent(boolean showHeader)
  {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  public void dispose()
  {
    // Do nothing
  }
}
