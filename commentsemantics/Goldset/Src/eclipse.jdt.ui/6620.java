/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @since 3.4
 */
public class ScrollVerticalRuler100Test extends ScrollVerticalRulerTest {

    private static final Class<ScrollVerticalRuler100Test> THIS = ScrollVerticalRuler100Test.class;

    public static Test suite() {
        return new PerformanceTestSetup(new TestSuite(THIS));
    }

    @Override
    protected int getNumberOfAnnotations() {
        return 100;
    }

    /**
	 * Measure the time spent while scrolling page wise in the text editor.
	 *
	 * @throws Exception
	 */
    public void testScrollTextEditorPageWise() throws Exception {
        measure(PAGE_WISE);
    }

    /**
	 * Measure the time spent while scrolling line wise in the text editor.
	 *
	 * @throws Exception
	 */
    public void testScrollTextEditorLineWiseMoveCaret2() throws Exception {
        measure(LINE_WISE);
    }
}
