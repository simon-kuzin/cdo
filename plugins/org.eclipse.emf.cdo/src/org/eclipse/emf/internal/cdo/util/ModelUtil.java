/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Simon McDuff - maintenance 
 */
package org.eclipse.emf.internal.cdo.util;

import org.eclipse.emf.cdo.common.id.CDOIDMetaRange;
import org.eclipse.emf.cdo.common.model.CDOClassifierRef;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.common.model.EMFUtil;
import org.eclipse.emf.cdo.common.util.CDOException;
import org.eclipse.emf.cdo.eresource.EresourcePackage;

import org.eclipse.emf.internal.cdo.CDOFactoryImpl;
import org.eclipse.emf.internal.cdo.bundle.OM;
import org.eclipse.emf.internal.cdo.session._CDOSessionPackageManagerImpl;

import org.eclipse.net4j.util.ImplementationError;
import org.eclipse.net4j.util.ObjectUtil;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EPackageImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.spi.cdo.InternalCDOSession;

/**
 * @author Eike Stepper
 */
public final class ModelUtil
{
  private static final ContextTracer MODEL_TRACER = new ContextTracer(OM.DEBUG_MODEL, ModelUtil.class);

  private ModelUtil()
  {
  }

  public static CDOType getCDOType(EStructuralFeature eFeature)
  {
    if (eFeature instanceof EReference)
    {
      throw new ImplementationError("Should only be called for attributes");
      // return CDOTypeImpl.OBJECT;
    }

    EClassifier classifier = eFeature.getEType();
    if (classifier.getEPackage() == EcorePackage.eINSTANCE)
    {
      int classifierID = classifier.getClassifierID();
      switch (classifierID)
      {
      case EcorePackage.EBOOLEAN:
      case EcorePackage.EBOOLEAN_OBJECT:
      case EcorePackage.EBYTE:
      case EcorePackage.EBYTE_OBJECT:
      case EcorePackage.ECHAR:
      case EcorePackage.ECHARACTER_OBJECT:
      case EcorePackage.EDATE:
      case EcorePackage.EDOUBLE:
      case EcorePackage.EDOUBLE_OBJECT:
      case EcorePackage.EFLOAT:
      case EcorePackage.EFLOAT_OBJECT:
      case EcorePackage.EINT:
      case EcorePackage.EINTEGER_OBJECT:
      case EcorePackage.ELONG:
      case EcorePackage.ELONG_OBJECT:
      case EcorePackage.ESHORT:
      case EcorePackage.ESHORT_OBJECT:
      case EcorePackage.EFEATURE_MAP_ENTRY:
        CDOType type = CDOModelUtil.getType(classifierID);
        if (type == CDOType.OBJECT)
        {
          throw new ImplementationError("Attributes can not be of type OBJECT");
        }

        return type;

      case EcorePackage.ESTRING:
        return CDOType.STRING;
      }
    }

    if (classifier instanceof EDataType)
    {
      return CDOType.CUSTOM;
    }

    throw new IllegalArgumentException("Invalid attribute type: " + classifier);
  }

  public static void initializeEPackage(EPackage ePackage, EPackage cdoPackage)
  {
    ((InternalEPackage)cdoPackage).setClientInfo(ePackage);
    for (EClass eClass : EMFUtil.getPersistentClasses(ePackage))
    {
      EClass cdoClass = createEClass(eClass, cdoPackage);
      ((InternalEPackage)cdoPackage).addClass(cdoClass);
    }
  }

  public static EPackage getEPackage(EPackage ePackage, _CDOSessionPackageManagerImpl packageManager)
  {
    String packageURI = ePackage.getNsURI();
    EPackage cdoPackage = packageManager.lookupPackage(packageURI);
    if (cdoPackage == null)
    {
      EPackage topLevelPackage = EMFUtil.getTopLevelPackage(ePackage);
      if (topLevelPackage != ePackage)
      {
        getEPackage(topLevelPackage, packageManager);
        cdoPackage = packageManager.lookupPackage(packageURI);
      }
      else
      {
        cdoPackage = addEPackage(topLevelPackage, packageManager);
      }
    }

    return cdoPackage;
  }

  public static EClass getEClass(EClass eClass, _CDOSessionPackageManagerImpl packageManager)
  {
    EPackage cdoPackage = getEPackage(eClass.getEPackage(), packageManager);
    return cdoPackage.lookupClass(eClass.getClassifierID());
  }

  public static EStructuralFeature getEStructuralFeature(EStructuralFeature eFeature,
      _CDOSessionPackageManagerImpl packageManager)
  {
    EClass cdoClass = getEClass(eFeature.getEContainingClass(), packageManager);
    return cdoClass.lookupFeature(eFeature.getFeatureID());
  }

  public static EPackage addEPackage(EPackage ePackage, _CDOSessionPackageManagerImpl packageManager)
  {
    EPackage cdoPackage = createEPackage(ePackage, packageManager);
    packageManager.addPackage(cdoPackage);

    for (EPackage subPackage : ePackage.getESubpackages())
    {
      addEPackage(subPackage, packageManager);
    }

    return cdoPackage;
  }

  /**
   * @see EMFUtil#getPersistentFeatures(org.eclipse.emf.common.util.EList)
   * @see http://www.eclipse.org/newsportal/article.php?id=26780&group=eclipse.tools.emf#26780
   */
  public static EPackage createEPackage(EPackage ePackage, _CDOSessionPackageManagerImpl packageManager)
  {
    InternalCDOSession session = packageManager.getSession();
    String uri = ePackage.getNsURI();
    String parentURI = EMFUtil.getParentURI(ePackage);
    String name = ePackage.getName();
    boolean dynamic = EMFUtil.isDynamicEPackage(ePackage);
    String ecore = null;
    CDOIDMetaRange idRange = null;

    if (parentURI == null)
    {
      if (!EcorePackage.eINSTANCE.getNsURI().equals(uri))
      {
        ecore = EMFUtil.ePackageToString(ePackage, session.getPackageRegistry());
      }

      idRange = session.mapMetaInstances(ePackage);
    }

    EPackage cdoPackage = CDOModelUtil.createPackage(packageManager, uri, name, ecore, dynamic, idRange, parentURI);
    initializeEPackage(ePackage, cdoPackage);
    return cdoPackage;
  }

  public static EClass createEClass(EClass eClass, EPackage containingPackage)
  {
    InternalEClass cdoClass = (InternalEClass)CDOModelUtil.createClass(containingPackage, eClass.getClassifierID(),
        eClass.getName(), eClass.isAbstract());
    cdoClass.setClientInfo(eClass);

    for (EClass superType : eClass.getESuperTypes())
    {
      CDOClassifierRef classRef = createClassRef(superType);
      cdoClass.addSuperType(classRef);
    }

    // Bugs: 247978 Make sure featureIndex are properly set for dynamic classes
    eClass.getEAllStructuralFeatures();

    for (EStructuralFeature eFeature : EMFUtil.getPersistentFeatures(eClass.getEStructuralFeatures()))
    {
      EStructuralFeature cdoFeature = createEStructuralFeature(eFeature, cdoClass);
      cdoClass.addFeature(cdoFeature);
    }

    return cdoClass;
  }

  public static EStructuralFeature createEStructuralFeature(EStructuralFeature eFeature, EClass containingClass)
  {
    InternalCDOFeature cdoFeature = (InternalCDOFeature)(EMFUtil.isReference(eFeature) ? createCDOReference(
        (EReference)eFeature, containingClass) : createCDOAttribute((EAttribute)eFeature, containingClass));
    cdoFeature.setClientInfo(eFeature);
    return cdoFeature;
  }

  public static EStructuralFeature createCDOReference(EReference eFeature, EClass containingClass)
  {
    CDOPackageRegistry packageManager = containingClass.getPackageUnitManager();
    int featureID = eFeature.getFeatureID();
    String name = eFeature.getName();
    CDOClassifierRef classRef = createClassRef(eFeature.getEType());
    boolean many = eFeature.isMany();
    boolean containment = EMFUtil.isContainment(eFeature);
    EStructuralFeature cdoFeature = CDOModelUtil.createReference(containingClass, featureID, name, new EClassProxy(
        classRef, packageManager), many, containment);

    EReference opposite = eFeature.getEOpposite();
    if (MODEL_TRACER.isEnabled() && opposite != null)
    {
      MODEL_TRACER.format("Opposite info: package={0}, class={1}, feature={2}", opposite.getEContainingClass()
          .getEPackage().getNsURI(), opposite.getEContainingClass().getName(), opposite.getName());
    }

    return cdoFeature;
  }

  public static EStructuralFeature createCDOAttribute(EAttribute eFeature, EClass containingClass)
  {
    int featureID = eFeature.getFeatureID();
    String name = eFeature.getName();
    CDOType type = getCDOType(eFeature);
    boolean many = EMFUtil.isMany(eFeature);
    Object defaultValue = eFeature.getDefaultValue();
    if (type == CDOType.CUSTOM)
    {
      try
      {
        defaultValue = EcoreUtil.convertToString((EDataType)eFeature.getEType(), defaultValue);
      }
      catch (RuntimeException ex)
      {
        if (defaultValue != null)
        {
          throw ex;
        }
      }
    }

    return CDOModelUtil.createAttribute(containingClass, featureID, name, type, defaultValue, many);
  }

  public static EPackage getEPackage(EPackage cdoPackage, CDOPackageRegistry packageRegistry)
  {
    EPackage ePackage = (EPackage)cdoPackage.getClientInfo();
    if (ePackage == null)
    {
      String uri = cdoPackage.getNsURI();
      ePackage = packageRegistry.getEPackage(uri);
      if (ePackage == null)
      {
        ePackage = createEPackage(cdoPackage);
        packageRegistry.put(uri, ePackage);
      }

      ((InternalEPackage)cdoPackage).setClientInfo(ePackage);
    }

    return ePackage;
  }

  public static EClass getEClass(EClass cdoClass, CDOPackageRegistry packageRegistry)
  {
    EClass eClass = (EClass)cdoClass.getClientInfo();
    if (eClass == null)
    {
      EPackage ePackage = getEPackage(cdoClass.getContainingPackage(), packageRegistry);
      eClass = (EClass)ePackage.getEClassifier(cdoClass.getName());
      ((InternalEClass)cdoClass).setClientInfo(eClass);
    }

    return eClass;
  }

  public static EStructuralFeature getEFeature(EStructuralFeature cdoFeature, CDOPackageRegistry packageRegistry)
  {
    EStructuralFeature eFeature = (EStructuralFeature)cdoFeature.getClientInfo();
    if (eFeature == null)
    {
      EClass eClass = getEClass(cdoFeature.getContainingClass(), packageRegistry);
      eFeature = eClass.getEStructuralFeature(cdoFeature.getFeatureID());
      ((InternalCDOFeature)cdoFeature).setClientInfo(eFeature);
    }

    return eFeature;
  }

  public static EPackage createEPackage(EPackage cdoPackage)
  {
    if (cdoPackage.isDynamic())
    {
      return createDynamicEPackage(cdoPackage);
    }

    EPackage ePackage = getGeneratedEPackage(cdoPackage);
    if (ePackage == null)
    {
      throw new CDOException("Generated package locally not available: " + cdoPackage.getNsURI());
    }

    return ePackage;
  }

  public static EPackage getGeneratedEPackage(EPackage cdoPackage)
  {
    String packageURI = cdoPackage.getNsURI();
    if (packageURI.equals(EcorePackage.eINSTANCE.getNsURI()))
    {
      return EcorePackage.eINSTANCE;
    }

    EPackage.Registry registry = EPackage.Registry.INSTANCE;
    return registry.getEPackage(packageURI);
  }

  public static EPackage createDynamicEPackage(EPackage cdoPackage)
  {
    EPackage topLevelPackage = cdoPackage.getTopLevelPackage();
    String ecore = topLevelPackage.getEcore();
    EPackageImpl topLevelPackageEPackage = (EPackageImpl)EMFUtil.ePackageFromString(ecore);
    EPackageImpl ePackage = CDOFactoryImpl.prepareDynamicEPackage(topLevelPackageEPackage, cdoPackage.getNsURI());
    return ePackage;
  }

  public static EPackageImpl prepareDynamicEPackage(EPackageImpl ePackage, String nsURI)
  {
    CDOFactoryImpl.prepareDynamicEPackage(ePackage);
    EPackageImpl result = ObjectUtil.equals(ePackage.getNsURI(), nsURI) ? ePackage : null;
    for (EPackage subPackage : ePackage.getESubpackages())
    {
      EPackageImpl p = prepareDynamicEPackage((EPackageImpl)subPackage, nsURI);
      if (p != null && result == null)
      {
        result = p;
      }
    }

    return result;
  }

  public static CDOClassifierRef createClassRef(EClassifier classifier)
  {
    if (classifier instanceof EClass)
    {
      String packageURI = classifier.getEPackage().getNsURI();
      int classifierID = classifier.getClassifierID();
      return CDOModelUtil.createClassRef(packageURI, classifierID);
    }

    return null;
  }

  public static void addModelInfos(_CDOSessionPackageManagerImpl packageManager)
  {
    // Ecore
    CDOCorePackage corePackage = packageManager.getCDOCorePackage();
    ((InternalEPackage)corePackage).setClientInfo(EcorePackage.eINSTANCE);
    ((InternalEClass)corePackage.getCDOObjectClass()).setClientInfo(EcorePackage.eINSTANCE.getEObject());

    // Eresource
    if (!ObjectUtil.equals(CDOResourcePackage.PACKAGE_URI, EresourcePackage.eNS_URI))
    {
      throw new ImplementationError();
    }

    CDOResourcePackage resourcePackage = packageManager.getCDOResourcePackage();
    ((InternalEPackage)resourcePackage).setClientInfo(EresourcePackage.eINSTANCE);

    CDOResourceNodeClass resourceNodeClass = resourcePackage.getCDOResourceNodeClass();
    ((InternalEClass)resourceNodeClass).setClientInfo(EresourcePackage.eINSTANCE.getCDOResourceNode());
    ((InternalCDOFeature)resourceNodeClass.getCDOFolderFeature()).setClientInfo(EresourcePackage.eINSTANCE
        .getCDOResourceNode_Folder());
    ((InternalCDOFeature)resourceNodeClass.getCDONameFeature()).setClientInfo(EresourcePackage.eINSTANCE
        .getCDOResourceNode_Name());

    CDOResourceFolderClass resourceFolderClass = resourcePackage.getCDOResourceFolderClass();
    ((InternalEClass)resourceFolderClass).setClientInfo(EresourcePackage.eINSTANCE.getCDOResourceFolder());
    ((InternalCDOFeature)resourceFolderClass.getCDONodesFeature()).setClientInfo(EresourcePackage.eINSTANCE
        .getCDOResourceFolder_Nodes());

    CDOResourceClass resourceClass = resourcePackage.getCDOResourceClass();
    ((InternalEClass)resourceClass).setClientInfo(EresourcePackage.eINSTANCE.getCDOResource());
    ((InternalCDOFeature)resourceClass.getCDOContentsFeature()).setClientInfo(EresourcePackage.eINSTANCE
        .getCDOResource_Contents());
  }
}
