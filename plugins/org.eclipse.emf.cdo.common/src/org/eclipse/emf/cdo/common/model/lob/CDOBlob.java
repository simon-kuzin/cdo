package org.eclipse.emf.cdo.common.model.lob;

import org.eclipse.emf.cdo.common.model.lob.CDOLobStore.Info;
import org.eclipse.emf.cdo.spi.common.model.CDOLobStoreImpl;

import org.eclipse.net4j.util.io.ExtendedDataInput;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Eike Stepper
 * @since 4.0
 */
public final class CDOBlob extends CDOLob<byte[], InputStream>
{
  public CDOBlob(InputStream contents) throws IOException
  {
    super(contents, CDOLobStoreImpl.INSTANCE);
  }

  public CDOBlob(InputStream contents, CDOLobStore store) throws IOException
  {
    super(contents, store);
  }

  CDOBlob(byte[] id, long size)
  {
    super(id, size);
  }

  CDOBlob(ExtendedDataInput in) throws IOException
  {
    super(in);
  }

  @Override
  public byte[] toArray() throws IOException
  {
    return getStore().getBinaryArray(getID());
  }

  @Override
  protected Info put(InputStream contents) throws IOException
  {
    return getStore().putBinary(contents);
  }

  @Override
  protected InputStream get(byte[] id) throws IOException
  {
    return getStore().getBinary(id);
  }
}
