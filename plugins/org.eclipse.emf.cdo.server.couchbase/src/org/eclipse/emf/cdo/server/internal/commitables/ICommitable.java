package org.eclipse.emf.cdo.server.internal.commitables;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

public interface ICommitable {

	public CDOID getCDOID();
	
	public void commit(OMMonitor monitor);
	
}
