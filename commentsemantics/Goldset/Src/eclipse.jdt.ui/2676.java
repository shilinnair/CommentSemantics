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
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.jdt.core.IClassFile;

/**
 * Editor input for class files.
 */
public interface IClassFileEditorInput extends IEditorInput {

    /**
	 * Returns the class file acting as input.
	 * 
	 * @return the class file
	 */
    public IClassFile getClassFile();
}
