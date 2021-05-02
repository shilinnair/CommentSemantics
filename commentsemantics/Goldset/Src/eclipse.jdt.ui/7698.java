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
import org.eclipse.ui.PlatformUI;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.preferences.formatter.CodeFormatterConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileConfigurationBlock;

/*
 * The page to configure the code formatter options.
 */
public class CodeFormatterPreferencePage extends ProfilePreferencePage {

    //$NON-NLS-1$
    public static final String PREF_ID = "org.eclipse.jdt.ui.preferences.CodeFormatterPreferencePage";

    //$NON-NLS-1$
    public static final String PROP_ID = "org.eclipse.jdt.ui.propertyPages.CodeFormatterPreferencePage";

    public  CodeFormatterPreferencePage() {
        // only used when page is shown programatically
        setTitle(PreferencesMessages.CodeFormatterPreferencePage_title);
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.CODEFORMATTER_PREFERENCE_PAGE);
    }

    @Override
    protected ProfileConfigurationBlock createConfigurationBlock(PreferencesAccess access) {
        return new CodeFormatterConfigurationBlock(getProject(), access);
    }

    @Override
    protected String getPreferencePageID() {
        return PREF_ID;
    }

    @Override
    protected String getPropertyPageID() {
        return PROP_ID;
    }
}
