package org.eclipse.emf.cdo.server.internal;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.emf.cdo.server.ISession;
import org.eclipse.emf.cdo.server.IStoreAccessor;
import org.eclipse.emf.cdo.server.ITransaction;
import org.eclipse.emf.cdo.server.IView;
import org.eclipse.emf.cdo.server.couchbase.ICouchbaseStore;
import org.eclipse.emf.cdo.server.internal.commitables.StoreMetaHandler;
import org.eclipse.emf.cdo.server.internal.couchbase.bundle.OM;
import org.eclipse.emf.cdo.spi.server.LongIDStore;
import org.eclipse.emf.cdo.spi.server.StoreAccessorBase;
import org.eclipse.emf.cdo.spi.server.StoreAccessorPool;
import org.eclipse.net4j.util.ReflectUtil.ExcludeFromDump;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.CouchbaseConnectionFactoryBuilder;

public class CouchbaseStore extends LongIDStore implements ICouchbaseStore {

	private static final int COUCHBASE_OPERATION_TIMEOUT = 10000;

	private List<URI> clusterList;

	private String bucketName;
	
	private CouchbaseClient client;

	@ExcludeFromDump
	private transient final StoreAccessorPool readerPool = new StoreAccessorPool(
			this, null);

	@ExcludeFromDump
	private transient final StoreAccessorPool writerPool = new StoreAccessorPool(
			this, null);

	@ExcludeFromDump
	private String password;

	@ExcludeFromDump
	private String user;
	
	private StoreMetaHandler storeMetaHandler;
	
	public CouchbaseStore(List<URI> clusterList, String bucketName, String user, String password) {
		super(ICouchbaseStore.TYPE, set(ChangeFormat.REVISION), set(
				RevisionTemporality.NONE, RevisionTemporality.AUDITING), set(
				RevisionParallelism.NONE, RevisionParallelism.BRANCHING));

		this.clusterList = clusterList;
		this.bucketName = bucketName;
		this.user = user;
		this.password = password;
	}

	public StoreMetaHandler getStoreMetaHandler() {
		if (storeMetaHandler == null) {
			storeMetaHandler = new StoreMetaHandler(getClient());
		}
		return storeMetaHandler;
	}
	
	public CouchbaseClient openClient() {
		try {
			CouchbaseConnectionFactoryBuilder connectionFactoryBuilder = new CouchbaseConnectionFactoryBuilder();
			// Reduces the rate of random "Cancelled" upon client.get() calls; still happens though
			connectionFactoryBuilder.setOpTimeout(COUCHBASE_OPERATION_TIMEOUT); 
		    CouchbaseConnectionFactory connectionFactory = connectionFactoryBuilder.buildCouchbaseConnection(clusterList, bucketName, user, password);
		    return new CouchbaseClient((CouchbaseConnectionFactory)connectionFactory);
		} catch (IOException e) {
			OM.LOG.error(e);
		}
		return null;
	}
	
	private CouchbaseClient getClient() {
		if (client == null) {
			client = openClient();
		}
		return client;
	}

	@Override
	protected void doActivate() throws Exception {
		super.doActivate();
		Long lastCDOID = getStoreMetaHandler().getLastCDOID();
		if (lastCDOID != null) {
			setLastObjectID(lastCDOID);
		}
		setLastCommitTime(storeMetaHandler.getLastCommitTime());
	}

	@Override
	protected void doDeactivate() throws Exception {
		super.doDeactivate();
		storeMetaHandler.setIsFirstStart(false);
		storeMetaHandler.setLastCommitTime(getLastCommitTime());
		if (client != null) {
			client.shutdown(10, TimeUnit.SECONDS); // Its important to gracefully shutdown, or we may lost data recently committed
			client = null;
		}
		StoreAccessorBase readerAccessor;
		while ((readerAccessor = readerPool.removeStoreAccessor(this)) != null) {
			LifecycleUtil.deactivate(readerAccessor);
		}
		StoreAccessorBase writerAccessor;
		while ((writerAccessor = writerPool.removeStoreAccessor(this)) != null) {
			LifecycleUtil.deactivate(writerAccessor);
		}
		
	}
	
	public void setCreationTime(long creationTime) {
		storeMetaHandler.setCreationTime(creationTime);
	}

	public boolean isFirstStart() {
		return storeMetaHandler.isFirstStart();
	}

	public long getCreationTime() {
		return storeMetaHandler.getCreationTime();		
	}

	public Map<String, String> getPersistentProperties(Set<String> names) {
		return storeMetaHandler.getPersistentProperties(names);	
	}

	public void setPersistentProperties(Map<String, String> properties) {
		storeMetaHandler.setPersistentProperties(properties);
	}

	public void removePersistentProperties(Set<String> names) {
		storeMetaHandler.removePersistentProperties(names);
	}

	@Override
	protected StoreAccessorPool getReaderPool(ISession session, boolean forReleasing) {
		return readerPool;
	}

	@Override
	protected StoreAccessorPool getWriterPool(IView view, boolean forReleasing) {
		return writerPool;
	}

	@Override
	protected IStoreAccessor createReader(ISession session) {
		return new CouchbaseStoreAccessor(this, session);
	}

	@Override
	protected IStoreAccessor createWriter(ITransaction transaction) {
		return new CouchbaseStoreAccessor(this, transaction);
	}

}
