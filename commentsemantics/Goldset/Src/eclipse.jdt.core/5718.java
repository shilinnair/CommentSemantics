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
package org.eclipse.jdt.internal.eval;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.internal.codeassist.ISelectionRequestor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;

/**
 * Maps back and forth a code snippet to a compilation unit.
 * The structure of the compilation unit is as follows:
 * <pre>
 * [package <package name>;]
 * [import <import name>;]*
 * public class <code snippet class name> extends <global variable class name> {
 *   [<declaring type> val$this;]
 *   public void run() {
 *     <code snippet>
 *   }
 * }
 * </pre>
 */
class CodeSnippetToCuMapper implements EvaluationConstants {

    /**
	 * The generated compilation unit.
	 */
    public char[] cuSource;

    /**
	 * Where the code snippet starts in the generated compilation unit.
	 */
    public int lineNumberOffset = 0;

    public int startPosOffset = 0;

    // Internal fields
    char[] codeSnippet;

    char[] snippetPackageName;

    char[][] snippetImports;

    char[] snippetClassName;

    char[] snippetVarClassName;

    char[] snippetDeclaringTypeName;

    // Mapping of external local variables
    char[][] localVarNames;

    char[][] localVarTypeNames;

    long complianceVersion;

    /**
 * Rebuild source in presence of external local variables
 */
    public  CodeSnippetToCuMapper(char[] codeSnippet, char[] packageName, char[][] imports, char[] className, char[] varClassName, char[][] localVarNames, char[][] localVarTypeNames, int[] localVarModifiers, char[] declaringTypeName, String lineSeparator, long complianceVersion) {
        this.codeSnippet = codeSnippet;
        this.snippetPackageName = packageName;
        this.snippetImports = imports;
        this.snippetClassName = className;
        this.snippetVarClassName = varClassName;
        this.localVarNames = localVarNames;
        this.localVarTypeNames = localVarTypeNames;
        this.snippetDeclaringTypeName = declaringTypeName;
        this.complianceVersion = complianceVersion;
        buildCUSource(lineSeparator);
    }

    private void buildCUSource(String lineSeparator) {
        StringBuffer buffer = new StringBuffer();
        // package declaration
        if (this.snippetPackageName != null && this.snippetPackageName.length != 0) {
            //$NON-NLS-1$
            buffer.append("package ");
            buffer.append(this.snippetPackageName);
            //$NON-NLS-1$
            buffer.append(";").append(lineSeparator);
            this.lineNumberOffset++;
        }
        // import declarations
        char[][] imports = this.snippetImports;
        for (int i = 0; i < imports.length; i++) {
            //$NON-NLS-1$
            buffer.append("import ");
            buffer.append(imports[i]);
            buffer.append(';').append(lineSeparator);
            this.lineNumberOffset++;
        }
        // class declaration
        //$NON-NLS-1$
        buffer.append("public class ");
        buffer.append(this.snippetClassName);
        // super class is either a global variable class or the CodeSnippet class
        if (this.snippetVarClassName != null) {
            //$NON-NLS-1$
            buffer.append(" extends ");
            buffer.append(this.snippetVarClassName);
        } else {
            //$NON-NLS-1$
            buffer.append(" extends ");
            buffer.append(PACKAGE_NAME);
            //$NON-NLS-1$
            buffer.append(".");
            buffer.append(ROOT_CLASS_NAME);
        }
        //$NON-NLS-1$
        buffer.append(" {").append(lineSeparator);
        this.lineNumberOffset++;
        if (this.snippetDeclaringTypeName != null) {
            //$NON-NLS-1$
            buffer.append("  ");
            buffer.append(this.snippetDeclaringTypeName);
            //$NON-NLS-1$
            buffer.append(" ");
            // val$this
            buffer.append(DELEGATE_THIS);
            buffer.append(';').append(lineSeparator);
            this.lineNumberOffset++;
        }
        // add some storage location for local variable persisted state
        if (this.localVarNames != null) {
            for (int i = 0, max = this.localVarNames.length; i < max; i++) {
                //$NON-NLS-1$
                buffer.append("    ");
                buffer.append(this.localVarTypeNames[i]);
                //$NON-NLS-1$
                buffer.append(" ");
                // val$...
                buffer.append(LOCAL_VAR_PREFIX);
                buffer.append(this.localVarNames[i]);
                buffer.append(';').append(lineSeparator);
                this.lineNumberOffset++;
            }
        }
        // run() method declaration
        if (this.complianceVersion >= ClassFileConstants.JDK1_5) {
            //$NON-NLS-1$
            buffer.append("@Override ");
        }
        //$NON-NLS-1$
        buffer.append("public void run() throws Throwable {").append(lineSeparator);
        this.lineNumberOffset++;
        this.startPosOffset = buffer.length();
        buffer.append(this.codeSnippet);
        // a line separator is required after the code snippet source code
        // in case the code snippet source code ends with a line comment
        // http://dev.eclipse.org/bugs/show_bug.cgi?id=14838
        buffer.append(lineSeparator).append('}').append(lineSeparator);
        // end of class declaration
        buffer.append('}').append(lineSeparator);
        // store result
        int length = buffer.length();
        this.cuSource = new char[length];
        buffer.getChars(0, length, this.cuSource, 0);
    }

    /**
 * Returns a completion requestor that wraps the given requestor and shift the results
 * according to the start offset and line number offset of the code snippet in the generated compilation unit.
 */
    public CompletionRequestor getCompletionRequestor(final CompletionRequestor originalRequestor) {
        return new CompletionRequestor() {

            public void accept(CompletionProposal proposal) {
                switch(proposal.getKind()) {
                    case CompletionProposal.TYPE_REF:
                        int flags = proposal.getFlags();
                        if ((flags & Flags.AccEnum) == 0 && (flags & Flags.AccInterface) == 0) {
                            // Remove completion on generated class name or generated global variable class name
                            char[] packageName = proposal.getDeclarationSignature();
                            char[] className = Signature.getSignatureSimpleName(proposal.getSignature());
                            if (CharOperation.equals(packageName, CodeSnippetToCuMapper.this.snippetPackageName) && (CharOperation.equals(className, CodeSnippetToCuMapper.this.snippetClassName) || CharOperation.equals(className, CodeSnippetToCuMapper.this.snippetVarClassName)))
                                return;
                            if (CharOperation.equals(packageName, PACKAGE_NAME) && CharOperation.equals(className, ROOT_CLASS_NAME))
                                return;
                        }
                        break;
                    case CompletionProposal.METHOD_REF:
                    case CompletionProposal.METHOD_DECLARATION:
                    case CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER:
                        // Remove completion on generated method
                        char[] declaringTypePackageName = Signature.getSignatureQualifier(proposal.getDeclarationSignature());
                        char[] declaringTypeName = Signature.getSignatureSimpleName(proposal.getDeclarationSignature());
                        if (CharOperation.equals(declaringTypePackageName, CodeSnippetToCuMapper.this.snippetPackageName) && CharOperation.equals(declaringTypeName, CodeSnippetToCuMapper.this.snippetClassName))
                            return;
                        if (CharOperation.equals(declaringTypePackageName, PACKAGE_NAME) && CharOperation.equals(declaringTypeName, ROOT_CLASS_NAME))
                            return;
                        break;
                }
                originalRequestor.accept(proposal);
            }

            public void completionFailure(IProblem problem) {
                problem.setSourceStart(problem.getSourceStart() - CodeSnippetToCuMapper.this.startPosOffset);
                problem.setSourceEnd(problem.getSourceEnd() - CodeSnippetToCuMapper.this.startPosOffset);
                problem.setSourceLineNumber(problem.getSourceLineNumber() - CodeSnippetToCuMapper.this.lineNumberOffset);
                originalRequestor.completionFailure(problem);
            }

            public void acceptContext(CompletionContext context) {
                originalRequestor.acceptContext(context);
            }

            public void beginReporting() {
                originalRequestor.beginReporting();
            }

            public void endReporting() {
                originalRequestor.endReporting();
            }

            public boolean isIgnored(int completionProposalKind) {
                return originalRequestor.isIgnored(completionProposalKind);
            }

            public void setIgnored(int completionProposalKind, boolean ignore) {
                originalRequestor.setIgnored(completionProposalKind, ignore);
            }

            public boolean isAllowingRequiredProposals(int mainKind, int requiredKind) {
                return originalRequestor.isAllowingRequiredProposals(mainKind, requiredKind);
            }

            public void setAllowsRequiredProposals(int mainKind, int requiredKind, boolean allow) {
                originalRequestor.setAllowsRequiredProposals(mainKind, requiredKind, allow);
            }
        };
    }

    public char[] getCUSource(String lineSeparator) {
        if (this.cuSource == null) {
            buildCUSource(lineSeparator);
        }
        return this.cuSource;
    }

    /**
 * Returns the type of evaluation that corresponds to the given line number in the generated compilation unit.
 */
    public int getEvaluationType(int lineNumber) {
        int currentLine = 1;
        // check package declaration
        if (this.snippetPackageName != null && this.snippetPackageName.length != 0) {
            if (lineNumber == 1) {
                return EvaluationResult.T_PACKAGE;
            }
            currentLine++;
        }
        // check imports
        char[][] imports = this.snippetImports;
        if ((currentLine <= lineNumber) && (lineNumber < (currentLine + imports.length))) {
            return EvaluationResult.T_IMPORT;
        }
        // + 1 to skip the class declaration line
        currentLine += imports.length + 1;
        // check generated fields
        currentLine += (this.snippetDeclaringTypeName == null ? 0 : 1) + (this.localVarNames == null ? 0 : this.localVarNames.length);
        if (currentLine > lineNumber) {
            return EvaluationResult.T_INTERNAL;
        }
        // + 1 to skip the method declaration line
        currentLine++;
        // check code snippet
        if (currentLine >= this.lineNumberOffset) {
            return EvaluationResult.T_CODE_SNIPPET;
        }
        // default
        return EvaluationResult.T_INTERNAL;
    }

    /**
 * Returns the import defined at the given line number.
 */
    public char[] getImport(int lineNumber) {
        int importStartLine = this.lineNumberOffset - 1 - this.snippetImports.length;
        return this.snippetImports[lineNumber - importStartLine];
    }

    /**
 * Returns a selection requestor that wraps the given requestor and shift the problems
 * according to the start offset and line number offset of the code snippet in the generated compilation unit.
 */
    public ISelectionRequestor getSelectionRequestor(final ISelectionRequestor originalRequestor) {
        return new ISelectionRequestor() {

            public void acceptType(char[] packageName, char[] typeName, int modifiers, boolean isDeclaration, char[] uniqueKey, int start, int end) {
                originalRequestor.acceptType(packageName, typeName, modifiers, isDeclaration, uniqueKey, start, end);
            }

            public void acceptError(CategorizedProblem error) {
                error.setSourceLineNumber(error.getSourceLineNumber() - CodeSnippetToCuMapper.this.lineNumberOffset);
                error.setSourceStart(error.getSourceStart() - CodeSnippetToCuMapper.this.startPosOffset);
                error.setSourceEnd(error.getSourceEnd() - CodeSnippetToCuMapper.this.startPosOffset);
                originalRequestor.acceptError(error);
            }

            public void acceptField(char[] declaringTypePackageName, char[] declaringTypeName, char[] name, boolean isDeclaration, char[] uniqueKey, int start, int end) {
                originalRequestor.acceptField(declaringTypePackageName, declaringTypeName, name, isDeclaration, uniqueKey, start, end);
            }

            public void acceptMethod(char[] declaringTypePackageName, char[] declaringTypeName, String enclosingDeclaringTypeSignature, char[] selector, char[][] parameterPackageNames, char[][] parameterTypeNames, String[] parameterSignatures, char[][] typeParameterNames, char[][][] typeParameterBoundNames, boolean isConstructor, boolean isDeclaration, char[] uniqueKey, int start, int end) {
                originalRequestor.acceptMethod(declaringTypePackageName, declaringTypeName, enclosingDeclaringTypeSignature, selector, parameterPackageNames, parameterTypeNames, parameterSignatures, typeParameterNames, typeParameterBoundNames, isConstructor, isDeclaration, uniqueKey, start, end);
            }

            public void acceptPackage(char[] packageName) {
                originalRequestor.acceptPackage(packageName);
            }

            public void acceptTypeParameter(char[] declaringTypePackageName, char[] declaringTypeName, char[] typeParameterName, boolean isDeclaration, int start, int end) {
                originalRequestor.acceptTypeParameter(declaringTypePackageName, declaringTypeName, typeParameterName, isDeclaration, start, end);
            }

            public void acceptMethodTypeParameter(char[] declaringTypePackageName, char[] declaringTypeName, char[] selector, int selectorStart, int selectorEnd, char[] typeParameterName, boolean isDeclaration, int start, int end) {
                originalRequestor.acceptMethodTypeParameter(declaringTypePackageName, declaringTypeName, selector, selectorStart, selectorEnd, typeParameterName, isDeclaration, start, end);
            }
        };
    }
}
