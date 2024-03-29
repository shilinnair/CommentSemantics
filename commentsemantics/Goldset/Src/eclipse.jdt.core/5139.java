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
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.*;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.parser.*;
import org.eclipse.jdt.internal.compiler.problem.*;

public class Clinit extends AbstractMethodDeclaration {

    //$NON-NLS-1$
    public static final char[] ConstantPoolName = "<clinit>".toCharArray();

    private FieldBinding assertionSyntheticFieldBinding = null;

    private FieldBinding classLiteralSyntheticField = null;

    public  Clinit(CompilationResult compilationResult) {
        super(compilationResult);
        modifiers = 0;
        selector = ConstantPoolName;
    }

    public void analyseCode(ClassScope classScope, InitializationFlowContext staticInitializerFlowContext, FlowInfo flowInfo) {
        if (ignoreFurtherInvestigation)
            return;
        try {
            ExceptionHandlingFlowContext clinitContext = new ExceptionHandlingFlowContext(staticInitializerFlowContext.parent, this, NoExceptions, scope, FlowInfo.DEAD_END);
            // check for missing returning path
            this.needFreeReturn = flowInfo.isReachable();
            // check missing blank final field initializations
            flowInfo = flowInfo.mergedWith(staticInitializerFlowContext.initsOnReturn);
            FieldBinding[] fields = scope.enclosingSourceType().fields();
            for (int i = 0, count = fields.length; i < count; i++) {
                FieldBinding field;
                if ((field = fields[i]).isStatic() && field.isFinal() && (!flowInfo.isDefinitelyAssigned(fields[i]))) {
                    scope.problemReporter().uninitializedBlankFinalField(field, scope.referenceType().declarationOf(field.original()));
                // can complain against the field decl, since only one <clinit>
                }
            }
            // check static initializers thrown exceptions
            staticInitializerFlowContext.checkInitializerExceptions(scope, clinitContext, flowInfo);
        } catch (AbortMethod e) {
            this.ignoreFurtherInvestigation = true;
        }
    }

    /**
	 * Bytecode generation for a <clinit> method
	 *
	 * @param classScope org.eclipse.jdt.internal.compiler.lookup.ClassScope
	 * @param classFile org.eclipse.jdt.internal.compiler.codegen.ClassFile
	 */
    public void generateCode(ClassScope classScope, ClassFile classFile) {
        int clinitOffset = 0;
        if (ignoreFurtherInvestigation) {
            // should never have to add any <clinit> problem method
            return;
        }
        try {
            clinitOffset = classFile.contentsOffset;
            this.generateCode(classScope, classFile, clinitOffset);
        } catch (AbortMethod e) {
            if (e.compilationResult == CodeStream.RESTART_IN_WIDE_MODE) {
                try {
                    classFile.contentsOffset = clinitOffset;
                    classFile.methodCount--;
                    classFile.codeStream.wideMode = true;
                    this.generateCode(classScope, classFile, clinitOffset);
                } catch (AbortMethod e2) {
                    classFile.contentsOffset = clinitOffset;
                    classFile.methodCount--;
                }
            } else {
                classFile.contentsOffset = clinitOffset;
                classFile.methodCount--;
            }
        }
    }

    /**
	 * Bytecode generation for a <clinit> method
	 *
	 * @param classScope org.eclipse.jdt.internal.compiler.lookup.ClassScope
	 * @param classFile org.eclipse.jdt.internal.compiler.codegen.ClassFile
	 */
    private void generateCode(ClassScope classScope, ClassFile classFile, int clinitOffset) {
        ConstantPool constantPool = classFile.constantPool;
        int constantPoolOffset = constantPool.currentOffset;
        int constantPoolIndex = constantPool.currentIndex;
        classFile.generateMethodInfoHeaderForClinit();
        int codeAttributeOffset = classFile.contentsOffset;
        classFile.generateCodeAttributeHeader();
        CodeStream codeStream = classFile.codeStream;
        this.resolve(classScope);
        codeStream.reset(this, classFile);
        TypeDeclaration declaringType = classScope.referenceContext;
        // initialize local positions - including initializer scope.
        MethodScope staticInitializerScope = declaringType.staticInitializerScope;
        staticInitializerScope.computeLocalVariablePositions(0, codeStream);
        // This has to be done before any other initialization
        if (this.assertionSyntheticFieldBinding != null) {
            // generate code related to the activation of assertion for this class
            codeStream.generateClassLiteralAccessForType(classScope.enclosingSourceType(), classLiteralSyntheticField);
            codeStream.invokeJavaLangClassDesiredAssertionStatus();
            Label falseLabel = new Label(codeStream);
            codeStream.ifne(falseLabel);
            codeStream.iconst_1();
            Label jumpLabel = new Label(codeStream);
            codeStream.goto_(jumpLabel);
            falseLabel.place();
            codeStream.iconst_0();
            jumpLabel.place();
            codeStream.putstatic(this.assertionSyntheticFieldBinding);
        }
        // generate initializers
        if (declaringType.fields != null) {
            for (int i = 0, max = declaringType.fields.length; i < max; i++) {
                FieldDeclaration fieldDecl;
                if ((fieldDecl = declaringType.fields[i]).isStatic()) {
                    fieldDecl.generateCode(staticInitializerScope, codeStream);
                }
            }
        }
        if (codeStream.position == 0) {
            // do not need to output a Clinit if no bytecodes
            // so we reset the offset inside the byte array contents.
            classFile.contentsOffset = clinitOffset;
            // like we don't addd a method we need to undo the increment on the method count
            classFile.methodCount--;
            // reset the constant pool to its state before the clinit
            constantPool.resetForClinit(constantPoolIndex, constantPoolOffset);
        } else {
            if (this.needFreeReturn) {
                int oldPosition = codeStream.position;
                codeStream.return_();
                codeStream.updateLocalVariablesAttribute(oldPosition);
            }
            // Record the end of the clinit: point to the declaration of the class
            codeStream.recordPositionsFrom(0, declaringType.sourceStart);
            classFile.completeCodeAttributeForClinit(codeAttributeOffset);
        }
    }

    public boolean isClinit() {
        return true;
    }

    public boolean isInitializationMethod() {
        return true;
    }

    public boolean isStatic() {
        return true;
    }

    public void parseStatements(Parser parser, CompilationUnitDeclaration unit) {
    //the clinit is filled by hand .... 
    }

    public StringBuffer print(int tab, StringBuffer output) {
        //$NON-NLS-1$
        printIndent(tab, output).append("<clinit>()");
        printBody(tab + 1, output);
        return output;
    }

    public void resolve(ClassScope classScope) {
        this.scope = new MethodScope(classScope, classScope.referenceContext, true);
    }

    public void traverse(ASTVisitor visitor, ClassScope classScope) {
        visitor.visit(this, classScope);
        visitor.endVisit(this, classScope);
    }

    // 1.4 feature
    public void setAssertionSupport(FieldBinding assertionSyntheticFieldBinding, boolean needClassLiteralField) {
        this.assertionSyntheticFieldBinding = assertionSyntheticFieldBinding;
        // we need to add the field right now, because the field infos are generated before the methods
        SourceTypeBinding sourceType = this.scope.outerMostMethodScope().enclosingSourceType();
        if (needClassLiteralField) {
            this.classLiteralSyntheticField = sourceType.addSyntheticField(sourceType, scope);
        }
    }
}
