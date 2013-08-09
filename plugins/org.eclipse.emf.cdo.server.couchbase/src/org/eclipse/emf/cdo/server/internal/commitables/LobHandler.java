package org.eclipse.emf.cdo.server.internal.commitables;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.eclipse.emf.cdo.common.lob.CDOLobHandler;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.net4j.util.HexUtil;
import org.eclipse.net4j.util.io.IOUtil;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import com.couchbase.client.CouchbaseClient;

public class LobHandler extends StoreHandler {

	public LobHandler(CouchbaseClient client, IStore store) {
		super(client, store);
	}
	
	public Object readLob(byte[] id) {
		Object result = getValue(getBlobKey(id));
		if (result == null) {
			return getValue(getClobKey(id));
		}
		return result;
	}	
	
	public void loadLob(byte[] id, OutputStream out) {
		Object rawObject = getValue(getBlobKey(id));
		if (rawObject != null) {
			byte[] byteArray = (byte[])rawObject;
			ByteArrayInputStream in = new ByteArrayInputStream(byteArray);
		    try {
				IOUtil.copyBinary(in, out, byteArray.length);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}			
		} else {
			rawObject = getValue(getClobKey(id));
			char[] charArray = (char[])rawObject;
			CharArrayReader in = new CharArrayReader(charArray);
		    try {
		    	IOUtil.copyCharacter(in, new OutputStreamWriter(out), charArray.length);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	
	}

	public ICommitable createWriteBlobCommitable(final byte[] id, final long size, final InputStream inputStream) {
		return new AbstractCommitable(getClient()) {			
			public void commit(OMMonitor monitor) {
			    ByteArrayOutputStream out = new ByteArrayOutputStream();
			    try {
					IOUtil.copyBinary(inputStream, out, size);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			    doCommit(getBlobKey(id), out.toByteArray(), null, PersistMethod.ADD);
			    doCommit("LOB", HexUtil.bytesToHex(id), null, PersistMethod.APPEND);
			}
		};
	}
	
	public ICommitable createWriteClobCommitable(final byte[] id, final long size, final Reader reader) {
		return new AbstractCommitable(getClient()) {			
			public void commit(OMMonitor monitor) {
			    CharArrayWriter out = new CharArrayWriter();
			    try {
				    IOUtil.copyCharacter(reader, out, size);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			    doCommit(getClobKey(id), out.toCharArray(), null, PersistMethod.ADD);
			    doCommit(getAllLobKey(), HexUtil.bytesToHex(id), null, PersistMethod.APPEND);
			}
		};
	}
	
	public void handleLobs(long fromTime, long toTime, CDOLobHandler handler) throws IOException {
		for (String id : getMultiValue(getAllLobKey())) {
			Object rawBlob = getValue(getBlobKey(id));
			if (rawBlob != null) {			      
				byte[] byteArray = (byte[])rawBlob;
				ByteArrayInputStream in = new ByteArrayInputStream(byteArray);
			    OutputStream out = handler.handleBlob(HexUtil.hexToBytes(id), byteArray.length);
			    if (out != null)
				    {
				    try
				    {
				    	IOUtil.copyBinary(in, out, byteArray.length);
				    }
				    finally
				    {
				    	IOUtil.close(out);
				    }
				}
			} else {
				Object rawClob = getValue(getClobKey(id));
				if (rawClob != null) {
					char[] clob = (char[])rawClob;
					CharArrayReader in = new CharArrayReader(clob);
				    Writer out = handler.handleClob(HexUtil.hexToBytes(id), clob.length);
				    if (out != null)
				    {
				    	try
				        {
				    		IOUtil.copyCharacter(in, out, clob.length);
				        }
				        finally
				        {
				          IOUtil.close(out);
				        }
				    }
				}
			}
		}
	}
	
	protected String getBlobKey(byte[] id) {
		return getBlobKey(HexUtil.bytesToHex(id));	
	}
	
	protected String getBlobKey(String id) {
		return "BLOB::" + id;	
	}
	
	protected String getClobKey(byte[] id) {
		return getClobKey(HexUtil.bytesToHex(id));	
	}
	
	protected String getClobKey(String id) {
		return "CLOB::" + id;	
	}
	
	protected String getAllLobKey() {
		return "LOB";	
	}
}
