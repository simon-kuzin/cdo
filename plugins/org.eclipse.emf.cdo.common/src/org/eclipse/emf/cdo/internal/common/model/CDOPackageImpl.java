/***************************************************************************
 * Copyright (c) 2004 - 2008 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Simon McDuff - http://bugs.eclipse.org/246442 
 **************************************************************************/
package org.eclipse.emf.cdo.internal.common.model;

import org.eclipse.emf.cdo.common.CDODataInput;
import org.eclipse.emf.cdo.common.CDODataOutput;
import org.eclipse.emf.cdo.common.id.CDOIDMetaRange;
import org.eclipse.emf.cdo.common.model.CDOClass;
import org.eclipse.emf.cdo.common.model.CDOClassifier;
import org.eclipse.emf.cdo.common.model.CDOPackage;
import org.eclipse.emf.cdo.common.model.CDOPackageManager;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOClassifier;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackage;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageManager;

import org.eclipse.net4j.util.ObjectUtil;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eike Stepper
 */
public class CDOPackageImpl extends CDONamedElementImpl implements InternalCDOPackage
{
  private static final ContextTracer MODEL_TRACER = new ContextTracer(OM.DEBUG_MODEL, CDOPackageImpl.class);

  private static final ContextTracer PROTOCOL_TRACER = new ContextTracer(OM.DEBUG_PROTOCOL, CDOPackageImpl.class);

  private CDOPackageManager packageManager;

  private String packageURI;

  private String parentURI;

  private CDOIDMetaRange metaIDRange;

  private List<CDOClassifier> classifiers;

  private boolean dynamic;

  private String ecore;

  private transient boolean ecoreLoaded;

  private transient State state = State.NEW;

  private transient List<CDOClass> classes;

  private transient List<CDOClassifier> index;

  public CDOPackageImpl()
  {
  }

  // public CDOPackageImpl(CDOPackageManager packageManager, String packageURI, String name, String ecore,
  // boolean dynamic, CDOIDMetaRange metaIDRange, String parentURI)
  // {
  // super(null, name);
  // this.packageManager = packageManager;
  // this.packageURI = packageURI;
  // this.dynamic = dynamic;
  // this.metaIDRange = metaIDRange;
  // this.parentURI = parentURI;
  // if (MODEL_TRACER.isEnabled())
  // {
  // MODEL_TRACER.format("Created {0}", this);
  // }
  //
  // setEcore(ecore);
  // createLists();
  // }
  //
  // /**
  // * Creates a proxy CDO package
  // */
  // public CDOPackageImpl(CDOPackageManager packageManager, String packageURI, boolean dynamic,
  // CDOIDMetaRange metaIDRange, String parentURI)
  // {
  // this.packageManager = packageManager;
  // this.packageURI = packageURI;
  // this.dynamic = dynamic;
  // this.metaIDRange = metaIDRange;
  // this.parentURI = parentURI;
  // if (MODEL_TRACER.isEnabled())
  // {
  // MODEL_TRACER.format("Created proxy package {0}, dynamic={1}, metaIDRange={2}, parentURI={3}", packageURI,
  // dynamic, metaIDRange, packageURI);
  // }
  // }

  public CDOPackageManager getPackageManager()
  {
    return packageManager;
  }

  public void setPackageManager(CDOPackageManager packageManager)
  {
    this.packageManager = packageManager;
  }

  public State getState()
  {
    return state;
  }

  public void setState(State state)
  {
    this.state = state;
  }

  public String getParentURI()
  {
    return parentURI;
  }

  public void setParentURI(String parentURI)
  {
    this.parentURI = parentURI;
  }

  public CDOPackage getTopLevelPackage()
  {
    CDOPackage parentPackage = getParentPackage();
    return parentPackage == null ? this : parentPackage.getTopLevelPackage();
  }

  public CDOPackage getParentPackage()
  {
    if (parentURI == null)
    {
      return null;
    }

    return packageManager.lookupPackage(parentURI);
  }

  public CDOPackage[] getSubPackages(boolean recursive)
  {
    List<CDOPackage> result = new ArrayList<CDOPackage>();
    CDOPackage[] allPackages = packageManager.getPackages();
    getSubPackages(this, allPackages, result, recursive);
    return result.toArray(new CDOPackage[result.size()]);
  }

  private void getSubPackages(CDOPackage parentPackage, CDOPackage[] allPackages, List<CDOPackage> result,
      boolean recursive)
  {
    for (CDOPackage cdoPackage : allPackages)
    {
      if (ObjectUtil.equals(cdoPackage.getParentURI(), parentPackage.getPackageURI()))
      {
        result.add(cdoPackage);
        if (recursive)
        {
          getSubPackages(cdoPackage, allPackages, result, true);
        }
      }
    }
  }

  public String getPackageURI()
  {
    return packageURI;
  }

  public void setPackageURI(String packageURI)
  {
    this.packageURI = packageURI;
  }

  public String getQualifiedName()
  {
    CDOPackage parentPackage = getParentPackage();
    if (parentPackage != null)
    {
      return parentPackage.getQualifiedName() + "." + getName();
    }

    return getName();
  }

  public CDOClassifier lookupClassifier(int classifierID)
  {
    load();
    return index.get(classifierID);
  }

  public int getClassifierCount()
  {
    load();
    return classifiers.size();
  }

  public CDOClassifier[] getClassifiers()
  {
    load();
    return classifiers.toArray(new CDOClassifier[classifiers.size()]);
  }

  public void setClassifiers(List<CDOClassifier> classifiers)
  {
    this.classifiers = classifiers;
    for (CDOClassifier cdoClassifier : classifiers)
    {
      ((InternalCDOClassifier)cdoClassifier).setContainingPackage(this);
      setIndex(cdoClassifier.getClassifierID(), cdoClassifier);
    }
  }

  public void addClassifier(CDOClassifier cdoClassifier)
  {
    int classifierID = cdoClassifier.getClassifierID();
    if (MODEL_TRACER.isEnabled())
    {
      MODEL_TRACER.format("Adding classifier: {0}", cdoClassifier);
    }

    setIndex(classifierID, cdoClassifier);
    classifiers.add(cdoClassifier);
    if (cdoClassifier.getClassifierKind() == CDOClassifier.Kind.CLASS)
    {
      classes.add((CDOClass)cdoClassifier);
    }
  }

  public CDOClass lookupClass(int classifierID)
  {
    CDOClassifier classifier = lookupClassifier(classifierID);
    if (classifier.getClassifierKind() == CDOClassifier.Kind.CLASS)
    {
      return (CDOClass)classifier;
    }

    return null;
  }

  public int getClassCount()
  {
    load();
    return classes.size();
  }

  public CDOClass[] getClasses()
  {
    load();
    return classes.toArray(new CDOClass[classes.size()]);
  }

  /**
   * @return All classes with <code>isAbstract() == false</code> and <code>isSystem() == false</code>.
   */
  public CDOClass[] getConcreteClasses()
  {
    load();
    List<CDOClass> result = new ArrayList<CDOClass>(0);
    for (CDOClass cdoClass : classes)
    {
      if (!cdoClass.isAbstract())
      {
        result.add(cdoClass);
      }
    }

    return result.toArray(new CDOClass[result.size()]);
  }

  public synchronized String basicGetEcore()
  {
    return ecore;
  }

  public synchronized String getEcore()
  {
    if (!ecoreLoaded)
    {
      if (parentURI == null && !isSystem())
      {
        ((InternalCDOPackageManager)packageManager).loadPackageEcore(this);
      }
    }

    return ecore;
  }

  public synchronized void setEcore(String ecore)
  {
    this.ecore = ecore;
    ecoreLoaded = true;
  }

  public CDOIDMetaRange getMetaIDRange()
  {
    return metaIDRange;
  }

  public void setMetaIDRange(CDOIDMetaRange metaIDRange)
  {
    this.metaIDRange = metaIDRange;
  }

  public boolean isDynamic()
  {
    return dynamic;
  }

  public void setDynamic(boolean dynamic)
  {
    this.dynamic = dynamic;
  }

  public boolean isSystem()
  {
    return false;
  }

  public boolean isProxy()
  {
    return classes == null;
  }

  // public CDOPackageImpl(CDOPackageManager packageManager, String packageURI, String name, String ecore,
  // boolean dynamic, CDOIDMetaRange metaIDRange, String parentURI)
  // {
  // super(null, name);
  // this.packageManager = packageManager;
  // this.packageURI = packageURI;
  // this.dynamic = dynamic;
  // this.metaIDRange = metaIDRange;
  // this.parentURI = parentURI;
  // if (MODEL_TRACER.isEnabled())
  // {
  // MODEL_TRACER.format("Created {0}", this);
  // }
  //
  // setEcore(ecore);
  // createLists();
  // }
  //
  // /**
  // * Creates a proxy CDO package
  // */
  // public CDOPackageImpl(CDOPackageManager packageManager, String packageURI, boolean dynamic,
  // CDOIDMetaRange metaIDRange, String parentURI)
  // {
  // this.packageManager = packageManager;
  // this.packageURI = packageURI;
  // this.dynamic = dynamic;
  // this.metaIDRange = metaIDRange;
  // this.parentURI = parentURI;
  // if (MODEL_TRACER.isEnabled())
  // {
  // MODEL_TRACER.format("Created proxy package {0}, dynamic={1}, metaIDRange={2}, parentURI={3}", packageURI,
  // dynamic, metaIDRange, packageURI);
  // }
  // }
  //
  // public CDOPackageImpl(CDOPackageManager packageManager, String packageURI, String name, String ecore,
  // boolean dynamic, CDOIDMetaRange metaIDRange, String parentURI)
  // {
  // super(null, name);
  // this.packageManager = packageManager;
  // this.packageURI = packageURI;
  // this.dynamic = dynamic;
  // this.metaIDRange = metaIDRange;
  // this.parentURI = parentURI;
  // if (MODEL_TRACER.isEnabled())
  // {
  // MODEL_TRACER.format("Created {0}", this);
  // }
  //
  // setEcore(ecore);
  // createLists();
  // }
  //
  // /**
  // * Creates a proxy CDO package
  // */
  // public CDOPackageImpl(CDOPackageManager packageManager, String packageURI, boolean dynamic,
  // CDOIDMetaRange metaIDRange, String parentURI)
  // {
  // this.packageManager = packageManager;
  // this.packageURI = packageURI;
  // this.dynamic = dynamic;
  // this.metaIDRange = metaIDRange;
  // this.parentURI = parentURI;
  // if (MODEL_TRACER.isEnabled())
  // {
  // MODEL_TRACER.format("Created proxy package {0}, dynamic={1}, metaIDRange={2}, parentURI={3}", packageURI,
  // dynamic, metaIDRange, packageURI);
  // }
  // }

  /**
   * If not called through {@link #CDOPackageImpl(CDOPackageManager, CDODataInput)} the following must becalled
   * <b>before</b>:
   * <p>
   * 
   * <pre>
   * setName(in.readString());
   * </pre>
   */
  @Override
  public void read(CDODataInput in, boolean proxy) throws IOException
  {
    super.read(in, proxy);
    createLists();
    packageURI = in.readCDOPackageURI();
    dynamic = in.readBoolean();
    metaIDRange = in.readCDOIDMetaRange();
    parentURI = in.readString();
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Read package: URI={0}, name={1}, dynamic={2}, metaIDRange={3}, parentURI={4}",
          packageURI, getName(), dynamic, metaIDRange, parentURI);
    }

    int size = in.readInt();
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Reading {0} classes", size);
    }

    for (int i = 0; i < size; i++)
    {
      CDOClassifier cdoClassifier = in.readCDOClassifier(this);
      addClassifier(cdoClassifier);
    }
  }

  @Override
  public void write(CDODataOutput out, boolean proxy) throws IOException
  {
    load();
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Writing package: URI={0}, name={1}, dynamic={2}, metaIDRange={3}, parentURI={4}",
          packageURI, getName(), dynamic, metaIDRange, parentURI);
    }

    super.write(out, proxy);
    out.writeCDOPackageURI(packageURI);
    out.writeBoolean(dynamic);
    out.writeCDOIDMetaRange(metaIDRange);
    out.writeString(parentURI);

    int size = classifiers.size();
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Writing {0} classifiers", size);
    }

    out.writeInt(size);
    for (CDOClassifier cdoClassifier : classifiers)
    {
      out.writeCDOClassifier(cdoClassifier);
    }
  }

  public int compareTo(CDOPackage that)
  {
    return getPackageURI().compareTo(that.getPackageURI());
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("CDOPackage(URI={0}, name={1}, dynamic={2}, metaIDRange={3}, parentURI={4})",
        packageURI, getName(), dynamic, metaIDRange, parentURI);
  }

  private void setIndex(int classifierID, CDOClassifier cdoClassifier)
  {
    while (classifierID >= index.size())
    {
      index.add(null);
    }

    index.set(classifierID, cdoClassifier);
  }

  private void createLists()
  {
    classifiers = new ArrayList<CDOClassifier>(0);
    classes = new ArrayList<CDOClass>(0);
    index = new ArrayList<CDOClassifier>(0);
  }

  private synchronized void load()
  {
    if (classes == null)
    {
      createLists();
      ((InternalCDOPackageManager)packageManager).loadPackage(this);
    }
  }
}
