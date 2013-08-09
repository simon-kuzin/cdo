package org.eclipse.emf.cdo.server.internal.commitables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.cdo.common.model.EMFUtil;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.internal.couchbase.marshall.JSONPackageUnit;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import com.couchbase.client.CouchbaseClient;

public class PackageUnitHandler extends JSONHandler {

	public PackageUnitHandler(CouchbaseClient client, IStore store) {
		super(client, store);
	}

	protected String getPackageUnitsKey() {
		return "CDOPackageUnits";
	}
	
	public Collection<InternalCDOPackageUnit> readPackageUnits() {
		List<String> packageUnits = getMultiValue(getPackageUnitsKey());
		List<InternalCDOPackageUnit> result = new ArrayList<InternalCDOPackageUnit>();
		for(String packageID : packageUnits) {
			result.add(readPackageUnit(packageID));
		}
		return result;
	}
	
	public InternalCDOPackageUnit readPackageUnit(String id) {
		return JSONPackageUnit.getPackageUnit(fromJson((String)getValue(id), JSONPackageUnit.class));
	}
	
	public EPackage[] loadPackageUnit(InternalCDOPackageUnit packageUnit) {
		JSONPackageUnit jsonPackageUnit = fromJson((String)getValue(packageUnit.getTopLevelPackageInfo().getPackageURI()), JSONPackageUnit.class);
		return EMFUtil.getAllPackages(jsonPackageUnit.getEPackage());
	}
	
	public ICommitable createPackageUnitCommitable(final InternalCDOPackageUnit packageUnit) {
		return new AbstractCommitable(getClient()) {			
			public void commit(OMMonitor monitor) {
				JSONPackageUnit jsonPackageUnit = JSONPackageUnit.getJsonPackageUnit(packageUnit, getStore());
				doCommit(getPackageUnitsKey(), packageUnit.getID(), null, PersistMethod.APPEND);
				doCommit(packageUnit.getID(), toJson(jsonPackageUnit), null, PersistMethod.SET);
			}
		};
	}
}
