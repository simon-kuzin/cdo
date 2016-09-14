/*
 * Copyright (c) 2004-2016 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.internal.cdo.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * For finding potential methods to optimize view locking.
 *
 * @author Eike Stepper
 */
public final class MethodStack
{
  private final Stack<String> methods = new Stack<String>();

  public void push()
  {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    String lockingMethod = getMethod(stackTrace[3]);
    String parentMethod = getMethod(stackTrace[4]);
    String stackedMethod = methods.isEmpty() ? null : methods.peek();

    if (parentMethod.equals(stackedMethod))
    {
      UnnecessaryLock.register(lockingMethod, parentMethod);
    }

    methods.push(lockingMethod);
  }

  public void pop()
  {
    methods.pop();
  }

  private static String getMethod(StackTraceElement element)
  {
    String className = element.getClassName();
    int lastDot = className.lastIndexOf('.');
    if (lastDot != -1)
    {
      className = className.substring(lastDot + 1);
    }

    return className + "." + element.getMethodName() + "()";
  }

  /**
   * @author Eike Stepper
   */
  private static final class UnnecessaryLock implements Comparable<MethodStack.UnnecessaryLock>
  {
    private static final Map<String, MethodStack.UnnecessaryLock> REGISTRY = new HashMap<String, MethodStack.UnnecessaryLock>();

    private static boolean shutdownHookAdded;

    private final String lockingMethod;

    private final Set<String> parentMethods = new HashSet<String>();

    private int counter;

    private UnnecessaryLock(String lockingMethod)
    {
      this.lockingMethod = lockingMethod;
    }

    private void addParentMethod(String parentMethod)
    {
      parentMethods.add(parentMethod);
      ++counter;
    }

    @Override
    public String toString()
    {
      String string = parentMethods.toString();
      return lockingMethod + "\t" + counter + "\t\t" + string.substring(1, string.length() - 1).replace(", ", "\t");
    }

    public int compareTo(MethodStack.UnnecessaryLock o)
    {
      return o.counter - counter;
    }

    public static void register(String lockingMethod, String parentMethod)
    {
      synchronized (REGISTRY)
      {
        MethodStack.UnnecessaryLock lock = REGISTRY.get(lockingMethod);
        if (lock == null)
        {
          lock = new UnnecessaryLock(lockingMethod);
          REGISTRY.put(lockingMethod, lock);

          if (!shutdownHookAdded)
          {
            Runtime.getRuntime().addShutdownHook(new Thread()
            {
              @Override
              public void run()
              {
                List<MethodStack.UnnecessaryLock> list = new ArrayList<MethodStack.UnnecessaryLock>(REGISTRY.values());
                Collections.sort(list);

                for (MethodStack.UnnecessaryLock lock : list)
                {
                  System.out.println(lock);
                }
              }
            });

            shutdownHookAdded = true;
          }
        }

        lock.addParentMethod(parentMethod);
      }
    }
  }
}
