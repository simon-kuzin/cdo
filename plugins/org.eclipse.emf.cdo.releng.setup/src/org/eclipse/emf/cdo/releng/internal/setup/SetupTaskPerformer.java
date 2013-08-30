/*
 * Copyright (c) 2004-2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.releng.internal.setup;

import org.eclipse.emf.cdo.releng.setup.Branch;
import org.eclipse.emf.cdo.releng.setup.InstallTask;
import org.eclipse.emf.cdo.releng.setup.Project;
import org.eclipse.emf.cdo.releng.setup.Setup;
import org.eclipse.emf.cdo.releng.setup.SetupTask;
import org.eclipse.emf.cdo.releng.setup.SetupTaskContext;
import org.eclipse.emf.cdo.releng.setup.util.OS;
import org.eclipse.emf.cdo.releng.setup.util.log.ProgressLog;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Eike Stepper
 */
public class SetupTaskPerformer extends HashMap<Object, Object> implements SetupTaskContext
{
  private static final long serialVersionUID = 1L;

  private static ProgressLog progress;

  private boolean onlyInstall;

  private File branchDir;

  private Setup setup;

  private transient boolean restartNeeded;

  public SetupTaskPerformer(boolean onlyInstall) throws Exception
  {
    this.onlyInstall = onlyInstall;

    ResourceSet resourceSet = new ResourceSetImpl();
    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());

    IPath branchDirPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().removeLastSegments(1);
    branchDir = new File(branchDirPath.toOSString()).getCanonicalFile();

    URI uri = URI.createFileURI(branchDirPath.append("setup.xmi").toOSString());
    Resource resource = resourceSet.getResource(uri, true);

    setup = (Setup)resource.getContents().get(0);
  }

  public void dispose()
  {
    setup = null;
    branchDir = null;
  }

  public void log(String line)
  {
    if (progress != null)
    {
      progress.log(line);
    }
    else
    {
      System.out.println(line);
    }
  }

  public boolean isCancelled()
  {
    if (progress != null)
    {
      return progress.isCancelled();
    }

    return false;
  }

  public boolean isRestartNeeded()
  {
    return restartNeeded;
  }

  public void setRestartNeeded()
  {
    restartNeeded = true;
  }

  public String expandString(String string)
  {
    Branch branch = getSetup().getBranch();
    String branchName = branch.getName();

    Project project = branch.getProject();
    String projectName = project.getName();

    StringBuilder builder = new StringBuilder(string);
    expandStringAll(builder, "${setup.git.prefix}", getSetup().getPreferences().getGitPrefix());
    expandStringAll(builder, "${setup.install.dir}", getPath(getInstallDir()));
    expandStringAll(builder, "${setup.project.dir}", getPath(getProjectDir()));
    expandStringAll(builder, "${setup.branch.dir}", getPath(getBranchDir()));
    expandStringAll(builder, "${setup.project.name}", projectName);
    expandStringAll(builder, "${setup.project.name.upper}", projectName.toUpperCase());
    expandStringAll(builder, "${setup.project.name.lower}", projectName.toLowerCase());
    expandStringAll(builder, "${setup.branch.name}", branchName);
    expandStringAll(builder, "${setup.branch.name.upper}", branchName.toUpperCase());
    expandStringAll(builder, "${setup.branch.name.lower}", branchName.toLowerCase());
    return builder.toString();
  }

  private String getPath(File installDir)
  {
    return installDir.toString().replace('\\', '/');
  }

  private void expandStringAll(StringBuilder builder, String search, String replace)
  {
    do
    {
    } while (expandString(builder, search, replace));
  }

  private boolean expandString(StringBuilder builder, String search, String replace)
  {
    int pos = builder.indexOf(search);
    if (pos == -1)
    {
      builder.replace(pos, pos + replace.length(), replace);
      return false;
    }

    return true;
  }

  public OS getOS()
  {
    return OS.INSTANCE;
  }

  public String getP2ProfileName()
  {
    Branch branch = setup.getBranch();
    Project project = branch.getProject();

    String profileName = project.getName() + "_" + branch.getName();
    profileName = profileName.replace('.', '_');
    profileName = profileName.replace('-', '_');
    profileName = profileName.replace('/', '_');
    profileName = profileName.replace('\\', '_');
    return profileName;
  }

  public File getP2ProfileDir()
  {
    return new File(getP2AgentDir(), "org.eclipse.equinox.p2.engine/profileRegistry/" + getP2ProfileName() + ".profile");
  }

  public File getP2AgentDir()
  {
    return new File(getP2PoolDir(), "p2");
  }

  public File getP2PoolDir()
  {
    return new File(getInstallDir(), ".p2pool-ide");
  }

  public File getInstallDir()
  {
    return getProjectDir().getParentFile();
  }

  public File getProjectDir()
  {
    return branchDir.getParentFile();
  }

  public File getBranchDir()
  {
    return branchDir;
  }

  public File getEclipseDir()
  {
    return new File(branchDir, "eclipse");
  }

  // TODO Is this Bucky-specific?
  public File getTargetPlatformDir()
  {
    return new File(branchDir, "tp");
  }

  public File getWorkspaceDir()
  {
    return new File(branchDir, "ws");
  }

  public Setup getSetup()
  {
    return setup;
  }

  public void perform() throws Exception
  {
    EList<SetupTask> setupTasks = setup.getSetupTasks(true, onlyInstall);
    if (setupTasks.isEmpty())
    {
      return;
    }

    Map<SetupTask, SetupTask> substitutions = getSubstitutions(setupTasks);
    setup = copySetup(setupTasks, substitutions);

    perform(setupTasks);

    for (SetupTask setupTask : setupTasks)
    {
      setupTask.dispose();
    }
  }

  private void perform(EList<SetupTask> setupTasks) throws Exception
  {
    EList<SetupTask> neededTasks = getNeededTasks(setupTasks);
    if (neededTasks.isEmpty())
    {
      return;
    }

    int xxx;
    // TODO Execute this loop in ProgressLogDialog
    Branch branch = setup.getBranch();
    log("Setting up " + branch.getProject().getName() + " " + branch.getName());

    for (SetupTask neededTask : neededTasks)
    {
      neededTask.perform(this);
    }
  }

  private Map<SetupTask, SetupTask> getSubstitutions(EList<SetupTask> setupTasks)
  {
    SetupTaskComparator.sort(setupTasks);

    Map<Object, SetupTask> overrides = new HashMap<Object, SetupTask>();
    Map<SetupTask, SetupTask> substitutions = new HashMap<SetupTask, SetupTask>();

    for (SetupTask setupTask : setupTasks)
    {
      Object overrideToken = setupTask.getOverrideToken();
      SetupTask overriddenTask = overrides.put(overrideToken, setupTask);
      if (overriddenTask != null)
      {
        substitutions.put(overriddenTask, setupTask);
      }
    }

    // Shorten the paths through the substitutions map
    for (Map.Entry<SetupTask, SetupTask> entry : substitutions.entrySet())
    {
      SetupTask task = entry.getValue();

      for (;;)
      {
        SetupTask overridingTask = substitutions.get(task);
        if (overridingTask == null)
        {
          break;
        }

        entry.setValue(overridingTask);
      }
    }

    return substitutions;
  }

  private Setup copySetup(EList<SetupTask> setupTasks, Map<SetupTask, SetupTask> substitutions)
  {
    Set<EObject> roots = new HashSet<EObject>();
    roots.add(setup);

    for (EObject eObject : setup.eCrossReferences())
    {
      EObject rootContainer = EcoreUtil.getRootContainer(eObject);
      roots.add(rootContainer);
    }

    EcoreUtil.Copier copier = new EcoreUtil.Copier();
    Setup setup = (Setup)copier.copyAll(roots);

    for (Map.Entry<SetupTask, SetupTask> entry : substitutions.entrySet())
    {
      SetupTask overriddenTask = entry.getKey();
      SetupTask overridingTask = entry.getValue();

      EObject copy = copier.get(overridingTask);
      copier.put(overriddenTask, copy);
    }

    copier.copyReferences();

    for (ListIterator<SetupTask> it = setupTasks.listIterator(); it.hasNext();)
    {
      SetupTask setupTask = it.next();
      if (substitutions.containsKey(setupTask))
      {
        it.remove();
      }
      else
      {
        SetupTask copy = (SetupTask)copier.get(setupTask);
        it.set(copy);
      }
    }

    SetupTaskComparator.sort(setupTasks);
    return setup;
  }

  private EList<SetupTask> getNeededTasks(EList<SetupTask> setupTasks) throws Exception
  {
    EList<SetupTask> result = new BasicEList<SetupTask>();

    for (Iterator<SetupTask> it = setupTasks.iterator(); it.hasNext();)
    {
      SetupTask setupTask = it.next();
      if (setupTask.isNeeded(this))
      {
        result.add(setupTask);
      }
    }

    return result;
  }

  public static ProgressLog getProgress()
  {
    return progress;
  }

  public static void setProgress(ProgressLog progress)
  {
    SetupTaskPerformer.progress = progress;
  }

  /**
   * @author Eike Stepper
   */
  public static class SetupTaskComparator implements Comparator<SetupTask>
  {
    public static void sort(EList<SetupTask> setupTasks)
    {
      Collections.sort(setupTasks, new SetupTaskComparator());
    }

    public int compare(SetupTask t1, SetupTask t2)
    {
      boolean install1 = t1 instanceof InstallTask;
      boolean install2 = t2 instanceof InstallTask;
      if (install1 && !install2)
      {
        return -1;
      }

      if (!install1 && install2)
      {
        return 1;
      }

      boolean t1RequiresT2 = t1.requires(t2);
      boolean t2RequiresT1 = t2.requires(t1);
      if (t1RequiresT2 && t2RequiresT1)
      {
        throw new IllegalStateException("Requirements cycle detected between " + t1 + " and " + t2);
      }

      if (t1RequiresT2 && !t2RequiresT1)
      {
        return 1;
      }

      if (!t1RequiresT2 && t2RequiresT1)
      {
        return -1;
      }

      int scope1 = t1.getScope().getValue();
      int scope2 = t2.getScope().getValue();
      if (scope1 < scope2)
      {
        return -1;
      }

      if (scope1 > scope2)
      {
        return 1;
      }

      String uri1 = EcoreUtil.getURI(t1).toString();
      String uri2 = EcoreUtil.getURI(t2).toString();
      return uri1.compareTo(uri2); // Arbitrary but symmetric within one ResourceSet
    }
  }
}
