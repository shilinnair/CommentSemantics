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
package org.eclipse.jdt.internal.debug.ui.actions;

/**
 * Opens the declaring type hierarchy of a stack frame.
 */
public class OpenDeclaringTypeHierarchyAction extends OpenDeclaringTypeAction {

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.ui.actions.OpenTypeAction#isHierarchy()
	 */
    @Override
    protected boolean isHierarchy() {
        return true;
    }
}
