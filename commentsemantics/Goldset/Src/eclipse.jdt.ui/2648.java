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
package org.eclipse.jdt.internal.ui.workingsets;

import org.eclipse.jface.action.IMenuManager;

public interface IWorkingSetActionGroup {

    //$NON-NLS-1$
    public static final String ACTION_GROUP = "working_set_action_group";

    public void fillViewMenu(IMenuManager mm);

    public void cleanViewMenu(IMenuManager menuManager);
}
