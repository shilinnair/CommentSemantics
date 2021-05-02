/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package a.classes.constructors;

/**
 * 
 */
public class RemoveProtectedConstructorNoOverride {

    /**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
    protected  RemoveProtectedConstructorNoOverride(int i) {
    }

    /**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
    protected  RemoveProtectedConstructorNoOverride(String foo) {
    }
}
