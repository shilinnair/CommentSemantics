/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.wizards.NewEnumWizardPage;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class NewEnumCreationWizard extends NewElementWizard {

    private NewEnumWizardPage fPage;

    private boolean fOpenEditorOnFinish;

    public  NewEnumCreationWizard() {
        this(null, true);
    }

    public  NewEnumCreationWizard(NewEnumWizardPage page, boolean openEditorOnFinish) {
        setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWENUM);
        setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
        setWindowTitle(NewWizardMessages.NewEnumCreationWizard_title);
        fPage = page;
        fOpenEditorOnFinish = openEditorOnFinish;
    }

    /*
	 * @see Wizard#addPages
	 */
    @Override
    public void addPages() {
        super.addPages();
        if (fPage == null) {
            fPage = new NewEnumWizardPage();
            fPage.init(getSelection());
        }
        addPage(fPage);
    }

    @Override
    protected boolean canRunForked() {
        return !fPage.isEnclosingTypeSelected();
    }

    @Override
    protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
        // use the full progress monitor
        fPage.createType(monitor);
    }

    @Override
    public boolean performFinish() {
        warnAboutTypeCommentDeprecation();
        boolean res = super.performFinish();
        if (res) {
            IResource resource = fPage.getModifiedResource();
            if (resource != null) {
                selectAndReveal(resource);
                if (fOpenEditorOnFinish) {
                    openResource((IFile) resource);
                }
            }
        }
        return res;
    }

    @Override
    public IJavaElement getCreatedElement() {
        return fPage.getCreatedType();
    }
}
