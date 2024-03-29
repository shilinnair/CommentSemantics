/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;

public class ASTFlattener extends GenericVisitor {

    /**
	 * @deprecated to avoid deprecation warnings
	 */
    @Deprecated
    private static final int JLS3 = AST.JLS3;

    /**
	 * @deprecated to avoid deprecation warnings
	 */
    @Deprecated
    private static final int JLS4 = AST.JLS4;

    /**
	 * The string buffer into which the serialized representation of the AST is
	 * written.
	 */
    protected StringBuffer fBuffer;

    /**
	 * Creates a new AST printer.
	 */
    public  ASTFlattener() {
        this.fBuffer = new StringBuffer();
    }

    /**
	 * Returns the string accumulated in the visit.
	 *
	 * @return the serialized
	 */
    public String getResult() {
        return this.fBuffer.toString();
    }

    /**
	 * Resets this printer so that it can be used again.
	 */
    public void reset() {
        this.fBuffer.setLength(0);
    }

    public static String asString(ASTNode node) {
        Assert.isTrue(node.getAST().apiLevel() == ASTProvider.SHARED_AST_LEVEL);
        ASTFlattener flattener = new ASTFlattener();
        node.accept(flattener);
        return flattener.getResult();
    }

    @Override
    protected boolean visitNode(ASTNode node) {
        //$NON-NLS-1$
        Assert.isTrue(false, "No implementation to flatten node: " + node.toString());
        return false;
    }

    /**
	 * Appends the text representation of the given modifier flags, followed by a single space.
	 * Used for 3.0 modifiers and annotations.
	 *
	 * @param ext the list of modifier and annotation nodes
	 * (element type: <code>IExtendedModifier</code>)
	 */
    private void printModifiers(List<IExtendedModifier> ext) {
        for (Iterator<IExtendedModifier> it = ext.iterator(); it.hasNext(); ) {
            ASTNode p = (ASTNode) it.next();
            p.accept(this);
            //$NON-NLS-1$
            this.fBuffer.append(" ");
        }
    }

    private void printReferenceTypeArguments(List<Type> typeArguments) {
        //$NON-NLS-1$
        this.fBuffer.append("::");
        if (!typeArguments.isEmpty()) {
            this.fBuffer.append('<');
            for (Iterator<Type> it = typeArguments.iterator(); it.hasNext(); ) {
                Type t = it.next();
                t.accept(this);
                if (it.hasNext()) {
                    this.fBuffer.append(',');
                }
            }
            this.fBuffer.append('>');
        }
    }

    void printTypeAnnotations(AnnotatableType node) {
        if (node.getAST().apiLevel() >= AST.JLS8) {
            printAnnotationsList(node.annotations());
        }
    }

    void printAnnotationsList(List<? extends Annotation> annotations) {
        for (Iterator<? extends Annotation> it = annotations.iterator(); it.hasNext(); ) {
            Annotation annotation = it.next();
            annotation.accept(this);
            this.fBuffer.append(' ');
        }
    }

    /**
	 * @param node node
	 * @return component type
	 * @deprecated to avoid deprecation warning
	 */
    @Deprecated
    private static Type getComponentType(ArrayType node) {
        return node.getComponentType();
    }

    /**
	 * @param node node
	 * @return thrown exception names
	 * @deprecated to avoid deprecation warning
	 */
    @Deprecated
    private static List<Name> getThrownExceptions(MethodDeclaration node) {
        return node.thrownExceptions();
    }

    /*
	 * @see ASTVisitor#visit(AnnotationTypeDeclaration)
	 * @since 3.0
	 */
    @Override
    public boolean visit(AnnotationTypeDeclaration node) {
        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }
        printModifiers(node.modifiers());
        //$NON-NLS-1$
        this.fBuffer.append("@interface ");
        node.getName().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(" {");
        for (Iterator<BodyDeclaration> it = node.bodyDeclarations().iterator(); it.hasNext(); ) {
            BodyDeclaration d = it.next();
            d.accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append("}");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(AnnotationTypeMemberDeclaration)
	 * @since 3.0
	 */
    @Override
    public boolean visit(AnnotationTypeMemberDeclaration node) {
        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }
        printModifiers(node.modifiers());
        node.getType().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(" ");
        node.getName().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append("()");
        if (node.getDefault() != null) {
            //$NON-NLS-1$
            this.fBuffer.append(" default ");
            node.getDefault().accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append(";");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(AnonymousClassDeclaration)
	 */
    @Override
    public boolean visit(AnonymousClassDeclaration node) {
        //$NON-NLS-1$
        this.fBuffer.append("{");
        List<BodyDeclaration> bodyDeclarations = node.bodyDeclarations();
        for (Iterator<BodyDeclaration> it = bodyDeclarations.iterator(); it.hasNext(); ) {
            BodyDeclaration b = it.next();
            b.accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append("}");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ArrayAccess)
	 */
    @Override
    public boolean visit(ArrayAccess node) {
        node.getArray().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append("[");
        node.getIndex().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append("]");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ArrayCreation)
	 */
    @Override
    public boolean visit(ArrayCreation node) {
        //$NON-NLS-1$
        this.fBuffer.append("new ");
        ArrayType at = node.getType();
        int dims = at.getDimensions();
        Type elementType = at.getElementType();
        elementType.accept(this);
        for (Iterator<Expression> it = node.dimensions().iterator(); it.hasNext(); ) {
            //$NON-NLS-1$
            this.fBuffer.append("[");
            Expression e = it.next();
            e.accept(this);
            //$NON-NLS-1$
            this.fBuffer.append("]");
            dims--;
        }
        // add empty "[]" for each extra array dimension
        for (int i = 0; i < dims; i++) {
            //$NON-NLS-1$
            this.fBuffer.append("[]");
        }
        if (node.getInitializer() != null) {
            node.getInitializer().accept(this);
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ArrayInitializer)
	 */
    @Override
    public boolean visit(ArrayInitializer node) {
        //$NON-NLS-1$
        this.fBuffer.append("{");
        for (Iterator<Expression> it = node.expressions().iterator(); it.hasNext(); ) {
            Expression e = it.next();
            e.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ",");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append("}");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ArrayType)
	 */
    @Override
    public boolean visit(ArrayType node) {
        if (node.getAST().apiLevel() < AST.JLS8) {
            getComponentType(node).accept(this);
            //$NON-NLS-1$
            this.fBuffer.append("[]");
        } else {
            node.getElementType().accept(this);
            List<Dimension> dimensions = node.dimensions();
            for (int i = 0; i < dimensions.size(); i++) {
                Dimension dimension = dimensions.get(i);
                dimension.accept(this);
            }
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(AssertStatement)
	 */
    @Override
    public boolean visit(AssertStatement node) {
        //$NON-NLS-1$
        this.fBuffer.append("assert ");
        node.getExpression().accept(this);
        if (node.getMessage() != null) {
            //$NON-NLS-1$
            this.fBuffer.append(" : ");
            node.getMessage().accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append(";");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(Assignment)
	 */
    @Override
    public boolean visit(Assignment node) {
        node.getLeftHandSide().accept(this);
        this.fBuffer.append(node.getOperator().toString());
        node.getRightHandSide().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(Block)
	 */
    @Override
    public boolean visit(Block node) {
        //$NON-NLS-1$
        this.fBuffer.append("{");
        for (Iterator<Statement> it = node.statements().iterator(); it.hasNext(); ) {
            Statement s = it.next();
            s.accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append("}");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(BlockComment)
	 * @since 3.0
	 */
    @Override
    public boolean visit(BlockComment node) {
        //$NON-NLS-1$
        this.fBuffer.append("/* */");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(BooleanLiteral)
	 */
    @Override
    public boolean visit(BooleanLiteral node) {
        if (node.booleanValue() == true) {
            //$NON-NLS-1$
            this.fBuffer.append("true");
        } else {
            //$NON-NLS-1$
            this.fBuffer.append("false");
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(BreakStatement)
	 */
    @Override
    public boolean visit(BreakStatement node) {
        //$NON-NLS-1$
        this.fBuffer.append("break");
        if (node.getLabel() != null) {
            //$NON-NLS-1$
            this.fBuffer.append(" ");
            node.getLabel().accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append(";");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(CastExpression)
	 */
    @Override
    public boolean visit(CastExpression node) {
        //$NON-NLS-1$
        this.fBuffer.append("(");
        node.getType().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(")");
        node.getExpression().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(CatchClause)
	 */
    @Override
    public boolean visit(CatchClause node) {
        //$NON-NLS-1$
        this.fBuffer.append("catch (");
        node.getException().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(") ");
        node.getBody().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(CharacterLiteral)
	 */
    @Override
    public boolean visit(CharacterLiteral node) {
        this.fBuffer.append(node.getEscapedValue());
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ClassInstanceCreation)
	 */
    @Override
    public boolean visit(ClassInstanceCreation node) {
        if (node.getExpression() != null) {
            node.getExpression().accept(this);
            //$NON-NLS-1$
            this.fBuffer.append(".");
        }
        //$NON-NLS-1$
        this.fBuffer.append("new ");
        if (node.getAST().apiLevel() >= JLS3) {
            if (!node.typeArguments().isEmpty()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "<");
                for (Iterator<Type> it = node.typeArguments().iterator(); it.hasNext(); ) {
                    Type t = it.next();
                    t.accept(this);
                    if (it.hasNext()) {
                        //$NON-NLS-1$
                        this.fBuffer.append(//$NON-NLS-1$
                        ",");
                    }
                }
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ">");
            }
            node.getType().accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append("(");
        for (Iterator<Expression> it = node.arguments().iterator(); it.hasNext(); ) {
            Expression e = it.next();
            e.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ",");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append(")");
        if (node.getAnonymousClassDeclaration() != null) {
            node.getAnonymousClassDeclaration().accept(this);
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(CompilationUnit)
	 */
    @Override
    public boolean visit(CompilationUnit node) {
        if (node.getPackage() != null) {
            node.getPackage().accept(this);
        }
        for (Iterator<ImportDeclaration> it = node.imports().iterator(); it.hasNext(); ) {
            ImportDeclaration d = it.next();
            d.accept(this);
        }
        for (Iterator<AbstractTypeDeclaration> it = node.types().iterator(); it.hasNext(); ) {
            AbstractTypeDeclaration d = it.next();
            d.accept(this);
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ConditionalExpression)
	 */
    @Override
    public boolean visit(ConditionalExpression node) {
        node.getExpression().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append("?");
        node.getThenExpression().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(":");
        node.getElseExpression().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ConstructorInvocation)
	 */
    @Override
    public boolean visit(ConstructorInvocation node) {
        if (node.getAST().apiLevel() >= JLS3) {
            if (!node.typeArguments().isEmpty()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "<");
                for (Iterator<Type> it = node.typeArguments().iterator(); it.hasNext(); ) {
                    Type t = it.next();
                    t.accept(this);
                    if (it.hasNext()) {
                        //$NON-NLS-1$
                        this.fBuffer.append(//$NON-NLS-1$
                        ",");
                    }
                }
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ">");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append("this(");
        for (Iterator<Expression> it = node.arguments().iterator(); it.hasNext(); ) {
            Expression e = it.next();
            e.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ",");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append(");");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ContinueStatement)
	 */
    @Override
    public boolean visit(ContinueStatement node) {
        //$NON-NLS-1$
        this.fBuffer.append("continue");
        if (node.getLabel() != null) {
            //$NON-NLS-1$
            this.fBuffer.append(" ");
            node.getLabel().accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append(";");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(CreationReference)
	 */
    @Override
    public boolean visit(CreationReference node) {
        node.getType().accept(this);
        printReferenceTypeArguments(node.typeArguments());
        //$NON-NLS-1$
        this.fBuffer.append("new");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(Dimension)
	 */
    @Override
    public boolean visit(Dimension node) {
        //$NON-NLS-1$
        this.fBuffer.append(" ");
        printAnnotationsList(node.annotations());
        //$NON-NLS-1$
        this.fBuffer.append("[]");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(DoStatement)
	 */
    @Override
    public boolean visit(DoStatement node) {
        //$NON-NLS-1$
        this.fBuffer.append("do ");
        node.getBody().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(" while (");
        node.getExpression().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(");");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(EmptyStatement)
	 */
    @Override
    public boolean visit(EmptyStatement node) {
        //$NON-NLS-1$
        this.fBuffer.append(";");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(EnhancedForStatement)
	 * @since 3.0
	 */
    @Override
    public boolean visit(EnhancedForStatement node) {
        //$NON-NLS-1$
        this.fBuffer.append("for (");
        node.getParameter().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(" : ");
        node.getExpression().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(") ");
        node.getBody().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(EnumConstantDeclaration)
	 * @since 3.0
	 */
    @Override
    public boolean visit(EnumConstantDeclaration node) {
        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }
        printModifiers(node.modifiers());
        node.getName().accept(this);
        if (!node.arguments().isEmpty()) {
            //$NON-NLS-1$
            this.fBuffer.append("(");
            for (Iterator<Expression> it = node.arguments().iterator(); it.hasNext(); ) {
                Expression e = it.next();
                e.accept(this);
                if (it.hasNext()) {
                    //$NON-NLS-1$
                    this.fBuffer.append(//$NON-NLS-1$
                    ",");
                }
            }
            //$NON-NLS-1$
            this.fBuffer.append(")");
        }
        if (node.getAnonymousClassDeclaration() != null) {
            node.getAnonymousClassDeclaration().accept(this);
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(EnumDeclaration)
	 * @since 3.0
	 */
    @Override
    public boolean visit(EnumDeclaration node) {
        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }
        printModifiers(node.modifiers());
        //$NON-NLS-1$
        this.fBuffer.append("enum ");
        node.getName().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(" ");
        if (!node.superInterfaceTypes().isEmpty()) {
            //$NON-NLS-1$
            this.fBuffer.append("implements ");
            for (Iterator<Type> it = node.superInterfaceTypes().iterator(); it.hasNext(); ) {
                Type t = it.next();
                t.accept(this);
                if (it.hasNext()) {
                    //$NON-NLS-1$
                    this.fBuffer.append(//$NON-NLS-1$
                    ", ");
                }
            }
            //$NON-NLS-1$
            this.fBuffer.append(" ");
        }
        //$NON-NLS-1$
        this.fBuffer.append("{");
        for (Iterator<EnumConstantDeclaration> it = node.enumConstants().iterator(); it.hasNext(); ) {
            EnumConstantDeclaration d = it.next();
            d.accept(this);
            // enum constant declarations do not include punctuation
            if (it.hasNext()) {
                // enum constant declarations are separated by commas
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ", ");
            }
        }
        if (!node.bodyDeclarations().isEmpty()) {
            //$NON-NLS-1$
            this.fBuffer.append("; ");
            for (Iterator<BodyDeclaration> it = node.bodyDeclarations().iterator(); it.hasNext(); ) {
                BodyDeclaration d = it.next();
                d.accept(this);
            // other body declarations include trailing punctuation
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append("}");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ExpressionMethodReference)
	 */
    @Override
    public boolean visit(ExpressionMethodReference node) {
        node.getExpression().accept(this);
        printReferenceTypeArguments(node.typeArguments());
        node.getName().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ExpressionStatement)
	 */
    @Override
    public boolean visit(ExpressionStatement node) {
        node.getExpression().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(";");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(FieldAccess)
	 */
    @Override
    public boolean visit(FieldAccess node) {
        node.getExpression().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(".");
        node.getName().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(FieldDeclaration)
	 */
    @Override
    public boolean visit(FieldDeclaration node) {
        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }
        if (node.getAST().apiLevel() >= JLS3) {
            printModifiers(node.modifiers());
        }
        node.getType().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(" ");
        for (Iterator<VariableDeclarationFragment> it = node.fragments().iterator(); it.hasNext(); ) {
            VariableDeclarationFragment f = it.next();
            f.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ", ");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append(";");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ForStatement)
	 */
    @Override
    public boolean visit(ForStatement node) {
        //$NON-NLS-1$
        this.fBuffer.append("for (");
        for (Iterator<Expression> it = node.initializers().iterator(); it.hasNext(); ) {
            Expression e = it.next();
            e.accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append("; ");
        if (node.getExpression() != null) {
            node.getExpression().accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append("; ");
        for (Iterator<Expression> it = node.updaters().iterator(); it.hasNext(); ) {
            Expression e = it.next();
            e.accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append(") ");
        node.getBody().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(IfStatement)
	 */
    @Override
    public boolean visit(IfStatement node) {
        //$NON-NLS-1$
        this.fBuffer.append("if (");
        node.getExpression().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(") ");
        node.getThenStatement().accept(this);
        if (node.getElseStatement() != null) {
            //$NON-NLS-1$
            this.fBuffer.append(" else ");
            node.getElseStatement().accept(this);
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ImportDeclaration)
	 */
    @Override
    public boolean visit(ImportDeclaration node) {
        //$NON-NLS-1$
        this.fBuffer.append("import ");
        if (node.getAST().apiLevel() >= JLS3) {
            if (node.isStatic()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "static ");
            }
        }
        node.getName().accept(this);
        if (node.isOnDemand()) {
            //$NON-NLS-1$
            this.fBuffer.append(".*");
        }
        //$NON-NLS-1$
        this.fBuffer.append(";");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(InfixExpression)
	 */
    @Override
    public boolean visit(InfixExpression node) {
        node.getLeftOperand().accept(this);
        // for cases like x= i - -1; or x= i++ + ++i;
        this.fBuffer.append(' ');
        this.fBuffer.append(node.getOperator().toString());
        this.fBuffer.append(' ');
        node.getRightOperand().accept(this);
        final List<Expression> extendedOperands = node.extendedOperands();
        if (extendedOperands.size() != 0) {
            this.fBuffer.append(' ');
            for (Iterator<Expression> it = extendedOperands.iterator(); it.hasNext(); ) {
                this.fBuffer.append(node.getOperator().toString()).append(' ');
                Expression e = it.next();
                e.accept(this);
            }
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(Initializer)
	 */
    @Override
    public boolean visit(Initializer node) {
        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }
        if (node.getAST().apiLevel() >= JLS3) {
            printModifiers(node.modifiers());
        }
        node.getBody().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(InstanceofExpression)
	 */
    @Override
    public boolean visit(InstanceofExpression node) {
        node.getLeftOperand().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(" instanceof ");
        node.getRightOperand().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(IntersectionType)
	 */
    @Override
    public boolean visit(IntersectionType node) {
        for (Iterator<Type> it = node.types().iterator(); it.hasNext(); ) {
            Type t = it.next();
            t.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                " & ");
            }
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(Javadoc)
	 */
    @Override
    public boolean visit(Javadoc node) {
        //$NON-NLS-1$
        this.fBuffer.append("/** ");
        for (Iterator<TagElement> it = node.tags().iterator(); it.hasNext(); ) {
            ASTNode e = it.next();
            e.accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append("\n */");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(LabeledStatement)
	 */
    @Override
    public boolean visit(LabeledStatement node) {
        node.getLabel().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(": ");
        node.getBody().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(LambdaExpression)
	 */
    @Override
    public boolean visit(LambdaExpression node) {
        boolean hasParentheses = node.hasParentheses();
        if (hasParentheses)
            this.fBuffer.append('(');
        for (Iterator<? extends VariableDeclaration> it = node.parameters().iterator(); it.hasNext(); ) {
            VariableDeclaration v = it.next();
            v.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ",");
            }
        }
        if (hasParentheses)
            this.fBuffer.append(')');
        //$NON-NLS-1$
        this.fBuffer.append(" -> ");
        node.getBody().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(LineComment)
	 * @since 3.0
	 */
    @Override
    public boolean visit(LineComment node) {
        //$NON-NLS-1$
        this.fBuffer.append("//\n");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(MarkerAnnotation)
	 * @since 3.0
	 */
    @Override
    public boolean visit(MarkerAnnotation node) {
        //$NON-NLS-1$
        this.fBuffer.append("@");
        node.getTypeName().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(MemberRef)
	 * @since 3.0
	 */
    @Override
    public boolean visit(MemberRef node) {
        if (node.getQualifier() != null) {
            node.getQualifier().accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append("#");
        node.getName().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(MemberValuePair)
	 * @since 3.0
	 */
    @Override
    public boolean visit(MemberValuePair node) {
        node.getName().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append("=");
        node.getValue().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(MethodRef)
	 * @since 3.0
	 */
    @Override
    public boolean visit(MethodRef node) {
        if (node.getQualifier() != null) {
            node.getQualifier().accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append("#");
        node.getName().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append("(");
        for (Iterator<MethodRefParameter> it = node.parameters().iterator(); it.hasNext(); ) {
            MethodRefParameter e = it.next();
            e.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ",");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append(")");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(MethodRefParameter)
	 * @since 3.0
	 */
    @Override
    public boolean visit(MethodRefParameter node) {
        node.getType().accept(this);
        if (node.getAST().apiLevel() >= JLS3) {
            if (node.isVarargs()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "...");
            }
        }
        if (node.getName() != null) {
            //$NON-NLS-1$
            this.fBuffer.append(" ");
            node.getName().accept(this);
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(MethodDeclaration)
	 */
    @Override
    public boolean visit(MethodDeclaration node) {
        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }
        if (node.getAST().apiLevel() >= JLS3) {
            printModifiers(node.modifiers());
            if (!node.typeParameters().isEmpty()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "<");
                for (Iterator<TypeParameter> it = node.typeParameters().iterator(); it.hasNext(); ) {
                    TypeParameter t = it.next();
                    t.accept(this);
                    if (it.hasNext()) {
                        //$NON-NLS-1$
                        this.fBuffer.append(//$NON-NLS-1$
                        ", ");
                    }
                }
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "> ");
            }
        }
        if (!node.isConstructor()) {
            if (node.getReturnType2() != null) {
                node.getReturnType2().accept(this);
            } else {
                // methods really ought to have a return type
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "void");
            }
            //$NON-NLS-1$
            this.fBuffer.append(" ");
        }
        node.getName().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append("(");
        if (node.getAST().apiLevel() >= AST.JLS8) {
            Type receiverType = node.getReceiverType();
            if (receiverType != null) {
                receiverType.accept(this);
                this.fBuffer.append(' ');
                SimpleName qualifier = node.getReceiverQualifier();
                if (qualifier != null) {
                    qualifier.accept(this);
                    this.fBuffer.append('.');
                }
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "this");
                if (node.parameters().size() > 0) {
                    this.fBuffer.append(',');
                }
            }
        }
        for (Iterator<SingleVariableDeclaration> it = node.parameters().iterator(); it.hasNext(); ) {
            SingleVariableDeclaration v = it.next();
            v.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ", ");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append(")");
        if (node.getAST().apiLevel() >= AST.JLS8) {
            List<Dimension> dimensions = node.extraDimensions();
            for (Iterator<Dimension> it = dimensions.iterator(); it.hasNext(); ) {
                Dimension e = it.next();
                e.accept(this);
            }
        } else {
            for (int i = 0; i < node.getExtraDimensions(); i++) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "[]");
            }
        }
        List<? extends ASTNode> thrownExceptions = node.getAST().apiLevel() >= AST.JLS8 ? node.thrownExceptionTypes() : getThrownExceptions(node);
        if (!thrownExceptions.isEmpty()) {
            //$NON-NLS-1$
            this.fBuffer.append(" throws ");
            for (Iterator<? extends ASTNode> it = thrownExceptions.iterator(); it.hasNext(); ) {
                ASTNode n = it.next();
                n.accept(this);
                if (it.hasNext()) {
                    //$NON-NLS-1$
                    this.fBuffer.append(//$NON-NLS-1$
                    ", ");
                }
            }
            //$NON-NLS-1$
            this.fBuffer.append(" ");
        }
        if (node.getBody() == null) {
            //$NON-NLS-1$
            this.fBuffer.append(";");
        } else {
            node.getBody().accept(this);
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(MethodInvocation)
	 */
    @Override
    public boolean visit(MethodInvocation node) {
        if (node.getExpression() != null) {
            node.getExpression().accept(this);
            //$NON-NLS-1$
            this.fBuffer.append(".");
        }
        if (node.getAST().apiLevel() >= JLS3) {
            if (!node.typeArguments().isEmpty()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "<");
                for (Iterator<Type> it = node.typeArguments().iterator(); it.hasNext(); ) {
                    Type t = it.next();
                    t.accept(this);
                    if (it.hasNext()) {
                        //$NON-NLS-1$
                        this.fBuffer.append(//$NON-NLS-1$
                        ",");
                    }
                }
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ">");
            }
        }
        node.getName().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append("(");
        for (Iterator<Expression> it = node.arguments().iterator(); it.hasNext(); ) {
            Expression e = it.next();
            e.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ",");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append(")");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(Modifier)
	 * @since 3.0
	 */
    @Override
    public boolean visit(Modifier node) {
        this.fBuffer.append(node.getKeyword().toString());
        return false;
    }

    /*
	 * @see ASTVisitor#visit(NameQualifiedType)
	 */
    @Override
    public boolean visit(NameQualifiedType node) {
        node.getQualifier().accept(this);
        this.fBuffer.append('.');
        printTypeAnnotations(node);
        node.getName().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(NormalAnnotation)
	 * @since 3.0
	 */
    @Override
    public boolean visit(NormalAnnotation node) {
        //$NON-NLS-1$
        this.fBuffer.append("@");
        node.getTypeName().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append("(");
        for (Iterator<MemberValuePair> it = node.values().iterator(); it.hasNext(); ) {
            MemberValuePair p = it.next();
            p.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ",");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append(")");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(NullLiteral)
	 */
    @Override
    public boolean visit(NullLiteral node) {
        //$NON-NLS-1$
        this.fBuffer.append("null");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(NumberLiteral)
	 */
    @Override
    public boolean visit(NumberLiteral node) {
        this.fBuffer.append(node.getToken());
        return false;
    }

    /*
	 * @see ASTVisitor#visit(PackageDeclaration)
	 */
    @Override
    public boolean visit(PackageDeclaration node) {
        if (node.getAST().apiLevel() >= JLS3) {
            if (node.getJavadoc() != null) {
                node.getJavadoc().accept(this);
            }
            for (Iterator<Annotation> it = node.annotations().iterator(); it.hasNext(); ) {
                Annotation p = it.next();
                p.accept(this);
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                " ");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append("package ");
        node.getName().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(";");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ParameterizedType)
	 * @since 3.0
	 */
    @Override
    public boolean visit(ParameterizedType node) {
        node.getType().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append("<");
        for (Iterator<Type> it = node.typeArguments().iterator(); it.hasNext(); ) {
            Type t = it.next();
            t.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ",");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append(">");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ParenthesizedExpression)
	 */
    @Override
    public boolean visit(ParenthesizedExpression node) {
        //$NON-NLS-1$
        this.fBuffer.append("(");
        node.getExpression().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(")");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(PostfixExpression)
	 */
    @Override
    public boolean visit(PostfixExpression node) {
        node.getOperand().accept(this);
        this.fBuffer.append(node.getOperator().toString());
        return false;
    }

    /*
	 * @see ASTVisitor#visit(PrefixExpression)
	 */
    @Override
    public boolean visit(PrefixExpression node) {
        this.fBuffer.append(node.getOperator().toString());
        node.getOperand().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(PrimitiveType)
	 */
    @Override
    public boolean visit(PrimitiveType node) {
        printTypeAnnotations(node);
        this.fBuffer.append(node.getPrimitiveTypeCode().toString());
        return false;
    }

    /*
	 * @see ASTVisitor#visit(QualifiedName)
	 */
    @Override
    public boolean visit(QualifiedName node) {
        node.getQualifier().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(".");
        node.getName().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(QualifiedType)
	 * @since 3.0
	 */
    @Override
    public boolean visit(QualifiedType node) {
        node.getQualifier().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(".");
        printTypeAnnotations(node);
        node.getName().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ReturnStatement)
	 */
    @Override
    public boolean visit(ReturnStatement node) {
        //$NON-NLS-1$
        this.fBuffer.append("return");
        if (node.getExpression() != null) {
            //$NON-NLS-1$
            this.fBuffer.append(" ");
            node.getExpression().accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append(";");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(SimpleName)
	 */
    @Override
    public boolean visit(SimpleName node) {
        this.fBuffer.append(node.getIdentifier());
        return false;
    }

    /*
	 * @see ASTVisitor#visit(SimpleType)
	 */
    @Override
    public boolean visit(SimpleType node) {
        printTypeAnnotations(node);
        node.getName().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(SingleMemberAnnotation)
	 * @since 3.0
	 */
    @Override
    public boolean visit(SingleMemberAnnotation node) {
        //$NON-NLS-1$
        this.fBuffer.append("@");
        node.getTypeName().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append("(");
        node.getValue().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(")");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(SingleVariableDeclaration)
	 */
    @Override
    public boolean visit(SingleVariableDeclaration node) {
        if (node.getAST().apiLevel() >= JLS3) {
            printModifiers(node.modifiers());
        }
        node.getType().accept(this);
        if (node.getAST().apiLevel() >= JLS3) {
            if (node.isVarargs()) {
                if (node.getAST().apiLevel() >= AST.JLS8) {
                    this.fBuffer.append(' ');
                    List<Annotation> annotations = node.varargsAnnotations();
                    printAnnotationsList(annotations);
                }
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "...");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append(" ");
        node.getName().accept(this);
        if (node.getAST().apiLevel() >= AST.JLS8) {
            List<Dimension> dimensions = node.extraDimensions();
            for (Iterator<Dimension> it = dimensions.iterator(); it.hasNext(); ) {
                Dimension e = it.next();
                e.accept(this);
            }
        } else {
            for (int i = 0; i < node.getExtraDimensions(); i++) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "[]");
            }
        }
        if (node.getInitializer() != null) {
            //$NON-NLS-1$
            this.fBuffer.append("=");
            node.getInitializer().accept(this);
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(StringLiteral)
	 */
    @Override
    public boolean visit(StringLiteral node) {
        this.fBuffer.append(node.getEscapedValue());
        return false;
    }

    /*
	 * @see ASTVisitor#visit(SuperConstructorInvocation)
	 */
    @Override
    public boolean visit(SuperConstructorInvocation node) {
        if (node.getExpression() != null) {
            node.getExpression().accept(this);
            //$NON-NLS-1$
            this.fBuffer.append(".");
        }
        if (node.getAST().apiLevel() >= JLS3) {
            if (!node.typeArguments().isEmpty()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "<");
                for (Iterator<Type> it = node.typeArguments().iterator(); it.hasNext(); ) {
                    Type t = it.next();
                    t.accept(this);
                    if (it.hasNext()) {
                        //$NON-NLS-1$
                        this.fBuffer.append(//$NON-NLS-1$
                        ",");
                    }
                }
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ">");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append("super(");
        for (Iterator<Expression> it = node.arguments().iterator(); it.hasNext(); ) {
            Expression e = it.next();
            e.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ",");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append(");");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(SuperFieldAccess)
	 */
    @Override
    public boolean visit(SuperFieldAccess node) {
        if (node.getQualifier() != null) {
            node.getQualifier().accept(this);
            //$NON-NLS-1$
            this.fBuffer.append(".");
        }
        //$NON-NLS-1$
        this.fBuffer.append("super.");
        node.getName().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(SuperMethodInvocation)
	 */
    @Override
    public boolean visit(SuperMethodInvocation node) {
        if (node.getQualifier() != null) {
            node.getQualifier().accept(this);
            //$NON-NLS-1$
            this.fBuffer.append(".");
        }
        //$NON-NLS-1$
        this.fBuffer.append("super.");
        if (node.getAST().apiLevel() >= JLS3) {
            if (!node.typeArguments().isEmpty()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "<");
                for (Iterator<Type> it = node.typeArguments().iterator(); it.hasNext(); ) {
                    Type t = it.next();
                    t.accept(this);
                    if (it.hasNext()) {
                        //$NON-NLS-1$
                        this.fBuffer.append(//$NON-NLS-1$
                        ",");
                    }
                }
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ">");
            }
        }
        node.getName().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append("(");
        for (Iterator<Expression> it = node.arguments().iterator(); it.hasNext(); ) {
            Expression e = it.next();
            e.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ",");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append(")");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(SuperMethodReference)
	 */
    @Override
    public boolean visit(SuperMethodReference node) {
        if (node.getQualifier() != null) {
            node.getQualifier().accept(this);
            this.fBuffer.append('.');
        }
        //$NON-NLS-1$
        this.fBuffer.append("super");
        printReferenceTypeArguments(node.typeArguments());
        node.getName().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(SwitchCase)
	 */
    @Override
    public boolean visit(SwitchCase node) {
        if (node.isDefault()) {
            //$NON-NLS-1$
            this.fBuffer.append("default :");
        } else {
            //$NON-NLS-1$
            this.fBuffer.append("case ");
            node.getExpression().accept(this);
            //$NON-NLS-1$
            this.fBuffer.append(":");
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(SwitchStatement)
	 */
    @Override
    public boolean visit(SwitchStatement node) {
        //$NON-NLS-1$
        this.fBuffer.append("switch (");
        node.getExpression().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(") ");
        //$NON-NLS-1$
        this.fBuffer.append("{");
        for (Iterator<Statement> it = node.statements().iterator(); it.hasNext(); ) {
            Statement s = it.next();
            s.accept(this);
        }
        //$NON-NLS-1$
        this.fBuffer.append("}");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(SynchronizedStatement)
	 */
    @Override
    public boolean visit(SynchronizedStatement node) {
        //$NON-NLS-1$
        this.fBuffer.append("synchronized (");
        node.getExpression().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(") ");
        node.getBody().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(TagElement)
	 * @since 3.0
	 */
    @Override
    public boolean visit(TagElement node) {
        if (node.isNested()) {
            // nested tags are always enclosed in braces
            //$NON-NLS-1$
            this.fBuffer.append("{");
        } else {
            // top-level tags always begin on a new line
            //$NON-NLS-1$
            this.fBuffer.append("\n * ");
        }
        boolean previousRequiresWhiteSpace = false;
        if (node.getTagName() != null) {
            this.fBuffer.append(node.getTagName());
            previousRequiresWhiteSpace = true;
        }
        boolean previousRequiresNewLine = false;
        for (Iterator<? extends ASTNode> it = node.fragments().iterator(); it.hasNext(); ) {
            ASTNode e = it.next();
            // assume text elements include necessary leading and trailing whitespace
            // but Name, MemberRef, MethodRef, and nested TagElement do not include white space
            boolean currentIncludesWhiteSpace = (e instanceof TextElement);
            if (previousRequiresNewLine && currentIncludesWhiteSpace) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "\n * ");
            }
            previousRequiresNewLine = currentIncludesWhiteSpace;
            // add space if required to separate
            if (previousRequiresWhiteSpace && !currentIncludesWhiteSpace) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                " ");
            }
            e.accept(this);
            previousRequiresWhiteSpace = !currentIncludesWhiteSpace && !(e instanceof TagElement);
        }
        if (node.isNested()) {
            //$NON-NLS-1$
            this.fBuffer.append("}");
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(TextElement)
	 * @since 3.0
	 */
    @Override
    public boolean visit(TextElement node) {
        this.fBuffer.append(node.getText());
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ThisExpression)
	 */
    @Override
    public boolean visit(ThisExpression node) {
        if (node.getQualifier() != null) {
            node.getQualifier().accept(this);
            //$NON-NLS-1$
            this.fBuffer.append(".");
        }
        //$NON-NLS-1$
        this.fBuffer.append("this");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(ThrowStatement)
	 */
    @Override
    public boolean visit(ThrowStatement node) {
        //$NON-NLS-1$
        this.fBuffer.append("throw ");
        node.getExpression().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(";");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(TryStatement)
	 */
    @Override
    public boolean visit(TryStatement node) {
        //$NON-NLS-1$
        this.fBuffer.append("try ");
        if (node.getAST().apiLevel() >= JLS4) {
            if (!node.resources().isEmpty()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "(");
                for (Iterator<VariableDeclarationExpression> it = node.resources().iterator(); it.hasNext(); ) {
                    VariableDeclarationExpression var = it.next();
                    var.accept(this);
                    if (it.hasNext()) {
                        //$NON-NLS-1$
                        this.fBuffer.append(//$NON-NLS-1$
                        ",");
                    }
                }
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ") ");
            }
        }
        node.getBody().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(" ");
        for (Iterator<CatchClause> it = node.catchClauses().iterator(); it.hasNext(); ) {
            CatchClause cc = it.next();
            cc.accept(this);
        }
        if (node.getFinally() != null) {
            //$NON-NLS-1$
            this.fBuffer.append("finally ");
            node.getFinally().accept(this);
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(TypeDeclaration)
	 */
    @Override
    public boolean visit(TypeDeclaration node) {
        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }
        if (node.getAST().apiLevel() >= JLS3) {
            printModifiers(node.modifiers());
        }
        //$NON-NLS-2$//$NON-NLS-1$
        this.fBuffer.append(node.isInterface() ? "interface " : "class ");
        node.getName().accept(this);
        if (node.getAST().apiLevel() >= JLS3) {
            if (!node.typeParameters().isEmpty()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "<");
                for (Iterator<TypeParameter> it = node.typeParameters().iterator(); it.hasNext(); ) {
                    TypeParameter t = it.next();
                    t.accept(this);
                    if (it.hasNext()) {
                        //$NON-NLS-1$
                        this.fBuffer.append(//$NON-NLS-1$
                        ",");
                    }
                }
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ">");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append(" ");
        if (node.getAST().apiLevel() >= JLS3) {
            if (node.getSuperclassType() != null) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "extends ");
                node.getSuperclassType().accept(this);
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                " ");
            }
            if (!node.superInterfaceTypes().isEmpty()) {
                //$NON-NLS-2$//$NON-NLS-1$
                this.fBuffer.append(node.isInterface() ? "extends " : "implements ");
                for (Iterator<Type> it = node.superInterfaceTypes().iterator(); it.hasNext(); ) {
                    Type t = it.next();
                    t.accept(this);
                    if (it.hasNext()) {
                        //$NON-NLS-1$
                        this.fBuffer.append(//$NON-NLS-1$
                        ", ");
                    }
                }
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                " ");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append("{");
        BodyDeclaration prev = null;
        for (Iterator<BodyDeclaration> it = node.bodyDeclarations().iterator(); it.hasNext(); ) {
            BodyDeclaration d = it.next();
            if (prev instanceof EnumConstantDeclaration) {
                // enum constant declarations do not include punctuation
                if (d instanceof EnumConstantDeclaration) {
                    // enum constant declarations are separated by commas
                    //$NON-NLS-1$
                    this.fBuffer.append(//$NON-NLS-1$
                    ", ");
                } else {
                    // semicolon separates last enum constant declaration from
                    // first class body declarations
                    //$NON-NLS-1$
                    this.fBuffer.append(//$NON-NLS-1$
                    "; ");
                }
            }
            d.accept(this);
            prev = d;
        }
        //$NON-NLS-1$
        this.fBuffer.append("}");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(TypeDeclarationStatement)
	 */
    @Override
    public boolean visit(TypeDeclarationStatement node) {
        if (node.getAST().apiLevel() >= JLS3) {
            node.getDeclaration().accept(this);
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(TypeLiteral)
	 */
    @Override
    public boolean visit(TypeLiteral node) {
        node.getType().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(".class");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(TypeMethodReference)
	 */
    @Override
    public boolean visit(TypeMethodReference node) {
        node.getType().accept(this);
        printReferenceTypeArguments(node.typeArguments());
        node.getName().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(TypeParameter)
	 * @since 3.0
	 */
    @Override
    public boolean visit(TypeParameter node) {
        printModifiers(node.modifiers());
        node.getName().accept(this);
        if (!node.typeBounds().isEmpty()) {
            //$NON-NLS-1$
            this.fBuffer.append(" extends ");
            for (Iterator<Type> it = node.typeBounds().iterator(); it.hasNext(); ) {
                Type t = it.next();
                t.accept(this);
                if (it.hasNext()) {
                    //$NON-NLS-1$
                    this.fBuffer.append(//$NON-NLS-1$
                    " & ");
                }
            }
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(UnionType)
	 */
    @Override
    public boolean visit(UnionType node) {
        for (Iterator<Type> it = node.types().iterator(); it.hasNext(); ) {
            Type t = it.next();
            t.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "|");
            }
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(VariableDeclarationExpression)
	 */
    @Override
    public boolean visit(VariableDeclarationExpression node) {
        if (node.getAST().apiLevel() >= JLS3) {
            printModifiers(node.modifiers());
        }
        node.getType().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(" ");
        for (Iterator<VariableDeclarationFragment> it = node.fragments().iterator(); it.hasNext(); ) {
            VariableDeclarationFragment f = it.next();
            f.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ", ");
            }
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(VariableDeclarationFragment)
	 */
    @Override
    public boolean visit(VariableDeclarationFragment node) {
        node.getName().accept(this);
        if (node.getAST().apiLevel() >= AST.JLS8) {
            List<Dimension> dimensions = node.extraDimensions();
            for (Iterator<Dimension> it = dimensions.iterator(); it.hasNext(); ) {
                Dimension e = it.next();
                e.accept(this);
            }
        } else {
            for (int i = 0; i < node.getExtraDimensions(); i++) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                "[]");
            }
        }
        if (node.getInitializer() != null) {
            //$NON-NLS-1$
            this.fBuffer.append("=");
            node.getInitializer().accept(this);
        }
        return false;
    }

    /*
	 * @see ASTVisitor#visit(VariableDeclarationStatement)
	 */
    @Override
    public boolean visit(VariableDeclarationStatement node) {
        if (node.getAST().apiLevel() >= JLS3) {
            printModifiers(node.modifiers());
        }
        node.getType().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(" ");
        for (Iterator<VariableDeclarationFragment> it = node.fragments().iterator(); it.hasNext(); ) {
            VariableDeclarationFragment f = it.next();
            f.accept(this);
            if (it.hasNext()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                ", ");
            }
        }
        //$NON-NLS-1$
        this.fBuffer.append(";");
        return false;
    }

    /*
	 * @see ASTVisitor#visit(WhileStatement)
	 */
    @Override
    public boolean visit(WhileStatement node) {
        //$NON-NLS-1$
        this.fBuffer.append("while (");
        node.getExpression().accept(this);
        //$NON-NLS-1$
        this.fBuffer.append(") ");
        node.getBody().accept(this);
        return false;
    }

    /*
	 * @see ASTVisitor#visit(WildcardType)
	 * @since 3.0
	 */
    @Override
    public boolean visit(WildcardType node) {
        printTypeAnnotations(node);
        //$NON-NLS-1$
        this.fBuffer.append("?");
        Type bound = node.getBound();
        if (bound != null) {
            if (node.isUpperBound()) {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                " extends ");
            } else {
                //$NON-NLS-1$
                this.fBuffer.append(//$NON-NLS-1$
                " super ");
            }
            bound.accept(this);
        }
        return false;
    }
}
