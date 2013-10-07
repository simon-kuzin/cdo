/*
 * Copyright (c) 2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Christian W. Damus (CEA LIST) - initial API and implementation
 */
package org.eclipse.emf.cdo.internal.security.ui.editor;

import org.eclipse.emf.cdo.internal.security.ui.messages.Messages;
import org.eclipse.emf.cdo.security.SecurityPackage;
import org.eclipse.emf.cdo.security.User;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.edit.domain.EditingDomain;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * 
 */
public class UserDetailsPage extends AbstractDetailsPage<User>
{

  public UserDetailsPage(EditingDomain domain, AdapterFactory adapterFactory)
  {
    super(User.class, SecurityPackage.Literals.USER, domain, adapterFactory);
  }

  @Override
  protected void createContents(Composite parent, FormToolkit toolkit)
  {
    super.createContents(parent, toolkit);

    text(parent, toolkit, Messages.UserDetailsPage_0, SecurityPackage.Literals.ASSIGNEE__ID);

    text(parent, toolkit, Messages.UserDetailsPage_1, SecurityPackage.Literals.USER__FIRST_NAME);
    text(parent, toolkit, Messages.UserDetailsPage_2, SecurityPackage.Literals.USER__LAST_NAME);
    text(parent, toolkit, Messages.UserDetailsPage_3, SecurityPackage.Literals.USER__LABEL);
    text(parent, toolkit, Messages.UserDetailsPage_4, SecurityPackage.Literals.USER__EMAIL);

    space(parent, toolkit);

    checkbox(parent, toolkit, Messages.UserDetailsPage_5, SecurityPackage.Literals.USER__LOCKED);

    space(parent, toolkit);

    combo(parent, toolkit, Messages.UserDetailsPage_6, SecurityPackage.Literals.USER__DEFAULT_ACCESS_OVERRIDE);

    space(parent, toolkit);

    oneToMany(parent, toolkit, Messages.UserDetailsPage_7, SecurityPackage.Literals.USER__GROUPS);

    oneToMany(parent, toolkit, Messages.UserDetailsPage_8, SecurityPackage.Literals.ASSIGNEE__ROLES);
  }

}
