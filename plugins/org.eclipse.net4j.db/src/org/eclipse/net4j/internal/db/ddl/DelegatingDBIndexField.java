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
import org.eclipse.net4j.db.ddl.IDBIndex;
import org.eclipse.net4j.db.ddl.IDBSchema;
import org.eclipse.net4j.db.ddl.IDBSchemaElement;
import org.eclipse.net4j.db.ddl.IDBSchemaVisitor;
import org.eclipse.net4j.spi.db.ddl.InternalDBIndexField;
import org.eclipse.net4j.util.event.IListener;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

/**
 * @author Eike Stepper
 */
public class DelegatingDBIndexField extends DelegatingDBSchemaElement implements InternalDBIndexField
{
  private final InternalDBIndexField delegate;

  public DelegatingDBIndexField(InternalDBIndexField delegate)
  {
    this.delegate = delegate;
  }

  @Override
  public InternalDBIndexField getDelegate()
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

  public Properties getProperties()
  {
    return delegate.getProperties();
  }

  public void setPosition(int position)
  {
    delegate.setPosition(position);
  }

  public IDBIndex getParent()
  {
    return delegate.getParent();
  }

  public String dumpToString()
  {
    return delegate.dumpToString();
  }

  public SchemaElementType getSchemaElementType()
  {
    return delegate.getSchemaElementType();
  }

  public void dump()
  {
    delegate.dump();
  }

  public IDBIndex getIndex()
  {
    return delegate.getIndex();
  }

  public void dump(Writer writer) throws IOException
  {
    delegate.dump(writer);
  }

  public IDBField getField()
  {
    return delegate.getField();
  }

  public IDBSchema getSchema()
  {
    return delegate.getSchema();
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

  @Deprecated
  public void setName(String name)
  {
    delegate.setName(name);
  }

  public String getFullName()
  {
    return delegate.getFullName();
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
