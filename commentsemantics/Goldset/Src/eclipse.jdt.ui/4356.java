/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.filters;

import org.eclipse.osgi.util.NLS;

public final class FilterMessages extends NLS {

    //$NON-NLS-1$
    private static final String BUNDLE_NAME = "org.eclipse.jdt.internal.ui.filters.FilterMessages";

    private  FilterMessages() {
    // Do not instantiate
    }

    public static String CustomFiltersDialog_title;

    public static String CustomFiltersDialog_patternInfo;

    public static String CustomFiltersDialog_enableUserDefinedPattern;

    public static String CustomFiltersDialog_filterList_label;

    public static String CustomFiltersDialog_description_label;

    public static String CustomFiltersDialog_SelectAllButton_label;

    public static String CustomFiltersDialog_DeselectAllButton_label;

    public static String CustomFiltersDialog_name_filter_pattern_description;

    public static String OpenCustomFiltersDialogAction_text;

    public static String FilterDescriptor_filterDescriptionCreationError_message;

    public static String FilterDescriptor_filterCreationError_message;

    static {
        NLS.initializeMessages(BUNDLE_NAME, FilterMessages.class);
    }
}
