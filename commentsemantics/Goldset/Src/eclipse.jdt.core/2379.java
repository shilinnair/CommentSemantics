/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.performance;

import java.lang.reflect.*;
import java.text.NumberFormat;
import org.eclipse.jdt.core.tests.junit.extension.PerformanceTestSuite;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import junit.framework.Test;

/**
 * Class to run all JDT/Core performance tests.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AllPerformanceTests extends junit.framework.TestCase {

    static final boolean ADD = System.getProperty("add", "false").equals("true");

    static final String RUN_ID = System.getProperty("runID");

    static final long MAX_MEM = 256L * 1024 * 1024;

    static final long TOTAL_MEM = MAX_MEM;

    /**
	 * Define performance tests classes to be run.
	 */
    public static Class[] getAllTestClasses() {
        return new Class[] { // run this test first to be sure that indexing is finished
        FullSourceWorkspaceSearchTests.class, FullSourceWorkspaceBuildTests.class, FullSourceWorkspaceASTTests.class, FullSourceWorkspaceTypeHierarchyTests.class, FullSourceWorkspaceModelTests.class, FullSourceWorkspaceCompletionTests.class, FullSourceWorkspaceFormatterTests.class, RegionPerformanceTests.class };
    }

    /**
	 * Additional test class(es).
	 *
	 * Classes put in this list will be run only if "add" VM parameter (-Dadd=true) is added
	 * while running JUnit test suite.
	 *
	 * @see #ADD
	 */
    public static Class[] getAdditionalTestClasses() {
        return new Class[] { SecondaryTypesPerformanceTest.class };
    }

    /**
	 * Build test suite.
	 * All classes suite method are called and bundle to main test suite.
	 */
    public static Test suite() {
        PerformanceTestSuite perfSuite = new PerformanceTestSuite(AllPerformanceTests.class.getName());
        Class[] testSuites = getAllTestClasses();
        // Display warning if one of subset static fields is not null
        // (this may modify tests run order and make stored results invalid)
        StringBuffer buffer = null;
        if (TestCase.TESTS_NAMES != null) {
            buffer = new StringBuffer("WARNING: Performance tests results may be invalid !!!\n");
            buffer.append("	- following subset is still defined and may alter tests order:\n");
            buffer.append("		+ TESTS_NAMES = new String[] { ");
            int length = TestCase.TESTS_NAMES.length;
            for (int i = 0; i < length; i++) {
                if (i > 0)
                    buffer.append(',');
                buffer.append('"');
                buffer.append(TestCase.TESTS_NAMES[i]);
                buffer.append('"');
            }
            buffer.append(" };\n");
        }
        if (TestCase.TESTS_PREFIX != null) {
            if (buffer == null) {
                buffer = new StringBuffer("WARNING: Performance tests results may be invalid !!!\n");
                buffer.append("	- following subset is still defined and may alter tests order:\n");
            }
            buffer.append("		+ TESTS_PREFIX = ");
            buffer.append('"');
            buffer.append(TestCase.TESTS_PREFIX);
            buffer.append('"');
            buffer.append(";\n");
        }
        if (TestCase.TESTS_NUMBERS != null) {
            if (buffer == null) {
                buffer = new StringBuffer("WARNING: Performance tests results may be invalid !!!\n");
                buffer.append("	- following subset is still defined and may alter tests order:\n");
            }
            buffer.append("		+ TESTS_NUMBERS = new int[] { ");
            int length = TestCase.TESTS_NUMBERS.length;
            for (int i = 0; i < length; i++) {
                if (i > 0)
                    buffer.append(',');
                buffer.append(TestCase.TESTS_NUMBERS[i]);
            }
            buffer.append(" };\n");
        }
        if (TestCase.TESTS_RANGE != null) {
            if (buffer == null) {
                buffer = new StringBuffer("WARNING: Performance tests results may be invalid !!!\n");
                buffer.append("	- following subset is still defined and may alter tests order:\n");
            }
            buffer.append("		+ TESTS_RANGE = new int[] { ");
            buffer.append(TestCase.TESTS_RANGE[0]);
            buffer.append(',');
            buffer.append(TestCase.TESTS_RANGE[1]);
            buffer.append(";\n");
        }
        // Verify VM memory arguments: should be -Xmx256M -Xms256M
        NumberFormat floatFormat = NumberFormat.getNumberInstance();
        floatFormat.setMaximumFractionDigits(1);
        // -Xmx
        long maxMem = Runtime.getRuntime().maxMemory();
        boolean tooMuch = false;
        if (maxMem < (MAX_MEM * 0.98) || (tooMuch = maxMem > (MAX_MEM * 1.02))) {
            if (buffer == null)
                buffer = new StringBuffer("WARNING: Performance tests results may be invalid !!!\n");
            buffer.append("	- ");
            buffer.append(tooMuch ? "too much " : "not enough ");
            buffer.append("max memory allocated (");
            buffer.append(floatFormat.format(((maxMem / 1024.0) / 1024.0)));
            buffer.append("M)!\n");
            buffer.append("		=> -Xmx");
            buffer.append(floatFormat.format(((MAX_MEM / 1024.0) / 1024.0)));
            buffer.append("M should have been specified.\n");
        }
        // -Xms
        long totalMem = Runtime.getRuntime().totalMemory();
        tooMuch = false;
        if (totalMem < (TOTAL_MEM * 0.98) || (tooMuch = totalMem > (TOTAL_MEM * 1.02))) {
            if (buffer == null)
                buffer = new StringBuffer("WARNING: Performance tests results may be invalid !!!\n");
            buffer.append("	- ");
            buffer.append(tooMuch ? "too much " : "not enough ");
            buffer.append("total memory allocated (");
            buffer.append(floatFormat.format(((totalMem / 1024.0) / 1024.0)));
            buffer.append("M)!\n");
            buffer.append("		=> -Xms");
            buffer.append(floatFormat.format(((MAX_MEM / 1024.0) / 1024.0)));
            buffer.append("M should have been specified.\n");
        }
        // Display warning message if any
        if (buffer != null) {
            System.err.println(buffer.toString());
        }
        // Get test suites subset
        int length = testSuites.length;
        if (RUN_ID != null) {
            Class[] subSetSuites = new Class[length];
            int count = 0;
            for (int i = 0; i < length; i++) {
                String name = FullSourceWorkspaceTests.suiteTypeShortName(testSuites[i]);
                if (RUN_ID.indexOf(name.charAt(0)) >= 0) {
                    subSetSuites[count++] = testSuites[i];
                }
            }
            System.arraycopy(subSetSuites, 0, testSuites = new Class[count], 0, count);
            length = count;
        }
        // Get test suites subset
        if (ADD) {
            Class[] complete = getAdditionalTestClasses();
            int completeLength = complete.length;
            Class[] newSuites = new Class[length + completeLength];
            System.arraycopy(testSuites, 0, newSuites, 0, length);
            System.arraycopy(complete, 0, newSuites, length, completeLength);
            testSuites = newSuites;
            length = testSuites.length;
        }
        // Get suite acronym
        if (length == 0) {
            System.err.println("There's no performances suites to run!!!");
            return perfSuite;
        }
        String suitesAcronym = "";
        if (RUN_ID == null) {
            for (int i = 0; i < length; i++) {
                String name = FullSourceWorkspaceTests.suiteTypeShortName(testSuites[i]);
                if (name != null) {
                    char firstChar = name.charAt(0);
                    if (suitesAcronym.indexOf(firstChar) >= 0) {
                        System.out.println("WARNING: Duplicate letter in RUN_ID for test suite: " + name);
                    }
                    suitesAcronym += firstChar;
                }
            }
        } else {
            suitesAcronym = RUN_ID;
        }
        //.toLowerCase();
        FullSourceWorkspaceTests.RUN_ID = suitesAcronym;
        // Get tests of suites
        for (int i = 0; i < length; i++) {
            Class testClass = testSuites[i];
            // call the suite() method and add the resulting suite to the suite
            try {
                Method suiteMethod = //$NON-NLS-1$
                testClass.getDeclaredMethod(//$NON-NLS-1$
                "suite", //$NON-NLS-1$
                new Class[0]);
                Test suite = (Test) suiteMethod.invoke(null, new Object[0]);
                perfSuite.addTest(suite);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.getTargetException().printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return perfSuite;
    }
}
