/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package a.b.c;

/**
 * Test supported @noreference tag on fields in an outer interface
 */
public interface test4 {

    public interface inner {

        /**
		 * @noreference
		 */
        public int f2 = 0;

        public interface inner2 {

            /**
			 * @noreference
			 */
            public char[] f3 = {};
        }
    }
}
