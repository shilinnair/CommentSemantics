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
package org.eclipse.jdt.ui.leaktest.reftracker;

import java.util.Arrays;

/**
 *
 */
public class IdentityHashSet {

    private Object[] fObjects;

    private int fThreshold;

    private int fSize;

    public  IdentityHashSet(int initialSize) {
        fObjects = new Object[initialSize];
        fThreshold = getThreshold(initialSize);
        fSize = 0;
    }

    private static int getThreshold(int initialSize) {
        return (initialSize * 5) / 6;
    }

    public boolean add(Object o) {
        if (o == null) {
            //$NON-NLS-1$
            throw new IllegalArgumentException("Can not add null");
        }
        int insertionIndex = getInsertionIndex(o, fObjects);
        if (insertionIndex == -1) {
            // already in set
            return false;
        }
        insertElement(o, insertionIndex);
        return true;
    }

    private void insertElement(Object o, int index) {
        fObjects[index] = o;
        fSize++;
        if (fSize > fThreshold) {
            increaseSize();
        }
    }

    private static int getInsertionIndex(Object elem, Object[] elements) {
        int hash = getHash(elem, elements.length);
        Object entry = elements[hash];
        while (entry != null && entry != elem) {
            hash++;
            if (hash == elements.length) {
                // wrap
                hash = 0;
            }
            entry = elements[hash];
        }
        if (entry == null) {
            return hash;
        }
        // already in set
        return -1;
    }

    private void increaseSize() {
        int newSize = fObjects.length * 2;
        Object[] newArray = new Object[newSize];
        for (int i = 0; i < fObjects.length; i++) {
            Object curr = fObjects[i];
            if (curr != null) {
                int insertionIndex = getInsertionIndex(curr, newArray);
                newArray[insertionIndex] = curr;
                // avoid unnecessary references
                fObjects[i] = null;
            }
        }
        fObjects = newArray;
        fThreshold = getThreshold(newSize);
    }

    public boolean contains(Object o) {
        return getInsertionIndex(o, fObjects) == -1;
    }

    public int size() {
        return fSize;
    }

    public void clear() {
        Arrays.fill(fObjects, null);
        fSize = 0;
    }

    /**
     * Return index for Object x.
     * @param x the object
     * @param length the length of the hashtable
     * @return returns the hash
     */
    private static int getHash(Object x, int length) {
        return (System.identityHashCode(x) & 0x7FFFFFFF) % length;
    }
}
