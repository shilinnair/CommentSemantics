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
package org.eclipse.jdt.internal.codeassist.select;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class SelectionOnQualifiedTypeReference extends QualifiedTypeReference {

    public  SelectionOnQualifiedTypeReference(char[][] previousIdentifiers, char[] selectionIdentifier, long[] positions) {
        super(CharOperation.arrayConcat(previousIdentifiers, selectionIdentifier), positions);
    }

    public void aboutToResolve(Scope scope) {
        // step up from the ClassScope
        getTypeBinding(scope.parent);
    }

    protected TypeBinding getTypeBinding(Scope scope) {
        // it can be a package, type or member type
        Binding binding = scope.getTypeOrPackage(this.tokens);
        if (!binding.isValidBinding()) {
            // tolerate some error cases
            if (binding.problemId() == ProblemReasons.NotVisible) {
                throw new SelectionNodeFound(binding);
            }
            if (binding instanceof TypeBinding) {
                scope.problemReporter().invalidType(this, (TypeBinding) binding);
            } else if (binding instanceof PackageBinding) {
                ProblemReferenceBinding problemBinding = new ProblemReferenceBinding(((PackageBinding) binding).compoundName, null, binding.problemId());
                scope.problemReporter().invalidType(this, problemBinding);
            }
            throw new SelectionNodeFound();
        }
        throw new SelectionNodeFound(binding);
    }

    public StringBuffer printExpression(int indent, StringBuffer output) {
        //$NON-NLS-1$
        output.append("<SelectOnType:");
        for (int i = 0, length = this.tokens.length; i < length; i++) {
            if (i > 0)
                output.append('.');
            output.append(this.tokens[i]);
        }
        return output.append('>');
    }
}
