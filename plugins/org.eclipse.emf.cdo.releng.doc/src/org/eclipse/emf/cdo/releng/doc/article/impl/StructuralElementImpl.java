/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.eclipse.emf.cdo.releng.doc.article.impl;

import org.eclipse.emf.cdo.releng.doc.article.ArticlePackage;
import org.eclipse.emf.cdo.releng.doc.article.Documentation;
import org.eclipse.emf.cdo.releng.doc.article.StructuralElement;
import org.eclipse.emf.cdo.releng.doc.article.util.HtmlWriter;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentWithInverseEList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.InternalEList;

import com.sun.javadoc.Doc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <!-- begin-user-doc --> An implementation of the model object '<em><b>Structural Element</b></em>'. <!-- end-user-doc
 * -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.emf.cdo.releng.doc.article.impl.StructuralElementImpl#getChildren <em>Children</em>}</li>
 * <li>{@link org.eclipse.emf.cdo.releng.doc.article.impl.StructuralElementImpl#getParent <em>Parent</em>}</li>
 * <li>{@link org.eclipse.emf.cdo.releng.doc.article.impl.StructuralElementImpl#getTitle <em>Title</em>}</li>
 * <li>{@link org.eclipse.emf.cdo.releng.doc.article.impl.StructuralElementImpl#getPath <em>Path</em>}</li>
 * <li>{@link org.eclipse.emf.cdo.releng.doc.article.impl.StructuralElementImpl#getOutputFile <em>Output File</em>}</li>
 * <li>{@link org.eclipse.emf.cdo.releng.doc.article.impl.StructuralElementImpl#getDocumentation <em>Documentation</em>}
 * </li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public abstract class StructuralElementImpl extends LinkTargetImpl implements StructuralElement
{
  /**
   * The cached value of the '{@link #getChildren() <em>Children</em>}' containment reference list. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   * 
   * @see #getChildren()
   * @generated
   * @ordered
   */
  protected EList<StructuralElement> children;

  /**
   * The default value of the '{@link #getTitle() <em>Title</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @see #getTitle()
   * @generated
   * @ordered
   */
  protected static final String TITLE_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getTitle() <em>Title</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @see #getTitle()
   * @generated
   * @ordered
   */
  protected String title = TITLE_EDEFAULT;

  /**
   * The default value of the '{@link #getPath() <em>Path</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @see #getPath()
   * @generated
   * @ordered
   */
  protected static final String PATH_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getPath() <em>Path</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getPath()
   * @generated
   * @ordered
   */
  protected String path = PATH_EDEFAULT;

  /**
   * The default value of the '{@link #getFullPath() <em>Full Path</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getFullPath()
   * @generated
   * @ordered
   */
  protected static final String FULL_PATH_EDEFAULT = null;

  /**
   * The default value of the '{@link #getOutputFile() <em>Output File</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getOutputFile()
   * @generated
   * @ordered
   */
  protected static final File OUTPUT_FILE_EDEFAULT = null;

  /**
   * The default value of the '{@link #getDoc() <em>Doc</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getDoc()
   * @generated
   * @ordered
   */
  protected static final Doc DOC_EDEFAULT = null;

  private String fullPath;

  private File outputFile;

  private Doc doc;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected StructuralElementImpl()
  {
    super();
  }

  StructuralElementImpl(StructuralElement parent, String path, Doc doc)
  {
    setParent(parent);
    this.path = path;
    this.doc = doc;
  }

  final void setTitle(String title)
  {
    this.title = title;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass()
  {
    return ArticlePackage.Literals.STRUCTURAL_ELEMENT;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public EList<StructuralElement> getChildren()
  {
    if (children == null)
    {
      children = new EObjectContainmentWithInverseEList<StructuralElement>(StructuralElement.class, this,
          ArticlePackage.STRUCTURAL_ELEMENT__CHILDREN, ArticlePackage.STRUCTURAL_ELEMENT__PARENT);
    }
    return children;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public StructuralElement getParent()
  {
    if (eContainerFeatureID() != ArticlePackage.STRUCTURAL_ELEMENT__PARENT)
      return null;
    return (StructuralElement)eContainer();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public NotificationChain basicSetParent(StructuralElement newParent, NotificationChain msgs)
  {
    msgs = eBasicSetContainer((InternalEObject)newParent, ArticlePackage.STRUCTURAL_ELEMENT__PARENT, msgs);
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setParent(StructuralElement newParent)
  {
    if (newParent != eInternalContainer()
        || (eContainerFeatureID() != ArticlePackage.STRUCTURAL_ELEMENT__PARENT && newParent != null))
    {
      if (EcoreUtil.isAncestor(this, newParent))
        throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
      NotificationChain msgs = null;
      if (eInternalContainer() != null)
        msgs = eBasicRemoveFromContainer(msgs);
      if (newParent != null)
        msgs = ((InternalEObject)newParent).eInverseAdd(this, ArticlePackage.STRUCTURAL_ELEMENT__CHILDREN,
            StructuralElement.class, msgs);
      msgs = basicSetParent(newParent, msgs);
      if (msgs != null)
        msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, ArticlePackage.STRUCTURAL_ELEMENT__PARENT, newParent,
          newParent));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getTitle()
  {
    return title;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getPath()
  {
    return path;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated NOT
   */
  public String getFullPath()
  {
    if (fullPath == null)
    {
      StructuralElement parent = getParent();
      if (parent != null)
      {
        fullPath = parent.getFullPath() + "/" + path;
      }
      else
      {
        fullPath = path;
      }
    }

    return fullPath;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated NOT
   */
  public File getOutputFile()
  {
    if (outputFile == null)
    {
      outputFile = new File(getDocumentation().getContext().getBaseFolder(), getFullPath());
    }

    return outputFile;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated NOT
   */
  public Documentation getDocumentation()
  {
    if (this instanceof Documentation)
    {
      return (Documentation)this;
    }

    StructuralElement parent = getParent();
    if (parent == null)
    {
      System.out.println();
    }

    return parent.getDocumentation();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated NOT
   */
  public Doc getDoc()
  {
    return doc;
  }

  @Override
  public String linkFrom(StructuralElement source)
  {
    List<String> sourceSegments = getSegments(source.getOutputFile());
    List<String> targetSegments = getSegments(getOutputFile());

    int minSize = Math.min(sourceSegments.size(), targetSegments.size());
    for (int i = 0; i < minSize; i++)
    {
      if (sourceSegments.get(0).equals(targetSegments.get(0)))
      {
        sourceSegments.remove(0);
        targetSegments.remove(0);
      }
      else
      {
        break;
      }
    }

    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < sourceSegments.size(); i++)
    {
      builder.append("../");
    }

    boolean first = true;
    for (String segment : targetSegments)
    {
      if (first)
      {
        first = false;
      }
      else
      {
        builder.append("/");
      }

      builder.append(segment);
    }

    return builder.toString();
  }

  private static List<String> getSegments(File file)
  {
    List<String> result = new ArrayList<String>();
    getSegments(file, result);
    return result;
  }

  private static void getSegments(File file, List<String> result)
  {
    File parent = file.getParentFile();
    if (parent != null)
    {
      getSegments(parent, result);
    }

    result.add(file.getName());
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @SuppressWarnings("unchecked")
  @Override
  public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs)
  {
    switch (featureID)
    {
    case ArticlePackage.STRUCTURAL_ELEMENT__CHILDREN:
      return ((InternalEList<InternalEObject>)(InternalEList<?>)getChildren()).basicAdd(otherEnd, msgs);
    case ArticlePackage.STRUCTURAL_ELEMENT__PARENT:
      if (eInternalContainer() != null)
        msgs = eBasicRemoveFromContainer(msgs);
      return basicSetParent((StructuralElement)otherEnd, msgs);
    }
    return super.eInverseAdd(otherEnd, featureID, msgs);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs)
  {
    switch (featureID)
    {
    case ArticlePackage.STRUCTURAL_ELEMENT__CHILDREN:
      return ((InternalEList<?>)getChildren()).basicRemove(otherEnd, msgs);
    case ArticlePackage.STRUCTURAL_ELEMENT__PARENT:
      return basicSetParent(null, msgs);
    }
    return super.eInverseRemove(otherEnd, featureID, msgs);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public NotificationChain eBasicRemoveFromContainerFeature(NotificationChain msgs)
  {
    switch (eContainerFeatureID())
    {
    case ArticlePackage.STRUCTURAL_ELEMENT__PARENT:
      return eInternalContainer().eInverseRemove(this, ArticlePackage.STRUCTURAL_ELEMENT__CHILDREN,
          StructuralElement.class, msgs);
    }
    return super.eBasicRemoveFromContainerFeature(msgs);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType)
  {
    switch (featureID)
    {
    case ArticlePackage.STRUCTURAL_ELEMENT__CHILDREN:
      return getChildren();
    case ArticlePackage.STRUCTURAL_ELEMENT__PARENT:
      return getParent();
    case ArticlePackage.STRUCTURAL_ELEMENT__TITLE:
      return getTitle();
    case ArticlePackage.STRUCTURAL_ELEMENT__PATH:
      return getPath();
    case ArticlePackage.STRUCTURAL_ELEMENT__FULL_PATH:
      return getFullPath();
    case ArticlePackage.STRUCTURAL_ELEMENT__OUTPUT_FILE:
      return getOutputFile();
    case ArticlePackage.STRUCTURAL_ELEMENT__DOCUMENTATION:
      return getDocumentation();
    case ArticlePackage.STRUCTURAL_ELEMENT__DOC:
      return getDoc();
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @SuppressWarnings("unchecked")
  @Override
  public void eSet(int featureID, Object newValue)
  {
    switch (featureID)
    {
    case ArticlePackage.STRUCTURAL_ELEMENT__CHILDREN:
      getChildren().clear();
      getChildren().addAll((Collection<? extends StructuralElement>)newValue);
      return;
    case ArticlePackage.STRUCTURAL_ELEMENT__PARENT:
      setParent((StructuralElement)newValue);
      return;
    }
    super.eSet(featureID, newValue);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public void eUnset(int featureID)
  {
    switch (featureID)
    {
    case ArticlePackage.STRUCTURAL_ELEMENT__CHILDREN:
      getChildren().clear();
      return;
    case ArticlePackage.STRUCTURAL_ELEMENT__PARENT:
      setParent((StructuralElement)null);
      return;
    }
    super.eUnset(featureID);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public boolean eIsSet(int featureID)
  {
    switch (featureID)
    {
    case ArticlePackage.STRUCTURAL_ELEMENT__CHILDREN:
      return children != null && !children.isEmpty();
    case ArticlePackage.STRUCTURAL_ELEMENT__PARENT:
      return getParent() != null;
    case ArticlePackage.STRUCTURAL_ELEMENT__TITLE:
      return TITLE_EDEFAULT == null ? title != null : !TITLE_EDEFAULT.equals(title);
    case ArticlePackage.STRUCTURAL_ELEMENT__PATH:
      return PATH_EDEFAULT == null ? path != null : !PATH_EDEFAULT.equals(path);
    case ArticlePackage.STRUCTURAL_ELEMENT__FULL_PATH:
      return FULL_PATH_EDEFAULT == null ? getFullPath() != null : !FULL_PATH_EDEFAULT.equals(getFullPath());
    case ArticlePackage.STRUCTURAL_ELEMENT__OUTPUT_FILE:
      return OUTPUT_FILE_EDEFAULT == null ? getOutputFile() != null : !OUTPUT_FILE_EDEFAULT.equals(getOutputFile());
    case ArticlePackage.STRUCTURAL_ELEMENT__DOCUMENTATION:
      return getDocumentation() != null;
    case ArticlePackage.STRUCTURAL_ELEMENT__DOC:
      return DOC_EDEFAULT == null ? getDoc() != null : !DOC_EDEFAULT.equals(getDoc());
    }
    return super.eIsSet(featureID);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public String toString()
  {
    if (eIsProxy())
      return super.toString();

    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (title: ");
    result.append(title);
    result.append(", path: ");
    result.append(path);
    result.append(')');
    return result.toString();
  }

  @Override
  public String getLabel()
  {
    return getTitle();
  }

  public void generate() throws IOException
  {
    for (StructuralElement child : getChildren())
    {
      child.generate();
    }
  }

  public void generate(HtmlWriter out) throws IOException
  {
    for (StructuralElement child : getChildren())
    {
      child.generate(out);
    }
  }
} // StructuralElementImpl