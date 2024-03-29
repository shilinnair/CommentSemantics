/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.util;

import org.eclipse.jdt.core.compiler.CharOperation;

/**
 * Hashtable of {char[] --> int}
 */
public final class HashtableOfIntValues implements Cloneable {

    public static final int NO_VALUE = Integer.MIN_VALUE;

    // to avoid using Enumerations, walk the individual tables skipping nulls
    public char[] keyTable[];

    public int valueTable[];

    // number of elements in the table
    public int elementSize;

    int threshold;

    public  HashtableOfIntValues() {
        this(13);
    }

    public  HashtableOfIntValues(int size) {
        this.elementSize = 0;
        // size represents the expected number of elements
        this.threshold = size;
        int extraRoom = (int) (size * 1.75f);
        if (this.threshold == extraRoom)
            extraRoom++;
        this.keyTable = new char[extraRoom][];
        this.valueTable = new int[extraRoom];
    }

    public Object clone() throws CloneNotSupportedException {
        HashtableOfIntValues result = (HashtableOfIntValues) super.clone();
        result.elementSize = this.elementSize;
        result.threshold = this.threshold;
        int length = this.keyTable.length;
        result.keyTable = new char[length][];
        System.arraycopy(this.keyTable, 0, result.keyTable, 0, length);
        length = this.valueTable.length;
        result.valueTable = new int[length];
        System.arraycopy(this.valueTable, 0, result.valueTable, 0, length);
        return result;
    }

    public boolean containsKey(char[] key) {
        int length = this.keyTable.length, index = CharOperation.hashCode(key) % length;
        int keyLength = key.length;
        char[] currentKey;
        while ((currentKey = this.keyTable[index]) != null) {
            if (currentKey.length == keyLength && CharOperation.equals(currentKey, key))
                return true;
            if (++index == length) {
                index = 0;
            }
        }
        return false;
    }

    public int get(char[] key) {
        int length = this.keyTable.length, index = CharOperation.hashCode(key) % length;
        int keyLength = key.length;
        char[] currentKey;
        while ((currentKey = this.keyTable[index]) != null) {
            if (currentKey.length == keyLength && CharOperation.equals(currentKey, key))
                return this.valueTable[index];
            if (++index == length) {
                index = 0;
            }
        }
        return NO_VALUE;
    }

    public int put(char[] key, int value) {
        int length = this.keyTable.length, index = CharOperation.hashCode(key) % length;
        int keyLength = key.length;
        char[] currentKey;
        while ((currentKey = this.keyTable[index]) != null) {
            if (currentKey.length == keyLength && CharOperation.equals(currentKey, key))
                return this.valueTable[index] = value;
            if (++index == length) {
                index = 0;
            }
        }
        this.keyTable[index] = key;
        this.valueTable[index] = value;
        // assumes the threshold is never equal to the size of the table
        if (++this.elementSize > this.threshold)
            rehash();
        return value;
    }

    public int removeKey(char[] key) {
        int length = this.keyTable.length, index = CharOperation.hashCode(key) % length;
        int keyLength = key.length;
        char[] currentKey;
        while ((currentKey = this.keyTable[index]) != null) {
            if (currentKey.length == keyLength && CharOperation.equals(currentKey, key)) {
                int value = this.valueTable[index];
                this.elementSize--;
                this.keyTable[index] = null;
                this.valueTable[index] = NO_VALUE;
                rehash();
                return value;
            }
            if (++index == length) {
                index = 0;
            }
        }
        return NO_VALUE;
    }

    private void rehash() {
        // double the number of expected elements
        HashtableOfIntValues newHashtable = new HashtableOfIntValues(this.elementSize * 2);
        char[] currentKey;
        for (int i = this.keyTable.length; --i >= 0; ) if ((currentKey = this.keyTable[i]) != null)
            newHashtable.put(currentKey, this.valueTable[i]);
        this.keyTable = newHashtable.keyTable;
        this.valueTable = newHashtable.valueTable;
        this.threshold = newHashtable.threshold;
    }

    public int size() {
        return this.elementSize;
    }

    public String toString() {
        //$NON-NLS-1$
        String s = "";
        char[] key;
        for (int i = 0, length = this.valueTable.length; i < length; i++) if ((key = this.keyTable[i]) != null)
            //$NON-NLS-2$ //$NON-NLS-1$
            s += new String(key) + " -> " + this.valueTable[i] + "\n";
        return s;
    }
}
