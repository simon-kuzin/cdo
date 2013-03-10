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

import org.eclipse.net4j.db.DBType;
import org.eclipse.net4j.db.ddl.IDBField;
import org.eclipse.net4j.db.ddl.IDBIndex;
import org.eclipse.net4j.db.ddl.IDBIndex.Type;
import org.eclipse.net4j.db.ddl.IDBSchema;
import org.eclipse.net4j.db.ddl.IDBSchemaElement;
import org.eclipse.net4j.db.ddl.IDBSchemaVisitor;
import org.eclipse.net4j.db.ddl.SchemaElementNotFoundException;
import org.eclipse.net4j.spi.db.ddl.InternalDBTable;
import org.eclipse.net4j.util.event.IListener;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

/**
 * @author Eike Stepper
 */
public class DelegatingDBTable extends DelegatingDBSchemaElement implements InternalDBTable
{
  private final InternalDBTable delegate;

  public DelegatingDBTable(InternalDBTable delegate)
  {
    this.delegate = delegate;
  }

  @Override
  public InternalDBTable getDelegate()
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

  public IDBSchema getParent()
  {
    return delegate.getParent();
  }

  public IDBField addField(String name, DBType type)
  {
    return delegate.addField(name, type);
  }

  public String dumpToString()
  {
    return delegate.dumpToString();
  }

  public SchemaElementType getSchemaElementType()
  {
    return delegate.getSchemaElementType();
  }

  public IDBField addField(String name, DBType type, boolean notNull)
  {
    return delegate.addField(name, type, notNull);
  }

  public void dump()
  {
    delegate.dump();
  }

  public void dump(Writer writer) throws IOException
  {
    delegate.dump(writer);
  }

  public void removeField(IDBField fieldToRemove)
  {
    delegate.removeField(fieldToRemove);
  }

  public IDBSchema getSchema()
  {
    return delegate.getSchema();
  }

  public IDBField addField(String name, DBType type, int precision)
  {
    return delegate.addField(name, type, precision);
  }

  public void removeIndex(IDBIndex indexToRemove)
  {
    delegate.removeIndex(indexToRemove);
  }

  public boolean isEmpty()
  {
    return delegate.isEmpty();
  }

  public void addListener(IListener listener)
  {
    delegate.addListener(listener);
  }

  public IDBField addField(String name, DBType type, int precision, boolean notNull)
  {
    return delegate.addField(name, type, precision, notNull);
  }

  public IDBSchemaElement[] getElements()
  {
    return delegate.getElements();
  }

  @Deprecated
  public void setName(String name)
  {
    delegate.setName(name);
  }

  public IDBField addField(String name, DBType type, int precision, int scale)
  {
    return delegate.addField(name, type, precision, scale);
  }

  public String getFullName()
  {
    return delegate.getFullName();
  }

  public <T extends IDBSchemaElement> T getElement(Class<T> type, String name)
  {
    return delegate.getElement(type, name);
  }

  public IDBField addField(String name, DBType type, int precision, int scale, boolean notNull)
  {
    return delegate.addField(name, type, precision, scale, notNull);
  }

  public void accept(IDBSchemaVisitor visitor)
  {
    delegate.accept(visitor);
  }

  public IDBField getFieldSafe(String name) throws SchemaElementNotFoundException
  {
    return delegate.getFieldSafe(name);
  }

  public void removeListener(IListener listener)
  {
    delegate.removeListener(listener);
  }

  public void remove()
  {
    delegate.remove();
  }

  public IDBField getField(String name)
  {
    return delegate.getField(name);
  }

  public IDBField getField(int position)
  {
    return delegate.getField(position);
  }

  public boolean hasListeners()
  {
    return delegate.hasListeners();
  }

  public int getFieldCount()
  {
    return delegate.getFieldCount();
  }

  public IDBField[] getFields()
  {
    return delegate.getFields();
  }

  public IDBField[] getFields(String... fieldNames) throws SchemaElementNotFoundException
  {
    return delegate.getFields(fieldNames);
  }

  public IListener[] getListeners()
  {
    return delegate.getListeners();
  }

  public IDBIndex addIndex(String name, Type type, IDBField... fields)
  {
    return delegate.addIndex(name, type, fields);
  }

  public IDBIndex addIndex(String name, Type type, String... fieldNames) throws SchemaElementNotFoundException
  {
    return delegate.addIndex(name, type, fieldNames);
  }

  public IDBIndex addIndexEmpty(String name, Type type)
  {
    return delegate.addIndexEmpty(name, type);
  }

  public IDBIndex addIndex(Type type, IDBField... fields)
  {
    return delegate.addIndex(type, fields);
  }

  public IDBIndex addIndex(Type type, String... fieldNames) throws SchemaElementNotFoundException
  {
    return delegate.addIndex(type, fieldNames);
  }

  public IDBIndex addIndexEmpty(Type type)
  {
    return delegate.addIndexEmpty(type);
  }

  public IDBIndex getIndexSafe(String name) throws SchemaElementNotFoundException
  {
    return delegate.getIndexSafe(name);
  }

  public IDBIndex getIndex(String name)
  {
    return delegate.getIndex(name);
  }

  public IDBIndex getIndex(int position)
  {
    return delegate.getIndex(position);
  }

  public int getIndexCount()
  {
    return delegate.getIndexCount();
  }

  public IDBIndex[] getIndices()
  {
    return delegate.getIndices();
  }

  public IDBIndex getPrimaryKeyIndex()
  {
    return delegate.getPrimaryKeyIndex();
  }

  public String sqlInsert()
  {
    return delegate.sqlInsert();
  }

  public int compareTo(IDBSchemaElement o)
  {
    return delegate.compareTo(o);
  }
}
