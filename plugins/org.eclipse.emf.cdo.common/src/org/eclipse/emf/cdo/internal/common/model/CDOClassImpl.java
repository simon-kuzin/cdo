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
package org.eclipse.emf.cdo.internal.common.model;

import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.model.EClass;
import org.eclipse.emf.cdo.common.model.EClassProxy;
import org.eclipse.emf.cdo.common.model.EClassRef;
import org.eclipse.emf.cdo.common.model.EStructuralFeature;
import org.eclipse.emf.cdo.common.model.CDOModelUtil;
import org.eclipse.emf.cdo.common.model.EPackage;
import org.eclipse.emf.cdo.common.model.CDOPackageManager;
import org.eclipse.emf.cdo.internal.common.bundle.OM;
import org.eclipse.emf.cdo.spi.common.model.InternalEClass;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOFeature;

import org.eclipse.net4j.util.ObjectUtil;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Eike Stepper
 */
public class EClassImpl extends EModelElementImpl implements InternalEClass
{
  private static final ContextTracer MODEL_TRACER = new ContextTracer(OM.DEBUG_MODEL, EClassImpl.class);

  private static final ContextTracer PROTOCOL_TRACER = new ContextTracer(OM.DEBUG_PROTOCOL, EClassImpl.class);

  private EPackage containingPackage;

  private int classifierID;

  private boolean isAbstract;

  private List<EClassProxy> superTypes = new ArrayList<EClassProxy>(0);

  private List<EStructuralFeature> features = new ArrayList<EStructuralFeature>(0);

  private transient List<Integer> indices;

  private transient EClass[] allSuperTypes;

  private transient EStructuralFeature[] allFeatures;

  public EClassImpl()
  {
  }

  public EClassImpl(EPackage containingPackage, int classifierID, String name, boolean isAbstract)
  {
    super(name);
    this.containingPackage = containingPackage;
    this.classifierID = classifierID;
    this.isAbstract = isAbstract;
    if (MODEL_TRACER.isEnabled())
    {
      MODEL_TRACER.format("Created {0}", this);
    }
  }

  public EClassImpl(EPackage containingPackage, CDODataInput in) throws IOException
  {
    this.containingPackage = containingPackage;
    read(in);
  }

  @Override
  public void read(CDODataInput in) throws IOException
  {
    super.read(in);
    classifierID = in.readInt();
    isAbstract = in.readBoolean();
    readSuperTypes(in);
    readFeatures(in);

    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Read class: ID={0}, name={1}, abstract={2}", classifierID, getName(), isAbstract);
    }
  }

  @Override
  public void write(CDODataOutput out) throws IOException
  {
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Writing class: ID={0}, name={1}, abstract={2}", classifierID, getName(), isAbstract);
    }

    super.write(out);
    out.writeInt(classifierID);
    out.writeBoolean(isAbstract);
    writeSuperTypes(out);
    writeFeatures(out);
  }

  public int getFeatureID(EStructuralFeature feature)
  {
    int index = feature.getFeatureIndex();
    if (index != -1)
    {
      EStructuralFeature[] features = getAllFeatures();
      while (index < features.length)
      {
        if (features[index] == feature)
        {
          return index;
        }

        ++index;
      }
    }

    return -1;
  }

  public CDOPackageManager getPackageManager()
  {
    return containingPackage.getPackageManager();
  }

  public EPackage getContainingPackage()
  {
    return containingPackage;
  }

  public void setContainingPackage(EPackage containingPackage)
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

  public String getQualifiedName()
  {
    return getContainingPackage().getQualifiedName() + "." + getName();
  }

  public boolean isAbstract()
  {
    return isAbstract;
  }

  public void setAbstract(boolean isAbstract)
  {
    this.isAbstract = isAbstract;
  }

  public boolean isResourceNode()
  {
    return false;
  }

  public boolean isResourceFolder()
  {
    return false;
  }

  public boolean isResource()
  {
    return false;
  }

  public boolean isRoot()
  {
    return false;
  }

  public int getSuperTypeCount()
  {
    return superTypes.size();
  }

  public EClass[] getSuperTypes()
  {
    int size = superTypes.size();
    EClass[] result = new EClass[size];
    for (int i = 0; i < size; i++)
    {
      result[i] = getSuperType(i);
    }

    return result;
  }

  public void setSuperTypes(List<EClass> superTypes)
  {
    this.superTypes = new ArrayList<EClassProxy>(superTypes.size());
    for (EClass cdoClass : superTypes)
    {
      this.superTypes.add(new EClassProxy(cdoClass));
    }
  }

  public EClass getSuperType(int index)
  {
    return superTypes.get(index).getCdoClass();
  }

  public List<EClassProxy> getSuperTypeProxies()
  {
    return Collections.unmodifiableList(superTypes);
  }

  public int getFeatureCount()
  {
    return features.size();
  }

  public EStructuralFeature[] getFeatures()
  {
    return features.toArray(new EStructuralFeature[features.size()]);
  }

  public void setFeatures(List<EStructuralFeature> features)
  {
    this.features = features;
    for (EStructuralFeature feature : features)
    {
      ((InternalCDOFeature)feature).setContainingClass(this);
    }
  }

  public EStructuralFeature lookupFeature(int featureID)
  {
    int i = getFeatureIndex(featureID);
    return getAllFeatures()[i];
  }

  public EStructuralFeature lookupFeature(String name)
  {
    for (EStructuralFeature feature : getAllFeatures())
    {
      if (ObjectUtil.equals(feature.getName(), name))
      {
        return feature;
      }
    }

    return null;
  }

  public EClassRef createClassRef()
  {
    return CDOModelUtil.createClassRef(containingPackage.getPackageURI(), classifierID);
  }

  public EClass[] getAllSuperTypes()
  {
    if (allSuperTypes == null)
    {
      List<EClass> result = new ArrayList<EClass>(0);
      for (EClass superType : getSuperTypes())
      {
        EClass[] higherSupers = superType.getAllSuperTypes();
        for (EClass higherSuper : higherSupers)
        {
          addUnique(higherSuper, result);
        }

        addUnique(superType, result);
      }

      allSuperTypes = result.toArray(new EClass[result.size()]);
    }

    return allSuperTypes;
  }

  public int getFeatureIndex(int featureID)
  {
    if (indices == null)
    {
      EStructuralFeature[] features = getAllFeatures();
      indices = new ArrayList<Integer>(features.length);
      int index = 0;
      for (EStructuralFeature feature : features)
      {
        if (feature.getContainingClass() == this)
        {
          ((InternalCDOFeature)feature).setFeatureIndex(index);
        }

        setIndex(feature.getFeatureID(), index);
        index++;
      }
    }

    return indices.get(featureID);
  }

  public EStructuralFeature[] getAllFeatures()
  {
    if (allFeatures == null)
    {
      List<EStructuralFeature> result = new ArrayList<EStructuralFeature>(0);
      for (EClass superType : getSuperTypes())
      {
        EStructuralFeature[] features = superType.getAllFeatures();
        addAllFeatures(features, result);
      }

      addAllFeatures(getFeatures(), result);
      allFeatures = result.toArray(new EStructuralFeature[result.size()]);
    }

    return allFeatures;
  }

  public void addSuperType(EClassRef classRef)
  {
    if (MODEL_TRACER.isEnabled())
    {
      MODEL_TRACER.format("Adding super type: {0}", classRef);
    }

    superTypes.add(new EClassProxy(classRef, containingPackage.getPackageManager()));
  }

  public void addFeature(EStructuralFeature cdoFeature)
  {
    if (MODEL_TRACER.isEnabled())
    {
      MODEL_TRACER.format("Adding feature: {0}", cdoFeature);
    }

    features.add(cdoFeature);
  }

  public int compareTo(EClass that)
  {
    return getName().compareTo(that.getName());
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("EClass(ID={0}, name={1})", classifierID, getName());
  }

  private void setIndex(int featureID, int index)
  {
    while (indices.size() <= featureID)
    {
      indices.add(null);
    }

    indices.set(featureID, index);
  }

  private void readSuperTypes(CDODataInput in) throws IOException
  {
    int size = in.readInt();
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Reading {0} super types", size);
    }

    for (int i = 0; i < size; i++)
    {
      EClassRef classRef = in.readEClassRef();
      if (PROTOCOL_TRACER.isEnabled())
      {
        PROTOCOL_TRACER.format("Read super type: classRef={0}", classRef, classifierID);
      }

      superTypes.add(new EClassProxy(classRef, containingPackage.getPackageManager()));
    }
  }

  private void readFeatures(CDODataInput in) throws IOException
  {
    int size = in.readInt();
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Reading {0} features", size);
    }

    for (int i = 0; i < size; i++)
    {
      EStructuralFeature cdoFeature = in.readEStructuralFeature(this);
      addFeature(cdoFeature);
    }
  }

  private void writeSuperTypes(CDODataOutput out) throws IOException
  {
    int size = superTypes.size();
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Writing {0} super types", size);
    }

    out.writeInt(size);
    for (EClassProxy proxy : superTypes)
    {
      EClassRef classRef = proxy.getClassRef();
      if (PROTOCOL_TRACER.isEnabled())
      {
        PROTOCOL_TRACER.format("Writing super type: classRef={0}", classRef);
      }

      out.writeEClassRef(classRef);
    }
  }

  private void writeFeatures(CDODataOutput out) throws IOException
  {
    int size = features.size();
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Writing {0} features", size);
    }

    out.writeInt(size);
    for (EStructuralFeature cdoFeature : features)
    {
      out.writeEStructuralFeature(cdoFeature);
    }
  }

  private static void addAllFeatures(EStructuralFeature[] features, List<EStructuralFeature> result)
  {
    for (EStructuralFeature feature : features)
    {
      addUnique(feature, result);
    }
  }

  @SuppressWarnings("unchecked")
  private static void addUnique(Object object, List result)
  {
    if (!result.contains(object))
    {
      result.add(object);
    }
  }
}
