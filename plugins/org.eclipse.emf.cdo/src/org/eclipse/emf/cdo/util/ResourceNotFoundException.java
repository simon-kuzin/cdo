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
package org.eclipse.emf.cdo.util;

import org.eclipse.emf.cdo.common.util.CDOException;

/**
 * @author Eike Stepper
 * @since 3.0
 */
public class ResourceNotFoundException extends CDOException
{
  private static final long serialVersionUID = 1L;

  public ResourceNotFoundException()
  {
  }

  public ResourceNotFoundException(String message)
  {
    super(message);
  }

  public ResourceNotFoundException(Throwable cause)
  {
    super(cause);
  }

  public ResourceNotFoundException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
