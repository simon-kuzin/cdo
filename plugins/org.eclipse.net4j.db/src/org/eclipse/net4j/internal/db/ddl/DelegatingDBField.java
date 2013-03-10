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
import org.eclipse.net4j.db.ddl.IDBSchema;
import org.eclipse.net4j.db.ddl.IDBSchemaElement;
import org.eclipse.net4j.db.ddl.IDBSchemaVisitor;
import org.eclipse.net4j.db.ddl.IDBTable;
import org.eclipse.net4j.spi.db.ddl.InternalDBField;
import org.eclipse.net4j.util.event.IListener;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

/**
 * @author Eike Stepper
 */
public class DelegatingDBField extends DelegatingDBSchemaElement implements InternalDBField
{
  private final InternalDBField delegate;

  public DelegatingDBField(InternalDBField delegate)
  {
    this.delegate = delegate;
  }

  @Override
  public InternalDBField getDelegate()
  {
    return delegate;
  }

  public int getPosition()
  {
    return delegate.getPosition();
  }

  public String getName()
  {
    return delegate.getName();
  }

  public void setPosition(int position)
  {
    delegate.setPosition(position);
  }

  public Properties getProperties()
  {
    return delegate.getProperties();
  }

  public Exception getConstructionStackTrace()
  {
    return delegate.getConstructionStackTrace();
  }

  public String dumpToString()
  {
    return delegate.dumpToString();
  }

  public SchemaElementType getSchemaElementType()
  {
    return delegate.getSchemaElementType();
  }

  public IDBTable getParent()
  {
    return delegate.getParent();
  }

  public void dump()
  {
    delegate.dump();
  }

  public void dump(Writer writer) throws IOException
  {
    delegate.dump(writer);
  }

  public IDBSchema getSchema()
  {
    return delegate.getSchema();
  }

  public IDBTable getTable()
  {
    return delegate.getTable();
  }

  public DBType getType()
  {
    return delegate.getType();
  }

  public boolean isEmpty()
  {
    return delegate.isEmpty();
  }

  public void setType(DBType type)
  {
    delegate.setType(type);
  }

  public void addListener(IListener listener)
  {
    delegate.addListener(listener);
  }

  public IDBSchemaElement[] getElements()
  {
    return delegate.getElements();
  }

  public int getPrecision()
  {
    return delegate.getPrecision();
  }

  @Deprecated
  public void setName(String name)
  {
    delegate.setName(name);
  }

  public void setPrecision(int precision)
  {
    delegate.setPrecision(precision);
  }

  public int getScale()
  {
    return delegate.getScale();
  }

  public void setScale(int scale)
  {
    delegate.setScale(scale);
  }

  public <T extends IDBSchemaElement> T getElement(Class<T> type, String name)
  {
    return delegate.getElement(type, name);
  }

  public boolean isNotNull()
  {
    return delegate.isNotNull();
  }

  public void setNotNull(boolean notNull)
  {
    delegate.setNotNull(notNull);
  }

  public String getFullName()
  {
    return delegate.getFullName();
  }

  public void accept(IDBSchemaVisitor visitor)
  {
    delegate.accept(visitor);
  }

  public String formatPrecision()
  {
    return delegate.formatPrecision();
  }

  public String formatPrecisionAndScale()
  {
    return delegate.formatPrecisionAndScale();
  }

  public void removeListener(IListener listener)
  {
    delegate.removeListener(listener);
  }

  public void remove()
  {
    delegate.remove();
  }

  public boolean hasListeners()
  {
    return delegate.hasListeners();
  }

  public IListener[] getListeners()
  {
    return delegate.getListeners();
  }

  public int compareTo(IDBSchemaElement o)
  {
    return delegate.compareTo(o);
  }
}
