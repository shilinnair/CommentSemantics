/*******************************************************************************
 * Copyright (c) 2015 Salesforce, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     igor@ifedorenko.com - initial API and implementation
 *******************************************************************************/
package targets.AnnotationProcessorTests.Bug471995;

public class B {

    public  B(@Bug471995 missing.Type p1, @Bug471995 missing.Type p2) {
    }
}
