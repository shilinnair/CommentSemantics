/*******************************************************************************
 * Copyright (c) 2005 BEA Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   wharley@bea.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.apt.tests.annotations.messager;

import java.util.Set;
import org.eclipse.jdt.apt.tests.annotations.BaseFactory;
import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;

public class MessagerAnnotationProcessorFactory extends BaseFactory {

    private static final String annotationName = "org.eclipse.jdt.apt.tests.annotations.messager.MessagerAnnotation";

    public  MessagerAnnotationProcessorFactory() {
        super(annotationName);
    }

    /* (non-Javadoc)
	 * @see com.sun.mirror.apt.AnnotationProcessorFactory#getProcessorFor(java.util.Set, com.sun.mirror.apt.AnnotationProcessorEnvironment)
	 */
    public AnnotationProcessor getProcessorFor(Set<AnnotationTypeDeclaration> decls, AnnotationProcessorEnvironment env) {
        return new MessagerAnnotationProcessor(decls, env);
    }
}
