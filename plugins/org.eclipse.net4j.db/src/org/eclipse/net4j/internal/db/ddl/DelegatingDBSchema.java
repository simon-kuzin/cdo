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

import org.eclipse.net4j.db.DBException;
import org.eclipse.net4j.db.IDBAdapter;
import org.eclipse.net4j.db.IDBConnectionProvider;
import org.eclipse.net4j.db.ddl.IDBField;
import org.eclipse.net4j.db.ddl.IDBIndex.Type;
import org.eclipse.net4j.db.ddl.IDBSchema;
import org.eclipse.net4j.db.ddl.IDBSchemaElement;
import org.eclipse.net4j.db.ddl.IDBSchemaVisitor;
import org.eclipse.net4j.db.ddl.IDBTable;
import org.eclipse.net4j.db.ddl.SchemaElementNotFoundException;
import org.eclipse.net4j.db.ddl.delta.IDBSchemaDelta;
import org.eclipse.net4j.spi.db.ddl.InternalDBSchema;
import org.eclipse.net4j.util.event.IListener;

import javax.sql.DataSource;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.sql.Connection;
import java.util.Properties;
import java.util.Set;

/**
 * @author Eike Stepper
 */
public class DelegatingDBSchema extends DelegatingDBSchemaElement implements InternalDBSchema
{
  private final InternalDBSchema delegate;

  public DelegatingDBSchema(InternalDBSchema delegate)
  {
    this.delegate = delegate;
  }

  @Override
  public InternalDBSchema getDelegate()
  {
    return delegate;
  }

  public String getName()
  {
    return delegate.getName();
  }

  public Properties getProperties()
  {
    return delegate.getProperties();
  }

  public String dumpToString()
  {
    return delegate.dumpToString();
  }

  public void dump()
  {
    delegate.dump();
  }

  public void dump(Writer writer) throws IOException
  {
    delegate.dump(writer);
  }

  public SchemaElementType getSchemaElementType()
  {
    return delegate.getSchemaElementType();
  }

  public IDBTable addTable(String name)
  {
    return delegate.addTable(name);
  }

  public IDBSchema getSchema()
  {
    return delegate.getSchema();
  }

  public boolean isEmpty()
  {
    return delegate.isEmpty();
  }

  public IDBTable removeTable(String name)
  {
    return delegate.removeTable(name);
  }

  public void addListener(IListener listener)
  {
    delegate.addListener(listener);
  }

  public IDBSchemaElement getParent()
  {
    return delegate.getParent();
  }

  public IDBSchemaElement[] getElements()
  {
    return delegate.getElements();
  }

  public String createIndexName(IDBTable table, Type type, IDBField[] fields, int position)
  {
    return delegate.createIndexName(table, type, fields, position);
  }

  @Deprecated
  public void setName(String name)
  {
    delegate.setName(name);
  }

  public boolean isLocked()
  {
    return delegate.isLocked();
  }

  public boolean lock()
  {
    return delegate.lock();
  }

  public <T extends IDBSchemaElement> T findElement(IDBSchemaElement prototype)
  {
    return delegate.findElement(prototype);
  }

  public boolean unlock()
  {
    return delegate.unlock();
  }

  public String getFullName()
  {
    return delegate.getFullName();
  }

  public void assertUnlocked() throws DBException
  {
    delegate.assertUnlocked();
  }

  public <T extends IDBSchemaElement> T getElement(Class<T> type, String name)
  {
    return delegate.getElement(type, name);
  }

  public void accept(IDBSchemaVisitor visitor)
  {
    delegate.accept(visitor);
  }

  public void removeListener(IListener listener)
  {
    delegate.removeListener(listener);
  }

  public void remove()
  {
    delegate.remove();
  }

  public IDBTable getTableSafe(String name) throws SchemaElementNotFoundException
  {
    return delegate.getTableSafe(name);
  }

  public boolean hasListeners()
  {
    return delegate.hasListeners();
  }

  public IDBTable getTable(String name)
  {
    return delegate.getTable(name);
  }

  public IDBTable[] getTables()
  {
    return delegate.getTables();
  }

  public Set<IDBTable> create(IDBAdapter dbAdapter, Connection connection) throws DBException
  {
    return delegate.create(dbAdapter, connection);
  }

  public IListener[] getListeners()
  {
    return delegate.getListeners();
  }

  public Set<IDBTable> create(IDBAdapter dbAdapter, DataSource dataSource) throws DBException
  {
    return delegate.create(dbAdapter, dataSource);
  }

  public Set<IDBTable> create(IDBAdapter dbAdapter, IDBConnectionProvider connectionProvider) throws DBException
  {
    return delegate.create(dbAdapter, connectionProvider);
  }

  public void drop(IDBAdapter dbAdapter, Connection connection) throws DBException
  {
    delegate.drop(dbAdapter, connection);
  }

  public void drop(IDBAdapter dbAdapter, DataSource dataSource) throws DBException
  {
    delegate.drop(dbAdapter, dataSource);
  }

  public void drop(IDBAdapter dbAdapter, IDBConnectionProvider connectionProvider) throws DBException
  {
    delegate.drop(dbAdapter, connectionProvider);
  }

  public void export(Connection connection, PrintStream out) throws DBException
  {
    delegate.export(connection, out);
  }

  public void export(DataSource dataSource, PrintStream out) throws DBException
  {
    delegate.export(dataSource, out);
  }

  public void export(IDBConnectionProvider connectionProvider, PrintStream out) throws DBException
  {
    delegate.export(connectionProvider, out);
  }

  public IDBSchemaDelta compare(IDBSchema oldSchema)
  {
    return delegate.compare(oldSchema);
  }

  public int compareTo(IDBSchemaElement o)
  {
    return delegate.compareTo(o);
  }
}
