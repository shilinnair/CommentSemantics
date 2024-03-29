/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.jdt.ui.PreferenceConstants;

/**
 *
 * @since 3.2
 */
public class MethodParameterGuessingCompletionTest extends AbstractCompletionTest {

    private static final Class<MethodParameterGuessingCompletionTest> THIS = MethodParameterGuessingCompletionTest.class;

    public static Test setUpTest(Test test) {
        return new CompletionTestSetup(test);
    }

    public static Test suite() {
        return setUpTest(new TestSuite(THIS, suiteName(THIS)));
    }

    /*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#setUp()
	 */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
        getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, true);
        addMembers("private java.util.List fList;");
        addLocalVariables("int foo= 3; Object obj= null;\n");
    }

    public void testMethodWithParam1() throws Exception {
        assertMethodBodyProposal("fList.", "add(O", "fList.add(|obj|)");
    }

    public void testMethodWithParam2() throws Exception {
        assertMethodBodyProposal("fList.", "add(int", "fList.add(|foo|, obj);");
    }

    public void testInsertMethodWithParam1() throws Exception {
        assertMethodBodyProposal("fList.|bar", "add(O", "fList.add(|obj|)bar");
    }

    public void testInsertMethodWithParam2() throws Exception {
        assertMethodBodyProposal("fList.|bar", "add(int", "fList.add(|foo|, obj);bar");
    }

    public void testOverwriteMethodWithParam1() throws Exception {
        getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
        assertMethodBodyProposal("fList.|bar", "add(O", "fList.add(|obj|)");
    }

    public void testOverwriteMethodWithParam2() throws Exception {
        getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
        assertMethodBodyProposal("fList.|bar", "add(int", "fList.add(|foo|, obj);");
    }
}
