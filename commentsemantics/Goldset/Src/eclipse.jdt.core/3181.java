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
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;

/**
 * Internal parser used for parsing source to create DOM AST nodes.
 *
 * @since 3.0
 */
public class CommentRecorderParser extends Parser {

    // support for comments
    int[] commentStops = new int[10];

    int[] commentStarts = new int[10];

    // no comment test with commentPtr value -1
    int commentPtr = -1;

    protected static final int CommentIncrement = 100;

    /**
	 * @param problemReporter
	 * @param optimizeStringLiterals
	 */
    public  CommentRecorderParser(ProblemReporter problemReporter, boolean optimizeStringLiterals) {
        super(problemReporter, optimizeStringLiterals);
    }

    // old javadoc style check which doesn't include all leading comments into declaration
    // for backward compatibility with 2.1 DOM
    public void checkComment() {
        // discard obsolete comments while inside methods or fields initializer (see bug 74369)
        if (!(this.diet && this.dietInt == 0) && this.scanner.commentPtr >= 0) {
            flushCommentsDefinedPriorTo(this.endStatementPosition);
        }
        boolean deprecated = false;
        boolean checkDeprecated = false;
        int lastCommentIndex = -1;
        //since jdk1.2 look only in the last java doc comment...
        nextComment: for (lastCommentIndex = this.scanner.commentPtr; lastCommentIndex >= 0; lastCommentIndex--) {
            //look for @deprecated into the first javadoc comment preceeding the declaration
            int commentSourceStart = this.scanner.commentStarts[lastCommentIndex];
            // javadoc only (non javadoc comment have negative start and/or end positions.)
            if ((commentSourceStart < 0) || (this.modifiersSourceStart != -1 && this.modifiersSourceStart < commentSourceStart) || (this.scanner.commentStops[lastCommentIndex] < 0)) {
                continue nextComment;
            }
            checkDeprecated = true;
            //stop is one over
            int commentSourceEnd = this.scanner.commentStops[lastCommentIndex] - 1;
            // do not report problem before last parsed comment while recovering code...
            if (this.javadocParser.shouldReportProblems) {
                this.javadocParser.reportProblems = this.currentElement == null || commentSourceEnd > this.lastJavadocEnd;
            } else {
                this.javadocParser.reportProblems = false;
            }
            deprecated = this.javadocParser.checkDeprecation(lastCommentIndex);
            this.javadoc = this.javadocParser.docComment;
            if (this.currentElement == null)
                this.lastJavadocEnd = commentSourceEnd;
            break nextComment;
        }
        if (deprecated) {
            checkAndSetModifiers(ClassFileConstants.AccDeprecated);
        }
        // modify the modifier source start to point at the first comment
        if (lastCommentIndex >= 0 && checkDeprecated) {
            int lastCommentStart = this.scanner.commentStarts[lastCommentIndex];
            if (lastCommentStart < 0)
                lastCommentStart = -lastCommentStart;
            if (// only if there is no "for" in between
            this.forStartPosition == 0 || this.forStartPosition < lastCommentStart) {
                this.modifiersSourceStart = lastCommentStart;
            }
        }
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.parser.Parser#consumeAnnotationTypeDeclarationHeader()
	 */
    protected void consumeAnnotationTypeDeclarationHeader() {
        pushOnCommentsStack(0, this.scanner.commentPtr);
        super.consumeAnnotationTypeDeclarationHeader();
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.parser.Parser#consumeClassHeader()
	 */
    protected void consumeClassHeader() {
        pushOnCommentsStack(0, this.scanner.commentPtr);
        super.consumeClassHeader();
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.parser.Parser#consumeEmptyTypeDeclaration()
	 */
    protected void consumeEmptyTypeDeclaration() {
        pushOnCommentsStack(0, this.scanner.commentPtr);
        super.consumeEmptyTypeDeclaration();
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.parser.Parser#consumeEnterAnonymousClassBody(boolean)
	 */
    @Override
    protected void consumeEnterAnonymousClassBody(boolean qualified) {
        pushOnCommentsStack(0, this.scanner.commentPtr);
        super.consumeEnterAnonymousClassBody(qualified);
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.parser.Parser#consumeEnumHeader()
	 */
    protected void consumeEnumHeader() {
        pushOnCommentsStack(0, this.scanner.commentPtr);
        super.consumeEnumHeader();
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.parser.Parser#consumeInterfaceHeader()
	 */
    protected void consumeInterfaceHeader() {
        pushOnCommentsStack(0, this.scanner.commentPtr);
        super.consumeInterfaceHeader();
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.parser.Parser#endParse(int)
	 */
    protected CompilationUnitDeclaration endParse(int act) {
        CompilationUnitDeclaration unit = super.endParse(act);
        if (unit.comments == null) {
            pushOnCommentsStack(0, this.scanner.commentPtr);
            unit.comments = getCommentsPositions();
        }
        return unit;
    }

    /* (non-Javadoc)
	 * Save all source comments currently stored before flushing them.
	 * @see org.eclipse.jdt.internal.compiler.parser.Parser#flushCommentsDefinedPriorTo(int)
	 */
    public int flushCommentsDefinedPriorTo(int position) {
        int lastCommentIndex = this.scanner.commentPtr;
        // no comment
        if (lastCommentIndex < 0)
            return position;
        // compute the index of the first obsolete comment
        int index = lastCommentIndex;
        int validCount = 0;
        while (index >= 0) {
            int commentEnd = this.scanner.commentStops[index];
            // negative end position for non-javadoc comments
            if (commentEnd < 0)
                commentEnd = -commentEnd;
            if (commentEnd <= position) {
                break;
            }
            index--;
            validCount++;
        }
        // flush this comment and shift <position> to the comment end.
        if (validCount > 0) {
            int immediateCommentEnd = 0;
            while (// only tolerating non-javadoc comments (non-javadoc comment end positions are negative)
            index < lastCommentIndex && (immediateCommentEnd = -this.scanner.commentStops[index + 1]) > 0) {
                // is there any line break until the end of the immediate comment ? (thus only tolerating line comment)
                // comment end in one char too far
                immediateCommentEnd--;
                if (org.eclipse.jdt.internal.compiler.util.Util.getLineNumber(position, this.scanner.lineEnds, 0, this.scanner.linePtr) != org.eclipse.jdt.internal.compiler.util.Util.getLineNumber(immediateCommentEnd, this.scanner.lineEnds, 0, this.scanner.linePtr))
                    break;
                position = immediateCommentEnd;
                // flush this comment
                validCount--;
                index++;
            }
        }
        // no obsolete comment
        if (index < 0)
            return position;
        // store comment before flushing them
        pushOnCommentsStack(0, index);
        switch(validCount) {
            case 0:
                // do nothing
                break;
            // move valid comment infos, overriding obsolete comment infos
            case 2:
                this.scanner.commentStarts[0] = this.scanner.commentStarts[index + 1];
                this.scanner.commentStops[0] = this.scanner.commentStops[index + 1];
                this.scanner.commentTagStarts[0] = this.scanner.commentTagStarts[index + 1];
                this.scanner.commentStarts[1] = this.scanner.commentStarts[index + 2];
                this.scanner.commentStops[1] = this.scanner.commentStops[index + 2];
                this.scanner.commentTagStarts[1] = this.scanner.commentTagStarts[index + 2];
                break;
            case 1:
                this.scanner.commentStarts[0] = this.scanner.commentStarts[index + 1];
                this.scanner.commentStops[0] = this.scanner.commentStops[index + 1];
                this.scanner.commentTagStarts[0] = this.scanner.commentTagStarts[index + 1];
                break;
            default:
                System.arraycopy(this.scanner.commentStarts, index + 1, this.scanner.commentStarts, 0, validCount);
                System.arraycopy(this.scanner.commentStops, index + 1, this.scanner.commentStops, 0, validCount);
                System.arraycopy(this.scanner.commentTagStarts, index + 1, this.scanner.commentTagStarts, 0, validCount);
        }
        this.scanner.commentPtr = validCount - 1;
        return position;
    }

    /*
	 * Build a n*2 matrix of comments positions.
	 * For each position, 0 is for start position and 1 for end position of the comment.
	 */
    public int[][] getCommentsPositions() {
        int[][] positions = new int[this.commentPtr + 1][2];
        for (int i = 0, max = this.commentPtr; i <= max; i++) {
            positions[i][0] = this.commentStarts[i];
            positions[i][1] = this.commentStops[i];
        }
        return positions;
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.parser.Parser#initialize()
	 */
    public void initialize(boolean parsingCompilationUnit) {
        super.initialize(parsingCompilationUnit);
        this.commentPtr = -1;
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.parser.Parser#initialize()
	 */
    public void initialize() {
        super.initialize();
        this.commentPtr = -1;
    }

    /* (non-Javadoc)
	 * Create and store a specific comment recorder scanner.
	 * @see org.eclipse.jdt.internal.compiler.parser.Parser#initializeScanner()
	 */
    public void initializeScanner() {
        this.scanner = new Scanner(/*comment*/
        false, /*whitespace*/
        false, this.options.getSeverity(CompilerOptions.NonExternalizedString) != ProblemSeverities/*nls*/
        .Ignore, this.options.sourceLevel, /*sourceLevel*/
        this.options.taskTags, this.options.taskPriorities, /*taskPriorities*/
        this.options.isTaskCaseSensitive);
    }

    /*
	 * Push all stored comments in stack.
	 */
    private void pushOnCommentsStack(int start, int end) {
        for (int i = start; i <= end; i++) {
            // First see if comment hasn't been already stored
            int scannerStart = this.scanner.commentStarts[i] < 0 ? -this.scanner.commentStarts[i] : this.scanner.commentStarts[i];
            int commentStart = this.commentPtr == -1 ? -1 : (this.commentStarts[this.commentPtr] < 0 ? -this.commentStarts[this.commentPtr] : this.commentStarts[this.commentPtr]);
            if (commentStart == -1 || scannerStart > commentStart) {
                int stackLength = this.commentStarts.length;
                if (++this.commentPtr >= stackLength) {
                    System.arraycopy(this.commentStarts, 0, this.commentStarts = new int[stackLength + CommentIncrement], 0, stackLength);
                    System.arraycopy(this.commentStops, 0, this.commentStops = new int[stackLength + CommentIncrement], 0, stackLength);
                }
                this.commentStarts[this.commentPtr] = this.scanner.commentStarts[i];
                this.commentStops[this.commentPtr] = this.scanner.commentStops[i];
            }
        }
    }

    /* (non-Javadoc)
	 * Save all source comments currently stored before flushing them.
	 * this.scanner.commentPtr is expected *not* yet being reset before calling this method.
	 * @see org.eclipse.jdt.internal.compiler.parser.Parser#resetModifiers()
	 */
    protected void resetModifiers() {
        pushOnCommentsStack(0, this.scanner.commentPtr);
        super.resetModifiers();
    }
}
