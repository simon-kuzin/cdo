/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Stefan Winkler - major refactoring
 */
package org.eclipse.emf.cdo.server.db.mapping;

import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.net4j.db.ddl.IDBField;
import org.eclipse.net4j.db.ddl.IDBTable;

import org.eclipse.emf.ecore.EStructuralFeature;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Eike Stepper
 * @author Stefan Winkler
 * @since 2.0
 */
public interface ITypeMapping
{
  public void setValueFromRevision(PreparedStatement stmt, int index, InternalCDORevision value) throws SQLException;

  public void setValue(PreparedStatement stmt, int index, Object value) throws SQLException;

  public void readValueToRevision(ResultSet resultSet, int i, InternalCDORevision revision) throws SQLException;

  public Object readValue(ResultSet resultSet, int i) throws SQLException;

  public void createDBField(IDBTable table);

  public void createDBField(IDBTable table, String fieldName);

  public IDBField getField();

  public EStructuralFeature getFeature();
}
