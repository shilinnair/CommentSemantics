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
package org.eclipse.jdt.internal.core.builder;

import org.eclipse.core.runtime.*;

/**
 * Exception thrown when there is an internal error in the image builder.
 * May wrapper another exception.
 */
public class ImageBuilderInternalException extends RuntimeException {

    // backward compatible
    private static final long serialVersionUID = 28252254530437336L;

    protected CoreException coreException;

    public  ImageBuilderInternalException(CoreException e) {
        this.coreException = e;
    }

    public CoreException getThrowable() {
        return this.coreException;
    }

    public void printStackTrace() {
        if (this.coreException != null) {
            System.err.println(this);
            //$NON-NLS-1$
            System.err.println("Stack trace of embedded core exception:");
            this.coreException.printStackTrace();
        } else {
            super.printStackTrace();
        }
    }
}
