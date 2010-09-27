/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.common.model.lob;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * @author Eike Stepper
 * @since 4.0
 */
public interface CDOLobStore
{
  public Info putBinary(InputStream contents) throws IOException;

  public InputStream getBinary(byte[] id) throws IOException;

  public byte[] getBinaryArray(byte[] id) throws IOException;

  public Info putCharacter(Reader contents) throws IOException;

  public Reader getCharacter(byte[] id) throws IOException;

  public char[] getCharacterArray(byte[] id) throws IOException;

  /**
   * @author Eike Stepper
   */
  public static final class Info
  {
    private byte[] id;

    private long size;

    public Info(byte[] id, long size)
    {
      this.id = id;
      this.size = size;
    }

    public byte[] getID()
    {
      return id;
    }

    public long getSize()
    {
      return size;
    }
  }
}
