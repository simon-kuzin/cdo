/*
 * Copyright (c) 2004 - 2012 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.net4j.internal.db.ddl;

import org.eclipse.net4j.spi.db.ddl.InternalDBSchemaElement;

/**
 * @author Eike Stepper
 */
public abstract class DelegatingDBSchemaElement implements InternalDBSchemaElement
{
  public abstract InternalDBSchemaElement getDelegate();
}
