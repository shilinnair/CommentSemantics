/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.elements;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class TreeContentProvider extends ListContentProvider implements ITreeContentProvider {

    public  TreeContentProvider() {
        super();
    }

    @Override
    public void dispose() {
    }

    @Override
    public Object[] getChildren(Object element) {
        if (element instanceof IPDEElement) {
            return ((IPDEElement) element).getChildren();
        }
        return null;
    }

    @Override
    public Object[] getElements(Object element) {
        if (element instanceof IPDEElement) {
            return ((IPDEElement) element).getChildren();
        }
        return null;
    }

    @Override
    public Object getParent(Object element) {
        if (element instanceof IPDEElement) {
            return ((IPDEElement) element).getParent();
        }
        return null;
    }

    @Override
    public boolean hasChildren(java.lang.Object element) {
        if (element instanceof IPDEElement) {
            Object[] children = ((IPDEElement) element).getChildren();
            return children != null && children.length > 0;
        }
        return false;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public boolean isDeleted(Object element) {
        return false;
    }
}
