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
package org.eclipse.jdt.internal.compiler.parser;

/**
 * Internal local variable structure for parsing recovery 
 */
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Statement;

public class RecoveredLocalVariable extends RecoveredStatement {

    public LocalDeclaration localDeclaration;

    boolean alreadyCompletedLocalInitialization;

    public  RecoveredLocalVariable(LocalDeclaration localDeclaration, RecoveredElement parent, int bracketBalance) {
        super(localDeclaration, parent, bracketBalance);
        this.localDeclaration = localDeclaration;
        this.alreadyCompletedLocalInitialization = localDeclaration.initialization != null;
    }

    /*
 * Record an expression statement if local variable is expecting an initialization expression. 
 */
    public RecoveredElement add(Statement stmt, int bracketBalanceValue) {
        if (this.alreadyCompletedLocalInitialization || !(stmt instanceof Expression)) {
            return super.add(stmt, bracketBalanceValue);
        } else {
            this.alreadyCompletedLocalInitialization = true;
            this.localDeclaration.initialization = (Expression) stmt;
            this.localDeclaration.declarationSourceEnd = stmt.sourceEnd;
            this.localDeclaration.declarationEnd = stmt.sourceEnd;
            return this;
        }
    }

    /* 
 * Answer the associated parsed structure
 */
    public ASTNode parseTree() {
        return localDeclaration;
    }

    /*
 * Answer the very source end of the corresponding parse node
 */
    public int sourceEnd() {
        return this.localDeclaration.declarationSourceEnd;
    }

    public String toString(int tab) {
        //$NON-NLS-1$
        return tabString(tab) + "Recovered local variable:\n" + localDeclaration.print(tab + 1, new StringBuffer(10));
    }

    public Statement updatedStatement() {
        return localDeclaration;
    }

    /*
 * A closing brace got consumed, might have closed the current element,
 * in which case both the currentElement is exited.
 *
 * Fields have no associated braces, thus if matches, then update parent.
 */
    public RecoveredElement updateOnClosingBrace(int braceStart, int braceEnd) {
        if (// was an array initializer
        bracketBalance > 0) {
            bracketBalance--;
            if (bracketBalance == 0)
                alreadyCompletedLocalInitialization = true;
            return this;
        }
        if (parent != null) {
            return parent.updateOnClosingBrace(braceStart, braceEnd);
        }
        return this;
    }

    /*
 * An opening brace got consumed, might be the expected opening one of the current element,
 * in which case the bodyStart is updated.
 */
    public RecoveredElement updateOnOpeningBrace(int braceStart, int braceEnd) {
        if (localDeclaration.declarationSourceEnd == 0 && localDeclaration.type instanceof ArrayTypeReference && !alreadyCompletedLocalInitialization) {
            bracketBalance++;
            // no update is necessary	(array initializer)
            return null;
        }
        // might be an array initializer
        this.updateSourceEndIfNecessary(braceStart - 1, braceEnd - 1);
        return this.parent.updateOnOpeningBrace(braceStart, braceEnd);
    }

    public void updateParseTree() {
        this.updatedStatement();
    }

    /*
 * Update the declarationSourceEnd of the corresponding parse node
 */
    public void updateSourceEndIfNecessary(int bodyStart, int bodyEnd) {
        if (this.localDeclaration.declarationSourceEnd == 0) {
            this.localDeclaration.declarationSourceEnd = bodyEnd;
            this.localDeclaration.declarationEnd = bodyEnd;
        }
    }
}
