/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ltk.core.refactoring.tests.history;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RefactoringHistoryTests {

    public static Test suite() {
        TestSuite suite = new TestSuite(RefactoringHistoryTests.class.getName());
        suite.addTestSuite(RefactoringHistorySerializationTests.class);
        suite.addTestSuite(RefactoringHistoryServiceTests.class);
        return suite;
    }
}
