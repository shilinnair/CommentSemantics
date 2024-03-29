/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.performance.parts;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaDescriptor;

/**
 * SchemaLoaderPerfTest
 *
 */
public class SchemaLoaderPerfTest extends AbstractSchemaPerfTest {

    private boolean fAbbreviated;

    /**
	 * @return
	 */
    public static Test suite() {
        return new TestSuite(SchemaLoaderPerfTest.class);
    }

    /**
	 * @throws Exception
	 */
    public void testSchemaUnabbreviated() throws Exception {
        fAbbreviated = false;
        fTestIterations = 5;
        fWarmupIterations = 50;
        fRuns = 600;
        executeTestRun();
    }

    /**
	 * @throws Exception
	 */
    public void testSchemaAbbreviated() throws Exception {
        fAbbreviated = true;
        fTestIterations = 5;
        fWarmupIterations = 50;
        fRuns = 3000;
        executeTestRun();
    }

    @Override
    protected void executeTest() throws Exception {
        SchemaDescriptor descriptor = new SchemaDescriptor(fXSDFile);
        Schema schema = (Schema) descriptor.getSchema(fAbbreviated);
        if (schema.getName() == null) {
            //$NON-NLS-1$
            throw new Exception("ERROR: Extension point schema name missing");
        }
    }
}
