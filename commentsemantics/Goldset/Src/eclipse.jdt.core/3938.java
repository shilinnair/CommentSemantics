/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.jdt.core.tests.compiler.regression.GenericTypeTest;
import org.eclipse.jdt.core.tests.compiler.regression.GenericsRegressionTest;
import org.eclipse.jdt.core.tests.compiler.regression.GenericsRegressionTest_1_7;
import org.eclipse.jdt.core.tests.compiler.regression.GenericsRegressionTest_1_8;
import org.eclipse.jdt.core.tests.compiler.regression.LambdaExpressionsTest;
import org.eclipse.jdt.core.tests.compiler.regression.LambdaRegressionTest;
import org.eclipse.jdt.core.tests.compiler.regression.LambdaShapeTests;
import org.eclipse.jdt.core.tests.compiler.regression.NegativeLambdaExpressionsTest;
import org.eclipse.jdt.core.tests.compiler.regression.NullTypeAnnotationTest;
import org.eclipse.jdt.core.tests.compiler.regression.OverloadResolutionTest8;
import org.eclipse.jdt.core.tests.dom.ConverterTestSetup;
import org.eclipse.jdt.core.tests.util.AbstractCompilerTest;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class RunOnly335CompilerTests extends TestCase {

    public  RunOnly335CompilerTests(String name) {
        super(name);
    }

    public static Class[] getAllTestClasses() {
        return new Class[] { GenericsRegressionTest_1_8.class, LambdaExpressionsTest.class, LambdaRegressionTest.class, NegativeLambdaExpressionsTest.class, OverloadResolutionTest8.class, LambdaShapeTests.class, // tests type inference
        NullTypeAnnotationTest.class };
    }

    public static Class[] getCompilerClasses() {
        return new Class[] { GenericTypeTest.class, GenericsRegressionTest.class, GenericsRegressionTest_1_7.class };
    }

    public static Test suite() {
        TestSuite ts = new TestSuite(RunOnly335CompilerTests.class.getName());
        Class[] testClasses = getAllTestClasses();
        addTestsToSuite(ts, testClasses);
        AbstractCompilerTest.setpossibleComplianceLevels(AbstractCompilerTest.F_1_8);
        addTestsToSuite(ts, getCompilerClasses());
        return ts;
    }

    public static void addTestsToSuite(TestSuite suite, Class[] testClasses) {
        for (int i = 0; i < testClasses.length; i++) {
            Class testClass = testClasses[i];
            // call the suite() method and add the resulting suite to the suite
            try {
                Method suiteMethod = //$NON-NLS-1$
                testClass.getDeclaredMethod(//$NON-NLS-1$
                "suite", //$NON-NLS-1$
                new Class[0]);
                Test test = (Test) suiteMethod.invoke(null, new Object[0]);
                suite.addTest(test);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.getTargetException().printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    protected void tearDown() throws Exception {
        ConverterTestSetup.PROJECT_SETUP = false;
        super.tearDown();
    }
}
