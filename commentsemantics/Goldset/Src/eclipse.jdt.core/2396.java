/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist.complete;

import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class CompletionOnKeyword3 extends SingleNameReference implements CompletionOnKeyword {

    private char[][] possibleKeywords;

    private boolean tryOrCatch;

    public  CompletionOnKeyword3(char[] token, long pos, char[] possibleKeyword) {
        this(token, pos, new char[][] { possibleKeyword }, false);
    }

    public  CompletionOnKeyword3(char[] token, long pos, char[][] possibleKeywords, boolean afterTryOrCatch) {
        super(token, pos);
        this.token = token;
        this.possibleKeywords = possibleKeywords;
        this.tryOrCatch = afterTryOrCatch;
    }

    public char[] getToken() {
        return this.token;
    }

    public char[][] getPossibleKeywords() {
        return this.possibleKeywords;
    }

    public boolean afterTryOrCatch() {
        return this.tryOrCatch;
    }

    public StringBuffer printExpression(int indent, StringBuffer output) {
        //$NON-NLS-1$
        return output.append("<CompleteOnKeyword:").append(this.token).append('>');
    }

    public TypeBinding resolveType(BlockScope scope) {
        throw new CompletionNodeFound(this, scope);
    }
}
