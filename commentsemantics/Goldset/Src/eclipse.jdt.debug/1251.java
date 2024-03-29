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
package org.eclipse.jdi.hcr;

/**
 * Thrown to indicate that the target VM refused to perform an operation.
 */
public class OperationRefusedException extends RuntimeException {

    /**
	 * All serializable objects should have a stable serialVersionUID
	 */
    private static final long serialVersionUID = 1L;

    public  OperationRefusedException() {
    }

    public  OperationRefusedException(String s) {
        super(s);
    }
}
