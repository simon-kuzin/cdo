/*
 * Copyright (c) 2010-2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Victor Roldan Betancort - initial API and implementation
 */

package org.eclipse.emf.cdo.server.couchbase;

import org.eclipse.emf.cdo.server.IStore;

/**
 * The main entry point to the API of CDO's integration with Couchbase database.
 *
 * @author Victor Roldan Betancort
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ICouchbaseStore extends IStore
{
  public static final String TYPE = "couchbase";
}
