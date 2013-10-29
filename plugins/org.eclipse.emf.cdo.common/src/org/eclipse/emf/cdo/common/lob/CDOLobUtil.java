/*
 * Copyright (c) 2011, 2012 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Christian W. Damus (CEA LIST) - bug 418454
 */
package org.eclipse.emf.cdo.common.lob;

import org.eclipse.net4j.util.io.ExtendedDataInput;
import org.eclipse.net4j.util.io.ExtendedDataOutput;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Some useful helpers for dealing with {@link CDOLob large objects}.
 * 
 * @author Eike Stepper
 * @since 4.0
 */
public final class CDOLobUtil
{
  private CDOLobUtil()
  {
  }

  public static CDOBlob readBlob(ExtendedDataInput in) throws IOException
  {
    return new CDOBlob(in);
  }

  public static CDOClob readClob(ExtendedDataInput in) throws IOException
  {
    return new CDOClob(in);
  }

  /**
   * @since 4.3
   */
  public static CDOClobWriter writeClob()
  {
    return new CDOClobWriter();
  }

  public static void write(ExtendedDataOutput out, CDOLob<?> lob) throws IOException
  {
    lob.write(out);
  }

  public static CDOBlob createBlob(byte[] id, long size)
  {
    return new CDOBlob(id, size);
  }

  public static CDOClob createClob(byte[] id, long size)
  {
    return new CDOClob(id, size);
  }

  public static void setStore(CDOLobStore store, CDOLob<?> lob) throws IOException
  {
    lob.setStore(store);
  }

  /**
   * A {@link Writer} that produces a {@link #getClob() CDOClob}.
   * 
   * @author Christian W. Damus (CEA LIST)
   * @since 4.3
   */
  public static class CDOClobWriter extends Writer
  {
    private StringWriter buffer = new StringWriter();

    private CDOClob clob;

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException
    {
      checkBuffer();
      buffer.write(cbuf, off, len);
    }

    @Override
    public void close() throws IOException
    {
      if (buffer != null)
      {
        clob = new CDOClob(new StringReader(buffer.toString()));
        buffer = null;
      }
    }

    @Override
    public void flush() throws IOException
    {
      checkBuffer();
      buffer.flush();
    }

    public CDOClob getClob()
    {
      return clob;
    }

    void checkBuffer() throws IOException
    {
      if (buffer == null)
      {
        throw new IOException("CDOClobWriter closed"); //$NON-NLS-1$
      }
    }
  }
}
