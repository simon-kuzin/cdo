package org.eclipse.emf.cdo.server.internal.couchbase.marshall;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.cdo.common.model.CDOModelUtil;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.model.EMFUtil;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageInfo;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.net4j.util.collection.Pair;

public class JSONPackageUnit {

	private String id;

	private Integer originalType;

	private Long timeStamp;

	private List<Byte> ePackageBytes;

	private List<Pair<String, String>> packageInfos;

	public JSONPackageUnit(String id, Integer originalType, Long timeStamp,
			List<Byte> ePackageBytes, List<Pair<String, String>> packageIntos) {
		this.setId(id);
		this.setOriginalType(originalType);
		this.setTimeStamp(timeStamp);
		this.setEPackageBytes(ePackageBytes);
		this.setPackageInfos(packageIntos);
	}

	public String getId() {
		return id;
	}

	public Integer getOriginalType() {
		return originalType;
	}

	public Long getTimeStamp() {
		return timeStamp;
	}

	public List<Byte> getEPackageBytes() {
		return ePackageBytes;
	}

	public List<Pair<String, String>> getPackageInfos() {
		return packageInfos;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setOriginalType(Integer originalType) {
		this.originalType = originalType;
	}

	public void setTimeStamp(Long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public void setEPackageBytes(List<Byte> ePackageBytes) {
		this.ePackageBytes = ePackageBytes;
	}

	public void setPackageInfos(List<Pair<String, String>> packageInfos) {
		this.packageInfos = packageInfos;
	}

	public EPackage getEPackage() {
		return getEPackageFromBytes(getEPackageBytes());
	}
	  
	public static InternalCDOPackageUnit getPackageUnit(JSONPackageUnit jsonPackageUnit) {
		InternalCDOPackageUnit cdoPackageUnit = (InternalCDOPackageUnit) CDOModelUtil.createPackageUnit();
		cdoPackageUnit.setOriginalType(CDOPackageUnit.Type.values()[jsonPackageUnit.getOriginalType()]);
		cdoPackageUnit.setTimeStamp(jsonPackageUnit.getTimeStamp());
		cdoPackageUnit.setPackageInfos(getPackageInfos(jsonPackageUnit.getPackageInfos()));
		return cdoPackageUnit;
	}

	public static JSONPackageUnit getJsonPackageUnit(InternalCDOPackageUnit packageUnit, IStore store) {
		String id = new String(packageUnit.getID());
		Integer originalType = new Integer(packageUnit.getOriginalType().ordinal());
		Long timeStamp = new Long(packageUnit.getTimeStamp());
		List<Byte> ePackageBytes = getEPackageBytes(store, packageUnit);
		List<Pair<String, String>> packageInfos = getPackageInfosAsPair(packageUnit.getPackageInfos());
		return new JSONPackageUnit(id, originalType, timeStamp, ePackageBytes, packageInfos);
		
	}
	
	private static EPackage getEPackageFromBytes(List<Byte> ePackageBytesList) {
		ResourceSet rSet = new ResourceSetImpl();
		Resource.Factory resourceFactory = new EcoreResourceFactoryImpl();
	    rSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", resourceFactory); //$NON-NLS-1$
		byte[] packageBytes = new byte[ePackageBytesList.size()];
		for (int i = 0; i < packageBytes.length; i++) {
			packageBytes[i] = ePackageBytesList.get(i);
		}
		EPackage ePackage = EMFUtil.createEPackage("", packageBytes, true, rSet, false);
		return ePackage;		
	}
	
	private static List<Byte> getEPackageBytes(IStore store,
			InternalCDOPackageUnit packageUnit) {
		EPackage ePackage = packageUnit.getTopLevelPackageInfo().getEPackage();
		CDOPackageRegistry packageRegistry = store.getRepository()
				.getPackageRegistry();
		byte[] bytes = EMFUtil
				.getEPackageBytes(ePackage, true, packageRegistry);
		List<Byte> bytesObject = new ArrayList<Byte>();
		for (byte bt : bytes) {
			bytesObject.add(new Byte(bt));
		}

		return bytesObject;
	}

	private static List<Pair<String, String>> getPackageInfosAsPair(
			InternalCDOPackageInfo[] packageInfos) {
		List<Pair<String, String>> infos = new ArrayList<Pair<String, String>>();
		for (InternalCDOPackageInfo info : packageInfos) {
			Pair<String, String> pair = Pair.create(info.getParentURI(),
					info.getPackageURI());
			infos.add(pair);
		}

		return infos;
	}

	private static InternalCDOPackageInfo[] getPackageInfos(
			List<Pair<String, String>> packagePairs) {
		List<InternalCDOPackageInfo> list = new ArrayList<InternalCDOPackageInfo>();
		for (Pair<String, String> infoPair : packagePairs) {
			InternalCDOPackageInfo packageInfo = (InternalCDOPackageInfo) CDOModelUtil
					.createPackageInfo();
			packageInfo.setParentURI(infoPair.getElement1());
			packageInfo.setPackageURI(infoPair.getElement2());
			list.add(packageInfo);
		}
		return list.toArray(new InternalCDOPackageInfo[list.size()]);
	}
}
