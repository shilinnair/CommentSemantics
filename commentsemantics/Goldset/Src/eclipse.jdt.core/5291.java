/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist.complete;

import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class CompletionOnQualifiedNameReference extends QualifiedNameReference {

    public char[] completionIdentifier;

    public boolean isInsideAnnotationAttribute;

    public  CompletionOnQualifiedNameReference(char[][] previousIdentifiers, char[] completionIdentifier, long[] positions, boolean isInsideAnnotationAttribute) {
        super(previousIdentifiers, positions, (int) (positions[0] >>> 32), (int) positions[positions.length - 1]);
        this.completionIdentifier = completionIdentifier;
        this.isInsideAnnotationAttribute = isInsideAnnotationAttribute;
    }

    public StringBuffer printExpression(int indent, StringBuffer output) {
        //$NON-NLS-1$
        output.append("<CompleteOnName:");
        for (int i = 0; i < this.tokens.length; i++) {
            output.append(this.tokens[i]);
            output.append('.');
        }
        output.append(this.completionIdentifier).append('>');
        return output;
    }

    public TypeBinding resolveType(BlockScope scope) {
        // it can be a package, type, member type, local variable or field
        this.binding = scope.getBinding(this.tokens, this);
        if (!this.binding.isValidBinding()) {
            if (this.binding instanceof ProblemFieldBinding) {
                scope.problemReporter().invalidField(this, (FieldBinding) this.binding);
            } else if (this.binding instanceof ProblemReferenceBinding || this.binding instanceof MissingTypeBinding) {
                scope.problemReporter().invalidType(this, (TypeBinding) this.binding);
            } else {
                scope.problemReporter().unresolvableReference(this, this.binding);
            }
            if (this.binding.problemId() == ProblemReasons.NotFound) {
                throw new CompletionNodeFound(this, this.binding, scope);
            }
            throw new CompletionNodeFound();
        }
        throw new CompletionNodeFound(this, this.binding, scope);
    }
}
