/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Herrmann - Contribution for
 *								bug 331649 - [compiler][null] consider null annotations for fields
 *								bug 383368 - [compiler][null] syntactic null analysis for field references
 *     Jesper S Moller - Contributions for
 *								Bug 378674 - "The method can be declared as static" is wrong
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.FlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class ThisReference extends Reference {

    public static ThisReference implicitThis() {
        ThisReference implicitThis = new ThisReference(0, 0);
        implicitThis.bits |= IsImplicitThis;
        return implicitThis;
    }

    public  ThisReference(int sourceStart, int sourceEnd) {
        this.sourceStart = sourceStart;
        this.sourceEnd = sourceEnd;
    }

    /*
	 * @see Reference#analyseAssignment(...)
	 */
    public FlowInfo analyseAssignment(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, Assignment assignment, boolean isCompound) {
        // this cannot be assigned
        return flowInfo;
    }

    public boolean checkAccess(BlockScope scope, ReferenceBinding receiverType) {
        MethodScope methodScope = scope.methodScope();
        // this/super cannot be used in constructor call
        if (methodScope.isConstructorCall) {
            methodScope.problemReporter().fieldsOrThisBeforeConstructorInvocation(this);
            return false;
        }
        // static may not refer to this/super
        if (methodScope.isStatic) {
            methodScope.problemReporter().errorThisSuperInStatic(this);
            return false;
        } else if (this.isUnqualifiedSuper()) {
            TypeDeclaration type = methodScope.referenceType();
            if (type != null && TypeDeclaration.kind(type.modifiers) == TypeDeclaration.INTERFACE_DECL) {
                methodScope.problemReporter().errorNoSuperInInterface(this);
                return false;
            }
        }
        if (receiverType != null)
            scope.tagAsAccessingEnclosingInstanceStateOf(receiverType, /* type variable access */
            false);
        return true;
    }

    public boolean checkNPE(BlockScope scope, FlowContext flowContext, FlowInfo flowInfo, int ttlForFieldCheck) {
        // never problematic
        return true;
    }

    /*
	 * @see Reference#generateAssignment(...)
	 */
    public void generateAssignment(BlockScope currentScope, CodeStream codeStream, Assignment assignment, boolean valueRequired) {
    // this cannot be assigned
    }

    public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
        int pc = codeStream.position;
        if (valueRequired)
            codeStream.aload_0();
        if ((this.bits & IsImplicitThis) == 0)
            codeStream.recordPositionsFrom(pc, this.sourceStart);
    }

    /*
	 * @see Reference#generateCompoundAssignment(...)
	 */
    public void generateCompoundAssignment(BlockScope currentScope, CodeStream codeStream, Expression expression, int operator, int assignmentImplicitConversion, boolean valueRequired) {
    // this cannot be assigned
    }

    /*
	 * @see org.eclipse.jdt.internal.compiler.ast.Reference#generatePostIncrement()
	 */
    public void generatePostIncrement(BlockScope currentScope, CodeStream codeStream, CompoundAssignment postIncrement, boolean valueRequired) {
    // this cannot be assigned
    }

    public boolean isImplicitThis() {
        return (this.bits & IsImplicitThis) != 0;
    }

    public boolean isThis() {
        return true;
    }

    public int nullStatus(FlowInfo flowInfo, FlowContext flowContext) {
        return FlowInfo.NON_NULL;
    }

    public StringBuffer printExpression(int indent, StringBuffer output) {
        if (isImplicitThis())
            return output;
        //$NON-NLS-1$
        return output.append("this");
    }

    public TypeBinding resolveType(BlockScope scope) {
        this.constant = Constant.NotAConstant;
        ReferenceBinding enclosingReceiverType = scope.enclosingReceiverType();
        if (!isImplicitThis() && !checkAccess(scope, enclosingReceiverType)) {
            return null;
        }
        this.resolvedType = enclosingReceiverType;
        MethodScope methodScope = scope.namedMethodScope();
        if (methodScope != null) {
            MethodBinding method = methodScope.referenceMethodBinding();
            if (method != null && method.receiver != null && TypeBinding.equalsEquals(method.receiver, this.resolvedType))
                this.resolvedType = method.receiver;
        }
        return this.resolvedType;
    }

    public void traverse(ASTVisitor visitor, BlockScope blockScope) {
        visitor.visit(this, blockScope);
        visitor.endVisit(this, blockScope);
    }

    public void traverse(ASTVisitor visitor, ClassScope blockScope) {
        visitor.visit(this, blockScope);
        visitor.endVisit(this, blockScope);
    }
}
