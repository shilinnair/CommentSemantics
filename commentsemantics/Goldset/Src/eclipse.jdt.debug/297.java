/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.sun.jdi;

/**
 * See http://docs.oracle.com/javase/6/docs/jdk/api/jpda/jdi/com/sun/jdi/VMCannotBeModifiedException.html
 */
public class VMCannotBeModifiedException extends UnsupportedOperationException {

    /**
	 * All serializable objects should have a stable serialVersionUID
	 */
    private static final long serialVersionUID = 1L;

    public  VMCannotBeModifiedException() {
    }

    public  VMCannotBeModifiedException(String arg1) {
        super(arg1);
    }
}
