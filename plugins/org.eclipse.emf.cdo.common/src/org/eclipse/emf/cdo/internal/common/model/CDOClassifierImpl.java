/***************************************************************************
 * Copyright (c) 2004 - 2008 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo.internal.common.model;

import org.eclipse.emf.cdo.common.CDODataInput;
import org.eclipse.emf.cdo.common.CDODataOutput;
import org.eclipse.emf.cdo.common.model.CDOPackage;
import org.eclipse.emf.cdo.common.model.CDOPackageManager;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOClassifier;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public abstract class CDOClassifierImpl extends CDONamedElementImpl implements InternalCDOClassifier
{
  private CDOPackage containingPackage;

  private int classifierID;

  protected CDOClassifierImpl()
  {
  }

  public CDOPackage getContainingPackage()
  {
    return containingPackage;
  }

  public void setContainingPackage(CDOPackage containingPackage)
  {
    this.containingPackage = containingPackage;
  }

  public int getClassifierID()
  {
    return classifierID;
  }

  public void setClassifierID(int classifierID)
  {
    this.classifierID = classifierID;
  }

  public CDOPackageManager getPackageManager()
  {
    return containingPackage.getPackageManager();
  }

  public String getQualifiedName()
  {
    return getContainingPackage().getQualifiedName() + "." + getName();
  }

  @Override
  public void read(CDODataInput in, boolean proxy) throws IOException
  {
    super.read(in, proxy);
    classifierID = in.readInt();
  }

  @Override
  public void write(CDODataOutput out, boolean proxy) throws IOException
  {
    super.write(out, proxy);
    out.writeInt(classifierID);
  }

  /**
   * @author Eike Stepper
   */
  public static abstract class Ref implements InternalCDOClassifier
  {
    private transient CDOPackageManager packageManager;

    private String packageURI;

    private int classifierID;

    private transient InternalCDOClassifier classifier;

    public Ref(CDOPackageManager packageManager, String packageURI, int classifierID)
    {
      this.packageManager = packageManager;
      this.packageURI = packageURI;
      this.classifierID = classifierID;
    }

    public CDOPackageManager getPackageManager()
    {
      return packageManager;
    }

    public String getPackageURI()
    {
      return packageURI;
    }

    public int getClassifierID()
    {
      return classifierID;
    }

    public void setClassifierID(int classifierId)
    {
      classifier.setClassifierID(classifierId);
    }

    public synchronized boolean isResolved()
    {
      return classifier != null;
    }

    public synchronized InternalCDOClassifier resolve()
    {
      if (classifier == null)
      {
        CDOPackage cdoPackage = packageManager.lookupPackage(packageURI);
        if (cdoPackage == null)
        {
          throw new IllegalStateException("Package not found: " + packageURI);
        }

        classifier = (InternalCDOClassifier)cdoPackage.lookupClassifier(classifierID);
        if (classifier == null)
        {
          throw new IllegalStateException("Classifier not found in package" + packageURI + ": " + classifierID);
        }
      }

      return classifier;
    }

    public CDOPackage getContainingPackage()
    {
      return resolve().getContainingPackage();
    }

    public void setContainingPackage(CDOPackage containingPackage)
    {
      classifier.setContainingPackage(containingPackage);
    }

    public String getName()
    {
      return resolve().getName();
    }

    public void setName(String name)
    {
      classifier.setName(name);
    }

    public String getQualifiedName()
    {
      return resolve().getQualifiedName();
    }

    public Object getClientInfo()
    {
      return resolve().getClientInfo();
    }

    public void setClientInfo(Object clientInfo)
    {
      classifier.setClientInfo(clientInfo);
    }

    public Object getServerInfo()
    {
      return resolve().getServerInfo();
    }

    public void setServerInfo(Object serverInfo)
    {
      classifier.setServerInfo(serverInfo);
    }

    public void read(CDODataInput in, boolean proxy) throws IOException
    {
      classifier.read(in, proxy);
    }

    public void write(CDODataOutput out, boolean proxy) throws IOException
    {
      classifier.write(out, proxy);
    }
  }
}
