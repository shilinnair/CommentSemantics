/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist.complete;

import org.eclipse.jdt.internal.compiler.ast.JavadocSingleTypeReference;

public class CompletionOnJavadocSingleTypeReference extends JavadocSingleTypeReference implements CompletionOnJavadoc {

    public int completionFlags = JAVADOC;

    public  CompletionOnJavadocSingleTypeReference(char[] source, long pos, int tagStart, int tagEnd) {
        super(source, pos, tagStart, tagEnd);
    }

    public  CompletionOnJavadocSingleTypeReference(JavadocSingleTypeReference typeRef) {
        super(typeRef.token, (((long) typeRef.sourceStart) << 32) + typeRef.sourceEnd, typeRef.tagSourceStart, typeRef.tagSourceStart);
    }

    /**
	 * @param flags The completionFlags to set.
	 */
    public void addCompletionFlags(int flags) {
        this.completionFlags |= flags;
    }

    public boolean completeAnException() {
        return (this.completionFlags & EXCEPTION) != 0;
    }

    public boolean completeInText() {
        return (this.completionFlags & TEXT) != 0;
    }

    public boolean completeBaseTypes() {
        return (this.completionFlags & BASE_TYPES) != 0;
    }

    public boolean completeFormalReference() {
        return (this.completionFlags & FORMAL_REFERENCE) != 0;
    }

    /**
	 * Get completion node flags.
	 *
	 * @return int Flags of the javadoc completion node.
	 */
    public int getCompletionFlags() {
        return this.completionFlags;
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.ast.SingleTypeReference#printExpression(int, java.lang.StringBuffer)
	 */
    public StringBuffer printExpression(int indent, StringBuffer output) {
        //$NON-NLS-1$
        output.append("<CompletionOnJavadocSingleTypeReference:");
        super.printExpression(indent, output);
        indent++;
        if (this.completionFlags > 0) {
            output.append('\n');
            for (int i = 0; i < indent; i++) output.append('\t');
            //$NON-NLS-1$
            output.append("infos:");
            char separator = 0;
            if (completeAnException()) {
                //$NON-NLS-1$
                output.append(//$NON-NLS-1$
                "exception");
                separator = ',';
            }
            if (completeInText()) {
                if (separator != 0)
                    output.append(separator);
                //$NON-NLS-1$
                output.append(//$NON-NLS-1$
                "text");
                separator = ',';
            }
            if (completeBaseTypes()) {
                if (separator != 0)
                    output.append(separator);
                //$NON-NLS-1$
                output.append(//$NON-NLS-1$
                "base types");
                separator = ',';
            }
            if (completeFormalReference()) {
                if (separator != 0)
                    output.append(separator);
                //$NON-NLS-1$
                output.append(//$NON-NLS-1$
                "formal reference");
                separator = ',';
            }
            output.append('\n');
        }
        indent--;
        for (int i = 0; i < indent; i++) output.append('\t');
        return output.append('>');
    }
}
