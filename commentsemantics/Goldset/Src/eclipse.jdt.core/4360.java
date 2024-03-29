/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.dom;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.tests.util.Util;

@SuppressWarnings("rawtypes")
public abstract class ConverterTestSetup extends AbstractASTTests {

    /**
	 * Internal synonym for deprecated constant AST.JSL3
	 * to alleviate deprecation warnings.
	 * @deprecated
	 */
    /*package*/
    static final int JLS3_INTERNAL = AST.JLS3;

    /**
	 * Internal synonym for deprecated constant AST.JSL4
	 * to alleviate deprecation warnings.
	 * @deprecated
	 */
    /*package*/
    static final int JLS4_INTERNAL = AST.JLS4;

    static int getJLS3() {
        return JLS3_INTERNAL;
    }

    static int getJLS4() {
        return JLS4_INTERNAL;
    }

    protected AST ast;

    public static List TEST_SUITES = null;

    public static boolean PROJECT_SETUP = false;

    protected  ConverterTestSetup(String name) {
        super(name);
    }

    protected IPath getConverterJCLPath() {
        //$NON-NLS-1$
        return getConverterJCLPath("");
    }

    protected IPath getConverterJCLPath(String compliance) {
        //$NON-NLS-1$
        return new Path(getExternalPath() + "converterJclMin" + compliance + ".jar");
    }

    protected IPath getConverterJCLSourcePath() {
        //$NON-NLS-1$
        return getConverterJCLSourcePath("");
    }

    protected IPath getConverterJCLSourcePath(String compliance) {
        //$NON-NLS-1$
        return new Path(getExternalPath() + "converterJclMin" + compliance + "src.zip");
    }

    protected IPath getConverterJCLRootSourcePath() {
        //$NON-NLS-1$
        return new Path("");
    }

    /**
	 * Reset the jar placeholder and delete project.
	 */
    public void tearDownSuite() throws Exception {
        this.ast = null;
        if (TEST_SUITES == null) {
            //$NON-NLS-1$
            this.deleteProject("Converter");
            //$NON-NLS-1$
            this.deleteProject("Converter15");
            //$NON-NLS-1$
            this.deleteProject("Converter16");
            //$NON-NLS-1$
            this.deleteProject("Converter17");
            //$NON-NLS-1$
            this.deleteProject("Converter18");
            PROJECT_SETUP = false;
        } else {
            TEST_SUITES.remove(getClass());
            if (TEST_SUITES.size() == 0) {
                //$NON-NLS-1$
                this.deleteProject(//$NON-NLS-1$
                "Converter");
                //$NON-NLS-1$
                this.deleteProject(//$NON-NLS-1$
                "Converter15");
                //$NON-NLS-1$
                this.deleteProject(//$NON-NLS-1$
                "Converter16");
                //$NON-NLS-1$
                this.deleteProject(//$NON-NLS-1$
                "Converter17");
                //$NON-NLS-1$
                this.deleteProject(//$NON-NLS-1$
                "Converter18");
                PROJECT_SETUP = false;
            }
        }
        super.tearDownSuite();
    }

    public void setUpJCLClasspathVariables(String compliance, boolean useFullJCL) throws JavaModelException, IOException {
        if ("1.5".equals(compliance) || "1.6".equals(compliance)) {
            if (JavaCore.getClasspathVariable("CONVERTER_JCL15_LIB") == null) {
                setupExternalJCL("converterJclMin1.5");
                JavaCore.setClasspathVariables(new String[] { "CONVERTER_JCL15_LIB", "CONVERTER_JCL15_SRC", "CONVERTER_JCL15_SRCROOT" }, new IPath[] { getConverterJCLPath(compliance), getConverterJCLSourcePath(compliance), getConverterJCLRootSourcePath() }, null);
            }
        } else if ("1.7".equals(compliance)) {
            if (JavaCore.getClasspathVariable("CONVERTER_JCL17_LIB") == null) {
                setupExternalJCL("converterJclMin1.7");
                JavaCore.setClasspathVariables(new String[] { "CONVERTER_JCL17_LIB", "CONVERTER_JCL17_SRC", "CONVERTER_JCL17_SRCROOT" }, new IPath[] { getConverterJCLPath("1.7"), getConverterJCLSourcePath("1.7"), getConverterJCLRootSourcePath() }, null);
            }
        } else if ("1.8".equals(compliance)) {
            if (JavaCore.getClasspathVariable("CONVERTER_JCL18_LIB") == null) {
                setupExternalJCL("converterJclMin1.8");
                JavaCore.setClasspathVariables(new String[] { "CONVERTER_JCL18_LIB", "CONVERTER_JCL18_SRC", "CONVERTER_JCL18_SRCROOT" }, new IPath[] { getConverterJCLPath("1.8"), getConverterJCLSourcePath("1.8"), getConverterJCLRootSourcePath() }, null);
            }
        } else if (JavaCore.getClasspathVariable("CONVERTER_JCL_LIB") == null) {
            setupExternalJCL("converterJclMin");
            JavaCore.setClasspathVariables(new String[] { "CONVERTER_JCL_LIB", "CONVERTER_JCL_SRC", "CONVERTER_JCL_SRCROOT" }, new IPath[] { getConverterJCLPath(), getConverterJCLSourcePath(), getConverterJCLRootSourcePath() }, null);
        }
    }

    /**
	 * Create project and set the jar placeholder.
	 */
    public void setUpSuite() throws Exception {
        super.setUpSuite();
        if (!PROJECT_SETUP) {
            //$NON-NLS-1$
            setUpJavaProject("Converter");
            //$NON-NLS-1$ //$NON-NLS-2$
            setUpJavaProject("Converter15", "1.5");
            //$NON-NLS-1$ //$NON-NLS-2$
            setUpJavaProject("Converter16", "1.6");
            //$NON-NLS-1$ //$NON-NLS-2$
            setUpJavaProject("Converter17", "1.7");
            //$NON-NLS-1$ //$NON-NLS-2$
            setUpJavaProject("Converter18", "1.8");
            // needed to find secondary types
            waitUntilIndexesReady();
            PROJECT_SETUP = true;
        }
    }

    protected void assertExtraDimensionsEqual(String message, List dimensions, String expected) {
        StringBuffer buffer = new StringBuffer();
        Iterator iter = dimensions.iterator();
        while (iter.hasNext()) {
            Dimension dim = (Dimension) iter.next();
            buffer.append(convertAnnotationsList(dim.annotations()));
            if (iter.hasNext()) {
                buffer.append("[] ");
            } else {
                buffer.append("[]");
            }
        }
        assertEquals(message, expected, buffer.toString());
    }

    protected String convertAnnotationsList(List annotations) {
        StringBuffer buffer = new StringBuffer();
        Iterator iter = annotations.iterator();
        while (iter.hasNext()) {
            buffer.append('@');
            buffer.append(((Annotation) iter.next()).getTypeName().getFullyQualifiedName());
            buffer.append(' ');
        }
        return buffer.toString();
    }

    public ASTNode runConversion(ICompilationUnit unit, boolean resolveBindings, boolean bindingsRecovery) {
        return runConversion(astInternalJLS2(), unit, resolveBindings, false, bindingsRecovery);
    }

    public ASTNode runConversion(ICompilationUnit unit, boolean resolveBindings) {
        return runConversion(astInternalJLS2(), unit, resolveBindings);
    }

    public ASTNode runConversion(ICompilationUnit unit, int position, boolean resolveBindings) {
        return runConversion(astInternalJLS2(), unit, position, resolveBindings);
    }

    public ASTNode runConversion(IClassFile classFile, int position, boolean resolveBindings) {
        return runConversion(astInternalJLS2(), classFile, position, resolveBindings);
    }

    public ASTNode runConversion(char[] source, String unitName, IJavaProject project) {
        return runConversion(astInternalJLS2(), source, unitName, project);
    }

    public ASTNode runConversion(char[] source, String unitName, IJavaProject project, boolean resolveBindings) {
        return runConversion(astInternalJLS2(), source, unitName, project, resolveBindings);
    }

    public ASTNode runConversion(int astLevel, ICompilationUnit unit, boolean resolveBindings) {
        return runConversion(astLevel, unit, resolveBindings, false);
    }

    public ASTNode runConversion(int astLevel, ICompilationUnit unit, boolean resolveBindings, boolean statementsRecovery) {
        return runConversion(astLevel, unit, resolveBindings, statementsRecovery, false);
    }

    public ASTNode runConversion(int astLevel, ICompilationUnit unit, boolean resolveBindings, boolean statementsRecovery, boolean bindingsRecovery) {
        ASTParser parser = ASTParser.newParser(astLevel);
        parser.setSource(unit);
        parser.setResolveBindings(resolveBindings);
        parser.setStatementsRecovery(statementsRecovery);
        parser.setBindingsRecovery(bindingsRecovery);
        return parser.createAST(null);
    }

    class NullBindingVerifier extends ASTVisitor {

        public void endVisit(ArrayAccess node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(ArrayCreation node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(ArrayInitializer node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(Assignment node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(BooleanLiteral node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(CastExpression node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(CharacterLiteral node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(ClassInstanceCreation node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(ConditionalExpression node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(FieldAccess node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(InfixExpression node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(InstanceofExpression node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(MarkerAnnotation node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(MethodInvocation node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(NormalAnnotation node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(NullLiteral node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(NumberLiteral node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(ParenthesizedExpression node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(PostfixExpression node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(PrefixExpression node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(SingleMemberAnnotation node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(StringLiteral node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(SuperFieldAccess node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(SuperMethodInvocation node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(ThisExpression node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(TypeLiteral node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(VariableDeclarationExpression node) {
            assertNotNull(node + " should have a binding", node.resolveTypeBinding());
            super.endVisit(node);
        }

        public void endVisit(AnnotationTypeDeclaration node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(AnnotationTypeMemberDeclaration node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(AnonymousClassDeclaration node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(ArrayType node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(EnumDeclaration node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(ImportDeclaration node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(MemberRef node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(MethodDeclaration node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(MethodRef node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(PackageDeclaration node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(ParameterizedType node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(PrimitiveType node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(QualifiedName node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(QualifiedType node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(SimpleName node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(SimpleType node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(SingleVariableDeclaration node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(TypeDeclaration node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(TypeDeclarationStatement node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(TypeParameter node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(VariableDeclarationFragment node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }

        public void endVisit(WildcardType node) {
            assertNotNull(node + " should have a binding", node.resolveBinding());
            super.endVisit(node);
        }
    }

    public ASTNode runJLS3Conversion(ICompilationUnit unit, boolean resolveBindings, boolean checkJLS2) {
        return runJLS3Conversion(unit, resolveBindings, checkJLS2, false);
    }

    public ASTNode runJLS3Conversion(ICompilationUnit unit, boolean resolveBindings, boolean checkJLS2, boolean bindingRecovery) {
        // Create parser
        ASTParser parser;
        if (checkJLS2) {
            parser = ASTParser.newParser(astInternalJLS2());
            parser.setSource(unit);
            parser.setResolveBindings(resolveBindings);
            parser.setBindingsRecovery(bindingRecovery);
            parser.createAST(null);
        }
        parser = ASTParser.newParser(JLS3_INTERNAL);
        parser.setSource(unit);
        parser.setResolveBindings(resolveBindings);
        parser.setBindingsRecovery(bindingRecovery);
        // Parse compilation unit
        ASTNode result = parser.createAST(null);
        // Verify we get a compilation unit node and that binding are correct
        assertTrue("Not a compilation unit", result.getNodeType() == ASTNode.COMPILATION_UNIT);
        CompilationUnit compilationUnit = (CompilationUnit) result;
        if (resolveBindings && compilationUnit.getProblems().length == 0) {
            compilationUnit.accept(new NullBindingVerifier());
        }
        return result;
    }

    public ASTNode runJLS4Conversion(ICompilationUnit unit, boolean resolveBindings, boolean checkJLS2) {
        return runJLS4Conversion(unit, resolveBindings, checkJLS2, false);
    }

    public ASTNode runJLS4Conversion(ICompilationUnit unit, boolean resolveBindings, boolean checkJLS2, boolean bindingRecovery) {
        // Create parser
        ASTParser parser;
        if (checkJLS2) {
            parser = ASTParser.newParser(astInternalJLS2());
            parser.setSource(unit);
            parser.setResolveBindings(resolveBindings);
            parser.setBindingsRecovery(bindingRecovery);
            parser.createAST(null);
        }
        parser = ASTParser.newParser(JLS4_INTERNAL);
        parser.setSource(unit);
        parser.setResolveBindings(resolveBindings);
        parser.setBindingsRecovery(bindingRecovery);
        // Parse compilation unit
        ASTNode result = parser.createAST(null);
        // Verify we get a compilation unit node and that binding are correct
        assertTrue("Not a compilation unit", result.getNodeType() == ASTNode.COMPILATION_UNIT);
        CompilationUnit compilationUnit = (CompilationUnit) result;
        if (resolveBindings && compilationUnit.getProblems().length == 0) {
            compilationUnit.accept(new NullBindingVerifier());
        }
        return result;
    }

    public ASTNode runJLS8Conversion(ICompilationUnit unit, boolean resolveBindings, boolean checkJLS2) {
        return runJLS8Conversion(unit, resolveBindings, checkJLS2, false);
    }

    public ASTNode runJLS8Conversion(ICompilationUnit unit, boolean resolveBindings, boolean checkJLS2, boolean bindingRecovery) {
        // Create parser
        ASTParser parser;
        if (checkJLS2) {
            parser = ASTParser.newParser(astInternalJLS2());
            parser.setSource(unit);
            parser.setResolveBindings(resolveBindings);
            parser.setBindingsRecovery(bindingRecovery);
            parser.createAST(null);
        }
        parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(unit);
        parser.setResolveBindings(resolveBindings);
        parser.setBindingsRecovery(bindingRecovery);
        // Parse compilation unit
        ASTNode result = parser.createAST(null);
        // Verify we get a compilation unit node and that binding are correct
        assertTrue("Not a compilation unit", result.getNodeType() == ASTNode.COMPILATION_UNIT);
        CompilationUnit compilationUnit = (CompilationUnit) result;
        if (resolveBindings && compilationUnit.getProblems().length == 0) {
            compilationUnit.accept(new NullBindingVerifier());
        }
        return result;
    }

    public ASTNode runConversion(int astLevel, ICompilationUnit unit, int position, boolean resolveBindings) {
        // Create parser
        ASTParser parser = ASTParser.newParser(astLevel);
        parser.setSource(unit);
        parser.setFocalPosition(position);
        parser.setResolveBindings(resolveBindings);
        // Parse compilation unit
        ASTNode result = parser.createAST(null);
        // Verify we get a compilation unit node and that binding are correct
        assertTrue("Not a compilation unit", result.getNodeType() == ASTNode.COMPILATION_UNIT);
        CompilationUnit compilationUnit = (CompilationUnit) result;
        if (resolveBindings && compilationUnit.getProblems().length == 0) {
            compilationUnit.accept(new NullBindingVerifier());
        }
        return result;
    }

    public ASTNode runConversion(int astLevel, IClassFile classFile, int position, boolean resolveBindings) {
        // Create parser
        ASTParser parser = ASTParser.newParser(astLevel);
        parser.setSource(classFile);
        parser.setFocalPosition(position);
        parser.setResolveBindings(resolveBindings);
        // Parse compilation unit
        ASTNode result = parser.createAST(null);
        // Verify we get a compilation unit node and that binding are correct
        assertTrue("Not a compilation unit", result.getNodeType() == ASTNode.COMPILATION_UNIT);
        CompilationUnit compilationUnit = (CompilationUnit) result;
        if (resolveBindings && compilationUnit.getProblems().length == 0) {
            compilationUnit.accept(new NullBindingVerifier());
        }
        return result;
    }

    public ASTNode runConversion(int astLevel, char[] source, String unitName, IJavaProject project) {
        return runConversion(astLevel, source, unitName, project, false);
    }

    public ASTNode runConversion(int astLevel, char[] source, String unitName, IJavaProject project, boolean resolveBindings) {
        return runConversion(astLevel, source, unitName, project, null, resolveBindings);
    }

    public ASTNode runConversion(int astLevel, char[] source, String unitName, IJavaProject project, Map<String, String> options, boolean resolveBindings) {
        ASTParser parser = ASTParser.newParser(astLevel);
        parser.setSource(source);
        parser.setUnitName(unitName);
        parser.setProject(project);
        if (options != null) {
            parser.setCompilerOptions(options);
        }
        parser.setResolveBindings(resolveBindings);
        return parser.createAST(null);
    }

    public ASTNode runConversion(int astLevel, char[] source, String unitName, IJavaProject project, Map<String, String> options) {
        return runConversion(astLevel, source, unitName, project, options, false);
    }

    public ASTNode runConversion(char[] source, String unitName, IJavaProject project, Map<String, String> options, boolean resolveBindings) {
        return runConversion(astInternalJLS2(), source, unitName, project, options, resolveBindings);
    }

    public ASTNode runConversion(char[] source, String unitName, IJavaProject project, Map<String, String> options) {
        return runConversion(astInternalJLS2(), source, unitName, project, options);
    }

    protected ASTNode getASTNodeToCompare(org.eclipse.jdt.core.dom.CompilationUnit unit) {
        ExpressionStatement statement = (ExpressionStatement) getASTNode(unit, 0, 0, 0);
        return (ASTNode) ((MethodInvocation) statement.getExpression()).arguments().get(0);
    }

    protected ASTNode getASTNode(org.eclipse.jdt.core.dom.CompilationUnit unit, int typeIndex, int bodyIndex, int statementIndex) {
        BodyDeclaration bodyDeclaration = (BodyDeclaration) getASTNode(unit, typeIndex, bodyIndex);
        if (bodyDeclaration instanceof MethodDeclaration) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
            Block block = methodDeclaration.getBody();
            return (ASTNode) block.statements().get(statementIndex);
        } else if (bodyDeclaration instanceof TypeDeclaration) {
            TypeDeclaration typeDeclaration = (TypeDeclaration) bodyDeclaration;
            return (ASTNode) typeDeclaration.bodyDeclarations().get(statementIndex);
        } else if (bodyDeclaration instanceof Initializer) {
            Initializer initializer = (Initializer) bodyDeclaration;
            Block block = initializer.getBody();
            return (ASTNode) block.statements().get(statementIndex);
        }
        return null;
    }

    protected ASTNode getASTNode(org.eclipse.jdt.core.dom.CompilationUnit unit, int typeIndex, int bodyIndex) {
        return (ASTNode) ((AbstractTypeDeclaration) unit.types().get(typeIndex)).bodyDeclarations().get(bodyIndex);
    }

    protected ASTNode getASTNode(org.eclipse.jdt.core.dom.CompilationUnit unit, int typeIndex) {
        return (ASTNode) unit.types().get(typeIndex);
    }

    protected void checkSourceRange(int start, int length, String expectedContents, String source) {
        //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue("length == 0", length != 0);
        //$NON-NLS-1$
        assertTrue("start == -1", start != -1);
        String actualContentsString = source.substring(start, start + length);
        assertSourceEquals("Unexpected source", Util.convertToIndependantLineDelimiter(expectedContents), Util.convertToIndependantLineDelimiter(actualContentsString));
    }

    protected void checkSourceRange(ASTNode node, String expectedContents, String source) {
        //$NON-NLS-1$
        assertNotNull("The node is null", node);
        //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue("The node(" + node.getClass() + ").getLength() == 0", node.getLength() != 0);
        //$NON-NLS-1$
        assertTrue("The node.getStartPosition() == -1", node.getStartPosition() != -1);
        int length = node.getLength();
        int start = node.getStartPosition();
        String actualContentsString = source.substring(start, start + length);
        assertSourceEquals("Unexpected source", Util.convertToIndependantLineDelimiter(expectedContents), Util.convertToIndependantLineDelimiter(actualContentsString));
    }

    protected void checkSourceRange(ASTNode node, String expectedContents, char[] source) {
        checkSourceRange(node, expectedContents, source, false);
    }

    protected void checkSourceRange(ASTNode node, String expectedContents, char[] source, boolean expectMalformed) {
        //$NON-NLS-1$
        assertNotNull("The node is null", node);
        //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue("The node(" + node.getClass() + ").getLength() == 0", node.getLength() != 0);
        //$NON-NLS-1$
        assertTrue("The node.getStartPosition() == -1", node.getStartPosition() != -1);
        int length = node.getLength();
        int start = node.getStartPosition();
        char[] actualContents = new char[length];
        System.arraycopy(source, start, actualContents, 0, length);
        String actualContentsString = new String(actualContents);
        assertSourceEquals("Unexpected source", Util.convertToIndependantLineDelimiter(expectedContents), Util.convertToIndependantLineDelimiter(actualContentsString));
        if (expectMalformed) {
            assertTrue("Is not malformed", isMalformed(node));
        } else {
            assertFalse("Is malformed", isMalformed(node));
        }
    }

    protected boolean isMalformed(ASTNode node) {
        return (node.getFlags() & ASTNode.MALFORMED) != 0;
    }

    protected boolean isRecovered(ASTNode node) {
        return (node.getFlags() & ASTNode.RECOVERED) != 0;
    }

    protected boolean isOriginal(ASTNode node) {
        return (node.getFlags() & ASTNode.ORIGINAL) != 0;
    }

    protected void assertProblemsSize(CompilationUnit compilationUnit, int expectedSize) {
        assertProblemsSize(compilationUnit, expectedSize, "");
    }

    protected void assertProblemsSize(CompilationUnit compilationUnit, int expectedSize, String expectedOutput) {
        final IProblem[] problems = compilationUnit.getProblems();
        final int length = problems.length;
        if (length != expectedSize) {
            checkProblemMessages(expectedOutput, problems, length);
            assertEquals("Wrong size", expectedSize, length);
        }
        checkProblemMessages(expectedOutput, problems, length);
    }

    private void checkProblemMessages(String expectedOutput, final IProblem[] problems, final int length) {
        if (length != 0) {
            if (expectedOutput != null) {
                StringBuffer buffer = new StringBuffer();
                for (int i = 0; i < length; i++) {
                    buffer.append(problems[i].getMessage());
                    if (i < length - 1) {
                        buffer.append('\n');
                    }
                }
                String actualOutput = String.valueOf(buffer);
                expectedOutput = Util.convertToIndependantLineDelimiter(expectedOutput);
                actualOutput = Util.convertToIndependantLineDelimiter(actualOutput);
                if (!expectedOutput.equals(actualOutput)) {
                    System.out.println(Util.displayString(actualOutput));
                    assertEquals("different output", expectedOutput, actualOutput);
                }
            }
        }
    }
}
