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

public final class HashtableOfInt {

    // to avoid using Enumerations, walk the individual tables skipping nulls
    public int[] keyTable;

    public Object[] valueTable;

    // number of elements in the table
    public int elementSize;

    int threshold;

    public  HashtableOfInt() {
        this(13);
    }

    public  HashtableOfInt(int size) {
        this.elementSize = 0;
        // size represents the expected number of elements
        this.threshold = size;
        int extraRoom = (int) (size * 1.75f);
        if (this.threshold == extraRoom)
            extraRoom++;
        this.keyTable = new int[extraRoom];
        this.valueTable = new Object[extraRoom];
    }

    public boolean containsKey(int key) {
        int length = this.keyTable.length, index = key % length;
        int currentKey;
        while ((currentKey = this.keyTable[index]) != 0) {
            if (currentKey == key)
                return true;
            if (++index == length) {
                index = 0;
            }
        }
        return false;
    }

    public Object get(int key) {
        int length = this.keyTable.length, index = key % length;
        int currentKey;
        while ((currentKey = this.keyTable[index]) != 0) {
            if (currentKey == key)
                return this.valueTable[index];
            if (++index == length) {
                index = 0;
            }
        }
        return null;
    }

    public Object put(int key, Object value) {
        int length = this.keyTable.length, index = key % length;
        int currentKey;
        while ((currentKey = this.keyTable[index]) != 0) {
            if (currentKey == key)
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

    private void rehash() {
        // double the number of expected elements
        HashtableOfInt newHashtable = new HashtableOfInt(this.elementSize * 2);
        int currentKey;
        for (int i = this.keyTable.length; --i >= 0; ) if ((currentKey = this.keyTable[i]) != 0)
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
        Object object;
        for (int i = 0, length = this.valueTable.length; i < length; i++) if ((object = this.valueTable[i]) != null)
            //$NON-NLS-2$ //$NON-NLS-1$
            s += this.keyTable[i] + " -> " + object.toString() + "\n";
        return s;
    }
}
