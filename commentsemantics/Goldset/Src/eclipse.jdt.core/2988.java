/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;

public class SignatureWrapper {

    public char[] signature;

    public int start;

    public int end;

    public int bracket;

    public  SignatureWrapper(char[] signature) {
        this.signature = signature;
        this.start = 0;
        this.end = this.bracket = -1;
    }

    public boolean atEnd() {
        return this.start < 0 || this.start >= this.signature.length;
    }

    public int computeEnd() {
        int index = this.start;
        while (this.signature[index] == '[') index++;
        switch(this.signature[index]) {
            case 'L':
            case 'T':
                this.end = CharOperation.indexOf(';', this.signature, this.start);
                if (// already know it if its > start
                this.bracket <= this.start)
                    this.bracket = CharOperation.indexOf('<', this.signature, this.start);
                if (this.bracket > this.start && this.bracket < this.end)
                    this.end = this.bracket;
                else if (this.end == -1)
                    this.end = this.signature.length + 1;
                break;
            default:
                this.end = this.start;
        }
        // skip ';'
        this.start = this.end + 1;
        return this.end;
    }

    public char[] nextWord() {
        this.end = CharOperation.indexOf(';', this.signature, this.start);
        if (// already know it if its > start
        this.bracket <= this.start)
            this.bracket = CharOperation.indexOf('<', this.signature, this.start);
        int dot = CharOperation.indexOf('.', this.signature, this.start);
        if (this.bracket > this.start && this.bracket < this.end)
            this.end = this.bracket;
        if (dot > this.start && dot < this.end)
            this.end = dot;
        // skip word
        return CharOperation.subarray(this.signature, this.start, this.start = this.end);
    }

    public String toString() {
        //$NON-NLS-1$
        return new String(this.signature) + " @ " + this.start;
    }
}
