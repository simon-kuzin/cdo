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

import org.eclipse.emf.cdo.common.model.lob.CDOLobStore.Info;

import org.eclipse.net4j.util.io.ExtendedDataInput;
import org.eclipse.net4j.util.io.ExtendedDataOutput;

import java.io.IOException;

/**
 * @author Eike Stepper
 * @since 4.0
 */
public abstract class CDOLob<ARRAY, IO>
{
  private CDOLobStore store;

  private byte[] id;

  private long size;

  CDOLob(byte[] id, long size)
  {
    this.id = id;
    this.size = size;
  }

  CDOLob(IO contents, CDOLobStore store) throws IOException
  {
    this.store = store;
    Info info = put(contents);
    id = info.getID();
    size = info.getSize();
  }

  CDOLob(ExtendedDataInput in) throws IOException
  {
    id = in.readByteArray();
    size = in.readLong();
  }

  final void write(ExtendedDataOutput out) throws IOException
  {
    out.writeByteArray(id);
    out.writeLong(size);
  }

  final void setStore(CDOLobStore store)
  {
    this.store = store;
  }

  public final CDOLobStore getStore()
  {
    return store;
  }

  public final byte[] getID()
  {
    return id;
  }

  public final long getSize()
  {
    return size;
  }

  public final IO getContents() throws IOException
  {
    return get(id);
  }

  public abstract ARRAY toArray() throws IOException;

  protected abstract Info put(IO contents) throws IOException;

  protected abstract IO get(byte[] id) throws IOException;
}
