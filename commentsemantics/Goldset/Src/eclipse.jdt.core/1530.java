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
package org.eclipse.jdt.core.tests.model;

import junit.framework.Test;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class CreateMembersTests extends AbstractJavaModelTests {

    public  CreateMembersTests(String name) {
        super(name);
    }

    // All specified tests which do not belong to the class are skipped...
    static {
    // Names of tests to run: can be "testBugXXXX" or "BugXXXX")
    //		TESTS_PREFIX = "testCombineAccessRestrictions";
    //		TESTS_NAMES = new String[] {"test004"};
    //		TESTS_NUMBERS = new int[] { 5, 6 };
    //		TESTS_RANGE = new int[] { 21, 38 };
    }

    public static Test suite() {
        return buildModelTestSuite(CreateMembersTests.class, ALPHABETICAL_SORT);
    }

    public void setUpSuite() throws Exception {
        super.setUpSuite();
        setUpJavaProject("CreateMembers", "1.5");
    }

    public void tearDownSuite() throws Exception {
        deleteProject("CreateMembers");
        super.tearDownSuite();
    }

    public void test001() throws JavaModelException {
        ICompilationUnit compilationUnit = getCompilationUnit("CreateMembers", "src", "", "A.java");
        assertNotNull("No compilation unit", compilationUnit);
        IType[] types = compilationUnit.getTypes();
        assertNotNull("No types", types);
        assertEquals("Wrong size", 1, types.length);
        IType type = types[0];
        type.createMethod("\tpublic void foo() {\n\t\tSystem.out.println(\"Hello World\");\n\t}\n", null, true, new NullProgressMonitor());
        String expectedSource = "public class A {\n" + "\n" + "	public void foo() {\n" + "		System.out.println(\"Hello World\");\n" + "	}\n" + "}";
        assertSourceEquals("Unexpected source", expectedSource, type.getSource());
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=86906
    public void test002() throws JavaModelException {
        ICompilationUnit compilationUnit = getCompilationUnit("CreateMembers", "src", "", "E.java");
        assertNotNull("No compilation unit", compilationUnit);
        IType[] types = compilationUnit.getTypes();
        assertNotNull("No types", types);
        assertEquals("Wrong size", 1, types.length);
        IType type = types[0];
        IField sibling = type.getField("j");
        type.createField("int i;", sibling, true, null);
        String expectedSource = "public enum E {\n" + "	E1, E2;\n" + "	int i;\n" + "	int j;\n" + "}";
        assertSourceEquals("Unexpected source", expectedSource, type.getSource());
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=86906
    public void test003() throws JavaModelException {
        ICompilationUnit compilationUnit = getCompilationUnit("CreateMembers", "src", "", "Annot.java");
        assertNotNull("No compilation unit", compilationUnit);
        IType[] types = compilationUnit.getTypes();
        assertNotNull("No types", types);
        assertEquals("Wrong size", 1, types.length);
        IType type = types[0];
        IMethod sibling = type.getMethod("foo", new String[] {});
        type.createMethod("String bar();", sibling, true, null);
        String expectedSource = "public @interface Annot {\n" + "	String bar();\n" + "\n" + "	String foo();\n" + "}";
        assertSourceEquals("Unexpected source", expectedSource, type.getSource());
    }

    /*
	 * Ensures that the handle for a created method that has varargs type arguments is correct.
	 * (regression test for bug 93487 IType#findMethods fails on vararg methods)
	 */
    public void test004() throws JavaModelException {
        IType type = getCompilationUnit("/CreateMembers/src/A.java").getType("A");
        IMethod method = type.createMethod("void bar(String... args) {}", // no siblings
        null, // don't force
        false, // no progress monitor
        null);
        assertTrue("Method should exist", method.exists());
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=95580
    public void test005() throws JavaModelException {
        ICompilationUnit compilationUnit = getCompilationUnit("CreateMembers", "src", "", "E2.java");
        assertNotNull("No compilation unit", compilationUnit);
        IType[] types = compilationUnit.getTypes();
        assertNotNull("No types", types);
        assertEquals("Wrong size", 1, types.length);
        IType type = types[0];
        type.createField("int i;", null, true, null);
        String expectedSource = "public enum E2 {\n" + "	A, B, C;\n\n" + "	int i;\n" + "}";
        assertSourceEquals("Unexpected source", expectedSource, type.getSource());
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=95580
    public void test006() throws JavaModelException {
        ICompilationUnit compilationUnit = getCompilationUnit("CreateMembers", "src", "", "E3.java");
        assertNotNull("No compilation unit", compilationUnit);
        IType[] types = compilationUnit.getTypes();
        assertNotNull("No types", types);
        assertEquals("Wrong size", 1, types.length);
        IType type = types[0];
        type.createType("class DD {}", null, true, null);
        String expectedSource = "public enum E3 {\n" + "	A, B, C;\n\n" + "	class DD {}\n" + "}";
        assertSourceEquals("Unexpected source", expectedSource, type.getSource());
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=95480
    public void test007() throws Exception {
        JavaModelException expected = null;
        try {
            IType type = getCompilationUnit("CreateMembers/src/E.java").getType("E");
            type.createType("class Member {}", type.getField("E1"), /*don't force*/
            false, /*no progress*/
            null);
        } catch (JavaModelException e) {
            expected = e;
        }
        assertExceptionEquals("Unexpected exception", "Invalid sibling: E1 [in E [in E.java [in <default> [in src [in CreateMembers]]]]]", expected);
    }
}
