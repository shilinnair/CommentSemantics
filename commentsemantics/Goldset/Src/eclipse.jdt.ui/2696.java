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
package org.eclipse.jdt.text.tests;

import org.eclipse.jdt.internal.ui.text.JavaWordIterator;

/**
 * @since 3.0
 */
public class JavaWordIteratorTest extends BreakIteratorTest {

    /*
	 * @see junit.framework.TestCase#setUp()
	 */
    @Override
    protected void setUp() throws Exception {
        fBreakIterator = new JavaWordIterator();
    }

    public void testNext1() {
        assertNextPositions("word word", new int[] { 5, 9 });
    }

    public void testNext2() {
        assertNextPositions("wordWord word", new int[] { 4, 9, 13 });
    }

    public void testNextSpace() {
        assertNextPositions(" word ", new int[] { 1, 6 });
    }

    public void testNextParen() {
        assertNextPositions("word(params)", new int[] { 4, 5, 11, 12 });
    }

    public void testNextLn() {
        String s = new String("word \n" + "  word2");
        assertNextPositions(s, new int[] { 5, 6, 8, 13 });
    }

    public void testMultiNextLn() {
        String s = new String("word \n" + "\n" + "\n" + "  word2");
        assertNextPositions(s, new int[] { 5, 6, 7, 8, 10, 15 });
    }

    public void testMultiNextLn2() {
        String s = new String("word \r\n" + "\r\n" + "\r\n" + "  word2");
        assertNextPositions(s, new int[] { 5, 7, 9, 11, 13, 18 });
    }

    public void testNextCamelCaseWord() {
        String s = new String("   _isURLConnection_   ");
        assertNextPositions(s, new int[] { 3, 6, 9, 23 });
    }

    public void testPrevious1() {
        String s = new String("word word");
        assertPreviousPositions(s, new int[] { 0, 5 });
    }

    public void testPrevious2() {
        String s = new String("wordWord word");
        assertPreviousPositions(s, new int[] { 0, 4, 9 });
    }

    public void testPreviousSpace() {
        String s = new String(" word ");
        assertPreviousPositions(s, new int[] { 1 });
    }

    public void testPreviousParen() {
        String s = new String("word(params)");
        assertPreviousPositions(s, new int[] { 0, 4, 5, 11 });
    }

    public void testPreviousLn() {
        String s = new String("word \n" + "  word2");
        assertPreviousPositions(s, new int[] { 0, 5, 6, 8 });
    }

    public void testMultiPreviousLn() {
        String s = new String("word \n" + "\n" + "\n" + "  word2");
        assertPreviousPositions(s, new int[] { 0, 5, 6, 7, 8, 10 });
    }

    public void testMultiPreviousLn2() {
        String s = new String("word \r\n" + "\r\n" + "\r\n" + "  word2");
        assertPreviousPositions(s, new int[] { 0, 5, 7, 9, 11, 13 });
    }

    public void testPreviousCamelCaseWord() {
        String s = new String("   _isURLConnection_   ");
        assertPreviousPositions(s, new int[] { 0, 3, 6, 9 });
    }
}
