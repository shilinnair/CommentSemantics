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
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/*
 * The page to configure the naming style options.
 */
public class ImportOrganizePreferencePage extends PropertyAndPreferencePage {

    //$NON-NLS-1$
    public static final String PREF_ID = "org.eclipse.jdt.ui.preferences.ImportOrganizePreferencePage";

    //$NON-NLS-1$
    public static final String PROP_ID = "org.eclipse.jdt.ui.propertyPages.ImportOrganizePreferencePage";

    private ImportOrganizeConfigurationBlock fConfigurationBlock;

    public  ImportOrganizePreferencePage() {
        setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
        //setDescription(PreferencesMessages.ImportOrganizePreferencePage_description);
        // only used when page is shown programatically
        setTitle(PreferencesMessages.ImportOrganizePreferencePage_title);
    }

    /*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
    @Override
    public void createControl(Composite parent) {
        IWorkbenchPreferenceContainer container = (IWorkbenchPreferenceContainer) getContainer();
        fConfigurationBlock = new ImportOrganizeConfigurationBlock(getNewStatusChangedListener(), getProject(), container);
        super.createControl(parent);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IJavaHelpContextIds.ORGANIZE_IMPORTS_PREFERENCE_PAGE);
    }

    @Override
    protected Control createPreferenceContent(Composite composite) {
        return fConfigurationBlock.createContents(composite);
    }

    @Override
    protected boolean hasProjectSpecificOptions(IProject project) {
        return fConfigurationBlock.hasProjectSpecificOptions(project);
    }

    @Override
    protected String getPreferencePageID() {
        return PREF_ID;
    }

    @Override
    protected String getPropertyPageID() {
        return PROP_ID;
    }

    @Override
    public void dispose() {
        if (fConfigurationBlock != null) {
            fConfigurationBlock.dispose();
        }
        super.dispose();
    }

    @Override
    protected void enableProjectSpecificSettings(boolean useProjectSpecificSettings) {
        super.enableProjectSpecificSettings(useProjectSpecificSettings);
        if (fConfigurationBlock != null) {
            fConfigurationBlock.useProjectSpecificSettings(useProjectSpecificSettings);
        }
    }

    /*
	 * @see org.eclipse.jface.preference.IPreferencePage#performDefaults()
	 */
    @Override
    protected void performDefaults() {
        super.performDefaults();
        if (fConfigurationBlock != null) {
            fConfigurationBlock.performDefaults();
        }
    }

    /*
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
    @Override
    public boolean performOk() {
        if (fConfigurationBlock != null && !fConfigurationBlock.performOk()) {
            return false;
        }
        return super.performOk();
    }

    /*
	 * @see org.eclipse.jface.preference.IPreferencePage#performApply()
	 */
    @Override
    public void performApply() {
        if (fConfigurationBlock != null) {
            fConfigurationBlock.performApply();
        }
    }

    @Override
    public void setElement(IAdaptable element) {
        super.setElement(element);
        // no description for property page
        setDescription(null);
    }
}
