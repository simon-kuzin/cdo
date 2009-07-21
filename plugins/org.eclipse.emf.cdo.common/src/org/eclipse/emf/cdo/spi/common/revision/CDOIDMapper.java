/***************************************************************************
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Simon McDuff - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo.spi.common.revision;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.revision.CDOReferenceAdjuster;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Simon McDuff
 * @since 2.0
 */
public class CDOIDMapper implements CDOReferenceAdjuster
{
  private Map<CDOID, CDOID> idMappings;

  public CDOIDMapper(Map<CDOID, CDOID> idMappings)
  {
    this.idMappings = idMappings;
  }

  /**
   * @since 3.0
   */
  public CDOIDMapper()
  {
    this(new HashMap<CDOID, CDOID>());
  }

  public Map<CDOID, CDOID> getIDMappings()
  {
    return idMappings;
  }

  /**
   * @since 3.0
   */
  public CDOID getIDMapping(CDOID key)
  {
    return idMappings.get(key);
  }

  /**
   * @since 3.0
   */
  public CDOID putIDMapping(CDOID key, CDOID value)
  {
    return idMappings.put(key, value);
  }

  /**
   * @since 3.0
   */
  public void reverseIDMappings()
  {
    if (idMappings.isEmpty())
    {
      return;
    }

    Map<CDOID, CDOID> newMappings = new HashMap<CDOID, CDOID>();
    for (Map.Entry<CDOID, CDOID> mapping : idMappings.entrySet())
    {
      CDOID old = newMappings.put(mapping.getValue(), mapping.getKey());
      if (old != null)
      {
        throw new IllegalStateException("Duplicate key: " + old);
      }
    }

    idMappings = newMappings;
  }

  /**
   * @since 3.0
   */
  public CDOID adjustReference(CDOID id)
  {
    if (CDOIDUtil.isNull(id))
    {
      return id;
    }

    return idMappings.get(id);
  }
}
