/*******************************************************************************
 * Copyright (c) 2005, 2015 BEA Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    tyeung@bea.com  - initial API and implementation
 *    IBM Corporation - fix for bug 342757
 *    Stephan Herrmann - Contribution for bug 186342 - [compiler][null] Using annotations for null checking
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.classfmt;

import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;

class MethodInfoWithParameterAnnotations extends MethodInfoWithAnnotations {

    private AnnotationInfo[][] parameterAnnotations;

     MethodInfoWithParameterAnnotations(MethodInfo methodInfo, AnnotationInfo[] annotations, AnnotationInfo[][] parameterAnnotations) {
        super(methodInfo, annotations);
        this.parameterAnnotations = parameterAnnotations;
    }

    public IBinaryAnnotation[] getParameterAnnotations(int index, char[] classFileName) {
        try {
            return this.parameterAnnotations == null ? null : this.parameterAnnotations[index];
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            StringBuffer message = new StringBuffer("Mismatching number of parameter annotations, ");
            message.append(index);
            message.append('>');
            message.append(this.parameterAnnotations.length - 1);
            message.append(" in ");
            message.append(getSelector());
            char[] desc = getGenericSignature();
            if (desc != null)
                message.append(desc);
            else
                message.append(getMethodDescriptor());
            if (classFileName != null)
                message.append(" in ").append(classFileName);
            throw new IllegalStateException(message.toString(), aioobe);
        }
    }

    public int getAnnotatedParametersCount() {
        return this.parameterAnnotations == null ? 0 : this.parameterAnnotations.length;
    }

    protected void initialize() {
        for (int i = 0, l = this.parameterAnnotations == null ? 0 : this.parameterAnnotations.length; i < l; i++) {
            AnnotationInfo[] infos = this.parameterAnnotations[i];
            for (int j = 0, k = infos == null ? 0 : infos.length; j < k; j++) infos[j].initialize();
        }
        super.initialize();
    }

    protected void reset() {
        for (int i = 0, l = this.parameterAnnotations == null ? 0 : this.parameterAnnotations.length; i < l; i++) {
            AnnotationInfo[] infos = this.parameterAnnotations[i];
            for (int j = 0, k = infos == null ? 0 : infos.length; j < k; j++) infos[j].reset();
        }
        super.reset();
    }

    protected void toStringContent(StringBuffer buffer) {
        super.toStringContent(buffer);
        for (int i = 0, l = this.parameterAnnotations == null ? 0 : this.parameterAnnotations.length; i < l; i++) {
            //$NON-NLS-1$
            buffer.append("param" + (i - 1));
            buffer.append('\n');
            AnnotationInfo[] infos = this.parameterAnnotations[i];
            for (int j = 0, k = infos == null ? 0 : infos.length; j < k; j++) {
                buffer.append(infos[j]);
                buffer.append('\n');
            }
        }
    }
}
