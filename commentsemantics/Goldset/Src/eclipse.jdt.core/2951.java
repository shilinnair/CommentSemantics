/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Herrmann - Contribution for Bug 464615 - [dom] ASTParser.createBindings() ignores parameterization of a method invocation
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

/**
 * Handle representing a binary field that is resolved.
 * The uniqueKey contains the genericSignature of the resolved field. Use BindingKey to decode it.
 */
public class ResolvedBinaryField extends BinaryField {

    private String uniqueKey;

    /*
	 * See class comments.
	 */
    public  ResolvedBinaryField(JavaElement parent, String name, String uniqueKey) {
        super(parent, name);
        this.uniqueKey = uniqueKey;
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.core.BinaryField#getKey()
	 */
    public String getKey() {
        return this.uniqueKey;
    }

    public String getKey(boolean forceOpen) {
        return this.uniqueKey;
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IField#isResolved()
	 */
    public boolean isResolved() {
        return true;
    }

    /**
	 * @private Debugging purposes
	 */
    protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
        super.toStringInfo(tab, buffer, info, showResolvedInfo);
        if (showResolvedInfo) {
            //$NON-NLS-1$
            buffer.append(" {key=");
            buffer.append(this.uniqueKey);
            //$NON-NLS-1$
            buffer.append("}");
        }
    }

    public JavaElement unresolved() {
        SourceRefElement handle = new BinaryField(this.parent, this.name);
        handle.occurrenceCount = this.occurrenceCount;
        return handle;
    }
}
