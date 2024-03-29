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
package org.eclipse.jdt.internal.compiler.codegen;

public class IntegerCache {

    public int keyTable[];

    public int valueTable[];

    int elementSize;

    int threshold;

    /**
 * Constructs a new, empty hashtable. A default capacity and
 * load factor is used. Note that the hashtable will automatically
 * grow when it gets full.
 */
    public  IntegerCache() {
        this(13);
    }

    /**
 * Constructs a new, empty hashtable with the specified initial
 * capacity.
 * @param initialCapacity int
 *  the initial number of buckets
 */
    public  IntegerCache(int initialCapacity) {
        this.elementSize = 0;
        this.threshold = (int) (initialCapacity * 0.66);
        this.keyTable = new int[initialCapacity];
        this.valueTable = new int[initialCapacity];
    }

    /**
 * Clears the hash table so that it has no more elements in it.
 */
    public void clear() {
        for (int i = this.keyTable.length; --i >= 0; ) {
            this.keyTable[i] = 0;
            this.valueTable[i] = 0;
        }
        this.elementSize = 0;
    }

    /** Returns true if the collection contains an element for the key.
 *
 * @param key <CODE>double</CODE> the key that we are looking for
 * @return boolean
 */
    public boolean containsKey(int key) {
        int index = hash(key), length = this.keyTable.length;
        while ((this.keyTable[index] != 0) || ((this.keyTable[index] == 0) && (this.valueTable[index] != 0))) {
            if (this.keyTable[index] == key)
                return true;
            if (++index == length) {
                index = 0;
            }
        }
        return false;
    }

    /**
 * Return a hashcode for the value of the key parameter.
 * @param key int
 * @return int the hash code corresponding to the key value
 */
    public int hash(int key) {
        return (key & 0x7FFFFFFF) % this.keyTable.length;
    }

    /**
 * Puts the specified element into the hashtable, using the specified
 * key.  The element may be retrieved by doing a get() with the same key.
 *
 * @param key <CODE>int</CODE> the specified key in the hashtable
 * @param value <CODE>int</CODE> the specified element
 * @return int value
 */
    public int put(int key, int value) {
        int index = hash(key), length = this.keyTable.length;
        while ((this.keyTable[index] != 0) || ((this.keyTable[index] == 0) && (this.valueTable[index] != 0))) {
            if (this.keyTable[index] == key)
                return this.valueTable[index] = value;
            if (++index == length) {
                index = 0;
            }
        }
        this.keyTable[index] = key;
        this.valueTable[index] = value;
        // assumes the threshold is never equal to the size of the table
        if (++this.elementSize > this.threshold) {
            rehash();
        }
        return value;
    }

    /**
 * Puts the specified element into the hashtable if absent, using the specified
 * key.  The element may be retrieved by doing a get() with the same key.
 *
 * @param key <CODE>int</CODE> the specified key in the hashtable
 * @param value <CODE>int</CODE> the specified element
 * @return int value
 */
    public int putIfAbsent(int key, int value) {
        int index = hash(key), length = this.keyTable.length;
        while ((this.keyTable[index] != 0) || ((this.keyTable[index] == 0) && (this.valueTable[index] != 0))) {
            if (this.keyTable[index] == key)
                return this.valueTable[index];
            if (++index == length) {
                index = 0;
            }
        }
        this.keyTable[index] = key;
        this.valueTable[index] = value;
        // assumes the threshold is never equal to the size of the table
        if (++this.elementSize > this.threshold) {
            rehash();
        }
        // negative when added, assumes value is > 0
        return -value;
    }

    /**
 * Rehashes the content of the table into a bigger table.
 * This method is called automatically when the hashtable's
 * size exceeds the threshold.
 */
    private void rehash() {
        IntegerCache newHashtable = new IntegerCache(this.keyTable.length * 2);
        for (int i = this.keyTable.length; --i >= 0; ) {
            int key = this.keyTable[i];
            int value = this.valueTable[i];
            if ((key != 0) || ((key == 0) && (value != 0))) {
                newHashtable.put(key, value);
            }
        }
        this.keyTable = newHashtable.keyTable;
        this.valueTable = newHashtable.valueTable;
        this.threshold = newHashtable.threshold;
    }

    /**
 * Returns the number of elements contained in the hashtable.
 *
 * @return <CODE>int</CODE> The size of the table
 */
    public int size() {
        return this.elementSize;
    }

    /**
 * Converts to a rather lengthy String.
 *
 * @return String the ascii representation of the receiver
 */
    public String toString() {
        int max = size();
        StringBuffer buf = new StringBuffer();
        //$NON-NLS-1$
        buf.append("{");
        for (int i = 0; i < max; ++i) {
            if ((this.keyTable[i] != 0) || ((this.keyTable[i] == 0) && (this.valueTable[i] != 0))) {
                //$NON-NLS-1$
                buf.append(this.keyTable[i]).append("->").append(this.valueTable[i]);
            }
            if (i < max) {
                //$NON-NLS-1$
                buf.append(", ");
            }
        }
        //$NON-NLS-1$
        buf.append("}");
        return buf.toString();
    }
}
