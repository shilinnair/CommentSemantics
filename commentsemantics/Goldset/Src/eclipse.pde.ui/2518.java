/*******************************************************************************
 * Copyright (c) Aug 22, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package x.y.z;

import a.NoRefAnnotation;

public class test9 {

    void method1() {
        @NoRefAnnotation
        class local {
        }
    }
}
