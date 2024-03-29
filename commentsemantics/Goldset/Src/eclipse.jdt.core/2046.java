/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.env;

public interface IBinaryMethod extends IGenericMethod {

    /**
 * Answer the resolved names of the exception types in the
 * class file format as specified in section 4.2 of the Java 2 VM spec
 * or null if the array is empty.
 *
 * For example, java.lang.String is java/lang/String.
 */
    char[][] getExceptionTypeNames();

    /**
 * Answer the receiver's method descriptor which describes the parameter &
 * return types as specified in section 4.4.3 of the Java 2 VM spec.
 *
 * For example:
 *   - int foo(String) is (Ljava/lang/String;)I
 *   - Object[] foo(int) is (I)[Ljava/lang/Object;
 */
    char[] getMethodDescriptor();

    /**
 * Answer the receiver's signature which describes the parameter &
 * return types as specified in section 4.4.4 of the Java 2 VM spec.
 */
    char[] getGenericSignature();

    /**
 * Answer whether the receiver represents a class initializer method.
 */
    boolean isClinit();
}
