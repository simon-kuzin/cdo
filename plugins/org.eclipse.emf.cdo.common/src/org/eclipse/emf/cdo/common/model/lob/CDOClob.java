package org.eclipse.emf.cdo.common.model.lob;

import org.eclipse.emf.cdo.common.model.lob.CDOLobStore.Info;
import org.eclipse.emf.cdo.spi.common.model.CDOLobStoreImpl;

import org.eclipse.net4j.util.io.ExtendedDataInput;

import java.io.IOException;
import java.io.Reader;

/**
 * @author Eike Stepper
 * @since 4.0
 */
public final class CDOClob extends CDOLob<char[], Reader>
{
  public CDOClob(Reader contents) throws IOException
  {
    super(contents, CDOLobStoreImpl.INSTANCE);
  }

  public CDOClob(Reader contents, CDOLobStore store) throws IOException
  {
    super(contents, store);
  }

  CDOClob(byte[] id, long size)
  {
    super(id, size);
  }

  CDOClob(ExtendedDataInput in) throws IOException
  {
    super(in);
  }

  @Override
  public char[] toArray() throws IOException
  {
    return getStore().getCharacterArray(getID());
  }

  @Override
  protected Info put(Reader contents) throws IOException
  {
    return getStore().putCharacter(contents);
  }

  @Override
  protected Reader get(byte[] id) throws IOException
  {
    return getStore().getCharacter(id);
  }
}
