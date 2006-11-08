/***************************************************************************
 * Copyright (c) 2004, 2005, 2006 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.net4j.util.concurrent;

import java.util.concurrent.ExecutorCompletionService;

/**
 * TODO Replace by {@link ExecutorCompletionService}?
 * 
 * @author Eike Stepper
 */
public interface WorkSerializer
{
  public void addWork(Runnable work);

  public void dispose();
}
