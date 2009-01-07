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
import org.eclipse.emf.cdo.common.model.CDOClass;
import org.eclipse.emf.cdo.common.model.CDOClassProxy;
import org.eclipse.emf.cdo.common.model.CDOClassifierRef;
import org.eclipse.emf.cdo.common.model.CDOFeature;
import org.eclipse.emf.cdo.common.model.CDOModelUtil;
import org.eclipse.emf.cdo.common.model.CDOPackage;
import org.eclipse.emf.cdo.common.model.CDOPackageManager;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOClass;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOFeature;

import org.eclipse.net4j.util.ObjectUtil;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Eike Stepper
 */
public class CDOClassImpl extends CDOClassifierImpl implements InternalCDOClass
{
  private boolean isAbstract;

  private List<CDOClassProxy> superTypes = new ArrayList<CDOClassProxy>(0);

  private List<CDOFeature> features = new ArrayList<CDOFeature>(0);

  private transient List<Integer> indices;

  private transient CDOClass[] allSuperTypes;

  private transient CDOFeature[] allFeatures;

  public CDOClassImpl()
  {
  }

  public Kind getClassifierKind()
  {
    return Kind.CLASS;
  }

  public boolean isAbstract()
  {
    return isAbstract;
  }

  public void setAbstract(boolean isAbstract)
  {
    this.isAbstract = isAbstract;
  }

  public int getSuperTypeCount()
  {
    return superTypes.size();
  }

  public CDOClass[] getSuperTypes()
  {
    int size = superTypes.size();
    CDOClass[] result = new CDOClass[size];
    for (int i = 0; i < size; i++)
    {
      result[i] = getSuperType(i);
    }

    return result;
  }

  public void setSuperTypes(List<CDOClass> superTypes)
  {
    this.superTypes = new ArrayList<CDOClassProxy>(superTypes.size());
    for (CDOClass cdoClass : superTypes)
    {
      this.superTypes.add(new CDOClassProxy(cdoClass));
    }
  }

  public CDOClass getSuperType(int index)
  {
    return superTypes.get(index).getCdoClass();
  }

  public List<CDOClassProxy> getSuperTypeProxies()
  {
    return Collections.unmodifiableList(superTypes);
  }

  public int getFeatureCount()
  {
    return features.size();
  }

  public CDOFeature[] getFeatures()
  {
    return features.toArray(new CDOFeature[features.size()]);
  }

  public void setFeatures(List<CDOFeature> features)
  {
    this.features = features;
    for (CDOFeature feature : features)
    {
      ((InternalCDOFeature)feature).setContainingClass(this);
    }
  }

  public CDOFeature lookupFeature(int featureID)
  {
    int i = getFeatureIndex(featureID);
    return getAllFeatures()[i];
  }

  public CDOFeature lookupFeature(String name)
  {
    for (CDOFeature feature : getAllFeatures())
    {
      if (ObjectUtil.equals(feature.getName(), name))
      {
        return feature;
      }
    }

    return null;
  }

  public int getFeatureID(CDOFeature feature)
  {
    int index = ((InternalCDOFeature)feature).getFeatureIndex();
    if (index != -1)
    {
      CDOFeature[] features = getAllFeatures();
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

  public CDOClassifierRef createClassRef()
  {
    return CDOModelUtil.createClassRef(getContainingPackage().getPackageURI(), getClassifierID());
  }

  public CDOClass[] getAllSuperTypes()
  {
    if (allSuperTypes == null)
    {
      List<CDOClass> result = new ArrayList<CDOClass>(0);
      for (CDOClass superType : getSuperTypes())
      {
        CDOClass[] higherSupers = superType.getAllSuperTypes();
        for (CDOClass higherSuper : higherSupers)
        {
          addUnique(higherSuper, result);
        }

        addUnique(superType, result);
      }

      allSuperTypes = result.toArray(new CDOClass[result.size()]);
    }

    return allSuperTypes;
  }

  public int getFeatureIndex(int featureID)
  {
    if (indices == null)
    {
      CDOFeature[] features = getAllFeatures();
      indices = new ArrayList<Integer>(features.length);
      int index = 0;
      for (CDOFeature feature : features)
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

  public CDOFeature[] getAllFeatures()
  {
    if (allFeatures == null)
    {
      List<CDOFeature> result = new ArrayList<CDOFeature>(0);
      for (CDOClass superType : getSuperTypes())
      {
        CDOFeature[] features = superType.getAllFeatures();
        addAllFeatures(features, result);
      }

      addAllFeatures(getFeatures(), result);
      allFeatures = result.toArray(new CDOFeature[result.size()]);
    }

    return allFeatures;
  }

  public void addSuperType(CDOClassifierRef classRef)
  {
    superTypes.add(new CDOClassProxy(classRef, getPackageManager()));
  }

  public void addFeature(CDOFeature cdoFeature)
  {
    features.add(cdoFeature);
  }

  public int compareTo(CDOClass that)
  {
    return getName().compareTo(that.getName());
  }

  @Override
  public void read(CDODataInput in, boolean proxy) throws IOException
  {
    super.read(in, proxy);
    isAbstract = in.readBoolean();
    readSuperTypes(in);
    readFeatures(in);
  }

  @Override
  public void write(CDODataOutput out, boolean proxy) throws IOException
  {
    super.write(out, proxy);
    out.writeBoolean(isAbstract);
    writeSuperTypes(out);
    writeFeatures(out);
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("CDOClass[classifierID={0}, name={1}, abstract={2}]", getClassifierID(), getName(),
        isAbstract);
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
    for (int i = 0; i < size; i++)
    {
      CDOClassifierRef classRef = in.readCDOClassRef();
      superTypes.add(new CDOClassProxy(classRef, getPackageManager()));
    }
  }

  private void readFeatures(CDODataInput in) throws IOException
  {
    int size = in.readInt();
    for (int i = 0; i < size; i++)
    {
      CDOFeature cdoFeature = in.readCDOFeature(this);
      addFeature(cdoFeature);
    }
  }

  private void writeSuperTypes(CDODataOutput out) throws IOException
  {
    int size = superTypes.size();
    out.writeInt(size);
    for (CDOClassProxy proxy : superTypes)
    {
      CDOClassifierRef classRef = proxy.getClassRef();
      out.writeCDOClassifierRef(classRef);
    }
  }

  private void writeFeatures(CDODataOutput out) throws IOException
  {
    int size = features.size();
    out.writeInt(size);
    for (CDOFeature cdoFeature : features)
    {
      out.writeCDOFeature(cdoFeature);
    }
  }

  private static void addAllFeatures(CDOFeature[] features, List<CDOFeature> result)
  {
    for (CDOFeature feature : features)
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

  /**
   * @author Eike Stepper
   */
  public static final class Ref extends CDOClassifierImpl.Ref implements CDOClass
  {
    public Ref(CDOPackageManager packageManager, String packageURI, int classifierID)
    {
      super(packageManager, packageURI, classifierID);
    }

    public Kind getClassifierKind()
    {
      return Kind.CLASS;
    }

    @Override
    public CDOClass resolve()
    {
      return (CDOClass)super.resolve();
    }

    @Override
    public String toString()
    {
      if (isResolved())
      {
        resolve().toString();
      }

      return MessageFormat.format("CDOClassRef[packageURI={0}, classifierID={1}]", getPackageURI(), getClassifierID());
    }

    public int compareTo(CDOClass o)
    {
      return resolve().compareTo(o);
    }

    public CDOClassifierRef createClassRef()
    {
      return resolve().createClassRef();
    }

    public CDOFeature[] getAllFeatures()
    {
      return resolve().getAllFeatures();
    }

    public CDOClass[] getAllSuperTypes()
    {
      return resolve().getAllSuperTypes();
    }

    @Override
    public int getClassifierID()
    {
      return resolve().getClassifierID();
    }

    @Override
    public Object getClientInfo()
    {
      return resolve().getClientInfo();
    }

    @Override
    public CDOPackage getContainingPackage()
    {
      return resolve().getContainingPackage();
    }

    public int getFeatureCount()
    {
      return resolve().getFeatureCount();
    }

    public int getFeatureID(CDOFeature feature)
    {
      return resolve().getFeatureID(feature);
    }

    public CDOFeature[] getFeatures()
    {
      return resolve().getFeatures();
    }

    @Override
    public String getName()
    {
      return resolve().getName();
    }

    @Override
    public CDOPackageManager getPackageManager()
    {
      return resolve().getPackageManager();
    }

    @Override
    public String getQualifiedName()
    {
      return resolve().getQualifiedName();
    }

    @Override
    public Object getServerInfo()
    {
      return resolve().getServerInfo();
    }

    public CDOClass getSuperType(int index)
    {
      return resolve().getSuperType(index);
    }

    public int getSuperTypeCount()
    {
      return resolve().getSuperTypeCount();
    }

    public CDOClass[] getSuperTypes()
    {
      return resolve().getSuperTypes();
    }

    public boolean isAbstract()
    {
      return resolve().isAbstract();
    }

    public boolean isResource()
    {
      return resolve().isResource();
    }

    public boolean isResourceFolder()
    {
      return resolve().isResourceFolder();
    }

    public boolean isResourceNode()
    {
      return resolve().isResourceNode();
    }

    public boolean isRoot()
    {
      return resolve().isRoot();
    }

    public CDOFeature lookupFeature(int featureId)
    {
      return resolve().lookupFeature(featureId);
    }

    public CDOFeature lookupFeature(String name)
    {
      return resolve().lookupFeature(name);
    }
  }
}
