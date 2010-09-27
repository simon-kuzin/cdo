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
package org.eclipse.emf.cdo.spi.common.model;

import org.eclipse.emf.cdo.common.model.lob.CDOLobStore;

import org.eclipse.net4j.util.HexUtil;
import org.eclipse.net4j.util.WrappedException;
import org.eclipse.net4j.util.io.DigestWriter;
import org.eclipse.net4j.util.io.IORuntimeException;
import org.eclipse.net4j.util.io.IOUtil;
import org.eclipse.net4j.util.om.OMPlatform;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Eike Stepper
 * @since 4.0
 */
public class CDOLobStoreImpl implements CDOLobStore
{
  public static final CDOLobStore INSTANCE = new CDOLobStoreImpl();

  private File folder;

  private int tempID;

  public CDOLobStoreImpl(File folder)
  {
    this.folder = folder;
    if (!folder.exists())
    {
      folder.mkdirs();
      if (!folder.exists())
      {
        throw new IORuntimeException("Folder could not be created: " + folder);
      }

      if (!folder.isDirectory())
      {
        throw new IORuntimeException("Not a folder: " + folder);
      }
    }
  }

  public CDOLobStoreImpl()
  {
    this(getDefaultFolder());
  }

  public File getFolder()
  {
    return folder;
  }

  public Info putBinary(InputStream contents) throws IOException
  {
    File tempFile = getTempFile();
    MessageDigest digest = createDigest();
    digest.update("BINARY".getBytes());

    FileOutputStream fos = null;
    long size;

    try
    {
      fos = new FileOutputStream(tempFile);
      DigestOutputStream dos = new DigestOutputStream(fos, digest);
      size = IOUtil.copyBinary(contents, dos);
    }
    finally
    {
      IOUtil.close(fos);
    }

    byte[] id = digest.digest();
    makePermanent(tempFile, getBinaryFile(id));
    return new Info(id, size);
  }

  public InputStream getBinary(byte[] id) throws IOException
  {
    File file = getBinaryFile(id);
    return new FileInputStream(file);
  }

  public byte[] getBinaryArray(byte[] id) throws IOException
  {
    InputStream inputStream = getBinary(id);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    IOUtil.copyBinary(inputStream, baos);
    return baos.toByteArray();
  }

  public Info putCharacter(Reader contents) throws IOException
  {
    File tempFile = getTempFile();
    MessageDigest digest = createDigest();
    digest.update("CHARACTER".getBytes());

    FileWriter fw = null;
    long size;

    try
    {
      fw = new FileWriter(tempFile);
      DigestWriter dw = new DigestWriter(fw, digest);
      size = IOUtil.copyCharacter(contents, dw);
    }
    finally
    {
      IOUtil.close(fw);
    }

    byte[] id = digest.digest();
    makePermanent(tempFile, getCharacterFile(id));
    return new Info(id, size);
  }

  public Reader getCharacter(byte[] id) throws IOException
  {
    File file = getCharacterFile(id);
    return new FileReader(file);
  }

  public char[] getCharacterArray(byte[] id) throws IOException
  {
    Reader reader = getCharacter(id);
    CharArrayWriter caw = new CharArrayWriter();
    IOUtil.copyCharacter(reader, caw);
    return caw.toCharArray();
  }

  protected MessageDigest createDigest()
  {
    try
    {
      return MessageDigest.getInstance("SHA-1");
    }
    catch (NoSuchAlgorithmException ex)
    {
      throw WrappedException.wrap(ex);
    }
  }

  protected synchronized File getTempFile()
  {
    for (;;)
    {
      ++tempID;
      File file = new File(folder, "contents" + tempID + ".tmp");
      if (!file.exists())
      {
        return file;
      }
    }
  }

  private File getBinaryFile(byte[] id)
  {
    return new File(folder, HexUtil.bytesToHex(id) + ".blob");
  }

  private File getCharacterFile(byte[] id)
  {
    return new File(folder, HexUtil.bytesToHex(id) + ".clob");
  }

  private void makePermanent(File tempFile, File file)
  {
    if (file.exists())
    {
      tempFile.delete();
    }
    else
    {
      tempFile.renameTo(file);
    }
  }

  private static File getDefaultFolder()
  {
    String path = OMPlatform.INSTANCE.getProperty("java.io.tmpdir");
    return new File(new File(path), "cdolobs");
  }
}
