/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.impl;

public class ByteConstant extends Constant {

    byte value;

    public  ByteConstant(byte value) {
        this.value = value;
    }

    public byte byteValue() {
        return this.value;
    }

    public char charValue() {
        return (char) value;
    }

    public double doubleValue() {
        // implicit cast to return type
        return value;
    }

    public float floatValue() {
        // implicit cast to return type
        return value;
    }

    public int intValue() {
        // implicit cast to return type
        return value;
    }

    public long longValue() {
        // implicit cast to return type
        return value;
    }

    public short shortValue() {
        // implicit cast to return type
        return value;
    }

    public String stringValue() {
        //spec 15.17.11
        String s = Integer.valueOf(value).toString();
        //$NON-NLS-1$
        if (s == null)
            return "null";
        return s;
    }

    public String toString() {
        //$NON-NLS-1$
        return "(byte)" + value;
    }

    public int typeID() {
        return T_byte;
    }
}
