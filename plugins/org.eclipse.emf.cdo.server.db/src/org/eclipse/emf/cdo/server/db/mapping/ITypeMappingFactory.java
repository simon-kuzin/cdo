/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Stefan Winkler - initial API and implementation
 */
package org.eclipse.emf.cdo.server.db.mapping;

import org.eclipse.net4j.db.DBType;

import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * @author Stefan Winkler
 * @since 3.0
 */
public interface ITypeMappingFactory
{
  public ITypeMapping createTypeMapping(IMappingStrategy mappingStrategy, EStructuralFeature feature, DBType dbType);
}
