/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package x.y.z;

import f.FieldUsageEnum;

/**
 * Tests the usage of restricted enum fields
 */
@SuppressWarnings({ "unused", "hiding" })
public class testF6 {

    int f2 = FieldUsageEnum.A.f2;

    int f3 = FieldUsageEnum.f3;

    Object o = FieldUsageEnum.A;

    public static class inner {

        /**
		 * Constructor
		 */
        public  inner() {
            int f2 = FieldUsageEnum.A.f2;
            int f3 = FieldUsageEnum.f3;
            Object o = FieldUsageEnum.A;
        }
    }

    class inner2 {

        public  inner2() {
            int f2 = FieldUsageEnum.A.f2;
            int f3 = FieldUsageEnum.f3;
            Object o = FieldUsageEnum.A;
        }
    }
}

@SuppressWarnings({ "unused" })
class outer {

    /**
	 * Constructor
	 */
    public  outer() {
        int f2 = FieldUsageEnum.A.f2;
        int f3 = FieldUsageEnum.f3;
        Object o = FieldUsageEnum.A;
    }
}
