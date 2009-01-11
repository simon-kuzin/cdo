/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.net4j.internal.util.om.pref;

import org.eclipse.net4j.util.om.pref.OMPreference;

/**
 * @author Eike Stepper
 */
public final class DoublePreference extends Preference<Double> implements OMPreference<Double>
{
  public DoublePreference(Preferences preferences, String name, Double defaultValue)
  {
    super(preferences, name, defaultValue);
  }

  @Override
  protected String getString()
  {
    return Double.toString(getValue());
  }

  @Override
  protected Double convert(String value)
  {
    return Double.parseDouble(value);
  }

  public Type getType()
  {
    return Type.DOUBLE;
  }
}
