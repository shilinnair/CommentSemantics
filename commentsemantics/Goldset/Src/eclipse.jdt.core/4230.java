/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Herrmann - Contributions for
 *     						bug 325755 - [compiler] wrong initialization state after conditional expression
 *     						bug 320170 - [compiler] [null] Whitebox issues in null analysis
 *     						bug 292478 - Report potentially null across variable assignment
 *     						bug 332637 - Dead Code detection removing code that isn't dead
 *     						bug 341499 - [compiler][null] allocate extra bits in all methods of UnconditionalFlowInfo
 *     						bug 349326 - [1.7] new warning for missing try-with-resources
 *							bug 345305 - [compiler][null] Compiler misidentifies a case of "variable can only be null"
 *							bug 386181 - [compiler][null] wrong transition in UnconditionalFlowInfo.mergedWith()
 *							bug 394768 - [compiler][resource] Incorrect resource leak warning when creating stream in conditional
 *							Bug 453483 - [compiler][null][loop] Improve null analysis for loops
 *							Bug 454031 - [compiler][null][loop] bug in null analysis; wrong "dead code" detection
 *							Bug 421035 - [resource] False alarm of resource leak warning when casting a closeable in its assignment
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.flow;

import java.util.Arrays;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;

/**
 * Record initialization status during definite assignment analysis
 *
 * No caching of pre-allocated instances.
 */
public class UnconditionalFlowInfo extends FlowInfo {

    /**
	 * Exception raised when unexpected behavior is detected.
	 */
    public static class AssertionFailedException extends RuntimeException {

        private static final long serialVersionUID = 1827352841030089703L;

        public  AssertionFailedException(String message) {
            super(message);
        }
    }

    // Coverage tests need that the code be instrumented. The following flag
    // controls whether the instrumented code is compiled in or not, and whether
    // the coverage tests methods run or not.
    public static final boolean COVERAGE_TEST_FLAG = false;

    // never release with the coverageTestFlag set to true
    public static int CoverageTestId;

    // assignment bits - first segment
    public long definiteInits;

    public long potentialInits;

    // null bits - first segment
    public long nullBit1, nullBit2, nullBit3, nullBit4;

    /*
		nullBit1
		 nullBit2...
		0000	start
		0001	pot. unknown
		0010	pot. non null
		0011	pot. nn & pot. un
		0100	pot. null
		0101	pot. n & pot. un
		0110	pot. n & pot. nn
		0111    pot. n & pot. nn & pot. un
		1001	def. unknown
		1010	def. non null
		1011	pot. nn & prot. nn
		1100	def. null
		1101	pot. n & prot. n
		1110	prot. null
		1111	prot. non null
 */
    public long // can an incoming null value reach the current point?
    iNBit, // can an incoming nonnull value reach the current point?
    iNNBit;

    // extra segments
    public static final int extraLength = 8;

    public long extra[][];

    // extra bit fields for larger numbers of fields/variables
    // extra[0] holds definiteInits values, extra[1] potentialInits, etc.
    // extra[1+1]... corresponds to nullBits1 ...
    // extra[IN] is iNBit
    // extra[INN] is iNNBit
    // lifecycle is extra == null or else all extra[]'s are allocated
    // arrays which have the same size
    // limit between fields and locals
    public int maxFieldCount;

    // Constants
    // 64 bits in a long.
    public static final int BitCacheSize = 64;

    public static final int IN = 6;

    public static final int INN = 7;

    /* fakeInitializedFlowInfo: For Lambda expressions tentative analysis during overload resolution. 
   We presume that any and all outer locals touched by the lambda are definitely assigned and 
   effectively final. Whether they are or not is immaterial for overload analysis (errors encountered
   in the body are not supposed to influence the resolution. It is pertinent only for the eventual 
   resolution/analysis post overload resolution. For lambda's the problem is that we start the control/data
   flow analysis abruptly at the start of the lambda, so we need to present a cogent world view and hence 
   all this charade.
*/
    public static UnconditionalFlowInfo fakeInitializedFlowInfo(int localsCount, int maxFieldCount) {
        UnconditionalFlowInfo flowInfo = new UnconditionalFlowInfo();
        flowInfo.maxFieldCount = maxFieldCount;
        for (int i = 0; i < localsCount; i++) flowInfo.markAsDefinitelyAssigned(i + maxFieldCount);
        return flowInfo;
    }

    public FlowInfo addInitializationsFrom(FlowInfo inits) {
        return addInfoFrom(inits, true);
    }

    public FlowInfo addNullInfoFrom(FlowInfo inits) {
        return addInfoFrom(inits, false);
    }

    private FlowInfo addInfoFrom(FlowInfo inits, boolean handleInits) {
        if (this == DEAD_END)
            return this;
        if (inits == DEAD_END)
            return this;
        UnconditionalFlowInfo otherInits = inits.unconditionalInits();
        if (handleInits) {
            // union of definitely assigned variables,
            this.definiteInits |= otherInits.definiteInits;
            // union of potentially set ones
            this.potentialInits |= otherInits.potentialInits;
        }
        // combine null information
        boolean thisHadNulls = (this.tagBits & NULL_FLAG_MASK) != 0, otherHasNulls = (otherInits.tagBits & NULL_FLAG_MASK) != 0;
        //	if ((otherInits.iNNBit | otherInits.iNBit) == 0)
        //		thisHadNulls = false; // suppress incoming null info, if none shines through in other
        long a1, a2, a3, a4, na1, na2, na3, na4, b1, b2, b3, b4, nb1, nb2, nb3, nb4;
        if (otherHasNulls) {
            if (!thisHadNulls) {
                this.nullBit1 = otherInits.nullBit1;
                this.nullBit2 = otherInits.nullBit2;
                this.nullBit3 = otherInits.nullBit3;
                this.nullBit4 = otherInits.nullBit4;
                this.iNBit = otherInits.iNBit;
                this.iNNBit = otherInits.iNNBit;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 1) {
                        this.nullBit4 = ~0;
                    }
                }
            } else {
                a1 = this.nullBit1;
                a2 = this.nullBit2;
                a3 = this.nullBit3;
                a4 = this.nullBit4;
                // state that breaks the correlation between bits and n or nn, used below:
                long protNN1111 = a1 & a2 & a3 & a4;
                // filter 'a' using iNBit,iNNBit from otherInits:
                // this implements that otherInit does not accept certain bits which are known to be superseded by info in otherInits.			
                long acceptNonNull = otherInits.iNNBit;
                long acceptNull = otherInits.iNBit | // for 1111 don't bother suppressing incoming null, logic operation would produce wrong result
                protNN1111;
                // for 1111 & ~acceptNonNull we reset all bits to 0000
                long dontResetToStart = ~protNN1111 | acceptNonNull;
                a1 &= dontResetToStart;
                a2 = dontResetToStart & acceptNull & a2;
                a3 = dontResetToStart & acceptNonNull & a3;
                a4 &= dontResetToStart;
                // translate 1000 (undefined state) to 0000
                a1 &= (a2 | a3 | a4);
                this.nullBit1 = (b1 = otherInits.nullBit1) | a1 & (a3 & a4 & (nb2 = ~(b2 = otherInits.nullBit2)) & (nb4 = ~(b4 = otherInits.nullBit4)) | ((na4 = ~a4) | (na3 = ~a3)) & ((na2 = ~a2) & nb2 | a2 & (nb3 = ~(b3 = otherInits.nullBit3)) & nb4));
                this.nullBit2 = b2 & (nb4 | nb3) | na3 & na4 & b2 | a2 & (nb3 & nb4 | (nb1 = ~b1) & (na3 | (na1 = ~a1)) | a1 & b2);
                this.nullBit3 = b3 & (nb1 & (b2 | a2 | na1) | b1 & (b4 | nb2 | a1 & a3) | na1 & na2 & na4) | a3 & nb2 & nb4 | nb1 & ((na2 & a4 | na1) & a3 | a1 & na2 & na4 & b2);
                this.nullBit4 = nb1 & (a4 & (na3 & nb3 | (a3 | na2) & nb2) | a1 & (a3 & nb2 & b4 | a2 & b2 & (b4 | a3 & na4 & nb3))) | b1 & (a3 & a4 & b4 | na2 & na4 & nb3 & b4 | a2 & ((b3 | a4) & b4 | na3 & a4 & b2 & b3) | na1 & (b4 | (a4 | a2) & b2 & b3)) | (na1 & (na3 & nb3 | na2 & nb2) | a1 & (nb2 & nb3 | a2 & a3)) & b4;
                // unconditional sequence, must shine through both to shine through in the end:
                this.iNBit &= otherInits.iNBit;
                this.iNNBit &= otherInits.iNNBit;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 2) {
                        this.nullBit4 = ~0;
                    }
                }
            }
            // in all cases - avoid forgetting extras
            this.tagBits |= NULL_FLAG_MASK;
        }
        // treating extra storage
        if (this.extra != null || otherInits.extra != null) {
            int mergeLimit = 0, copyLimit = 0;
            if (this.extra != null) {
                if (otherInits.extra != null) {
                    // both sides have extra storage
                    int length, otherLength;
                    if ((length = this.extra[0].length) < (otherLength = otherInits.extra[0].length)) {
                        // current storage is shorter -> grow current
                        for (int j = 0; j < extraLength; j++) {
                            System.arraycopy(this.extra[j], 0, (this.extra[j] = new long[otherLength]), 0, length);
                        }
                        mergeLimit = length;
                        copyLimit = otherLength;
                        if (COVERAGE_TEST_FLAG) {
                            if (CoverageTestId == 3) {
                                throw new //$NON-NLS-1$
                                AssertionFailedException(//$NON-NLS-1$
                                "COVERAGE 3");
                            }
                        }
                    } else {
                        // current storage is longer
                        mergeLimit = otherLength;
                        if (COVERAGE_TEST_FLAG) {
                            if (CoverageTestId == 4) {
                                throw new //$NON-NLS-1$
                                AssertionFailedException(//$NON-NLS-1$
                                "COVERAGE 4");
                            }
                        }
                    }
                }
            } else if (otherInits.extra != null) {
                // no storage here, but other has extra storage.
                // shortcut regular copy because array copy is better
                int otherLength;
                this.extra = new long[extraLength][];
                System.arraycopy(otherInits.extra[0], 0, (this.extra[0] = new long[otherLength = otherInits.extra[0].length]), 0, otherLength);
                System.arraycopy(otherInits.extra[1], 0, (this.extra[1] = new long[otherLength]), 0, otherLength);
                if (otherHasNulls) {
                    for (int j = 2; j < extraLength; j++) {
                        System.arraycopy(otherInits.extra[j], 0, (this.extra[j] = new long[otherLength]), 0, otherLength);
                    }
                    if (COVERAGE_TEST_FLAG) {
                        if (CoverageTestId == 5) {
                            this.extra[5][otherLength - 1] = ~0;
                        }
                    }
                } else {
                    for (int j = 2; j < extraLength; j++) {
                        this.extra[j] = new long[otherLength];
                    }
                    System.arraycopy(otherInits.extra[IN], 0, this.extra[IN], 0, otherLength);
                    System.arraycopy(otherInits.extra[INN], 0, this.extra[INN], 0, otherLength);
                    if (COVERAGE_TEST_FLAG) {
                        if (CoverageTestId == 6) {
                            throw new //$NON-NLS-1$
                            AssertionFailedException(//$NON-NLS-1$
                            "COVERAGE 6");
                        }
                    }
                }
            }
            int i;
            if (handleInits) {
                // manage definite assignment info
                for (i = 0; i < mergeLimit; i++) {
                    this.extra[0][i] |= otherInits.extra[0][i];
                    this.extra[1][i] |= otherInits.extra[1][i];
                }
                for (; i < copyLimit; i++) {
                    this.extra[0][i] = otherInits.extra[0][i];
                    this.extra[1][i] = otherInits.extra[1][i];
                }
            }
            // tweak limits for nulls
            if (!thisHadNulls) {
                if (copyLimit < mergeLimit) {
                    copyLimit = mergeLimit;
                }
                mergeLimit = 0;
            }
            if (!otherHasNulls) {
                copyLimit = 0;
                mergeLimit = 0;
            }
            for (i = 0; i < mergeLimit; i++) {
                a1 = this.extra[1 + 1][i];
                a2 = this.extra[2 + 1][i];
                a3 = this.extra[3 + 1][i];
                a4 = this.extra[4 + 1][i];
                // state that breaks the correlation between bits and n or nn, used below:
                long protNN1111 = a1 & a2 & a3 & a4;
                // filter 'a' using iNBit,iNNBit from otherInits:
                // this implements that otherInit does not accept certain bits which are known to be superseded by info in otherInits.			
                long acceptNonNull = otherInits.extra[INN][i];
                long acceptNull = otherInits.extra[IN][i] | // for 1111 don't bother suppressing incoming null, logic operation would produce wrong result
                protNN1111;
                // for 1111 & ~acceptNonNull we reset all bits to 0000
                long dontResetToStart = ~protNN1111 | acceptNonNull;
                a1 &= dontResetToStart;
                a2 = dontResetToStart & acceptNull & a2;
                a3 = dontResetToStart & acceptNonNull & a3;
                a4 &= dontResetToStart;
                // translate 1000 (undefined state) to 0000
                a1 &= (a2 | a3 | a4);
                this.extra[1 + 1][i] = (b1 = otherInits.extra[1 + 1][i]) | a1 & (a3 & a4 & (nb2 = ~(b2 = otherInits.extra[2 + 1][i])) & (nb4 = ~(b4 = otherInits.extra[4 + 1][i])) | ((na4 = ~a4) | (na3 = ~a3)) & ((na2 = ~a2) & nb2 | a2 & (nb3 = ~(b3 = otherInits.extra[3 + 1][i])) & nb4));
                this.extra[2 + 1][i] = b2 & (nb4 | nb3) | na3 & na4 & b2 | a2 & (nb3 & nb4 | (nb1 = ~b1) & (na3 | (na1 = ~a1)) | a1 & b2);
                this.extra[3 + 1][i] = b3 & (nb1 & (b2 | a2 | na1) | b1 & (b4 | nb2 | a1 & a3) | na1 & na2 & na4) | a3 & nb2 & nb4 | nb1 & ((na2 & a4 | na1) & a3 | a1 & na2 & na4 & b2);
                this.extra[4 + 1][i] = nb1 & (a4 & (na3 & nb3 | (a3 | na2) & nb2) | a1 & (a3 & nb2 & b4 | a2 & b2 & (b4 | a3 & na4 & nb3))) | b1 & (a3 & a4 & b4 | na2 & na4 & nb3 & b4 | a2 & ((b3 | a4) & b4 | na3 & a4 & b2 & b3) | na1 & (b4 | (a4 | a2) & b2 & b3)) | (na1 & (na3 & nb3 | na2 & nb2) | a1 & (nb2 & nb3 | a2 & a3)) & b4;
                // unconditional sequence, must shine through both to shine through in the end:
                this.extra[IN][i] &= otherInits.extra[IN][i];
                this.extra[INN][i] &= otherInits.extra[INN][i];
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 7) {
                        this.extra[5][i] = ~0;
                    }
                }
            }
            for (; i < copyLimit; i++) {
                for (int j = 2; j < extraLength; j++) {
                    this.extra[j][i] = otherInits.extra[j][i];
                }
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 8) {
                        this.extra[5][i] = ~0;
                    }
                }
            }
        }
        return this;
    }

    public FlowInfo addPotentialInitializationsFrom(FlowInfo inits) {
        if (this == DEAD_END) {
            return this;
        }
        if (inits == DEAD_END) {
            return this;
        }
        UnconditionalFlowInfo otherInits = inits.unconditionalInits();
        // union of potentially set ones
        this.potentialInits |= otherInits.potentialInits;
        // treating extra storage
        if (this.extra != null) {
            if (otherInits.extra != null) {
                // both sides have extra storage
                int i = 0, length, otherLength;
                if ((length = this.extra[0].length) < (otherLength = otherInits.extra[0].length)) {
                    // current storage is shorter -> grow current
                    for (int j = 0; j < extraLength; j++) {
                        System.arraycopy(this.extra[j], 0, (this.extra[j] = new long[otherLength]), 0, length);
                    }
                    for (; i < length; i++) {
                        this.extra[1][i] |= otherInits.extra[1][i];
                    }
                    for (; i < otherLength; i++) {
                        this.extra[1][i] = otherInits.extra[1][i];
                    }
                } else {
                    // current storage is longer
                    for (; i < otherLength; i++) {
                        this.extra[1][i] |= otherInits.extra[1][i];
                    }
                }
            }
        } else if (otherInits.extra != null) {
            // no storage here, but other has extra storage.
            int otherLength = otherInits.extra[0].length;
            createExtraSpace(otherLength);
            System.arraycopy(otherInits.extra[1], 0, this.extra[1], 0, otherLength);
        }
        addPotentialNullInfoFrom(otherInits);
        return this;
    }

    /**
 * Compose other inits over this flow info, then return this. The operation
 * semantics are to wave into this flow info the consequences upon null
 * information of a possible path into the operations that resulted into
 * otherInits. The fact that this path may be left unexecuted under peculiar
 * conditions results into less specific results than
 * {@link #addInitializationsFrom(FlowInfo) addInitializationsFrom}; moreover,
 * only the null information is affected.
 * @param otherInits other null inits to compose over this
 * @return this, modified according to otherInits information
 */
    public UnconditionalFlowInfo addPotentialNullInfoFrom(UnconditionalFlowInfo otherInits) {
        if ((this.tagBits & UNREACHABLE) != 0 || (otherInits.tagBits & UNREACHABLE) != 0 || (otherInits.tagBits & NULL_FLAG_MASK) == 0) {
            return this;
        }
        // if we get here, otherInits has some null info
        boolean thisHadNulls = (this.tagBits & NULL_FLAG_MASK) != 0, thisHasNulls = false;
        long a1, a2, a3, a4, na1, na2, na3, na4, b1, b2, b3, b4, nb1, nb2, nb3, nb4;
        if (thisHadNulls) {
            this.nullBit1 = (a1 = this.nullBit1) & ((a3 = this.nullBit3) & (a4 = this.nullBit4) & ((nb2 = ~(b2 = otherInits.nullBit2)) & (nb4 = ~(b4 = otherInits.nullBit4)) | (b1 = otherInits.nullBit1) & (b3 = otherInits.nullBit3)) | (na2 = ~(a2 = this.nullBit2)) & (b1 & b3 | ((na4 = ~a4) | (na3 = ~a3)) & nb2) | a2 & ((na4 | na3) & ((nb3 = ~b3) & nb4 | b1 & b2)));
            this.nullBit2 = b2 & (nb3 | (nb1 = ~b1)) | a2 & (nb3 & nb4 | b2 | na3 | (na1 = ~a1));
            this.nullBit3 = b3 & (nb1 & b2 | a2 & (nb2 | a3) | na1 & nb2 | a1 & na2 & na4 & b1) | a3 & (nb2 & nb4 | na2 & a4 | na1) | a1 & na2 & na4 & b2;
            this.nullBit4 = na3 & (nb1 & nb3 & b4 | a4 & (nb3 | b1 & b2)) | nb2 & (na3 & b1 & nb3 | na2 & (nb1 & b4 | b1 & nb3 | a4)) | a3 & (a4 & (nb2 | b1 & b3) | a1 & a2 & (nb1 & b4 | na4 & (b2 | b1) & nb3));
            // this and then pot.other: leave iNBit & iNNBit untouched
            if (COVERAGE_TEST_FLAG) {
                if (CoverageTestId == 9) {
                    this.nullBit4 = ~0;
                }
            }
            if (//  bit1 is redundant
            (this.nullBit2 | this.nullBit3 | this.nullBit4) != 0) {
                thisHasNulls = true;
            }
        } else {
            this.nullBit1 = 0;
            this.nullBit2 = (b2 = otherInits.nullBit2) & ((nb3 = ~(b3 = otherInits.nullBit3)) | (nb1 = ~(b1 = otherInits.nullBit1)));
            this.nullBit3 = b3 & (nb1 | (nb2 = ~b2));
            this.nullBit4 = ~b1 & ~b3 & (b4 = otherInits.nullBit4) | ~b2 & (b1 & ~b3 | ~b1 & b4);
            // this and then pot.other: leave iNBit & iNNBit untouched
            if (COVERAGE_TEST_FLAG) {
                if (CoverageTestId == 10) {
                    this.nullBit4 = ~0;
                }
            }
            if (//  bit1 is redundant
            (this.nullBit2 | this.nullBit3 | this.nullBit4) != 0) {
                thisHasNulls = true;
            }
        }
        // extra storage management
        if (otherInits.extra != null) {
            int mergeLimit = 0, copyLimit = otherInits.extra[0].length;
            if (this.extra == null) {
                createExtraSpace(copyLimit);
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 11) {
                        throw new //$NON-NLS-1$
                        AssertionFailedException(//$NON-NLS-1$
                        "COVERAGE 11");
                    }
                }
            } else {
                mergeLimit = copyLimit;
                if (mergeLimit > this.extra[0].length) {
                    mergeLimit = this.extra[0].length;
                    for (int j = 0; j < extraLength; j++) {
                        System.arraycopy(this.extra[j], 0, this.extra[j] = new long[copyLimit], 0, mergeLimit);
                    }
                    if (!thisHadNulls) {
                        mergeLimit = 0;
                        // will do with a copy -- caveat: only valid because definite assignment bits copied above
                        if (COVERAGE_TEST_FLAG) {
                            if (CoverageTestId == 12) {
                                throw new //$NON-NLS-1$
                                AssertionFailedException(//$NON-NLS-1$
                                "COVERAGE 12");
                            }
                        }
                    }
                }
            }
            // PREMATURE skip operations for fields
            int i;
            for (i = 0; i < mergeLimit; i++) {
                this.extra[1 + 1][i] = (a1 = this.extra[1 + 1][i]) & ((a3 = this.extra[3 + 1][i]) & (a4 = this.extra[4 + 1][i]) & ((nb2 = ~(b2 = otherInits.extra[2 + 1][i])) & (nb4 = ~(b4 = otherInits.extra[4 + 1][i])) | (b1 = otherInits.extra[1 + 1][i]) & (b3 = otherInits.extra[3 + 1][i])) | (na2 = ~(a2 = this.extra[2 + 1][i])) & (b1 & b3 | ((na4 = ~a4) | (na3 = ~a3)) & nb2) | a2 & ((na4 | na3) & ((nb3 = ~b3) & nb4 | b1 & b2)));
                this.extra[2 + 1][i] = b2 & (nb3 | (nb1 = ~b1)) | a2 & (nb3 & nb4 | b2 | na3 | (na1 = ~a1));
                this.extra[3 + 1][i] = b3 & (nb1 & b2 | a2 & (nb2 | a3) | na1 & nb2 | a1 & na2 & na4 & b1) | a3 & (nb2 & nb4 | na2 & a4 | na1) | a1 & na2 & na4 & b2;
                this.extra[4 + 1][i] = na3 & (nb1 & nb3 & b4 | a4 & (nb3 | b1 & b2)) | nb2 & (na3 & b1 & nb3 | na2 & (nb1 & b4 | b1 & nb3 | a4)) | a3 & (a4 & (nb2 | b1 & b3) | a1 & a2 & (nb1 & b4 | na4 & (b2 | b1) & nb3));
                // this and then pot.other: leave iNBit & iNNBit untouched
                if (//  bit1 is redundant
                (this.extra[2 + 1][i] | this.extra[3 + 1][i] | this.extra[4 + 1][i]) != 0) {
                    thisHasNulls = true;
                }
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 13) {
                        this.nullBit4 = ~0;
                    }
                }
            }
            for (; i < copyLimit; i++) {
                this.extra[1 + 1][i] = 0;
                this.extra[2 + 1][i] = (b2 = otherInits.extra[2 + 1][i]) & ((nb3 = ~(b3 = otherInits.extra[3 + 1][i])) | (nb1 = ~(b1 = otherInits.extra[1 + 1][i])));
                this.extra[3 + 1][i] = b3 & (nb1 | (nb2 = ~b2));
                this.extra[4 + 1][i] = ~b1 & ~b3 & (b4 = otherInits.extra[4 + 1][i]) | ~b2 & (b1 & ~b3 | ~b1 & b4);
                // this and then pot.other: leave iNBit & iNNBit untouched
                if (//  bit1 is redundant
                (this.extra[2 + 1][i] | this.extra[3 + 1][i] | this.extra[4 + 1][i]) != 0) {
                    thisHasNulls = true;
                }
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 14) {
                        this.extra[5][i] = ~0;
                    }
                }
            }
        }
        if (thisHasNulls) {
            this.tagBits |= NULL_FLAG_MASK;
        } else {
            this.tagBits &= NULL_FLAG_MASK;
        }
        return this;
    }

    public final boolean cannotBeDefinitelyNullOrNonNull(LocalVariableBinding local) {
        if ((this.tagBits & NULL_FLAG_MASK) == 0 || (local.type.tagBits & TagBits.IsBaseType) != 0) {
            return false;
        }
        int position;
        if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
            // use bits
            return ((~this.nullBit1 & (this.nullBit2 & this.nullBit3 | this.nullBit4) | ~this.nullBit2 & ~this.nullBit3 & this.nullBit4) & (1L << position)) != 0;
        }
        // use extra vector
        if (this.extra == null) {
            // if vector not yet allocated, then not initialized
            return false;
        }
        int vectorIndex;
        if ((vectorIndex = (position / BitCacheSize) - 1) >= this.extra[0].length) {
            // if not enough room in vector, then not initialized
            return false;
        }
        long a2, a3, a4;
        return ((~this.extra[2][vectorIndex] & ((a2 = this.extra[3][vectorIndex]) & (a3 = this.extra[4][vectorIndex]) | (a4 = this.extra[5][vectorIndex])) | ~a2 & ~a3 & a4) & (1L << (position % BitCacheSize))) != 0;
    }

    public final boolean cannotBeNull(LocalVariableBinding local) {
        if ((this.tagBits & NULL_FLAG_MASK) == 0 || (local.type.tagBits & TagBits.IsBaseType) != 0) {
            return false;
        }
        int position;
        if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
            // use bits
            return (this.nullBit1 & this.nullBit3 & ((this.nullBit2 & this.nullBit4) | ~this.nullBit2) & (1L << position)) != 0;
        }
        // use extra vector
        if (this.extra == null) {
            // if vector not yet allocated, then not initialized
            return false;
        }
        int vectorIndex;
        if ((vectorIndex = (position / BitCacheSize) - 1) >= this.extra[0].length) {
            // if not enough room in vector, then not initialized
            return false;
        }
        return (this.extra[2][vectorIndex] & this.extra[4][vectorIndex] & ((this.extra[3][vectorIndex] & this.extra[5][vectorIndex]) | ~this.extra[3][vectorIndex]) & (1L << (position % BitCacheSize))) != 0;
    }

    public final boolean canOnlyBeNull(LocalVariableBinding local) {
        if ((this.tagBits & NULL_FLAG_MASK) == 0 || (local.type.tagBits & TagBits.IsBaseType) != 0) {
            return false;
        }
        int position;
        if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
            // use bits
            return (this.nullBit1 & this.nullBit2 & (~this.nullBit3 | ~this.nullBit4) & (1L << position)) != 0;
        }
        // use extra vector
        if (this.extra == null) {
            // if vector not yet allocated, then not initialized
            return false;
        }
        int vectorIndex;
        if ((vectorIndex = (position / BitCacheSize) - 1) >= this.extra[0].length) {
            // if not enough room in vector, then not initialized
            return false;
        }
        return (this.extra[2][vectorIndex] & this.extra[3][vectorIndex] & (~this.extra[4][vectorIndex] | ~this.extra[5][vectorIndex]) & (1L << (position % BitCacheSize))) != 0;
    }

    public FlowInfo copy() {
        // do not clone the DeadEnd
        if (this == DEAD_END) {
            return this;
        }
        UnconditionalFlowInfo copy = new UnconditionalFlowInfo();
        // copy slots
        copy.definiteInits = this.definiteInits;
        copy.potentialInits = this.potentialInits;
        boolean hasNullInfo = (this.tagBits & NULL_FLAG_MASK) != 0;
        if (hasNullInfo) {
            copy.nullBit1 = this.nullBit1;
            copy.nullBit2 = this.nullBit2;
            copy.nullBit3 = this.nullBit3;
            copy.nullBit4 = this.nullBit4;
        }
        copy.iNBit = this.iNBit;
        copy.iNNBit = this.iNNBit;
        copy.tagBits = this.tagBits;
        copy.maxFieldCount = this.maxFieldCount;
        if (this.extra != null) {
            int length;
            copy.extra = new long[extraLength][];
            System.arraycopy(this.extra[0], 0, (copy.extra[0] = new long[length = this.extra[0].length]), 0, length);
            System.arraycopy(this.extra[1], 0, (copy.extra[1] = new long[length]), 0, length);
            if (hasNullInfo) {
                for (int j = 2; j < extraLength; j++) {
                    System.arraycopy(this.extra[j], 0, (copy.extra[j] = new long[length]), 0, length);
                }
            } else {
                for (int j = 2; j < extraLength; j++) {
                    copy.extra[j] = new long[length];
                }
            }
        }
        return copy;
    }

    /**
 * Discard definite inits and potential inits from this, then return this.
 * The returned flow info only holds null related information.
 * @return this flow info, minus definite inits and potential inits
 */
    public UnconditionalFlowInfo discardInitializationInfo() {
        if (this == DEAD_END) {
            return this;
        }
        this.definiteInits = this.potentialInits = 0;
        if (this.extra != null) {
            for (int i = 0, length = this.extra[0].length; i < length; i++) {
                this.extra[0][i] = this.extra[1][i] = 0;
            }
        }
        return this;
    }

    /**
 * Remove local variables information from this flow info and return this.
 * @return this, deprived from any local variable information
 */
    public UnconditionalFlowInfo discardNonFieldInitializations() {
        int limit = this.maxFieldCount;
        if (limit < BitCacheSize) {
            long mask = (1L << limit) - 1;
            this.definiteInits &= mask;
            this.potentialInits &= mask;
            this.nullBit1 &= mask;
            this.nullBit2 &= mask;
            this.nullBit3 &= mask;
            this.nullBit4 &= mask;
            this.iNBit &= mask;
            this.iNNBit &= mask;
        }
        // use extra vector
        if (this.extra == null) {
            // if vector not yet allocated, then not initialized
            return this;
        }
        int vectorIndex, length = this.extra[0].length;
        if ((vectorIndex = (limit / BitCacheSize) - 1) >= length) {
            // not enough room yet
            return this;
        }
        if (vectorIndex >= 0) {
            // else we only have complete non field array items left
            long mask = (1L << (limit % BitCacheSize)) - 1;
            for (int j = 0; j < extraLength; j++) {
                this.extra[j][vectorIndex] &= mask;
            }
        }
        for (int i = vectorIndex + 1; i < length; i++) {
            for (int j = 0; j < extraLength; j++) {
                this.extra[j][i] = 0;
            }
        }
        return this;
    }

    public FlowInfo initsWhenFalse() {
        return this;
    }

    public FlowInfo initsWhenTrue() {
        return this;
    }

    /**
 * Check status of definite assignment at a given position.
 * It deals with the dual representation of the InitializationInfo2:
 * bits for the first 64 entries, then an array of booleans.
 */
    private final boolean isDefinitelyAssigned(int position) {
        if (position < BitCacheSize) {
            // use bits
            return (this.definiteInits & (1L << position)) != 0;
        }
        // use extra vector
        if (this.extra == null)
            // if vector not yet allocated, then not initialized
            return false;
        int vectorIndex;
        if ((vectorIndex = (position / BitCacheSize) - 1) >= this.extra[0].length) {
            // if not enough room in vector, then not initialized
            return false;
        }
        return ((this.extra[0][vectorIndex]) & (1L << (position % BitCacheSize))) != 0;
    }

    public final boolean isDefinitelyAssigned(FieldBinding field) {
        // do not want to complain in unreachable code
        if ((this.tagBits & UNREACHABLE_OR_DEAD) != 0) {
            return true;
        }
        return isDefinitelyAssigned(field.id);
    }

    public final boolean isDefinitelyAssigned(LocalVariableBinding local) {
        // do not want to complain in unreachable code if local declared in reachable code
        if ((this.tagBits & UNREACHABLE_OR_DEAD) != 0 && (local.declaration.bits & ASTNode.IsLocalDeclarationReachable) != 0) {
            return true;
        }
        return isDefinitelyAssigned(local.id + this.maxFieldCount);
    }

    public final boolean isDefinitelyNonNull(LocalVariableBinding local) {
        // do not want to complain in unreachable code
        if ((this.tagBits & UNREACHABLE) != 0 || (this.tagBits & NULL_FLAG_MASK) == 0) {
            return false;
        }
        if ((local.type.tagBits & TagBits.IsBaseType) != 0 || // String instances
        local.constant() != Constant.NotAConstant) {
            return true;
        }
        int position = local.id + this.maxFieldCount;
        if (// use bits
        position < BitCacheSize) {
            return ((this.nullBit1 & this.nullBit3 & (~this.nullBit2 | this.nullBit4)) & (1L << position)) != 0;
        }
        // use extra vector
        if (this.extra == null) {
            // if vector not yet allocated, then not initialized
            return false;
        }
        int vectorIndex;
        if ((vectorIndex = (position / BitCacheSize) - 1) >= this.extra[2].length) {
            // if not enough room in vector, then not initialized
            return false;
        }
        return ((this.extra[2][vectorIndex] & this.extra[4][vectorIndex] & (~this.extra[3][vectorIndex] | this.extra[5][vectorIndex])) & (1L << (position % BitCacheSize))) != 0;
    }

    public final boolean isDefinitelyNull(LocalVariableBinding local) {
        // do not want to complain in unreachable code
        if ((this.tagBits & UNREACHABLE) != 0 || (this.tagBits & NULL_FLAG_MASK) == 0 || (local.type.tagBits & TagBits.IsBaseType) != 0) {
            return false;
        }
        int position = local.id + this.maxFieldCount;
        if (// use bits
        position < BitCacheSize) {
            return ((this.nullBit1 & this.nullBit2 & (~this.nullBit3 | ~this.nullBit4)) & (1L << position)) != 0;
        }
        // use extra vector
        if (this.extra == null) {
            // if vector not yet allocated, then not initialized
            return false;
        }
        int vectorIndex;
        if ((vectorIndex = (position / BitCacheSize) - 1) >= this.extra[2].length) {
            // if not enough room in vector, then not initialized
            return false;
        }
        return ((this.extra[2][vectorIndex] & this.extra[3][vectorIndex] & (~this.extra[4][vectorIndex] | ~this.extra[5][vectorIndex])) & (1L << (position % BitCacheSize))) != 0;
    }

    public final boolean isDefinitelyUnknown(LocalVariableBinding local) {
        // do not want to complain in unreachable code
        if ((this.tagBits & UNREACHABLE) != 0 || (this.tagBits & NULL_FLAG_MASK) == 0) {
            return false;
        }
        int position = local.id + this.maxFieldCount;
        if (// use bits
        position < BitCacheSize) {
            return ((this.nullBit1 & this.nullBit4 & ~this.nullBit2 & ~this.nullBit3) & (1L << position)) != 0;
        }
        // use extra vector
        if (this.extra == null) {
            // if vector not yet allocated, then not initialized
            return false;
        }
        int vectorIndex;
        if ((vectorIndex = (position / BitCacheSize) - 1) >= this.extra[2].length) {
            // if not enough room in vector, then not initialized
            return false;
        }
        return ((this.extra[2][vectorIndex] & this.extra[5][vectorIndex] & ~this.extra[3][vectorIndex] & ~this.extra[4][vectorIndex]) & (1L << (position % BitCacheSize))) != 0;
    }

    public final boolean hasNullInfoFor(LocalVariableBinding local) {
        // do not want to complain in unreachable code
        if ((this.tagBits & UNREACHABLE) != 0 || (this.tagBits & NULL_FLAG_MASK) == 0) {
            return false;
        }
        int position = local.id + this.maxFieldCount;
        if (// use bits
        position < BitCacheSize) {
            return ((this.nullBit1 | this.nullBit2 | this.nullBit3 | this.nullBit4) & (1L << position)) != 0;
        }
        // use extra vector
        if (this.extra == null) {
            // if vector not yet allocated, then not initialized
            return false;
        }
        int vectorIndex;
        if ((vectorIndex = (position / BitCacheSize) - 1) >= this.extra[2].length) {
            // if not enough room in vector, then not initialized
            return false;
        }
        return ((this.extra[2][vectorIndex] | this.extra[3][vectorIndex] | this.extra[4][vectorIndex] | this.extra[5][vectorIndex]) & (1L << (position % BitCacheSize))) != 0;
    }

    /**
 * Check status of potential assignment at a given position.
 */
    private final boolean isPotentiallyAssigned(int position) {
        // id is zero-based
        if (position < BitCacheSize) {
            // use bits
            return (this.potentialInits & (1L << position)) != 0;
        }
        // use extra vector
        if (this.extra == null) {
            // if vector not yet allocated, then not initialized
            return false;
        }
        int vectorIndex;
        if ((vectorIndex = (position / BitCacheSize) - 1) >= this.extra[0].length) {
            // if not enough room in vector, then not initialized
            return false;
        }
        return ((this.extra[1][vectorIndex]) & (1L << (position % BitCacheSize))) != 0;
    }

    public final boolean isPotentiallyAssigned(FieldBinding field) {
        return isPotentiallyAssigned(field.id);
    }

    public final boolean isPotentiallyAssigned(LocalVariableBinding local) {
        // final constants are inlined, and thus considered as always initialized
        if (local.constant() != Constant.NotAConstant) {
            return true;
        }
        return isPotentiallyAssigned(local.id + this.maxFieldCount);
    }

    // TODO (Ayush) Check why this method does not return true for protected non null (1111)
    public final boolean isPotentiallyNonNull(LocalVariableBinding local) {
        if ((this.tagBits & NULL_FLAG_MASK) == 0 || (local.type.tagBits & TagBits.IsBaseType) != 0) {
            return false;
        }
        int position;
        if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
            // use bits
            return ((this.nullBit3 & (~this.nullBit1 | ~this.nullBit2)) & (1L << position)) != 0;
        }
        // use extra vector
        if (this.extra == null) {
            // if vector not yet allocated, then not initialized
            return false;
        }
        int vectorIndex;
        if ((vectorIndex = (position / BitCacheSize) - 1) >= this.extra[2].length) {
            // if not enough room in vector, then not initialized
            return false;
        }
        return ((this.extra[4][vectorIndex] & (~this.extra[2][vectorIndex] | ~this.extra[3][vectorIndex])) & (1L << (position % BitCacheSize))) != 0;
    }

    // TODO (Ayush) Check why this method does not return true for protected null
    public final boolean isPotentiallyNull(LocalVariableBinding local) {
        if ((this.tagBits & NULL_FLAG_MASK) == 0 || (local.type.tagBits & TagBits.IsBaseType) != 0) {
            return false;
        }
        int position;
        if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
            // use bits
            return ((this.nullBit2 & (~this.nullBit1 | ~this.nullBit3)) & (1L << position)) != 0;
        }
        // use extra vector
        if (this.extra == null) {
            // if vector not yet allocated, then not initialized
            return false;
        }
        int vectorIndex;
        if ((vectorIndex = (position / BitCacheSize) - 1) >= this.extra[2].length) {
            // if not enough room in vector, then not initialized
            return false;
        }
        return ((this.extra[3][vectorIndex] & (~this.extra[2][vectorIndex] | ~this.extra[4][vectorIndex])) & (1L << (position % BitCacheSize))) != 0;
    }

    public final boolean isPotentiallyUnknown(LocalVariableBinding local) {
        // do not want to complain in unreachable code
        if ((this.tagBits & UNREACHABLE) != 0 || (this.tagBits & NULL_FLAG_MASK) == 0) {
            return false;
        }
        int position = local.id + this.maxFieldCount;
        if (// use bits
        position < BitCacheSize) {
            return (this.nullBit4 & (~this.nullBit1 | ~this.nullBit2 & ~this.nullBit3) & (1L << position)) != 0;
        }
        // use extra vector
        if (this.extra == null) {
            // if vector not yet allocated, then not initialized
            return false;
        }
        int vectorIndex;
        if ((vectorIndex = (position / BitCacheSize) - 1) >= this.extra[2].length) {
            // if not enough room in vector, then not initialized
            return false;
        }
        return (this.extra[5][vectorIndex] & (~this.extra[2][vectorIndex] | ~this.extra[3][vectorIndex] & ~this.extra[4][vectorIndex]) & (1L << (position % BitCacheSize))) != 0;
    }

    public final boolean isProtectedNonNull(LocalVariableBinding local) {
        if ((this.tagBits & NULL_FLAG_MASK) == 0 || (local.type.tagBits & TagBits.IsBaseType) != 0) {
            return false;
        }
        int position;
        if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
            // use bits
            return (this.nullBit1 & this.nullBit3 & this.nullBit4 & (1L << position)) != 0;
        }
        // use extra vector
        if (this.extra == null) {
            // if vector not yet allocated, then not initialized
            return false;
        }
        int vectorIndex;
        if ((vectorIndex = (position / BitCacheSize) - 1) >= this.extra[0].length) {
            // if not enough room in vector, then not initialized
            return false;
        }
        return (this.extra[2][vectorIndex] & this.extra[4][vectorIndex] & this.extra[5][vectorIndex] & (1L << (position % BitCacheSize))) != 0;
    }

    public final boolean isProtectedNull(LocalVariableBinding local) {
        if ((this.tagBits & NULL_FLAG_MASK) == 0 || (local.type.tagBits & TagBits.IsBaseType) != 0) {
            return false;
        }
        int position;
        if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
            // use bits
            return (this.nullBit1 & this.nullBit2 & (this.nullBit3 ^ this.nullBit4) & (1L << position)) != 0;
        }
        // use extra vector
        if (this.extra == null) {
            // if vector not yet allocated, then not initialized
            return false;
        }
        int vectorIndex;
        if ((vectorIndex = (position / BitCacheSize) - 1) >= this.extra[0].length) {
            // if not enough room in vector, then not initialized
            return false;
        }
        return (this.extra[2][vectorIndex] & this.extra[3][vectorIndex] & (this.extra[4][vectorIndex] ^ this.extra[5][vectorIndex]) & (1L << (position % BitCacheSize))) != 0;
    }

    /** Asserts that the given boolean is <code>true</code>. If this
 * is not the case, some kind of unchecked exception is thrown.
 * The given message is included in that exception, to aid debugging.
 *
 * @param expression the outcome of the check
 * @param message the message to include in the exception
 * @return <code>true</code> if the check passes (does not return
 *    if the check fails)
 */
    protected static boolean isTrue(boolean expression, String message) {
        if (!expression)
            //$NON-NLS-1$
            throw new AssertionFailedException("assertion failed: " + message);
        return expression;
    }

    public void markAsComparedEqualToNonNull(LocalVariableBinding local) {
        // protected from non-object locals in calling methods
        if (this != DEAD_END) {
            this.tagBits |= NULL_FLAG_MASK;
            int position;
            long mask;
            long a1, a2, a3, a4, na2;
            // position is zero-based
            if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
                // use bits
                if (((mask = 1L << position) & (a1 = this.nullBit1) & (na2 = ~(a2 = this.nullBit2)) & ~(a3 = this.nullBit3) & (a4 = this.nullBit4)) != 0) {
                    this.nullBit4 &= ~mask;
                } else if ((mask & a1 & na2 & a3) == 0) {
                    this.nullBit4 |= mask;
                    if ((mask & a1) == 0) {
                        if ((mask & a2 & (a3 ^ a4)) != 0) {
                            this.nullBit2 &= ~mask;
                        } else if ((mask & (a2 | a3 | a4)) == 0) {
                            this.nullBit2 |= mask;
                        }
                    }
                }
                this.nullBit1 |= mask;
                this.nullBit3 |= mask;
                // it was not null;
                this.iNBit &= ~mask;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 15) {
                        this.nullBit4 = ~0;
                    }
                }
            } else {
                // use extra vector
                int vectorIndex = (position / BitCacheSize) - 1;
                if (this.extra == null) {
                    int length = vectorIndex + 1;
                    createExtraSpace(length);
                    if (COVERAGE_TEST_FLAG) {
                        if (CoverageTestId == 16) {
                            throw new //$NON-NLS-1$
                            AssertionFailedException(//$NON-NLS-1$
                            "COVERAGE 16");
                        }
                    }
                } else {
                    int oldLength;
                    if (vectorIndex >= (oldLength = this.extra[0].length)) {
                        int newLength = vectorIndex + 1;
                        for (int j = 0; j < extraLength; j++) {
                            System.arraycopy(this.extra[j], 0, (this.extra[j] = new long[newLength]), 0, oldLength);
                        }
                        if (COVERAGE_TEST_FLAG) {
                            if (CoverageTestId == 17) {
                                throw new //$NON-NLS-1$
                                AssertionFailedException(//$NON-NLS-1$
                                "COVERAGE 17");
                            }
                        }
                    }
                }
                // MACRO :'b,'es/nullBit\(.\)/extra[\1 + 1][vectorIndex]/gc
                if (((mask = 1L << (position % BitCacheSize)) & (a1 = this.extra[1 + 1][vectorIndex]) & (na2 = ~(a2 = this.extra[2 + 1][vectorIndex])) & ~(a3 = this.extra[3 + 1][vectorIndex]) & (a4 = this.extra[4 + 1][vectorIndex])) != 0) {
                    this.extra[4 + 1][vectorIndex] &= ~mask;
                } else if ((mask & a1 & na2 & a3) == 0) {
                    this.extra[4 + 1][vectorIndex] |= mask;
                    if ((mask & a1) == 0) {
                        if ((mask & a2 & (a3 ^ a4)) != 0) {
                            this.extra[2 + 1][vectorIndex] &= ~mask;
                        } else if ((mask & (a2 | a3 | a4)) == 0) {
                            this.extra[2 + 1][vectorIndex] |= mask;
                        }
                    }
                }
                this.extra[1 + 1][vectorIndex] |= mask;
                this.extra[3 + 1][vectorIndex] |= mask;
                // it was not null;
                this.extra[IN][vectorIndex] &= ~mask;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 18) {
                        this.extra[5][vectorIndex] = ~0;
                    }
                }
            }
        }
    }

    public void markAsComparedEqualToNull(LocalVariableBinding local) {
        // protected from non-object locals in calling methods
        if (this != DEAD_END) {
            this.tagBits |= NULL_FLAG_MASK;
            int position;
            long mask;
            // position is zero-based
            if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
                // use bits
                if (((mask = 1L << position) & this.nullBit1) != 0) {
                    if ((mask & (~this.nullBit2 | this.nullBit3 | ~this.nullBit4)) != 0) {
                        this.nullBit4 &= ~mask;
                    }
                } else if ((mask & this.nullBit4) != 0) {
                    this.nullBit3 &= ~mask;
                } else {
                    if ((mask & this.nullBit2) != 0) {
                        this.nullBit3 &= ~mask;
                        this.nullBit4 |= mask;
                    } else {
                        this.nullBit3 |= mask;
                    }
                }
                this.nullBit1 |= mask;
                this.nullBit2 |= mask;
                // it was null;
                this.iNNBit &= ~mask;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 19) {
                        this.nullBit4 = ~0;
                    }
                }
            } else {
                // use extra vector
                int vectorIndex = (position / BitCacheSize) - 1;
                mask = 1L << (position % BitCacheSize);
                if (this.extra == null) {
                    int length = vectorIndex + 1;
                    createExtraSpace(length);
                    if (COVERAGE_TEST_FLAG) {
                        if (CoverageTestId == 20) {
                            throw new //$NON-NLS-1$
                            AssertionFailedException(//$NON-NLS-1$
                            "COVERAGE 20");
                        }
                    }
                } else {
                    int oldLength;
                    if (vectorIndex >= (oldLength = this.extra[0].length)) {
                        int newLength = vectorIndex + 1;
                        for (int j = 0; j < extraLength; j++) {
                            System.arraycopy(this.extra[j], 0, (this.extra[j] = new long[newLength]), 0, oldLength);
                        }
                        if (COVERAGE_TEST_FLAG) {
                            if (CoverageTestId == 21) {
                                throw new //$NON-NLS-1$
                                AssertionFailedException(//$NON-NLS-1$
                                "COVERAGE 21");
                            }
                        }
                    }
                }
                if ((mask & this.extra[1 + 1][vectorIndex]) != 0) {
                    if ((mask & (~this.extra[2 + 1][vectorIndex] | this.extra[3 + 1][vectorIndex] | ~this.extra[4 + 1][vectorIndex])) != 0) {
                        this.extra[4 + 1][vectorIndex] &= ~mask;
                    }
                } else if ((mask & this.extra[4 + 1][vectorIndex]) != 0) {
                    this.extra[3 + 1][vectorIndex] &= ~mask;
                } else {
                    if ((mask & this.extra[2 + 1][vectorIndex]) != 0) {
                        this.extra[3 + 1][vectorIndex] &= ~mask;
                        this.extra[4 + 1][vectorIndex] |= mask;
                    } else {
                        this.extra[3 + 1][vectorIndex] |= mask;
                    }
                }
                this.extra[1 + 1][vectorIndex] |= mask;
                this.extra[2 + 1][vectorIndex] |= mask;
                // it was null;
                this.extra[INN][vectorIndex] &= ~mask;
            }
        }
    }

    /**
 * Record a definite assignment at a given position.
 */
    private final void markAsDefinitelyAssigned(int position) {
        if (this != DEAD_END) {
            // position is zero-based
            if (position < BitCacheSize) {
                // use bits
                long mask;
                this.definiteInits |= (mask = 1L << position);
                this.potentialInits |= mask;
            } else {
                // use extra vector
                int vectorIndex = (position / BitCacheSize) - 1;
                if (this.extra == null) {
                    int length = vectorIndex + 1;
                    createExtraSpace(length);
                } else {
                    // might need to grow the arrays
                    int oldLength;
                    if (vectorIndex >= (oldLength = this.extra[0].length)) {
                        for (int j = 0; j < extraLength; j++) {
                            System.arraycopy(this.extra[j], 0, (this.extra[j] = new long[vectorIndex + 1]), 0, oldLength);
                        }
                    }
                }
                long mask;
                this.extra[0][vectorIndex] |= (mask = 1L << (position % BitCacheSize));
                this.extra[1][vectorIndex] |= mask;
            }
        }
    }

    public void markAsDefinitelyAssigned(FieldBinding field) {
        if (this != DEAD_END)
            markAsDefinitelyAssigned(field.id);
    }

    public void markAsDefinitelyAssigned(LocalVariableBinding local) {
        if (this != DEAD_END)
            markAsDefinitelyAssigned(local.id + this.maxFieldCount);
    }

    public void markAsDefinitelyNonNull(LocalVariableBinding local) {
        // protected from non-object locals in calling methods
        if (this != DEAD_END) {
            this.tagBits |= NULL_FLAG_MASK;
            long mask;
            int position;
            // position is zero-based
            if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
                // use bits
                // set assigned non null
                this.nullBit1 |= (mask = 1L << position);
                this.nullBit3 |= mask;
                // clear others
                this.nullBit2 &= (mask = ~mask);
                this.nullBit4 &= mask;
                // old value no longer shining through
                this.iNBit &= mask;
                this.iNNBit &= mask;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 22) {
                        this.nullBit1 = 0;
                    }
                }
            } else {
                // use extra vector
                int vectorIndex = (position / BitCacheSize) - 1;
                if (this.extra == null) {
                    int length = vectorIndex + 1;
                    createExtraSpace(length);
                } else {
                    // might need to grow the arrays
                    int oldLength;
                    if (vectorIndex >= (oldLength = this.extra[0].length)) {
                        for (int j = 0; j < extraLength; j++) {
                            System.arraycopy(this.extra[j], 0, (this.extra[j] = new long[vectorIndex + 1]), 0, oldLength);
                        }
                    }
                }
                this.extra[2][vectorIndex] |= (mask = 1L << (position % BitCacheSize));
                this.extra[4][vectorIndex] |= mask;
                this.extra[3][vectorIndex] &= (mask = ~mask);
                this.extra[5][vectorIndex] &= mask;
                // old value no longer shining through
                this.extra[IN][vectorIndex] &= mask;
                this.extra[INN][vectorIndex] &= mask;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 23) {
                        this.extra[2][vectorIndex] = 0;
                    }
                }
            }
        }
    }

    public void markAsDefinitelyNull(LocalVariableBinding local) {
        // protected from non-object locals in calling methods
        if (this != DEAD_END) {
            this.tagBits |= NULL_FLAG_MASK;
            long mask;
            int position;
            // position is zero-based
            if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
                // use bits
                // mark assigned null
                this.nullBit1 |= (mask = 1L << position);
                this.nullBit2 |= mask;
                // clear others
                this.nullBit3 &= (mask = ~mask);
                this.nullBit4 &= mask;
                // old value no longer shining through
                this.iNBit &= mask;
                this.iNNBit &= mask;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 24) {
                        this.nullBit4 = ~0;
                    }
                }
            } else {
                // use extra vector
                int vectorIndex = (position / BitCacheSize) - 1;
                if (this.extra == null) {
                    int length = vectorIndex + 1;
                    createExtraSpace(length);
                } else {
                    // might need to grow the arrays
                    int oldLength;
                    if (vectorIndex >= (oldLength = this.extra[0].length)) {
                        for (int j = 0; j < extraLength; j++) {
                            System.arraycopy(this.extra[j], 0, (this.extra[j] = new long[vectorIndex + 1]), 0, oldLength);
                        }
                    }
                }
                this.extra[2][vectorIndex] |= (mask = 1L << (position % BitCacheSize));
                this.extra[3][vectorIndex] |= mask;
                this.extra[4][vectorIndex] &= (mask = ~mask);
                this.extra[5][vectorIndex] &= mask;
                // old value no longer shining through
                this.extra[IN][vectorIndex] &= mask;
                this.extra[INN][vectorIndex] &= mask;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 25) {
                        this.extra[5][vectorIndex] = ~0;
                    }
                }
            }
        }
    }

    /**
 * Mark a local as having been assigned to an unknown value.
 * @param local the local to mark
 */
    // PREMATURE may try to get closer to markAsDefinitelyAssigned, but not
    //			 obvious
    public void markAsDefinitelyUnknown(LocalVariableBinding local) {
        // protected from non-object locals in calling methods
        if (this != DEAD_END) {
            this.tagBits |= NULL_FLAG_MASK;
            long mask;
            int position;
            // position is zero-based
            if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
                // use bits
                // mark assigned null
                this.nullBit1 |= (mask = 1L << position);
                this.nullBit4 |= mask;
                // clear others
                this.nullBit2 &= (mask = ~mask);
                this.nullBit3 &= mask;
                // old value no longer shining through
                this.iNBit &= mask;
                this.iNNBit &= mask;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 26) {
                        this.nullBit4 = 0;
                    }
                }
            } else {
                // use extra vector
                int vectorIndex = (position / BitCacheSize) - 1;
                if (this.extra == null) {
                    int length = vectorIndex + 1;
                    createExtraSpace(length);
                } else {
                    // might need to grow the arrays
                    int oldLength;
                    if (vectorIndex >= (oldLength = this.extra[0].length)) {
                        for (int j = 0; j < extraLength; j++) {
                            System.arraycopy(this.extra[j], 0, (this.extra[j] = new long[vectorIndex + 1]), 0, oldLength);
                        }
                    }
                }
                this.extra[2][vectorIndex] |= (mask = 1L << (position % BitCacheSize));
                this.extra[5][vectorIndex] |= mask;
                this.extra[3][vectorIndex] &= (mask = ~mask);
                this.extra[4][vectorIndex] &= mask;
                // old value no longer shining through
                this.extra[IN][vectorIndex] &= mask;
                this.extra[INN][vectorIndex] &= mask;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 27) {
                        this.extra[5][vectorIndex] = 0;
                    }
                }
            }
        }
    }

    public void resetNullInfo(LocalVariableBinding local) {
        if (this != DEAD_END) {
            this.tagBits |= NULL_FLAG_MASK;
            int position;
            long mask;
            if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
                // use bits
                this.nullBit1 &= (mask = ~(1L << position));
                this.nullBit2 &= mask;
                this.nullBit3 &= mask;
                this.nullBit4 &= mask;
                this.iNBit &= mask;
                this.iNNBit &= mask;
            } else {
                // use extra vector
                int vectorIndex = (position / BitCacheSize) - 1;
                if (this.extra == null || vectorIndex >= this.extra[2].length) {
                    // before and for which no null bits exist.
                    return;
                }
                this.extra[2][vectorIndex] &= (mask = ~(1L << (position % BitCacheSize)));
                this.extra[3][vectorIndex] &= mask;
                this.extra[4][vectorIndex] &= mask;
                this.extra[5][vectorIndex] &= mask;
                this.extra[IN][vectorIndex] &= mask;
                this.extra[INN][vectorIndex] &= mask;
            }
        }
    }

    /**
 * Mark a local as potentially having been assigned to an unknown value.
 * @param local the local to mark
 */
    public void markPotentiallyUnknownBit(LocalVariableBinding local) {
        // protected from non-object locals in calling methods
        if (this != DEAD_END) {
            this.tagBits |= NULL_FLAG_MASK;
            int position;
            long mask;
            if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
                // use bits
                mask = 1L << position;
                //$NON-NLS-1$
                isTrue((this.nullBit1 & mask) == 0, "Adding 'unknown' mark in unexpected state");
                this.nullBit4 |= mask;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 44) {
                        this.nullBit4 = 0;
                    }
                }
            } else {
                // use extra vector
                int vectorIndex = (position / BitCacheSize) - 1;
                if (this.extra == null) {
                    int length = vectorIndex + 1;
                    createExtraSpace(length);
                } else {
                    // might need to grow the arrays
                    int oldLength;
                    if (vectorIndex >= (oldLength = this.extra[0].length)) {
                        for (int j = 0; j < extraLength; j++) {
                            System.arraycopy(this.extra[j], 0, (this.extra[j] = new long[vectorIndex + 1]), 0, oldLength);
                        }
                    }
                }
                mask = 1L << (position % BitCacheSize);
                //$NON-NLS-1$
                isTrue((this.extra[2][vectorIndex] & mask) == 0, "Adding 'unknown' mark in unexpected state");
                this.extra[5][vectorIndex] |= mask;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 45) {
                        this.extra[2][vectorIndex] = ~0;
                        this.extra[3][vectorIndex] = ~0;
                        this.extra[4][vectorIndex] = 0;
                        this.extra[5][vectorIndex] = 0;
                    }
                }
            }
        }
    }

    public void markPotentiallyNullBit(LocalVariableBinding local) {
        if (this != DEAD_END) {
            this.tagBits |= NULL_FLAG_MASK;
            int position;
            long mask;
            if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
                // use bits
                mask = 1L << position;
                //$NON-NLS-1$
                isTrue((this.nullBit1 & mask) == 0, "Adding 'potentially null' mark in unexpected state");
                this.nullBit2 |= mask;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 40) {
                        this.nullBit2 = 0;
                    }
                }
            } else {
                // use extra vector
                int vectorIndex = (position / BitCacheSize) - 1;
                if (this.extra == null) {
                    int length = vectorIndex + 1;
                    createExtraSpace(length);
                } else {
                    // might need to grow the arrays
                    int oldLength;
                    if (vectorIndex >= (oldLength = this.extra[0].length)) {
                        for (int j = 0; j < extraLength; j++) {
                            System.arraycopy(this.extra[j], 0, (this.extra[j] = new long[vectorIndex + 1]), 0, oldLength);
                        }
                    }
                }
                mask = 1L << (position % BitCacheSize);
                this.extra[3][vectorIndex] |= mask;
                //$NON-NLS-1$
                isTrue((this.extra[2][vectorIndex] & mask) == 0, "Adding 'potentially null' mark in unexpected state");
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 41) {
                        this.extra[3][vectorIndex] = 0;
                    }
                }
            }
        }
    }

    public void markPotentiallyNonNullBit(LocalVariableBinding local) {
        if (this != DEAD_END) {
            this.tagBits |= NULL_FLAG_MASK;
            int position;
            long mask;
            if ((position = local.id + this.maxFieldCount) < BitCacheSize) {
                // use bits
                mask = 1L << position;
                //$NON-NLS-1$
                isTrue((this.nullBit1 & mask) == 0, "Adding 'potentially non-null' mark in unexpected state");
                this.nullBit3 |= mask;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 42) {
                        this.nullBit1 = ~0;
                        this.nullBit2 = 0;
                        this.nullBit3 = ~0;
                        this.nullBit4 = 0;
                    }
                }
            } else {
                // use extra vector
                int vectorIndex = (position / BitCacheSize) - 1;
                if (this.extra == null) {
                    int length = vectorIndex + 1;
                    createExtraSpace(length);
                } else {
                    // might need to grow the arrays
                    int oldLength;
                    if (vectorIndex >= (oldLength = this.extra[0].length)) {
                        for (int j = 0; j < extraLength; j++) {
                            System.arraycopy(this.extra[j], 0, (this.extra[j] = new long[vectorIndex + 1]), 0, oldLength);
                        }
                    }
                }
                mask = 1L << (position % BitCacheSize);
                //$NON-NLS-1$
                isTrue((this.extra[2][vectorIndex] & mask) == 0, "Adding 'potentially non-null' mark in unexpected state");
                this.extra[4][vectorIndex] |= mask;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 43) {
                        this.extra[2][vectorIndex] = ~0;
                        this.extra[3][vectorIndex] = 0;
                        this.extra[4][vectorIndex] = ~0;
                        this.extra[5][vectorIndex] = 0;
                    }
                }
            }
        }
    }

    public UnconditionalFlowInfo mergedWith(UnconditionalFlowInfo otherInits) {
        if ((otherInits.tagBits & UNREACHABLE_OR_DEAD) != 0 && this != DEAD_END) {
            if (COVERAGE_TEST_FLAG) {
                if (CoverageTestId == 28) {
                    throw new //$NON-NLS-1$
                    AssertionFailedException(//$NON-NLS-1$
                    "COVERAGE 28");
                }
            }
            return this;
        }
        if ((this.tagBits & UNREACHABLE_OR_DEAD) != 0) {
            if (COVERAGE_TEST_FLAG) {
                if (CoverageTestId == 29) {
                    throw new //$NON-NLS-1$
                    AssertionFailedException(//$NON-NLS-1$
                    "COVERAGE 29");
                }
            }
            // make sure otherInits won't be affected
            return (UnconditionalFlowInfo) otherInits.copy();
        }
        // intersection of definitely assigned variables,
        this.definiteInits &= otherInits.definiteInits;
        // union of potentially set ones
        this.potentialInits |= otherInits.potentialInits;
        // null combinations
        boolean thisHasNulls = (this.tagBits & NULL_FLAG_MASK) != 0, otherHasNulls = (otherInits.tagBits & NULL_FLAG_MASK) != 0, thisHadNulls = thisHasNulls;
        long a1, a2, a3, a4, na1, na2, na3, na4, nb1, nb2, nb3, nb4, b1, b2, b3, b4;
        if ((otherInits.tagBits & FlowInfo.UNREACHABLE_BY_NULLANALYSIS) != 0) {
            // skip merging, otherInits is unreachable by null analysis
            otherHasNulls = false;
        } else if (// directly copy if this is unreachable by null analysis
        (this.tagBits & FlowInfo.UNREACHABLE_BY_NULLANALYSIS) != 0) {
            this.nullBit1 = otherInits.nullBit1;
            this.nullBit2 = otherInits.nullBit2;
            this.nullBit3 = otherInits.nullBit3;
            this.nullBit4 = otherInits.nullBit4;
            this.iNBit = otherInits.iNBit;
            this.iNNBit = otherInits.iNNBit;
            thisHadNulls = false;
            thisHasNulls = otherHasNulls;
            this.tagBits = otherInits.tagBits;
        } else if (thisHadNulls) {
            if (otherHasNulls) {
                this.nullBit1 = (a1 = this.nullBit1) & (b1 = otherInits.nullBit1) & (((a2 = this.nullBit2) & (((b2 = otherInits.nullBit2) & ~(((a3 = this.nullBit3) & (a4 = this.nullBit4)) ^ ((b3 = otherInits.nullBit3) & (b4 = otherInits.nullBit4)))) | (a3 & a4 & (nb2 = ~b2)))) | ((na2 = ~a2) & ((b2 & b3 & b4) | (nb2 & ((na3 = ~a3) ^ b3)))));
                this.nullBit2 = b2 & ((nb3 = ~b3) | (nb1 = ~b1) | a3 & (a4 | (na1 = ~a1)) & (nb4 = ~b4)) | a2 & (b2 | (na4 = ~a4) & b3 & (b4 | nb1) | na3 | na1);
                this.nullBit3 = a3 & (na1 | a1 & na2 | b3 & (na4 ^ b4)) | b3 & (nb1 | b1 & nb2);
                this.nullBit4 = na3 & (nb1 & nb3 & b4 | b1 & (nb2 & nb3 | a4 & b2 & nb4) | na1 & a4 & (nb3 | b1 & b2)) | a3 & a4 & (b3 & b4 | b1 & nb2 | na1 & a2) | na2 & (nb1 & b4 | b1 & nb3 | na1 & a4) & nb2 | a1 & (na3 & (nb3 & b4 | b1 & b2 & b3 & nb4 | na2 & (nb3 | nb2)) | na2 & b3 & b4 | a2 & (nb1 & b4 | a3 & na4 & b1) & nb3) | nb1 & b2 & b3 & b4;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 30) {
                        this.nullBit4 = ~0;
                    }
                }
            } else {
                // other has no null info
                a1 = this.nullBit1;
                this.nullBit1 = 0;
                this.nullBit2 = (a2 = this.nullBit2) & (na3 = ~(a3 = this.nullBit3) | (na1 = ~a1));
                this.nullBit3 = a3 & ((na2 = ~a2) & (a4 = this.nullBit4) | na1) | a1 & na2 & ~a4;
                this.nullBit4 = (na3 | na2) & na1 & a4 | a1 & na3 & na2;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 31) {
                        this.nullBit4 = ~0;
                    }
                }
            }
            this.iNBit |= otherInits.iNBit;
            this.iNNBit |= otherInits.iNNBit;
        } else if (// only other had nulls
        otherHasNulls) {
            this.nullBit1 = 0;
            this.nullBit2 = (b2 = otherInits.nullBit2) & (nb3 = ~(b3 = otherInits.nullBit3) | (nb1 = ~(b1 = otherInits.nullBit1)));
            this.nullBit3 = b3 & ((nb2 = ~b2) & (b4 = otherInits.nullBit4) | nb1) | b1 & nb2 & ~b4;
            this.nullBit4 = (nb3 | nb2) & nb1 & b4 | b1 & nb3 & nb2;
            this.iNBit |= otherInits.iNBit;
            this.iNNBit |= otherInits.iNNBit;
            if (COVERAGE_TEST_FLAG) {
                if (CoverageTestId == 32) {
                    this.nullBit4 = ~0;
                }
            }
            thisHasNulls = // redundant with the three following ones
            this.nullBit2 != 0 || this.nullBit3 != 0 || this.nullBit4 != 0;
        }
        // treating extra storage
        if (this.extra != null || otherInits.extra != null) {
            int mergeLimit = 0, copyLimit = 0, resetLimit = 0;
            int i;
            if (this.extra != null) {
                if (otherInits.extra != null) {
                    // both sides have extra storage
                    int length, otherLength;
                    if ((length = this.extra[0].length) < (otherLength = otherInits.extra[0].length)) {
                        // current storage is shorter -> grow current
                        for (int j = 0; j < extraLength; j++) {
                            System.arraycopy(this.extra[j], 0, (this.extra[j] = new long[otherLength]), 0, length);
                        }
                        mergeLimit = length;
                        copyLimit = otherLength;
                        if (COVERAGE_TEST_FLAG) {
                            if (CoverageTestId == 33) {
                                throw new //$NON-NLS-1$
                                AssertionFailedException(//$NON-NLS-1$
                                "COVERAGE 33");
                            }
                        }
                    } else {
                        // current storage is longer
                        mergeLimit = otherLength;
                        resetLimit = length;
                        if (COVERAGE_TEST_FLAG) {
                            if (CoverageTestId == 34) {
                                throw new //$NON-NLS-1$
                                AssertionFailedException(//$NON-NLS-1$
                                "COVERAGE 34");
                            }
                        }
                    }
                } else {
                    resetLimit = this.extra[0].length;
                    if (COVERAGE_TEST_FLAG) {
                        if (CoverageTestId == 35) {
                            throw new //$NON-NLS-1$
                            AssertionFailedException(//$NON-NLS-1$
                            "COVERAGE 35");
                        }
                    }
                }
            } else if (otherInits.extra != null) {
                // no storage here, but other has extra storage.
                int otherLength = otherInits.extra[0].length;
                this.extra = new long[extraLength][];
                for (int j = 0; j < extraLength; j++) {
                    this.extra[j] = new long[otherLength];
                }
                System.arraycopy(otherInits.extra[1], 0, this.extra[1], 0, otherLength);
                System.arraycopy(otherInits.extra[IN], 0, this.extra[IN], 0, otherLength);
                System.arraycopy(otherInits.extra[INN], 0, this.extra[INN], 0, otherLength);
                copyLimit = otherLength;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 36) {
                        throw new //$NON-NLS-1$
                        AssertionFailedException(//$NON-NLS-1$
                        "COVERAGE 36");
                    }
                }
            }
            // manage definite assignment
            for (i = 0; i < mergeLimit; i++) {
                this.extra[0][i] &= otherInits.extra[0][i];
                this.extra[1][i] |= otherInits.extra[1][i];
            }
            for (; i < copyLimit; i++) {
                this.extra[1][i] = otherInits.extra[1][i];
            }
            for (; i < resetLimit; i++) {
                this.extra[0][i] = 0;
            }
            // refine null bits requirements
            if (!otherHasNulls) {
                if (resetLimit < mergeLimit) {
                    resetLimit = mergeLimit;
                }
                // no need to carry inexisting nulls
                copyLimit = 0;
                mergeLimit = 0;
            }
            if (!thisHadNulls) {
                // no need to reset anything
                resetLimit = 0;
            }
            // compose nulls
            for (i = 0; i < mergeLimit; i++) {
                this.extra[1 + 1][i] = (a1 = this.extra[1 + 1][i]) & (b1 = otherInits.extra[1 + 1][i]) & (((a2 = this.extra[2 + 1][i]) & (((b2 = otherInits.extra[2 + 1][i]) & ~(((a3 = this.extra[3 + 1][i]) & (a4 = this.extra[4 + 1][i])) ^ ((b3 = otherInits.extra[3 + 1][i]) & (b4 = otherInits.extra[4 + 1][i])))) | (a3 & a4 & (nb2 = ~b2)))) | ((na2 = ~a2) & ((b2 & b3 & b4) | (nb2 & ((na3 = ~a3) ^ b3)))));
                this.extra[2 + 1][i] = b2 & ((nb3 = ~b3) | (nb1 = ~b1) | a3 & (a4 | (na1 = ~a1)) & (nb4 = ~b4)) | a2 & (b2 | (na4 = ~a4) & b3 & (b4 | nb1) | na3 | na1);
                this.extra[3 + 1][i] = a3 & (na1 | a1 & na2 | b3 & (na4 ^ b4)) | b3 & (nb1 | b1 & nb2);
                this.extra[4 + 1][i] = na3 & (nb1 & nb3 & b4 | b1 & (nb2 & nb3 | a4 & b2 & nb4) | na1 & a4 & (nb3 | b1 & b2)) | a3 & a4 & (b3 & b4 | b1 & nb2 | na1 & a2) | na2 & (nb1 & b4 | b1 & nb3 | na1 & a4) & nb2 | a1 & (na3 & (nb3 & b4 | b1 & b2 & b3 & nb4 | na2 & (nb3 | nb2)) | na2 & b3 & b4 | a2 & (nb1 & b4 | a3 & na4 & b1) & nb3) | nb1 & b2 & b3 & b4;
                this.extra[IN][i] |= otherInits.extra[IN][i];
                this.extra[INN][i] |= otherInits.extra[INN][i];
                thisHasNulls = thisHasNulls || this.extra[3][i] != 0 || this.extra[4][i] != 0 || this.extra[5][i] != 0;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 37) {
                        this.extra[5][i] = ~0;
                    }
                }
            }
            for (; i < copyLimit; i++) {
                this.extra[1 + 1][i] = 0;
                this.extra[2 + 1][i] = (b2 = otherInits.extra[2 + 1][i]) & (nb3 = ~(b3 = otherInits.extra[3 + 1][i]) | (nb1 = ~(b1 = otherInits.extra[1 + 1][i])));
                this.extra[3 + 1][i] = b3 & ((nb2 = ~b2) & (b4 = otherInits.extra[4 + 1][i]) | nb1) | b1 & nb2 & ~b4;
                this.extra[4 + 1][i] = (nb3 | nb2) & nb1 & b4 | b1 & nb3 & nb2;
                this.extra[IN][i] |= otherInits.extra[IN][i];
                this.extra[INN][i] |= otherInits.extra[INN][i];
                thisHasNulls = thisHasNulls || this.extra[3][i] != 0 || this.extra[4][i] != 0 || this.extra[5][i] != 0;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 38) {
                        this.extra[5][i] = ~0;
                    }
                }
            }
            for (; i < resetLimit; i++) {
                a1 = this.extra[1 + 1][i];
                this.extra[1 + 1][i] = 0;
                this.extra[2 + 1][i] = (a2 = this.extra[2 + 1][i]) & (na3 = ~(a3 = this.extra[3 + 1][i]) | (na1 = ~a1));
                this.extra[3 + 1][i] = a3 & ((na2 = ~a2) & (a4 = this.extra[4 + 1][i]) | na1) | a1 & na2 & ~a4;
                this.extra[4 + 1][i] = (na3 | na2) & na1 & a4 | a1 & na3 & na2;
                thisHasNulls = thisHasNulls || this.extra[3][i] != 0 || this.extra[4][i] != 0 || this.extra[5][i] != 0;
                if (COVERAGE_TEST_FLAG) {
                    if (CoverageTestId == 39) {
                        this.extra[5][i] = ~0;
                    }
                }
            }
        }
        if (thisHasNulls) {
            this.tagBits |= NULL_FLAG_MASK;
        } else {
            this.tagBits &= ~NULL_FLAG_MASK;
        }
        return this;
    }

    /*
 * Answer the total number of fields in enclosing types of a given type
 */
    static int numberOfEnclosingFields(ReferenceBinding type) {
        int count = 0;
        type = type.enclosingType();
        while (type != null) {
            count += type.fieldCount();
            type = type.enclosingType();
        }
        return count;
    }

    public UnconditionalFlowInfo nullInfoLessUnconditionalCopy() {
        if (this == DEAD_END) {
            return this;
        }
        UnconditionalFlowInfo copy = new UnconditionalFlowInfo();
        copy.definiteInits = this.definiteInits;
        copy.potentialInits = this.potentialInits;
        // no nullness known means: any previous nullness could shine through:
        copy.iNBit = -1L;
        copy.iNNBit = -1L;
        copy.tagBits = this.tagBits & ~NULL_FLAG_MASK;
        copy.tagBits |= UNROOTED;
        copy.maxFieldCount = this.maxFieldCount;
        if (this.extra != null) {
            int length;
            copy.extra = new long[extraLength][];
            System.arraycopy(this.extra[0], 0, (copy.extra[0] = new long[length = this.extra[0].length]), 0, length);
            System.arraycopy(this.extra[1], 0, (copy.extra[1] = new long[length]), 0, length);
            for (int j = 2; j < extraLength; j++) {
                copy.extra[j] = new long[length];
            }
            // no nullness known means: any previous nullness could shine through:
            Arrays.fill(copy.extra[IN], -1L);
            Arrays.fill(copy.extra[INN], -1L);
        }
        return copy;
    }

    public FlowInfo safeInitsWhenTrue() {
        return copy();
    }

    public FlowInfo setReachMode(int reachMode) {
        if (// cannot modify DEAD_END
        this == DEAD_END) {
            return this;
        }
        if (reachMode == REACHABLE) {
            this.tagBits &= ~UNREACHABLE;
        } else if (reachMode == UNREACHABLE_BY_NULLANALYSIS) {
            // do not interfere with definite assignment analysis
            this.tagBits |= UNREACHABLE_BY_NULLANALYSIS;
        } else {
            if ((this.tagBits & UNREACHABLE) == 0) {
                // reset optional inits when becoming unreachable
                // see InitializationTest#test090 (and others)
                this.potentialInits = 0;
                if (this.extra != null) {
                    for (int i = 0, length = this.extra[0].length; i < length; i++) {
                        this.extra[1][i] = 0;
                    }
                }
            }
            this.tagBits |= reachMode;
        }
        return this;
    }

    public String toString() {
        // PREMATURE consider printing bit fields as 0001 0001 1000 0001...
        if (this == DEAD_END) {
            //$NON-NLS-1$
            return "FlowInfo.DEAD_END";
        }
        if ((this.tagBits & NULL_FLAG_MASK) != 0) {
            if (this.extra == null) {
                return //$NON-NLS-1$
                "FlowInfo<def: " + this.definiteInits + ", pot: " + //$NON-NLS-1$
                this.potentialInits + ", reachable:" + (//$NON-NLS-1$
                (this.tagBits & UNREACHABLE) == //$NON-NLS-1$
                0) + //$NON-NLS-1$
                ", null: " + //$NON-NLS-1$
                this.nullBit1 + this.nullBit2 + this.nullBit3 + this.nullBit4 + ", incoming: " + //$NON-NLS-1$
                this.iNBit + //$NON-NLS-1$
                this.iNNBit + //$NON-NLS-1$
                ">";
            } else {
                //$NON-NLS-1$
                String //$NON-NLS-1$
                def = "FlowInfo<def:[" + this.definiteInits, pot = "], pot:[" + //$NON-NLS-1$
                this.potentialInits, nullS = //$NON-NLS-1$
                ", null:[" + //$NON-NLS-1$
                this.nullBit1 + this.nullBit2 + this.nullBit3 + this.nullBit4;
                int i, ceil;
                for (i = 0, ceil = this.extra[0].length > 3 ? 3 : this.extra[0].length; i < ceil; i++) {
                    def += "," + //$NON-NLS-1$
                    this.extra[0][//$NON-NLS-1$
                    i];
                    pot += "," + //$NON-NLS-1$
                    this.extra[1][//$NON-NLS-1$
                    i];
                    nullS += "," + //$NON-NLS-1$
                    this.extra[2][//$NON-NLS-1$
                    i] + this.extra[3][i] + this.extra[4][i] + this.extra[5][i] + ", incoming: " + //$NON-NLS-1$
                    this.extra[IN][i] + //$NON-NLS-1$
                    this.extra[INN];
                }
                if (ceil < this.extra[0].length) {
                    //$NON-NLS-1$
                    def += ",...";
                    //$NON-NLS-1$
                    pot += ",...";
                    //$NON-NLS-1$
                    nullS += //$NON-NLS-1$
                    ",...";
                }
                return def + pot + "], reachable:" + (//$NON-NLS-1$
                (this.tagBits & UNREACHABLE) == //$NON-NLS-1$
                0) + nullS + //$NON-NLS-1$
                "]>";
            }
        } else {
            if (this.extra == null) {
                return //$NON-NLS-1$
                "FlowInfo<def: " + this.definiteInits + ", pot: " + //$NON-NLS-1$
                this.potentialInits + ", reachable:" + (//$NON-NLS-1$
                (this.tagBits & UNREACHABLE) == //$NON-NLS-1$
                0) + ", no null info>";
            } else {
                //$NON-NLS-1$
                String //$NON-NLS-1$
                def = "FlowInfo<def:[" + this.definiteInits, pot = "], pot:[" + //$NON-NLS-1$
                this.potentialInits;
                int i, ceil;
                for (i = 0, ceil = this.extra[0].length > 3 ? 3 : this.extra[0].length; i < ceil; i++) {
                    def += "," + //$NON-NLS-1$
                    this.extra[0][//$NON-NLS-1$
                    i];
                    pot += "," + //$NON-NLS-1$
                    this.extra[1][//$NON-NLS-1$
                    i];
                }
                if (ceil < this.extra[0].length) {
                    //$NON-NLS-1$
                    def += ",...";
                    //$NON-NLS-1$
                    pot += ",...";
                }
                return def + pot + "], reachable:" + (//$NON-NLS-1$
                (this.tagBits & UNREACHABLE) == //$NON-NLS-1$
                0) + ", no null info>";
            }
        }
    }

    public UnconditionalFlowInfo unconditionalCopy() {
        return (UnconditionalFlowInfo) copy();
    }

    public UnconditionalFlowInfo unconditionalFieldLessCopy() {
        // TODO (maxime) may consider leveraging null contribution verification as it is done in copy
        UnconditionalFlowInfo copy = new UnconditionalFlowInfo();
        copy.tagBits = this.tagBits;
        copy.maxFieldCount = this.maxFieldCount;
        int limit = this.maxFieldCount;
        if (limit < BitCacheSize) {
            long mask;
            copy.definiteInits = this.definiteInits & (mask = ~((1L << limit) - 1));
            copy.potentialInits = this.potentialInits & mask;
            copy.nullBit1 = this.nullBit1 & mask;
            copy.nullBit2 = this.nullBit2 & mask;
            copy.nullBit3 = this.nullBit3 & mask;
            copy.nullBit4 = this.nullBit4 & mask;
            copy.iNBit = this.iNBit & mask;
            copy.iNNBit = this.iNNBit & mask;
        }
        // use extra vector
        if (this.extra == null) {
            // if vector not yet allocated, then not initialized
            return copy;
        }
        int vectorIndex, length, copyStart;
        if ((vectorIndex = (limit / BitCacheSize) - 1) >= (length = this.extra[0].length)) {
            // not enough room yet
            return copy;
        }
        long mask;
        copy.extra = new long[extraLength][];
        if ((copyStart = vectorIndex + 1) < length) {
            int copyLength = length - copyStart;
            for (int j = 0; j < extraLength; j++) {
                System.arraycopy(this.extra[j], copyStart, (copy.extra[j] = new long[length]), copyStart, copyLength);
            }
        } else if (vectorIndex >= 0) {
            copy.createExtraSpace(length);
        }
        if (vectorIndex >= 0) {
            mask = ~((1L << (limit % BitCacheSize)) - 1);
            for (int j = 0; j < extraLength; j++) {
                copy.extra[j][vectorIndex] = this.extra[j][vectorIndex] & mask;
            }
        }
        return copy;
    }

    public UnconditionalFlowInfo unconditionalInits() {
        // also see conditional inits, where it requests them to merge
        return this;
    }

    public UnconditionalFlowInfo unconditionalInitsWithoutSideEffect() {
        return this;
    }

    public void resetAssignmentInfo(LocalVariableBinding local) {
        resetAssignmentInfo(local.id + this.maxFieldCount);
    }

    public void resetAssignmentInfo(int position) {
        if (this != DEAD_END) {
            // position is zero-based
            if (position < BitCacheSize) {
                // use bits
                long mask;
                this.definiteInits &= (mask = ~(1L << position));
                this.potentialInits &= mask;
            } else {
                // use extra vector
                int vectorIndex = (position / BitCacheSize) - 1;
                // variable doesnt exist in flow info
                if (this.extra == null || vectorIndex >= this.extra[0].length)
                    return;
                long mask;
                this.extra[0][vectorIndex] &= (mask = ~(1L << (position % BitCacheSize)));
                this.extra[1][vectorIndex] &= mask;
            }
        }
    }

    private void createExtraSpace(int length) {
        this.extra = new long[extraLength][];
        for (int j = 0; j < extraLength; j++) {
            this.extra[j] = new long[length];
        }
        if ((this.tagBits & UNROOTED) != 0) {
            Arrays.fill(this.extra[IN], -1L);
            Arrays.fill(this.extra[INN], -1L);
        }
    }
}
