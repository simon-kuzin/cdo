/*
 * Copyright (c) 2004 - 2012 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.releng.internal.version;

import org.eclipse.emf.cdo.releng.version.IBuildState;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Eike Stepper
 */
public class BuildState implements IBuildState, Serializable
{
  private static final long serialVersionUID = 1L;

  private byte[] releaseSpecDigest;

  private long propertiesTimeStamp;

  private boolean deviations;

  private boolean integration;

  private Set<String> rootProjects;

  private boolean changedSinceRelease;

  private long validatorTimeStamp;

  private Serializable validatorState;

  private Map<String, String> arguments;

  BuildState()
  {
  }

  public byte[] getReleaseSpecDigest()
  {
    return releaseSpecDigest;
  }

  public void setReleaseSpecDigest(byte[] releaseSpecDigest)
  {
    this.releaseSpecDigest = releaseSpecDigest;
  }

  public long getPropertiesTimeStamp()
  {
    return propertiesTimeStamp;
  }

  public void setPropertiesTimeStamp(long propertiesTimeStamp)
  {
    this.propertiesTimeStamp = propertiesTimeStamp;
  }

  public boolean isDeviations()
  {
    return deviations;
  }

  public void setDeviations(boolean deviations)
  {
    this.deviations = deviations;
  }

  public boolean isIntegration()
  {
    return integration;
  }

  public void setIntegration(boolean integration)
  {
    this.integration = integration;
  }

  public Set<String> getRootProjects()
  {
    return rootProjects;
  }

  public void setRootProjects(Set<String> rootProjects)
  {
    this.rootProjects = rootProjects;
  }

  public long getValidatorTimeStamp()
  {
    return validatorTimeStamp;
  }

  public void setValidatorTimeStamp(long validatorTimeStamp)
  {
    this.validatorTimeStamp = validatorTimeStamp;
  }

  public boolean isChangedSinceRelease()
  {
    return changedSinceRelease;
  }

  public void setChangedSinceRelease(boolean changedSinceRelease)
  {
    this.changedSinceRelease = changedSinceRelease;
  }

  public Serializable getValidatorState()
  {
    return validatorState;
  }

  public void setValidatorState(Serializable validatorState)
  {
    this.validatorState = validatorState;
  }

  public Map<String, String> getArguments()
  {
    return arguments == null ? new HashMap<String, String>() : arguments;
  }

  public void setArguments(Map<String, String> arguments)
  {
    this.arguments = arguments;
  }
}
