/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Herrmann - Contribution for
 *								bug 349326 - [1.7] new warning for missing try-with-resources
 *								bug 370930 - NonNull annotation not considered for enhanced for loops
 *								bug 365859 - [compiler][null] distinguish warnings based on flow analysis vs. null annotations
 *								bug 345305 - [compiler][null] Compiler misidentifies a case of "variable can only be null"
 *								bug 393719 - [compiler] inconsistent warnings on iteration variables
 *								Bug 411964 - [1.8][null] leverage null type annotation in foreach statement
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 453483 - [compiler][null][loop] Improve null analysis for loops
 *								Bug 415790 - [compiler][resource]Incorrect potential resource leak warning in for loop with close in try/catch
 *     Jesper S Moller -  Contribution for
 *								bug 401853 - Eclipse Java compiler creates invalid bytecode (java.lang.VerifyError)
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.BranchLabel;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.flow.FlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.flow.LoopingFlowContext;
import org.eclipse.jdt.internal.compiler.flow.UnconditionalFlowInfo;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.CaptureBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;

public class ForeachStatement extends Statement {

    public LocalDeclaration elementVariable;

    public int elementVariableImplicitWidening = -1;

    public Expression collection;

    public Statement action;

    // set the kind of foreach
    private int kind;

    // possible kinds of iterating behavior
    private static final int ARRAY = 0;

    private static final int RAW_ITERABLE = 1;

    private static final int GENERIC_ITERABLE = 2;

    private TypeBinding iteratorReceiverType;

    private TypeBinding collectionElementType;

    // loop labels
    private BranchLabel breakLabel;

    private BranchLabel continueLabel;

    public BlockScope scope;

    // secret variables for codegen
    public LocalVariableBinding indexVariable;

    // to store the collection expression value
    public LocalVariableBinding collectionVariable;

    public LocalVariableBinding maxVariable;

    // secret variable names
    //$NON-NLS-1$
    private static final char[] SecretIteratorVariableName = " iterator".toCharArray();

    //$NON-NLS-1$
    private static final char[] SecretIndexVariableName = " index".toCharArray();

    //$NON-NLS-1$
    private static final char[] SecretCollectionVariableName = " collection".toCharArray();

    //$NON-NLS-1$
    private static final char[] SecretMaxVariableName = " max".toCharArray();

    int postCollectionInitStateIndex = -1;

    int mergedInitStateIndex = -1;

    public  ForeachStatement(LocalDeclaration elementVariable, int start) {
        this.elementVariable = elementVariable;
        this.sourceStart = start;
        this.kind = -1;
    }

    public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
        // initialize break and continue labels
        this.breakLabel = new BranchLabel();
        this.continueLabel = new BranchLabel();
        int initialComplaintLevel = (flowInfo.reachMode() & FlowInfo.UNREACHABLE) != 0 ? Statement.COMPLAINED_FAKE_REACHABLE : Statement.NOT_COMPLAINED;
        // process the element variable and collection
        this.collection.checkNPE(currentScope, flowContext, flowInfo, 1);
        flowInfo = this.elementVariable.analyseCode(this.scope, flowContext, flowInfo);
        FlowInfo condInfo = this.collection.analyseCode(this.scope, flowContext, flowInfo.copy());
        LocalVariableBinding elementVarBinding = this.elementVariable.binding;
        // element variable will be assigned when iterating
        condInfo.markAsDefinitelyAssigned(elementVarBinding);
        this.postCollectionInitStateIndex = currentScope.methodScope().recordInitializationStates(condInfo);
        // process the action
        LoopingFlowContext loopingContext = new LoopingFlowContext(flowContext, flowInfo, this, this.breakLabel, this.continueLabel, this.scope, true);
        UnconditionalFlowInfo actionInfo = condInfo.nullInfoLessUnconditionalCopy();
        actionInfo.markAsDefinitelyUnknown(elementVarBinding);
        if (currentScope.compilerOptions().isAnnotationBasedNullAnalysisEnabled) {
            int elementNullStatus = NullAnnotationMatching.nullStatusFromExpressionType(this.collectionElementType);
            int nullStatus = // have no useful flowinfo for element var
            NullAnnotationMatching.checkAssignment(// have no useful flowinfo for element var
            currentScope, // have no useful flowinfo for element var
            flowContext, // have no useful flowinfo for element var
            elementVarBinding, // have no useful flowinfo for element var
            null, elementNullStatus, this.collection, this.collectionElementType);
            if ((elementVarBinding.type.tagBits & TagBits.IsBaseType) == 0) {
                actionInfo.markNullStatus(elementVarBinding, nullStatus);
            }
        }
        FlowInfo exitBranch;
        if (!(this.action == null || (this.action.isEmptyBlock() && currentScope.compilerOptions().complianceLevel <= ClassFileConstants.JDK1_3))) {
            if (this.action.complainIfUnreachable(actionInfo, this.scope, initialComplaintLevel, true) < Statement.COMPLAINED_UNREACHABLE) {
                actionInfo = this.action.analyseCode(this.scope, loopingContext, actionInfo).unconditionalCopy();
            }
            // code generation can be optimized when no need to continue in the loop
            exitBranch = flowInfo.unconditionalCopy().addInitializationsFrom(condInfo.initsWhenFalse());
            // TODO (maxime) no need to test when false: can optimize (same for action being unreachable above)
            if ((actionInfo.tagBits & loopingContext.initsOnContinue.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) != 0) {
                this.continueLabel = null;
            } else {
                actionInfo = actionInfo.mergedWith(loopingContext.initsOnContinue);
                loopingContext.complainOnDeferredFinalChecks(this.scope, actionInfo);
                exitBranch.addPotentialInitializationsFrom(actionInfo);
            }
        } else {
            exitBranch = condInfo.initsWhenFalse();
        }
        // we need the variable to iterate the collection even if the
        // element variable is not used
        final boolean hasEmptyAction = this.action == null || this.action.isEmptyBlock() || ((this.action.bits & IsUsefulEmptyStatement) != 0);
        switch(this.kind) {
            case ARRAY:
                if (!hasEmptyAction || elementVarBinding.resolvedPosition != -1) {
                    this.collectionVariable.useFlag = LocalVariableBinding.USED;
                    if (this.continueLabel != null) {
                        this.indexVariable.useFlag = LocalVariableBinding.USED;
                        this.maxVariable.useFlag = LocalVariableBinding.USED;
                    }
                }
                break;
            case RAW_ITERABLE:
            case GENERIC_ITERABLE:
                this.indexVariable.useFlag = LocalVariableBinding.USED;
                break;
        }
        //end of loop
        loopingContext.complainOnDeferredNullChecks(currentScope, actionInfo);
        if (// https://bugs.eclipse.org/bugs/show_bug.cgi?id=321926
        loopingContext.hasEscapingExceptions()) {
            FlowInfo loopbackFlowInfo = flowInfo.copy();
            if (// we do get to the bottom
            this.continueLabel != null) {
                // loopback | (loopback + action):
                loopbackFlowInfo = loopbackFlowInfo.mergedWith(loopbackFlowInfo.unconditionalCopy().addNullInfoFrom(actionInfo).unconditionalInits());
            }
            loopingContext.simulateThrowAfterLoopBack(loopbackFlowInfo);
        }
        FlowInfo mergedInfo = FlowInfo.mergedOptimizedBranches((loopingContext.initsOnBreak.tagBits & FlowInfo.UNREACHABLE) != 0 ? loopingContext.initsOnBreak : // recover upstream null info
        flowInfo.addInitializationsFrom(// recover upstream null info
        loopingContext.initsOnBreak), false, exitBranch, false, /*for(;;){}while(true); unreachable(); */
        true);
        mergedInfo.resetAssignmentInfo(this.elementVariable.binding);
        this.mergedInitStateIndex = currentScope.methodScope().recordInitializationStates(mergedInfo);
        return mergedInfo;
    }

    /**
	 * For statement code generation
	 *
	 * @param currentScope org.eclipse.jdt.internal.compiler.lookup.BlockScope
	 * @param codeStream org.eclipse.jdt.internal.compiler.codegen.CodeStream
	 */
    public void generateCode(BlockScope currentScope, CodeStream codeStream) {
        if ((this.bits & IsReachable) == 0) {
            return;
        }
        int pc = codeStream.position;
        final boolean hasEmptyAction = this.action == null || this.action.isEmptyBlock() || ((this.action.bits & IsUsefulEmptyStatement) != 0);
        if (hasEmptyAction && this.elementVariable.binding.resolvedPosition == -1 && this.kind == ARRAY) {
            this.collection.generateCode(this.scope, codeStream, false);
            codeStream.exitUserScope(this.scope);
            if (this.mergedInitStateIndex != -1) {
                codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
                codeStream.addDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
            }
            codeStream.recordPositionsFrom(pc, this.sourceStart);
            return;
        }
        // generate the initializations
        switch(this.kind) {
            case ARRAY:
                this.collection.generateCode(this.scope, codeStream, true);
                codeStream.store(this.collectionVariable, true);
                codeStream.addVariable(this.collectionVariable);
                if (this.continueLabel != null) {
                    // int length = (collectionVariable = [collection]).length;
                    codeStream.arraylength();
                    codeStream.store(this.maxVariable, false);
                    codeStream.addVariable(this.maxVariable);
                    codeStream.iconst_0();
                    codeStream.store(this.indexVariable, false);
                    codeStream.addVariable(this.indexVariable);
                } else {
                // leave collectionVariable on execution stack (will be consumed when swapping condition further down)
                }
                break;
            case RAW_ITERABLE:
            case GENERIC_ITERABLE:
                this.collection.generateCode(this.scope, codeStream, true);
                // declaringClass.iterator();
                codeStream.invokeIterableIterator(this.iteratorReceiverType);
                codeStream.store(this.indexVariable, false);
                codeStream.addVariable(this.indexVariable);
                break;
        }
        // label management
        BranchLabel actionLabel = new BranchLabel(codeStream);
        actionLabel.tagBits |= BranchLabel.USED;
        BranchLabel conditionLabel = new BranchLabel(codeStream);
        conditionLabel.tagBits |= BranchLabel.USED;
        this.breakLabel.initialize(codeStream);
        if (this.continueLabel == null) {
            // generate the condition (swapped for optimizing)
            conditionLabel.place();
            int conditionPC = codeStream.position;
            switch(this.kind) {
                case ARRAY:
                    // inline the arraylength call
                    // collectionVariable is already on execution stack
                    codeStream.arraylength();
                    codeStream.ifeq(this.breakLabel);
                    break;
                case RAW_ITERABLE:
                case GENERIC_ITERABLE:
                    codeStream.load(this.indexVariable);
                    codeStream.invokeJavaUtilIteratorHasNext();
                    codeStream.ifeq(this.breakLabel);
                    break;
            }
            codeStream.recordPositionsFrom(conditionPC, this.elementVariable.sourceStart);
        } else {
            this.continueLabel.initialize(codeStream);
            this.continueLabel.tagBits |= BranchLabel.USED;
            // jump over the actionBlock
            codeStream.goto_(conditionLabel);
        }
        // generate the loop action
        actionLabel.place();
        // generate the loop action
        switch(this.kind) {
            case ARRAY:
                if (this.elementVariable.binding.resolvedPosition != -1) {
                    codeStream.load(this.collectionVariable);
                    if (this.continueLabel == null) {
                        // no continue, thus simply hardcode offset 0
                        codeStream.iconst_0();
                    } else {
                        codeStream.load(this.indexVariable);
                    }
                    codeStream.arrayAt(this.collectionElementType.id);
                    if (this.elementVariableImplicitWidening != -1) {
                        codeStream.generateImplicitConversion(this.elementVariableImplicitWidening);
                    }
                    codeStream.store(this.elementVariable.binding, false);
                    codeStream.addVisibleLocalVariable(this.elementVariable.binding);
                    if (this.postCollectionInitStateIndex != -1) {
                        codeStream.addDefinitelyAssignedVariables(currentScope, this.postCollectionInitStateIndex);
                    }
                }
                break;
            case RAW_ITERABLE:
            case GENERIC_ITERABLE:
                codeStream.load(this.indexVariable);
                codeStream.invokeJavaUtilIteratorNext();
                if (this.elementVariable.binding.type.id != T_JavaLangObject) {
                    if (this.elementVariableImplicitWidening != -1) {
                        codeStream.checkcast(this.collectionElementType);
                        codeStream.generateImplicitConversion(this.elementVariableImplicitWidening);
                    } else {
                        codeStream.checkcast(this.elementVariable.binding.type);
                    }
                }
                if (this.elementVariable.binding.resolvedPosition == -1) {
                    switch(this.elementVariable.binding.type.id) {
                        case TypeIds.T_long:
                        case TypeIds.T_double:
                            codeStream.pop2();
                            break;
                        default:
                            codeStream.pop();
                            break;
                    }
                } else {
                    codeStream.store(this.elementVariable.binding, false);
                    codeStream.addVisibleLocalVariable(this.elementVariable.binding);
                    if (this.postCollectionInitStateIndex != -1) {
                        codeStream.addDefinitelyAssignedVariables(currentScope, this.postCollectionInitStateIndex);
                    }
                }
                break;
        }
        if (!hasEmptyAction) {
            this.action.generateCode(this.scope, codeStream);
        }
        codeStream.removeVariable(this.elementVariable.binding);
        if (this.postCollectionInitStateIndex != -1) {
            codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.postCollectionInitStateIndex);
        }
        // continuation point
        if (this.continueLabel != null) {
            this.continueLabel.place();
            int continuationPC = codeStream.position;
            // generate the increments for next iteration
            switch(this.kind) {
                case ARRAY:
                    if (!hasEmptyAction || this.elementVariable.binding.resolvedPosition >= 0) {
                        codeStream.iinc(this.indexVariable.resolvedPosition, 1);
                    }
                    // generate the condition
                    conditionLabel.place();
                    codeStream.load(this.indexVariable);
                    codeStream.load(this.maxVariable);
                    codeStream.if_icmplt(actionLabel);
                    break;
                case RAW_ITERABLE:
                case GENERIC_ITERABLE:
                    // generate the condition
                    conditionLabel.place();
                    codeStream.load(this.indexVariable);
                    codeStream.invokeJavaUtilIteratorHasNext();
                    codeStream.ifne(actionLabel);
                    break;
            }
            codeStream.recordPositionsFrom(continuationPC, this.elementVariable.sourceStart);
        }
        switch(this.kind) {
            case ARRAY:
                codeStream.removeVariable(this.indexVariable);
                codeStream.removeVariable(this.maxVariable);
                codeStream.removeVariable(this.collectionVariable);
                break;
            case RAW_ITERABLE:
            case GENERIC_ITERABLE:
                // generate the condition
                codeStream.removeVariable(this.indexVariable);
                break;
        }
        codeStream.exitUserScope(this.scope);
        if (this.mergedInitStateIndex != -1) {
            codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
            codeStream.addDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
        }
        this.breakLabel.place();
        codeStream.recordPositionsFrom(pc, this.sourceStart);
    }

    public StringBuffer printStatement(int indent, StringBuffer output) {
        //$NON-NLS-1$
        printIndent(indent, output).append("for (");
        this.elementVariable.printAsExpression(0, output);
        //$NON-NLS-1$
        output.append(" : ");
        if (this.collection != null) {
            //$NON-NLS-1$
            this.collection.print(0, output).append(") ");
        } else {
            output.append(')');
        }
        //block
        if (this.action == null) {
            output.append(';');
        } else {
            output.append('\n');
            this.action.printStatement(indent + 1, output);
        }
        return output;
    }

    public void resolve(BlockScope upperScope) {
        // use the scope that will hold the init declarations
        this.scope = new BlockScope(upperScope);
        // collection expression can see itemVariable
        this.elementVariable.resolve(this.scope);
        TypeBinding elementType = this.elementVariable.type.resolvedType;
        TypeBinding collectionType = this.collection == null ? null : this.collection.resolveType(upperScope);
        TypeBinding expectedCollectionType = null;
        if (elementType != null && collectionType != null) {
            boolean isTargetJsr14 = this.scope.compilerOptions().targetJDK == ClassFileConstants.JDK1_4;
            if (collectionType.isCapture()) {
                TypeBinding upperBound = ((CaptureBinding) collectionType).firstBound;
                if (upperBound != null && upperBound.isArrayType())
                    // partially anticipating the fix for https://bugs.openjdk.java.net/browse/JDK-8013843
                    collectionType = upperBound;
            }
            if (// for(E e : E[])
            collectionType.isArrayType()) {
                this.kind = ARRAY;
                this.collectionElementType = ((ArrayBinding) collectionType).elementsType();
                if (!this.collectionElementType.isCompatibleWith(elementType) && !this.scope.isBoxingCompatibleWith(this.collectionElementType, elementType)) {
                    this.scope.problemReporter().notCompatibleTypesErrorInForeach(this.collection, this.collectionElementType, elementType);
                } else if (// https://bugs.eclipse.org/bugs/show_bug.cgi?id=321085
                this.collectionElementType.needsUncheckedConversion(elementType)) {
                    this.scope.problemReporter().unsafeElementTypeConversion(this.collection, this.collectionElementType, elementType);
                }
                // in case we need to do a conversion
                int compileTimeTypeID = this.collectionElementType.id;
                if (elementType.isBaseType()) {
                    this.collection.computeConversion(this.scope, collectionType, collectionType);
                    if (!this.collectionElementType.isBaseType()) {
                        compileTimeTypeID = this.scope.environment().computeBoxingType(this.collectionElementType).id;
                        this.elementVariableImplicitWidening = UNBOXING;
                        if (elementType.isBaseType()) {
                            this.elementVariableImplicitWidening |= (elementType.id << 4) + compileTimeTypeID;
                            this.scope.problemReporter().autoboxing(this.collection, this.collectionElementType, elementType);
                        }
                    } else {
                        this.elementVariableImplicitWidening = (elementType.id << 4) + compileTimeTypeID;
                    }
                } else if (this.collectionElementType.isBaseType()) {
                    this.collection.computeConversion(this.scope, collectionType, collectionType);
                    int boxedID = this.scope.environment().computeBoxingType(this.collectionElementType).id;
                    // use primitive type in implicit conversion
                    this.elementVariableImplicitWidening = BOXING | (compileTimeTypeID << 4) | compileTimeTypeID;
                    compileTimeTypeID = boxedID;
                    this.scope.problemReporter().autoboxing(this.collection, this.collectionElementType, elementType);
                } else {
                    expectedCollectionType = upperScope.createArrayType(elementType, 1);
                    this.collection.computeConversion(this.scope, expectedCollectionType, collectionType);
                }
            } else if (collectionType instanceof ReferenceBinding) {
                ReferenceBinding iterableType = ((ReferenceBinding) collectionType).findSuperTypeOriginatingFrom(T_JavaLangIterable, /*Iterable is not a class*/
                false);
                if (iterableType == null && isTargetJsr14) {
                    iterableType = ((ReferenceBinding) collectionType).findSuperTypeOriginatingFrom(T_JavaUtilCollection, /*Iterable is not a class*/
                    false);
                }
                checkIterable: {
                    if (iterableType == null)
                        break checkIterable;
                    this.iteratorReceiverType = collectionType.erasure();
                    if (isTargetJsr14) {
                        if (((ReferenceBinding) this.iteratorReceiverType).findSuperTypeOriginatingFrom(T_JavaUtilCollection, false) == null) {
                            // handle indirect inheritance thru variable secondary bound
                            this.iteratorReceiverType = iterableType;
                            this.collection.computeConversion(this.scope, iterableType, collectionType);
                        } else {
                            this.collection.computeConversion(this.scope, collectionType, collectionType);
                        }
                    } else if (((ReferenceBinding) this.iteratorReceiverType).findSuperTypeOriginatingFrom(T_JavaLangIterable, false) == null) {
                        // handle indirect inheritance thru variable secondary bound
                        this.iteratorReceiverType = iterableType;
                        this.collection.computeConversion(this.scope, iterableType, collectionType);
                    } else {
                        this.collection.computeConversion(this.scope, collectionType, collectionType);
                    }
                    TypeBinding[] arguments = null;
                    switch(iterableType.kind()) {
                        case // for(Object o : Iterable)
                        Binding.RAW_TYPE:
                            this.kind = RAW_ITERABLE;
                            this.collectionElementType = this.scope.getJavaLangObject();
                            if (!this.collectionElementType.isCompatibleWith(elementType) && !this.scope.isBoxingCompatibleWith(this.collectionElementType, elementType)) {
                                this.scope.problemReporter().notCompatibleTypesErrorInForeach(this.collection, this.collectionElementType, elementType);
                            }
                            // no conversion needed as only for reference types
                            break checkIterable;
                        case // for (T t : Iterable<T>) - in case used inside Iterable itself
                        Binding.GENERIC_TYPE:
                            arguments = iterableType.typeVariables();
                            break;
                        case // for(E e : Iterable<E>)
                        Binding.PARAMETERIZED_TYPE:
                            arguments = ((ParameterizedTypeBinding) iterableType).arguments;
                            break;
                        default:
                            break checkIterable;
                    }
                    // per construction can only be one
                    if (arguments.length != 1)
                        break checkIterable;
                    this.kind = GENERIC_ITERABLE;
                    this.collectionElementType = arguments[0];
                    if (!this.collectionElementType.isCompatibleWith(elementType) && !this.scope.isBoxingCompatibleWith(this.collectionElementType, elementType)) {
                        this.scope.problemReporter().notCompatibleTypesErrorInForeach(this.collection, this.collectionElementType, elementType);
                    } else if (// https://bugs.eclipse.org/bugs/show_bug.cgi?id=393719
                    this.collectionElementType.needsUncheckedConversion(elementType)) {
                        this.scope.problemReporter().unsafeElementTypeConversion(this.collection, this.collectionElementType, elementType);
                    }
                    int compileTimeTypeID = this.collectionElementType.id;
                    // no conversion needed as only for reference types
                    if (elementType.isBaseType()) {
                        if (!this.collectionElementType.isBaseType()) {
                            compileTimeTypeID = this.scope.environment().computeBoxingType(this.collectionElementType).id;
                            this.elementVariableImplicitWidening = UNBOXING;
                            if (elementType.isBaseType()) {
                                this.elementVariableImplicitWidening |= (elementType.id << 4) + compileTimeTypeID;
                            }
                        } else {
                            this.elementVariableImplicitWidening = (elementType.id << 4) + compileTimeTypeID;
                        }
                    } else {
                        if (this.collectionElementType.isBaseType()) {
                            // use primitive type in implicit conversion
                            this.elementVariableImplicitWidening = BOXING | (compileTimeTypeID << 4) | compileTimeTypeID;
                        }
                    }
                }
            }
            switch(this.kind) {
                case ARRAY:
                    // allocate #index secret variable (of type int)
                    this.indexVariable = new LocalVariableBinding(SecretIndexVariableName, TypeBinding.INT, ClassFileConstants.AccDefault, false);
                    this.scope.addLocalVariable(this.indexVariable);
                    this.indexVariable.setConstant(// not inlinable
                    Constant.NotAConstant);
                    // allocate #max secret variable
                    this.maxVariable = new LocalVariableBinding(SecretMaxVariableName, TypeBinding.INT, ClassFileConstants.AccDefault, false);
                    this.scope.addLocalVariable(this.maxVariable);
                    this.maxVariable.setConstant(// not inlinable
                    Constant.NotAConstant);
                    // add #array secret variable (of collection type)
                    if (expectedCollectionType == null) {
                        this.collectionVariable = new LocalVariableBinding(SecretCollectionVariableName, collectionType, ClassFileConstants.AccDefault, false);
                    } else {
                        this.collectionVariable = new LocalVariableBinding(SecretCollectionVariableName, expectedCollectionType, ClassFileConstants.AccDefault, false);
                    }
                    this.scope.addLocalVariable(this.collectionVariable);
                    this.collectionVariable.setConstant(// not inlinable
                    Constant.NotAConstant);
                    break;
                case RAW_ITERABLE:
                case GENERIC_ITERABLE:
                    // allocate #index secret variable (of type Iterator)
                    this.indexVariable = new LocalVariableBinding(SecretIteratorVariableName, this.scope.getJavaUtilIterator(), ClassFileConstants.AccDefault, false);
                    this.scope.addLocalVariable(this.indexVariable);
                    this.indexVariable.setConstant(// not inlinable
                    Constant.NotAConstant);
                    break;
                default:
                    if (isTargetJsr14) {
                        this.scope.problemReporter().invalidTypeForCollectionTarget14(this.collection);
                    } else {
                        this.scope.problemReporter().invalidTypeForCollection(this.collection);
                    }
            }
        }
        if (this.action != null) {
            this.action.resolve(this.scope);
        }
    }

    public void traverse(ASTVisitor visitor, BlockScope blockScope) {
        if (visitor.visit(this, blockScope)) {
            this.elementVariable.traverse(visitor, this.scope);
            if (this.collection != null) {
                this.collection.traverse(visitor, this.scope);
            }
            if (this.action != null) {
                this.action.traverse(visitor, this.scope);
            }
        }
        visitor.endVisit(this, blockScope);
    }

    @Override
    public boolean doesNotCompleteNormally() {
        // may not be entered at all.
        return false;
    }
}
