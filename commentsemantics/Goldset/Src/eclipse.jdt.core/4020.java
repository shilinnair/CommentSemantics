/*******************************************************************************
 * Copyright (c) 2005, 2007 BEA Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    tyeung@bea.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.apt.core.internal.declaration;

import com.sun.mirror.declaration.MethodDeclaration;
import com.sun.mirror.declaration.ParameterDeclaration;
import com.sun.mirror.type.TypeMirror;
import com.sun.mirror.util.DeclarationVisitor;
import org.eclipse.jdt.apt.core.internal.env.BaseProcessorEnv;
import org.eclipse.jdt.apt.core.internal.util.Factory;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class MethodDeclarationImpl extends ExecutableDeclarationImpl implements MethodDeclaration {

    public  MethodDeclarationImpl(final IMethodBinding binding, final BaseProcessorEnv env) {
        super(binding, env);
        //$NON-NLS-1$
        assert !binding.isConstructor() : "binding does not represent a method.";
    }

    public TypeMirror getReturnType() {
        final IMethodBinding methodBinding = getDeclarationBinding();
        final ITypeBinding retType = methodBinding.getReturnType();
        final TypeMirror type = Factory.createTypeMirror(retType, _env);
        if (type == null)
            return Factory.createErrorClassType(retType);
        return type;
    }

    public void accept(DeclarationVisitor visitor) {
        visitor.visitMethodDeclaration(this);
    }

    public MirrorKind kind() {
        return MirrorKind.METHOD;
    }

    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        final IMethodBinding methodBinding = getDeclarationBinding();
        final ITypeBinding[] typeParams = methodBinding.getTypeParameters();
        if (typeParams != null && typeParams.length > 0) {
            buffer.append('<');
            for (int i = 0; i < typeParams.length; i++) {
                if (i != 0)
                    //$NON-NLS-1$
                    buffer.append(", ");
                buffer.append(typeParams[i]);
            }
            buffer.append('>');
        }
        if (methodBinding.getReturnType() != null)
            buffer.append(methodBinding.getReturnType().getName());
        buffer.append(' ');
        buffer.append(methodBinding.getName());
        buffer.append('(');
        int i = 0;
        for (ParameterDeclaration param : getParameters()) {
            if (i++ != 0)
                //$NON-NLS-1$
                buffer.append(", ");
            buffer.append(param);
        }
        buffer.append(')');
        return buffer.toString();
    }
}
