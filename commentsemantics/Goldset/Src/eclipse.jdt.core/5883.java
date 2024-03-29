/*******************************************************************************
 * Copyright (c) 2005, 2007 BEA Systems, Inc. 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    jgarms@bea.com - initial API and implementation
 *    
 *******************************************************************************/
package org.eclipse.jdt.apt.core.util;

import java.util.Collection;
import java.util.List;
import org.eclipse.jdt.apt.core.internal.AnnotationProcessorFactoryLoader;
import org.eclipse.jdt.core.IJavaProject;
import com.sun.mirror.apt.AnnotationProcessorFactory;

public final class AptUtil {

    // Private c-tor to prevent construction
    private  AptUtil() {
    }

    /**
	 * Returns the matching annotation processor factory for a given
	 * annotation in a given project.
	 * 
	 * @param fullyQualifiedAnnotation the annotation for which a factory
	 * is desired. This must be fully qualfied -- e.g. "org.eclipse.annotation.Foo"
	 */
    public static AnnotationProcessorFactory getFactoryForAnnotation(final String fullyQualifiedAnnotation, final IJavaProject jproj) {
        AnnotationProcessorFactoryLoader loader = AnnotationProcessorFactoryLoader.getLoader();
        List<AnnotationProcessorFactory> factories = loader.getJava5FactoriesForProject(jproj);
        for (AnnotationProcessorFactory factory : factories) {
            Collection<String> supportedAnnos = factory.supportedAnnotationTypes();
            for (String anno : supportedAnnos) {
                if (anno.equals(fullyQualifiedAnnotation)) {
                    return factory;
                } else if (//$NON-NLS-1$
                "*".equals(anno)) {
                    return factory;
                } else if (//$NON-NLS-1$
                anno.endsWith("*")) {
                    final String prefix = anno.substring(0, anno.length() - 2);
                    if (fullyQualifiedAnnotation.startsWith(prefix)) {
                        return factory;
                    }
                }
            }
        }
        return null;
    }
}
