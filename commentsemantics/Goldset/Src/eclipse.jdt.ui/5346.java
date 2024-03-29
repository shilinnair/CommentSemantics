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
package org.eclipse.ltk.core.refactoring.tests;

import org.eclipse.core.runtime.Plugin;

public class RefactoringCoreTestPlugin extends Plugin {

    private static RefactoringCoreTestPlugin fgDefault;

    public  RefactoringCoreTestPlugin() {
        fgDefault = this;
    }

    public static RefactoringCoreTestPlugin getDefault() {
        return fgDefault;
    }

    public static String getPluginId() {
        //$NON-NLS-1$
        return "org.eclipse.ltk.core.refactoring.tests";
    }
}
