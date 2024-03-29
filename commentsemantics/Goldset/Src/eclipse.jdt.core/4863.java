/*******************************************************************************
 * Copyright (c) 2005, 2007 BEA Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   wharley@bea.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.apt.ui.internal.preferences;

import org.eclipse.jdt.apt.ui.internal.AptUIPlugin;
import org.eclipse.jdt.apt.ui.internal.util.IAptHelpContextIds;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * 
 */
public class FactoryPathPreferencePage extends BasePreferencePage {

    //$NON-NLS-1$
    private static final String PREF_ID = "org.eclipse.jdt.apt.ui.preferences.factoryPathPreferences";

    //$NON-NLS-1$
    private static final String PROP_ID = "org.eclipse.jdt.apt.ui.propertyPages.factoryPathPreferences";

    /**
	 * 
	 */
    public  FactoryPathPreferencePage() {
        setPreferenceStore(AptUIPlugin.getDefault().getPreferenceStore());
        //setDescription(Messages.FactoryPathPreferencePage_factoryPath);
        // only used when page is shown programatically
        setTitle(Messages.FactoryPathPreferencePage_preferences);
    }

    /*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
    public void createControl(Composite parent) {
        IWorkbenchPreferenceContainer container = (IWorkbenchPreferenceContainer) getContainer();
        setConfigurationBlock(new FactoryPathConfigurationBlock(getNewStatusChangedListener(), getProject(), container));
        super.createControl(parent);
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage#getPreferencePageID()
	 */
    protected String getPreferencePageID() {
        return PREF_ID;
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage#getPropertyPageID()
	 */
    protected String getPropertyPageID() {
        return PROP_ID;
    }

    @Override
    protected String getContextHelpId() {
        return IAptHelpContextIds.FACTORYPATH_PREFERENCE_PAGE;
    }
}
