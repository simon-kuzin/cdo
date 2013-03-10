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

import org.eclipse.net4j.db.ddl.IDBField;
import org.eclipse.net4j.db.ddl.IDBIndexField;
import org.eclipse.net4j.db.ddl.IDBSchema;
import org.eclipse.net4j.db.ddl.IDBSchemaElement;
import org.eclipse.net4j.db.ddl.IDBSchemaVisitor;
import org.eclipse.net4j.db.ddl.IDBTable;
import org.eclipse.net4j.db.ddl.SchemaElementNotFoundException;
import org.eclipse.net4j.spi.db.ddl.InternalDBIndex;
import org.eclipse.net4j.util.event.IListener;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

/**
 * @author Eike Stepper
 */
public class DelegatingDBIndex extends DelegatingDBSchemaElement implements InternalDBIndex
{
  private final InternalDBIndex delegate;

  public DelegatingDBIndex(InternalDBIndex delegate)
  {
    this.delegate = delegate;
  }

  @Override
  public InternalDBIndex getDelegate()
  {
    return delegate;
  }

  public String getName()
  {
    return delegate.getName();
  }

  public IDBTable getParent()
  {
    return delegate.getParent();
  }

  public Properties getProperties()
  {
    return delegate.getProperties();
  }

  public IDBTable getTable()
  {
    return delegate.getTable();
  }

  public Type getType()
  {
    return delegate.getType();
  }

  public void setType(Type type)
  {
    delegate.setType(type);
  }

  public String dumpToString()
  {
    return delegate.dumpToString();
  }

  public void removeIndexField(IDBIndexField indexFieldToRemove)
  {
    delegate.removeIndexField(indexFieldToRemove);
  }

  public SchemaElementType getSchemaElementType()
  {
    return delegate.getSchemaElementType();
  }

  public void dump()
  {
    delegate.dump();
  }

  public void dump(Writer writer) throws IOException
  {
    delegate.dump(writer);
  }

  @Deprecated
  public int getPosition()
  {
    return delegate.getPosition();
  }

  public IDBSchema getSchema()
  {
    return delegate.getSchema();
  }

  public IDBIndexField addIndexField(IDBField field)
  {
    return delegate.addIndexField(field);
  }

  public boolean isEmpty()
  {
    return delegate.isEmpty();
  }

  public void addListener(IListener listener)
  {
    delegate.addListener(listener);
  }

  public IDBSchemaElement[] getElements()
  {
    return delegate.getElements();
  }

  public IDBIndexField addIndexField(String name) throws SchemaElementNotFoundException
  {
    return delegate.addIndexField(name);
  }

  @Deprecated
  public void setName(String name)
  {
    delegate.setName(name);
  }

  public String getFullName()
  {
    return delegate.getFullName();
  }

  public IDBIndexField getIndexFieldSafe(String name) throws SchemaElementNotFoundException
  {
    return delegate.getIndexFieldSafe(name);
  }

  public <T extends IDBSchemaElement> T getElement(Class<T> type, String name)
  {
    return delegate.getElement(type, name);
  }

  public void accept(IDBSchemaVisitor visitor)
  {
    delegate.accept(visitor);
  }

  public IDBIndexField getIndexField(String name)
  {
    return delegate.getIndexField(name);
  }

  public void removeListener(IListener listener)
  {
    delegate.removeListener(listener);
  }

  public void remove()
  {
    delegate.remove();
  }

  public IDBIndexField getIndexField(int position)
  {
    return delegate.getIndexField(position);
  }

  public boolean hasListeners()
  {
    return delegate.hasListeners();
  }

  public IDBField getFieldSafe(String name) throws SchemaElementNotFoundException
  {
    return delegate.getFieldSafe(name);
  }

  public IDBField getField(String name)
  {
    return delegate.getField(name);
  }

  public IDBField getField(int position)
  {
    return delegate.getField(position);
  }

  public IListener[] getListeners()
  {
    return delegate.getListeners();
  }

  public int getFieldCount()
  {
    return delegate.getFieldCount();
  }

  public IDBIndexField[] getIndexFields()
  {
    return delegate.getIndexFields();
  }

  public IDBField[] getFields()
  {
    return delegate.getFields();
  }

  public int compareTo(IDBSchemaElement o)
  {
    return delegate.compareTo(o);
  }
}
