/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.debug.eval.ast.instructions;

public class RightShiftAssignmentOperator extends RightShiftOperator {

    public  RightShiftAssignmentOperator(int variableTypeId, int valueTypeId, int start) {
        super(variableTypeId, variableTypeId, valueTypeId, true, start);
    }

    @Override
    public String toString() {
        return InstructionsEvaluationMessages.RightShiftAssignmentOperator_operator_1;
    }
}
