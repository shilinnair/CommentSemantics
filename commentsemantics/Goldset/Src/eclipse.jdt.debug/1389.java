/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 * See http://docs.oracle.com/javase/6/docs/jdk/api/jpda/jdi/com/sun/jdi/Field.html
 */
public interface Field extends TypeComponent, Comparable<Field> {

    @Override
    public boolean equals(Object arg1);

    @Override
    public int hashCode();

    public boolean isEnumConstant();

    public boolean isTransient();

    public boolean isVolatile();

    public Type type() throws ClassNotLoadedException;

    public String typeName();
}
