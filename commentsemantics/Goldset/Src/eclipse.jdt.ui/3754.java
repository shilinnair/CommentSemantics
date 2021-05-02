/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.examples;

import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;

public class AddJavaNatureAction extends Action implements IActionDelegate {

    private IProject fProject;

    @Override
    public void run(IAction action) {
        try {
            if (fProject != null) {
                BuildPathsBlock.addJavaNature(fProject, null);
            }
        } catch (Exception e) {
            JavaTestPlugin.log(e);
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        fProject = null;
        if (selection instanceof IStructuredSelection) {
            Object object = ((IStructuredSelection) selection).getFirstElement();
            if (object instanceof IProject) {
                fProject = (IProject) object;
            }
        }
    }
}
