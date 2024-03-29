/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist.complete;

import org.eclipse.jdt.internal.compiler.ast.ReferenceExpression;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class CompletionOnReferenceExpressionName extends ReferenceExpression {

    @Override
    public TypeBinding resolveType(BlockScope scope) {
        final CompilerOptions compilerOptions = scope.compilerOptions();
        TypeBinding lhsType;
        boolean typeArgumentsHaveErrors;
        this.constant = Constant.NotAConstant;
        lhsType = this.lhs.resolveType(scope);
        if (this.typeArguments != null) {
            int length = this.typeArguments.length;
            typeArgumentsHaveErrors = compilerOptions.sourceLevel < ClassFileConstants.JDK1_5;
            this.resolvedTypeArguments = new TypeBinding[length];
            for (int i = 0; i < length; i++) {
                TypeReference typeReference = this.typeArguments[i];
                if ((this.resolvedTypeArguments[i] = typeReference.resolveType(/* check bounds*/
                scope, true)) == null) {
                    typeArgumentsHaveErrors = true;
                }
                if (// resolveType on wildcard always return null above, resolveTypeArgument is the real workhorse.
                typeArgumentsHaveErrors && typeReference instanceof Wildcard) {
                    scope.problemReporter().illegalUsageOfWildcard(typeReference);
                }
            }
            if (typeArgumentsHaveErrors || lhsType == null)
                throw new CompletionNodeFound();
        }
        if (lhsType != null && lhsType.isValidBinding())
            throw new CompletionNodeFound(this, lhsType, scope);
        throw new CompletionNodeFound();
    }

    @Override
    public StringBuffer printExpression(int tab, StringBuffer output) {
        //$NON-NLS-1$
        output.append("<CompletionOnReferenceExpressionName:");
        this.lhs.print(0, output);
        //$NON-NLS-1$
        output.append("::");
        if (this.typeArguments != null) {
            output.append('<');
            int max = this.typeArguments.length - 1;
            for (int j = 0; j < max; j++) {
                this.typeArguments[j].print(0, output);
                //$NON-NLS-1$
                output.append(//$NON-NLS-1$
                ", ");
            }
            this.typeArguments[max].print(0, output);
            output.append('>');
        }
        output.append(this.selector);
        return output.append('>');
    }
}
