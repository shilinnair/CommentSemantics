/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.parser.NLSTag;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.compiler.util.Util;

public class PublicScanner implements IScanner, ITerminalSymbols {

    //public int newIdentCount = 0;
    /* APIs ares
	 - getNextToken() which return the current type of the token
	   (this value is not memorized by the scanner)
	 - getCurrentTokenSource() which provides with the token "REAL" source
	   (aka all unicode have been transformed into a correct char)
	 - sourceStart gives the position into the stream
	 - currentPosition-1 gives the sourceEnd position into the stream
	*/
    public long sourceLevel;

    public long complianceLevel;

    // 1.4 feature
    public boolean useAssertAsAnIndentifier = false;

    //flag indicating if processed source contains occurrences of keyword assert
    public boolean containsAssertKeyword = false;

    // 1.5 feature
    public boolean useEnumAsAnIndentifier = false;

    public boolean recordLineSeparator = false;

    public char currentCharacter;

    public int startPosition;

    public int currentPosition;

    public int initialPosition, eofPosition;

    // after this position eof are generated instead of real token from the source
    public boolean skipComments = false;

    public boolean tokenizeComments = false;

    public boolean tokenizeWhiteSpace = false;

    //source should be viewed as a window (aka a part)
    //of a entire very large stream
    public char source[];

    //unicode support
    public char[] withoutUnicodeBuffer;

    //when == 0 ==> no unicode in the current token
    public int withoutUnicodePtr;

    public boolean unicodeAsBackSlash = false;

    public boolean scanningFloatLiteral = false;

    //support for /** comments
    public static final int COMMENT_ARRAYS_SIZE = 30;

    public int[] commentStops = new int[COMMENT_ARRAYS_SIZE];

    public int[] commentStarts = new int[COMMENT_ARRAYS_SIZE];

    public int[] commentTagStarts = new int[COMMENT_ARRAYS_SIZE];

    // no comment test with commentPtr value -1
    public int commentPtr = -1;

    protected int lastCommentLinePosition = -1;

    // task tag support
    public char[][] foundTaskTags = null;

    public char[][] foundTaskMessages;

    public char[][] foundTaskPriorities = null;

    public int[][] foundTaskPositions;

    public int foundTaskCount = 0;

    public char[][] taskTags = null;

    public char[][] taskPriorities = null;

    public boolean isTaskCaseSensitive = true;

    //diet parsing support - jump over some method body when requested
    public boolean diet = false;

    //support for the  poor-line-debuggers ....
    //remember the position of the cr/lf
    public int[] lineEnds = new int[250];

    public int linePtr = -1;

    public boolean wasAcr = false;

    //$NON-NLS-1$
    public static final String END_OF_SOURCE = "End_Of_Source";

    //$NON-NLS-1$
    public static final String INVALID_HEXA = "Invalid_Hexa_Literal";

    //$NON-NLS-1$
    public static final String INVALID_OCTAL = "Invalid_Octal_Literal";

    //$NON-NLS-1$
    public static final String INVALID_CHARACTER_CONSTANT = "Invalid_Character_Constant";

    //$NON-NLS-1$
    public static final String INVALID_ESCAPE = "Invalid_Escape";

    //$NON-NLS-1$
    public static final String INVALID_INPUT = "Invalid_Input";

    //$NON-NLS-1$
    public static final String INVALID_UNICODE_ESCAPE = "Invalid_Unicode_Escape";

    //$NON-NLS-1$
    public static final String INVALID_FLOAT = "Invalid_Float_Literal";

    //$NON-NLS-1$
    public static final String INVALID_LOW_SURROGATE = "Invalid_Low_Surrogate";

    //$NON-NLS-1$
    public static final String INVALID_HIGH_SURROGATE = "Invalid_High_Surrogate";

    //$NON-NLS-1$
    public static final String NULL_SOURCE_STRING = "Null_Source_String";

    //$NON-NLS-1$
    public static final String UNTERMINATED_STRING = "Unterminated_String";

    //$NON-NLS-1$
    public static final String UNTERMINATED_COMMENT = "Unterminated_Comment";

    //$NON-NLS-1$
    public static final String INVALID_CHAR_IN_STRING = "Invalid_Char_In_String";

    //$NON-NLS-1$
    public static final String INVALID_DIGIT = "Invalid_Digit";

    private static final int[] EMPTY_LINE_ENDS = Util.EMPTY_INT_ARRAY;

    //$NON-NLS-1$
    public static final String INVALID_BINARY = "Invalid_Binary_Literal";

    //$NON-NLS-1$
    public static final String BINARY_LITERAL_NOT_BELOW_17 = "Binary_Literal_Not_Below_17";

    //$NON-NLS-1$
    public static final String ILLEGAL_HEXA_LITERAL = "Illegal_Hexa_Literal";

    //$NON-NLS-1$
    public static final String INVALID_UNDERSCORE = "Invalid_Underscore";

    //$NON-NLS-1$`
    public static final String UNDERSCORES_IN_LITERALS_NOT_BELOW_17 = "Underscores_In_Literals_Not_Below_17";

    //----------------optimized identifier managment------------------
    static final char[] charArray_a = new char[] { 'a' }, charArray_b = new char[] { 'b' }, charArray_c = new char[] { 'c' }, charArray_d = new char[] { 'd' }, charArray_e = new char[] { 'e' }, charArray_f = new char[] { 'f' }, charArray_g = new char[] { 'g' }, charArray_h = new char[] { 'h' }, charArray_i = new char[] { 'i' }, charArray_j = new char[] { 'j' }, charArray_k = new char[] { 'k' }, charArray_l = new char[] { 'l' }, charArray_m = new char[] { 'm' }, charArray_n = new char[] { 'n' }, charArray_o = new char[] { 'o' }, charArray_p = new char[] { 'p' }, charArray_q = new char[] { 'q' }, charArray_r = new char[] { 'r' }, charArray_s = new char[] { 's' }, charArray_t = new char[] { 't' }, charArray_u = new char[] { 'u' }, charArray_v = new char[] { 'v' }, charArray_w = new char[] { 'w' }, charArray_x = new char[] { 'x' }, charArray_y = new char[] { 'y' }, charArray_z = new char[] { 'z' };

    static final char[] initCharArray = new char[] { ' ', ' ', ' ', ' ', ' ', ' ' };

    //30*6 =210 entries
    static final int TableSize = 30, InternalTableSize = 6;

    public static final int OptimizedLength = 7;

    public final /*static*/
    char[][][][] charArray_length = new char[OptimizedLength][TableSize][InternalTableSize][];

    // support for detecting non-externalized string literals
    //$NON-NLS-1$
    public static final char[] TAG_PREFIX = "//$NON-NLS-".toCharArray();

    public static final int TAG_PREFIX_LENGTH = TAG_PREFIX.length;

    public static final char TAG_POSTFIX = '$';

    public static final int TAG_POSTFIX_LENGTH = 1;

    private NLSTag[] nlsTags = null;

    protected int nlsTagsPtr;

    public boolean checkNonExternalizedStringLiterals;

    protected int lastPosition;

    // generic support
    public boolean returnOnlyGreater = false;

    /*static*/
    {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < TableSize; j++) {
                for (int k = 0; k < InternalTableSize; k++) {
                    this.charArray_length[i][j][k] = initCharArray;
                }
            }
        }
    }

    /*static*/
    int newEntry2 = 0, newEntry3 = 0, newEntry4 = 0, newEntry5 = 0, newEntry6 = 0;

    public boolean insideRecovery = false;

    public static final int RoundBracket = 0;

    public static final int SquareBracket = 1;

    public static final int CurlyBracket = 2;

    public static final int BracketKinds = 3;

    // extended unicode support
    public static final int LOW_SURROGATE_MIN_VALUE = 0xDC00;

    public static final int HIGH_SURROGATE_MIN_VALUE = 0xD800;

    public static final int HIGH_SURROGATE_MAX_VALUE = 0xDBFF;

    public static final int LOW_SURROGATE_MAX_VALUE = 0xDFFF;

    public  PublicScanner() {
        this(false, /*comment*/
        false, /*whitespace*/
        false, /*nls*/
        ClassFileConstants.JDK1_3, /*sourceLevel*/
        null, /*taskTag*/
        null, /*taskPriorities*/
        true);
    }

    public  PublicScanner(boolean tokenizeComments, boolean tokenizeWhiteSpace, boolean checkNonExternalizedStringLiterals, long sourceLevel, long complianceLevel, char[][] taskTags, char[][] taskPriorities, boolean isTaskCaseSensitive) {
        this.eofPosition = Integer.MAX_VALUE;
        this.tokenizeComments = tokenizeComments;
        this.tokenizeWhiteSpace = tokenizeWhiteSpace;
        this.sourceLevel = sourceLevel;
        this.complianceLevel = complianceLevel;
        this.checkNonExternalizedStringLiterals = checkNonExternalizedStringLiterals;
        if (taskTags != null) {
            int taskTagsLength = taskTags.length;
            int length = taskTagsLength;
            if (taskPriorities != null) {
                int taskPrioritiesLength = taskPriorities.length;
                if (taskPrioritiesLength != taskTagsLength) {
                    if (taskPrioritiesLength > taskTagsLength) {
                        System.arraycopy(taskPriorities, 0, (taskPriorities = new char[taskTagsLength][]), 0, taskTagsLength);
                    } else {
                        System.arraycopy(taskTags, 0, (taskTags = new char[taskPrioritiesLength][]), 0, taskPrioritiesLength);
                        length = taskPrioritiesLength;
                    }
                }
                int[] initialIndexes = new int[length];
                for (int i = 0; i < length; i++) {
                    initialIndexes[i] = i;
                }
                Util.reverseQuickSort(taskTags, 0, length - 1, initialIndexes);
                char[][] temp = new char[length][];
                for (int i = 0; i < length; i++) {
                    temp[i] = taskPriorities[initialIndexes[i]];
                }
                this.taskPriorities = temp;
            } else {
                Util.reverseQuickSort(taskTags, 0, length - 1);
            }
            this.taskTags = taskTags;
            this.isTaskCaseSensitive = isTaskCaseSensitive;
        }
    }

    public  PublicScanner(boolean tokenizeComments, boolean tokenizeWhiteSpace, boolean checkNonExternalizedStringLiterals, long sourceLevel, char[][] taskTags, char[][] taskPriorities, boolean isTaskCaseSensitive) {
        this(tokenizeComments, tokenizeWhiteSpace, checkNonExternalizedStringLiterals, sourceLevel, sourceLevel, taskTags, taskPriorities, isTaskCaseSensitive);
    }

    public final boolean atEnd() {
        return this.eofPosition <= this.currentPosition;
    }

    // chech presence of task: tags
    // TODO (frederic) see if we need to take unicode characters into account...
    public void checkTaskTag(int commentStart, int commentEnd) throws InvalidInputException {
        char[] src = this.source;
        // only look for newer task: tags
        if (this.foundTaskCount > 0 && this.foundTaskPositions[this.foundTaskCount - 1][0] >= commentStart) {
            return;
        }
        int foundTaskIndex = this.foundTaskCount;
        // should be '*' or '/'
        char previous = src[commentStart + 1];
        for (int i = commentStart + 2; i < commentEnd && i < this.eofPosition; i++) {
            char[] tag = null;
            char[] priority = null;
            // check for tag occurrence only if not ambiguous with javadoc tag
            if (previous != '@') {
                nextTag: for (int itag = 0; itag < this.taskTags.length; itag++) {
                    tag = this.taskTags[itag];
                    int tagLength = tag.length;
                    if (tagLength == 0)
                        continue nextTag;
                    // ensure tag is not leaded with letter if tag starts with a letter
                    if (ScannerHelper.isJavaIdentifierStart(this.complianceLevel, tag[0])) {
                        if (ScannerHelper.isJavaIdentifierPart(this.complianceLevel, previous)) {
                            continue nextTag;
                        }
                    }
                    for (int t = 0; t < tagLength; t++) {
                        char sc, tc;
                        int x = i + t;
                        if (x >= this.eofPosition || x >= commentEnd)
                            continue nextTag;
                        // case sensitive check
                        if ((sc = src[i + t]) != (tc = tag[t])) {
                            // case insensitive check
                            if (this.isTaskCaseSensitive || (ScannerHelper.toLowerCase(sc) != ScannerHelper.toLowerCase(tc))) {
                                continue nextTag;
                            }
                        }
                    }
                    // ensure tag is not followed with letter if tag finishes with a letter
                    if (i + tagLength < commentEnd && ScannerHelper.isJavaIdentifierPart(this.complianceLevel, src[i + tagLength - 1])) {
                        if (ScannerHelper.isJavaIdentifierPart(this.complianceLevel, src[i + tagLength]))
                            continue nextTag;
                    }
                    if (this.foundTaskTags == null) {
                        this.foundTaskTags = new char[5][];
                        this.foundTaskMessages = new char[5][];
                        this.foundTaskPriorities = new char[5][];
                        this.foundTaskPositions = new int[5][];
                    } else if (this.foundTaskCount == this.foundTaskTags.length) {
                        System.arraycopy(this.foundTaskTags, 0, this.foundTaskTags = new char[this.foundTaskCount * 2][], 0, this.foundTaskCount);
                        System.arraycopy(this.foundTaskMessages, 0, this.foundTaskMessages = new char[this.foundTaskCount * 2][], 0, this.foundTaskCount);
                        System.arraycopy(this.foundTaskPriorities, 0, this.foundTaskPriorities = new char[this.foundTaskCount * 2][], 0, this.foundTaskCount);
                        System.arraycopy(this.foundTaskPositions, 0, this.foundTaskPositions = new int[this.foundTaskCount * 2][], 0, this.foundTaskCount);
                    }
                    priority = this.taskPriorities != null && itag < this.taskPriorities.length ? this.taskPriorities[itag] : null;
                    this.foundTaskTags[this.foundTaskCount] = tag;
                    this.foundTaskPriorities[this.foundTaskCount] = priority;
                    this.foundTaskPositions[this.foundTaskCount] = new int[] { i, i + tagLength - 1 };
                    this.foundTaskMessages[this.foundTaskCount] = CharOperation.NO_CHAR;
                    this.foundTaskCount++;
                    // will be incremented when looping
                    i += tagLength - 1;
                    break nextTag;
                }
            }
            previous = src[i];
        }
        boolean containsEmptyTask = false;
        for (int i = foundTaskIndex; i < this.foundTaskCount; i++) {
            // retrieve message start and end positions
            int msgStart = this.foundTaskPositions[i][0] + this.foundTaskTags[i].length;
            int max_value = i + 1 < this.foundTaskCount ? this.foundTaskPositions[i + 1][0] - 1 : commentEnd - 1;
            // at most beginning of next task
            if (max_value < msgStart) {
                // would only occur if tag is before EOF.
                max_value = msgStart;
            }
            int end = -1;
            char c;
            for (int j = msgStart; j < max_value; j++) {
                if ((c = src[j]) == '\n' || c == '\r') {
                    end = j - 1;
                    break;
                }
            }
            if (end == -1) {
                for (int j = max_value; j > msgStart; j--) {
                    if ((c = src[j]) == '*') {
                        end = j - 1;
                        break;
                    }
                }
                if (end == -1)
                    end = max_value;
            }
            if (msgStart == end) {
                // if the description is empty, we might want to see if two tags are not sharing the same message
                // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=110797
                containsEmptyTask = true;
                continue;
            }
            // we don't trim the beginning of the message to be able to show it after the task tag
            while (CharOperation.isWhitespace(src[end]) && msgStart <= end) end--;
            // update the end position of the task
            this.foundTaskPositions[i][1] = end;
            // get the message source
            final int messageLength = end - msgStart + 1;
            char[] message = new char[messageLength];
            System.arraycopy(src, msgStart, message, 0, messageLength);
            this.foundTaskMessages[i] = message;
        }
        if (containsEmptyTask) {
            for (int i = foundTaskIndex, max = this.foundTaskCount; i < max; i++) {
                if (this.foundTaskMessages[i].length == 0) {
                    loop: for (int j = i + 1; j < max; j++) {
                        if (this.foundTaskMessages[j].length != 0) {
                            this.foundTaskMessages[i] = this.foundTaskMessages[j];
                            this.foundTaskPositions[i][1] = this.foundTaskPositions[j][1];
                            break loop;
                        }
                    }
                }
            }
        }
    }

    public char[] getCurrentIdentifierSource() {
        //return the token REAL source (aka unicodes are precomputed)
        if (this.withoutUnicodePtr != 0) {
            //0 is used as a fast test flag so the real first char is in position 1
            char[] result = new char[this.withoutUnicodePtr];
            System.arraycopy(this.withoutUnicodeBuffer, 1, result, 0, this.withoutUnicodePtr);
            return result;
        }
        int length = this.currentPosition - this.startPosition;
        if (length == this.eofPosition)
            return this.source;
        switch(// see OptimizedLength
        length) {
            case 1:
                return optimizedCurrentTokenSource1();
            case 2:
                return optimizedCurrentTokenSource2();
            case 3:
                return optimizedCurrentTokenSource3();
            case 4:
                return optimizedCurrentTokenSource4();
            case 5:
                return optimizedCurrentTokenSource5();
            case 6:
                return optimizedCurrentTokenSource6();
        }
        char[] result = new char[length];
        System.arraycopy(this.source, this.startPosition, result, 0, length);
        return result;
    }

    public int getCurrentTokenEndPosition() {
        return this.currentPosition - 1;
    }

    public char[] getCurrentTokenSource() {
        // Return the token REAL source (aka unicodes are precomputed)
        char[] result;
        if (this.withoutUnicodePtr != 0)
            // 0 is used as a fast test flag so the real first char is in position 1
            System.arraycopy(this.withoutUnicodeBuffer, 1, result = new char[this.withoutUnicodePtr], 0, this.withoutUnicodePtr);
        else {
            int length;
            System.arraycopy(this.source, this.startPosition, result = new char[length = this.currentPosition - this.startPosition], 0, length);
        }
        return result;
    }

    public final String getCurrentTokenString() {
        if (this.withoutUnicodePtr != 0) {
            // 0 is used as a fast test flag so the real first char is in position 1
            return new String(this.withoutUnicodeBuffer, 1, this.withoutUnicodePtr);
        }
        return new String(this.source, this.startPosition, this.currentPosition - this.startPosition);
    }

    public char[] getCurrentTokenSourceString() {
        //return the token REAL source (aka unicodes are precomputed).
        //REMOVE the two " that are at the beginning and the end.
        char[] result;
        if (this.withoutUnicodePtr != 0)
            //0 is used as a fast test flag so the real first char is in position 1
            System.arraycopy(this.withoutUnicodeBuffer, 2, //2 is 1 (real start) + 1 (to jump over the ")
            result = new char[this.withoutUnicodePtr - 2], 0, this.withoutUnicodePtr - 2);
        else {
            int length;
            System.arraycopy(this.source, this.startPosition + 1, result = new char[length = this.currentPosition - this.startPosition - 2], 0, length);
        }
        return result;
    }

    public final String getCurrentStringLiteral() {
        if (this.withoutUnicodePtr != 0)
            //2 is 1 (real start) + 1 (to jump over the ")
            return new String(this.withoutUnicodeBuffer, 2, this.withoutUnicodePtr - 2);
        else {
            return new String(this.source, this.startPosition + 1, this.currentPosition - this.startPosition - 2);
        }
    }

    public final char[] getRawTokenSource() {
        int length = this.currentPosition - this.startPosition;
        char[] tokenSource = new char[length];
        System.arraycopy(this.source, this.startPosition, tokenSource, 0, length);
        return tokenSource;
    }

    public final char[] getRawTokenSourceEnd() {
        int length = this.eofPosition - this.currentPosition - 1;
        char[] sourceEnd = new char[length];
        System.arraycopy(this.source, this.currentPosition, sourceEnd, 0, length);
        return sourceEnd;
    }

    public int getCurrentTokenStartPosition() {
        return this.startPosition;
    }

    /*
 * Search the source position corresponding to the end of a given line number
 *
 * Line numbers are 1-based, and relative to the scanner initialPosition.
 * Character positions are 0-based.
 *
 * In case the given line number is inconsistent, answers -1.
 */
    public final int getLineEnd(int lineNumber) {
        if (this.lineEnds == null || this.linePtr == -1)
            return -1;
        if (lineNumber > this.lineEnds.length + 1)
            return -1;
        if (lineNumber <= 0)
            return -1;
        if (lineNumber == this.lineEnds.length + 1)
            return this.eofPosition;
        // next line start one character behind the lineEnd of the previous line
        return this.lineEnds[lineNumber - 1];
    }

    public final int[] getLineEnds() {
        //return a bounded copy of this.lineEnds
        if (this.linePtr == -1) {
            return EMPTY_LINE_ENDS;
        }
        int[] copy;
        System.arraycopy(this.lineEnds, 0, copy = new int[this.linePtr + 1], 0, this.linePtr + 1);
        return copy;
    }

    /**
 * Search the source position corresponding to the beginning of a given line number
 *
 * Line numbers are 1-based, and relative to the scanner initialPosition.
 * Character positions are 0-based.
 *
 * e.g.	getLineStart(1) --> 0	indicates that the first line starts at character 0.
 *
 * In case the given line number is inconsistent, answers -1.
 *
 * @param lineNumber int
 * @return int
 */
    public final int getLineStart(int lineNumber) {
        if (this.lineEnds == null || this.linePtr == -1)
            return -1;
        if (lineNumber > this.lineEnds.length + 1)
            return -1;
        if (lineNumber <= 0)
            return -1;
        if (lineNumber == 1)
            return this.initialPosition;
        // next line start one character behind the lineEnd of the previous line
        return this.lineEnds[lineNumber - 2] + 1;
    }

    public final int getNextChar() {
        try {
            if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                getNextUnicodeChar();
            } else {
                this.unicodeAsBackSlash = false;
                if (this.withoutUnicodePtr != 0) {
                    unicodeStore();
                }
            }
            return this.currentCharacter;
        } catch (IndexOutOfBoundsException e) {
            return -1;
        } catch (InvalidInputException e) {
            return -1;
        }
    }

    public final int getNextCharWithBoundChecks() {
        if (this.currentPosition >= this.eofPosition) {
            return -1;
        }
        this.currentCharacter = this.source[this.currentPosition++];
        if (this.currentPosition >= this.eofPosition) {
            this.unicodeAsBackSlash = false;
            if (this.withoutUnicodePtr != 0) {
                unicodeStore();
            }
            return this.currentCharacter;
        }
        if (this.currentCharacter == '\\' && this.source[this.currentPosition] == 'u') {
            try {
                getNextUnicodeChar();
            } catch (InvalidInputException e) {
                return -1;
            }
        } else {
            this.unicodeAsBackSlash = false;
            if (this.withoutUnicodePtr != 0) {
                unicodeStore();
            }
        }
        return this.currentCharacter;
    }

    public final boolean getNextChar(char testedChar) {
        if (this.currentPosition >= this.eofPosition) {
            this.unicodeAsBackSlash = false;
            return false;
        }
        int temp = this.currentPosition;
        try {
            if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                getNextUnicodeChar();
                if (this.currentCharacter != testedChar) {
                    this.currentPosition = temp;
                    this.withoutUnicodePtr--;
                    return false;
                }
                return true;
            } else {
                if (this.currentCharacter != testedChar) {
                    this.currentPosition = temp;
                    return false;
                }
                this.unicodeAsBackSlash = false;
                if (this.withoutUnicodePtr != 0)
                    unicodeStore();
                return true;
            }
        } catch (IndexOutOfBoundsException e) {
            this.unicodeAsBackSlash = false;
            this.currentPosition = temp;
            return false;
        } catch (InvalidInputException e) {
            this.unicodeAsBackSlash = false;
            this.currentPosition = temp;
            return false;
        }
    }

    public final int getNextChar(char testedChar1, char testedChar2) {
        if (this.currentPosition >= this.eofPosition)
            return -1;
        int temp = this.currentPosition;
        try {
            int result;
            if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                getNextUnicodeChar();
                if (this.currentCharacter == testedChar1) {
                    result = 0;
                } else if (this.currentCharacter == testedChar2) {
                    result = 1;
                } else {
                    this.currentPosition = temp;
                    this.withoutUnicodePtr--;
                    result = -1;
                }
                return result;
            } else {
                if (this.currentCharacter == testedChar1) {
                    result = 0;
                } else if (this.currentCharacter == testedChar2) {
                    result = 1;
                } else {
                    this.currentPosition = temp;
                    return -1;
                }
                if (this.withoutUnicodePtr != 0)
                    unicodeStore();
                return result;
            }
        } catch (IndexOutOfBoundsException e) {
            this.currentPosition = temp;
            return -1;
        } catch (InvalidInputException e) {
            this.currentPosition = temp;
            return -1;
        }
    }

    private final void consumeDigits(int radix) throws InvalidInputException {
        consumeDigits(radix, false);
    }

    private final void consumeDigits(int radix, boolean expectingDigitFirst) throws InvalidInputException {
        final int USING_UNDERSCORE = 1;
        final int INVALID_POSITION = 2;
        switch(consumeDigits0(radix, USING_UNDERSCORE, INVALID_POSITION, expectingDigitFirst)) {
            case USING_UNDERSCORE:
                if (this.sourceLevel < ClassFileConstants.JDK1_7) {
                    throw new InvalidInputException(UNDERSCORES_IN_LITERALS_NOT_BELOW_17);
                }
                break;
            case INVALID_POSITION:
                if (this.sourceLevel < ClassFileConstants.JDK1_7) {
                    throw new InvalidInputException(UNDERSCORES_IN_LITERALS_NOT_BELOW_17);
                }
                throw new InvalidInputException(INVALID_UNDERSCORE);
        }
    }

    private final int consumeDigits0(int radix, int usingUnderscore, int invalidPosition, boolean expectingDigitFirst) throws InvalidInputException {
        int kind = 0;
        if (getNextChar('_')) {
            if (expectingDigitFirst) {
                return invalidPosition;
            }
            kind = usingUnderscore;
            while (getNextChar('_')) {
            }
        }
        if (getNextCharAsDigit(radix)) {
            while (getNextCharAsDigit(radix)) {
            }
            int kind2 = consumeDigits0(radix, usingUnderscore, invalidPosition, false);
            if (kind2 == 0) {
                return kind;
            }
            return kind2;
        }
        if (kind == usingUnderscore)
            return invalidPosition;
        return kind;
    }

    public final boolean getNextCharAsDigit() throws InvalidInputException {
        if (this.currentPosition >= this.eofPosition)
            return false;
        int temp = this.currentPosition;
        try {
            if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                getNextUnicodeChar();
                if (!ScannerHelper.isDigit(this.currentCharacter)) {
                    this.currentPosition = temp;
                    this.withoutUnicodePtr--;
                    return false;
                }
                return true;
            } else {
                if (!ScannerHelper.isDigit(this.currentCharacter)) {
                    this.currentPosition = temp;
                    return false;
                }
                if (this.withoutUnicodePtr != 0)
                    unicodeStore();
                return true;
            }
        } catch (IndexOutOfBoundsException e) {
            this.currentPosition = temp;
            return false;
        } catch (InvalidInputException e) {
            this.currentPosition = temp;
            return false;
        }
    }

    public final boolean getNextCharAsDigit(int radix) {
        if (this.currentPosition >= this.eofPosition)
            return false;
        int temp = this.currentPosition;
        try {
            if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                getNextUnicodeChar();
                if (ScannerHelper.digit(this.currentCharacter, radix) == -1) {
                    this.currentPosition = temp;
                    this.withoutUnicodePtr--;
                    return false;
                }
                return true;
            } else {
                if (ScannerHelper.digit(this.currentCharacter, radix) == -1) {
                    this.currentPosition = temp;
                    return false;
                }
                if (this.withoutUnicodePtr != 0)
                    unicodeStore();
                return true;
            }
        } catch (IndexOutOfBoundsException e) {
            this.currentPosition = temp;
            return false;
        } catch (InvalidInputException e) {
            this.currentPosition = temp;
            return false;
        }
    }

    public boolean getNextCharAsJavaIdentifierPartWithBoundCheck() {
        int pos = this.currentPosition;
        if (pos >= this.eofPosition)
            return false;
        int temp2 = this.withoutUnicodePtr;
        try {
            boolean unicode = false;
            this.currentCharacter = this.source[this.currentPosition++];
            if (this.currentPosition < this.eofPosition) {
                if (this.currentCharacter == '\\' && this.source[this.currentPosition] == 'u') {
                    getNextUnicodeChar();
                    unicode = true;
                }
            }
            char c = this.currentCharacter;
            boolean isJavaIdentifierPart = false;
            if (c >= HIGH_SURROGATE_MIN_VALUE && c <= HIGH_SURROGATE_MAX_VALUE) {
                if (this.complianceLevel < ClassFileConstants.JDK1_5) {
                    this.currentPosition = pos;
                    this.withoutUnicodePtr = temp2;
                    return false;
                }
                char low = (char) getNextCharWithBoundChecks();
                if (low < LOW_SURROGATE_MIN_VALUE || low > LOW_SURROGATE_MAX_VALUE) {
                    this.currentPosition = pos;
                    this.withoutUnicodePtr = temp2;
                    return false;
                }
                isJavaIdentifierPart = ScannerHelper.isJavaIdentifierPart(this.complianceLevel, c, low);
            } else if (c >= LOW_SURROGATE_MIN_VALUE && c <= LOW_SURROGATE_MAX_VALUE) {
                this.currentPosition = pos;
                this.withoutUnicodePtr = temp2;
                return false;
            } else {
                isJavaIdentifierPart = ScannerHelper.isJavaIdentifierPart(this.complianceLevel, c);
            }
            if (unicode) {
                if (!isJavaIdentifierPart) {
                    this.currentPosition = pos;
                    this.withoutUnicodePtr = temp2;
                    return false;
                }
                return true;
            } else {
                if (!isJavaIdentifierPart) {
                    this.currentPosition = pos;
                    return false;
                }
                if (this.withoutUnicodePtr != 0)
                    unicodeStore();
                return true;
            }
        } catch (InvalidInputException e) {
            this.currentPosition = pos;
            this.withoutUnicodePtr = temp2;
            return false;
        }
    }

    public boolean getNextCharAsJavaIdentifierPart() {
        int pos;
        if ((pos = this.currentPosition) >= this.eofPosition)
            return false;
        int temp2 = this.withoutUnicodePtr;
        try {
            boolean unicode = false;
            if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                getNextUnicodeChar();
                unicode = true;
            }
            char c = this.currentCharacter;
            boolean isJavaIdentifierPart = false;
            if (c >= HIGH_SURROGATE_MIN_VALUE && c <= HIGH_SURROGATE_MAX_VALUE) {
                if (this.complianceLevel < ClassFileConstants.JDK1_5) {
                    this.currentPosition = pos;
                    this.withoutUnicodePtr = temp2;
                    return false;
                }
                char low = (char) getNextChar();
                if (low < LOW_SURROGATE_MIN_VALUE || low > LOW_SURROGATE_MAX_VALUE) {
                    this.currentPosition = pos;
                    this.withoutUnicodePtr = temp2;
                    return false;
                }
                isJavaIdentifierPart = ScannerHelper.isJavaIdentifierPart(this.complianceLevel, c, low);
            } else if (c >= LOW_SURROGATE_MIN_VALUE && c <= LOW_SURROGATE_MAX_VALUE) {
                this.currentPosition = pos;
                this.withoutUnicodePtr = temp2;
                return false;
            } else {
                isJavaIdentifierPart = ScannerHelper.isJavaIdentifierPart(this.complianceLevel, c);
            }
            if (unicode) {
                if (!isJavaIdentifierPart) {
                    this.currentPosition = pos;
                    this.withoutUnicodePtr = temp2;
                    return false;
                }
                return true;
            } else {
                if (!isJavaIdentifierPart) {
                    this.currentPosition = pos;
                    return false;
                }
                if (this.withoutUnicodePtr != 0)
                    unicodeStore();
                return true;
            }
        } catch (IndexOutOfBoundsException e) {
            this.currentPosition = pos;
            this.withoutUnicodePtr = temp2;
            return false;
        } catch (InvalidInputException e) {
            this.currentPosition = pos;
            this.withoutUnicodePtr = temp2;
            return false;
        }
    }

    public int scanIdentifier() throws InvalidInputException {
        int whiteStart = 0;
        while (true) {
            this.withoutUnicodePtr = 0;
            whiteStart = this.currentPosition;
            boolean isWhiteSpace, hasWhiteSpaces = false;
            int offset;
            int unicodePtr;
            boolean checkIfUnicode = false;
            do {
                unicodePtr = this.withoutUnicodePtr;
                offset = this.currentPosition;
                this.startPosition = this.currentPosition;
                if (this.currentPosition < this.eofPosition) {
                    this.currentCharacter = this.source[this.currentPosition++];
                    checkIfUnicode = this.currentPosition < this.eofPosition && this.currentCharacter == '\\' && this.source[this.currentPosition] == 'u';
                } else if (this.tokenizeWhiteSpace && (whiteStart != this.currentPosition - 1)) {
                    this.currentPosition--;
                    this.startPosition = whiteStart;
                    return TokenNameWHITESPACE;
                } else {
                    return TokenNameEOF;
                }
                if (checkIfUnicode) {
                    isWhiteSpace = jumpOverUnicodeWhiteSpace();
                    offset = this.currentPosition - offset;
                } else {
                    offset = this.currentPosition - offset;
                    switch(this.currentCharacter) {
                        case 10:
                        case 12:
                        case 13:
                        case 32:
                        case 9:
                            isWhiteSpace = true;
                            break;
                        default:
                            isWhiteSpace = false;
                    }
                }
                if (isWhiteSpace) {
                    hasWhiteSpaces = true;
                }
            } while (isWhiteSpace);
            if (hasWhiteSpaces) {
                if (this.tokenizeWhiteSpace) {
                    this.currentPosition -= offset;
                    this.startPosition = whiteStart;
                    if (checkIfUnicode) {
                        this.withoutUnicodePtr = unicodePtr;
                    }
                    return TokenNameWHITESPACE;
                } else if (checkIfUnicode) {
                    this.withoutUnicodePtr = 0;
                    unicodeStore();
                } else {
                    this.withoutUnicodePtr = 0;
                }
            }
            char c = this.currentCharacter;
            if (c < ScannerHelper.MAX_OBVIOUS) {
                if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[c] & ScannerHelper.C_IDENT_START) != 0) {
                    return scanIdentifierOrKeywordWithBoundCheck();
                }
                return TokenNameERROR;
            }
            boolean isJavaIdStart;
            if (c >= HIGH_SURROGATE_MIN_VALUE && c <= HIGH_SURROGATE_MAX_VALUE) {
                if (this.complianceLevel < ClassFileConstants.JDK1_5) {
                    throw new InvalidInputException(INVALID_UNICODE_ESCAPE);
                }
                char low = (char) getNextCharWithBoundChecks();
                if (low < LOW_SURROGATE_MIN_VALUE || low > LOW_SURROGATE_MAX_VALUE) {
                    throw new InvalidInputException(INVALID_LOW_SURROGATE);
                }
                isJavaIdStart = ScannerHelper.isJavaIdentifierStart(this.complianceLevel, c, low);
            } else if (c >= LOW_SURROGATE_MIN_VALUE && c <= LOW_SURROGATE_MAX_VALUE) {
                if (this.complianceLevel < ClassFileConstants.JDK1_5) {
                    throw new InvalidInputException(INVALID_UNICODE_ESCAPE);
                }
                throw new InvalidInputException(INVALID_HIGH_SURROGATE);
            } else {
                isJavaIdStart = ScannerHelper.isJavaIdentifierStart(this.complianceLevel, c);
            }
            if (isJavaIdStart)
                return scanIdentifierOrKeywordWithBoundCheck();
            return TokenNameERROR;
        }
    }

    public int getNextToken() throws InvalidInputException {
        this.wasAcr = false;
        if (this.diet) {
            jumpOverMethodBody();
            this.diet = false;
            return this.currentPosition > this.eofPosition ? TokenNameEOF : TokenNameRBRACE;
        }
        int whiteStart = 0;
        try {
            while (true) {
                this.withoutUnicodePtr = 0;
                whiteStart = this.currentPosition;
                boolean isWhiteSpace, hasWhiteSpaces = false;
                int offset;
                int unicodePtr;
                boolean checkIfUnicode = false;
                do {
                    unicodePtr = this.withoutUnicodePtr;
                    offset = this.currentPosition;
                    this.startPosition = this.currentPosition;
                    try {
                        checkIfUnicode = ((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u');
                    } catch (IndexOutOfBoundsException e) {
                        if (this.tokenizeWhiteSpace && (whiteStart != this.currentPosition - 1)) {
                            this.currentPosition--;
                            this.startPosition = whiteStart;
                            return TokenNameWHITESPACE;
                        }
                        if (this.currentPosition > this.eofPosition)
                            return TokenNameEOF;
                    }
                    if (this.currentPosition > this.eofPosition) {
                        if (this.tokenizeWhiteSpace && (whiteStart != this.currentPosition - 1)) {
                            this.currentPosition--;
                            this.startPosition = whiteStart;
                            return TokenNameWHITESPACE;
                        }
                        return TokenNameEOF;
                    }
                    if (checkIfUnicode) {
                        isWhiteSpace = jumpOverUnicodeWhiteSpace();
                        offset = this.currentPosition - offset;
                    } else {
                        offset = this.currentPosition - offset;
                        if ((this.currentCharacter == '\r') || (this.currentCharacter == '\n')) {
                            if (this.recordLineSeparator) {
                                pushLineSeparator();
                            }
                        }
                        switch(this.currentCharacter) {
                            case 10:
                            case 12:
                            case 13:
                            case 32:
                            case 9:
                                isWhiteSpace = true;
                                break;
                            default:
                                isWhiteSpace = false;
                        }
                    }
                    if (isWhiteSpace) {
                        hasWhiteSpaces = true;
                    }
                } while (isWhiteSpace);
                if (hasWhiteSpaces) {
                    if (this.tokenizeWhiteSpace) {
                        this.currentPosition -= offset;
                        this.startPosition = whiteStart;
                        if (checkIfUnicode) {
                            this.withoutUnicodePtr = unicodePtr;
                        }
                        return TokenNameWHITESPACE;
                    } else if (checkIfUnicode) {
                        this.withoutUnicodePtr = 0;
                        unicodeStore();
                    } else {
                        this.withoutUnicodePtr = 0;
                    }
                }
                switch(this.currentCharacter) {
                    case '@':
                        return TokenNameAT;
                    case '(':
                        return TokenNameLPAREN;
                    case ')':
                        return TokenNameRPAREN;
                    case '{':
                        return TokenNameLBRACE;
                    case '}':
                        return TokenNameRBRACE;
                    case '[':
                        return TokenNameLBRACKET;
                    case ']':
                        return TokenNameRBRACKET;
                    case ';':
                        return TokenNameSEMICOLON;
                    case ',':
                        return TokenNameCOMMA;
                    case '.':
                        if (getNextCharAsDigit()) {
                            return scanNumber(true);
                        }
                        int temp = this.currentPosition;
                        if (getNextChar('.')) {
                            if (getNextChar('.')) {
                                return TokenNameELLIPSIS;
                            } else {
                                this.currentPosition = temp;
                                return TokenNameDOT;
                            }
                        } else {
                            this.currentPosition = temp;
                            return TokenNameDOT;
                        }
                    case '+':
                        {
                            int test;
                            if ((test = getNextChar('+', '=')) == 0)
                                return TokenNamePLUS_PLUS;
                            if (test > 0)
                                return TokenNamePLUS_EQUAL;
                            return TokenNamePLUS;
                        }
                    case '-':
                        {
                            int test;
                            if ((test = getNextChar('-', '=')) == 0)
                                return TokenNameMINUS_MINUS;
                            if (test > 0)
                                return TokenNameMINUS_EQUAL;
                            if (getNextChar('>'))
                                return TokenNameARROW;
                            return TokenNameMINUS;
                        }
                    case '~':
                        return TokenNameTWIDDLE;
                    case '!':
                        if (getNextChar('='))
                            return TokenNameNOT_EQUAL;
                        return TokenNameNOT;
                    case '*':
                        if (getNextChar('='))
                            return TokenNameMULTIPLY_EQUAL;
                        return TokenNameMULTIPLY;
                    case '%':
                        if (getNextChar('='))
                            return TokenNameREMAINDER_EQUAL;
                        return TokenNameREMAINDER;
                    case '<':
                        {
                            int test;
                            if ((test = getNextChar('=', '<')) == 0)
                                return TokenNameLESS_EQUAL;
                            if (test > 0) {
                                if (getNextChar('='))
                                    return TokenNameLEFT_SHIFT_EQUAL;
                                return TokenNameLEFT_SHIFT;
                            }
                            return TokenNameLESS;
                        }
                    case '>':
                        {
                            int test;
                            if (this.returnOnlyGreater) {
                                return TokenNameGREATER;
                            }
                            if ((test = getNextChar('=', '>')) == 0)
                                return TokenNameGREATER_EQUAL;
                            if (test > 0) {
                                if ((test = getNextChar('=', '>')) == 0)
                                    return TokenNameRIGHT_SHIFT_EQUAL;
                                if (test > 0) {
                                    if (getNextChar('='))
                                        return TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL;
                                    return TokenNameUNSIGNED_RIGHT_SHIFT;
                                }
                                return TokenNameRIGHT_SHIFT;
                            }
                            return TokenNameGREATER;
                        }
                    case '=':
                        if (getNextChar('='))
                            return TokenNameEQUAL_EQUAL;
                        return TokenNameEQUAL;
                    case '&':
                        {
                            int test;
                            if ((test = getNextChar('&', '=')) == 0)
                                return TokenNameAND_AND;
                            if (test > 0)
                                return TokenNameAND_EQUAL;
                            return TokenNameAND;
                        }
                    case '|':
                        {
                            int test;
                            if ((test = getNextChar('|', '=')) == 0)
                                return TokenNameOR_OR;
                            if (test > 0)
                                return TokenNameOR_EQUAL;
                            return TokenNameOR;
                        }
                    case '^':
                        if (getNextChar('='))
                            return TokenNameXOR_EQUAL;
                        return TokenNameXOR;
                    case '?':
                        return TokenNameQUESTION;
                    case ':':
                        if (getNextChar(':'))
                            return TokenNameCOLON_COLON;
                        return TokenNameCOLON;
                    case '\'':
                        {
                            int test;
                            if ((test = getNextChar('\n', '\r')) == 0) {
                                throw new InvalidInputException(INVALID_CHARACTER_CONSTANT);
                            }
                            if (test > 0) {
                                // relocate if finding another quote fairly close: thus unicode '/u000D' will be fully consumed
                                for (int lookAhead = 0; lookAhead < 3; lookAhead++) {
                                    if (this.currentPosition + lookAhead == this.eofPosition)
                                        break;
                                    if (this.source[this.currentPosition + lookAhead] == '\n')
                                        break;
                                    if (this.source[this.currentPosition + lookAhead] == '\'') {
                                        this.currentPosition += lookAhead + 1;
                                        break;
                                    }
                                }
                                throw new InvalidInputException(INVALID_CHARACTER_CONSTANT);
                            }
                        }
                        if (getNextChar('\'')) {
                            // relocate if finding another quote fairly close: thus unicode '/u000D' will be fully consumed
                            for (int lookAhead = 0; lookAhead < 3; lookAhead++) {
                                if (this.currentPosition + lookAhead == this.eofPosition)
                                    break;
                                if (this.source[this.currentPosition + lookAhead] == '\n')
                                    break;
                                if (this.source[this.currentPosition + lookAhead] == '\'') {
                                    this.currentPosition += lookAhead + 1;
                                    break;
                                }
                            }
                            throw new InvalidInputException(INVALID_CHARACTER_CONSTANT);
                        }
                        if (getNextChar('\\')) {
                            if (this.unicodeAsBackSlash) {
                                this.unicodeAsBackSlash = false;
                                if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                    getNextUnicodeChar();
                                } else {
                                    if (this.withoutUnicodePtr != 0) {
                                        unicodeStore();
                                    }
                                }
                            } else {
                                this.currentCharacter = this.source[this.currentPosition++];
                            }
                            scanEscapeCharacter();
                        } else {
                            this.unicodeAsBackSlash = false;
                            checkIfUnicode = false;
                            try {
                                checkIfUnicode = ((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u');
                            } catch (IndexOutOfBoundsException e) {
                                this.currentPosition--;
                                throw new InvalidInputException(INVALID_CHARACTER_CONSTANT);
                            }
                            if (checkIfUnicode) {
                                getNextUnicodeChar();
                            } else {
                                if (this.withoutUnicodePtr != 0) {
                                    unicodeStore();
                                }
                            }
                        }
                        if (getNextChar('\''))
                            return TokenNameCharacterLiteral;
                        // relocate if finding another quote fairly close: thus unicode '/u000D' will be fully consumed
                        for (int lookAhead = 0; lookAhead < 20; lookAhead++) {
                            if (this.currentPosition + lookAhead == this.eofPosition)
                                break;
                            if (this.source[this.currentPosition + lookAhead] == '\n')
                                break;
                            if (this.source[this.currentPosition + lookAhead] == '\'') {
                                this.currentPosition += lookAhead + 1;
                                break;
                            }
                        }
                        throw new InvalidInputException(INVALID_CHARACTER_CONSTANT);
                    case '"':
                        try {
                            // consume next character
                            this.unicodeAsBackSlash = false;
                            boolean isUnicode = false;
                            if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                getNextUnicodeChar();
                                isUnicode = true;
                            } else {
                                if (this.withoutUnicodePtr != 0) {
                                    unicodeStore();
                                }
                            }
                            while (this.currentCharacter != '"') {
                                if (this.currentPosition >= this.eofPosition) {
                                    throw new InvalidInputException(UNTERMINATED_STRING);
                                }
                                if ((this.currentCharacter == '\n') || (this.currentCharacter == '\r')) {
                                    if (isUnicode) {
                                        int start = this.currentPosition;
                                        for (int lookAhead = 0; lookAhead < 50; lookAhead++) {
                                            if (this.currentPosition >= this.eofPosition) {
                                                this.currentPosition = start;
                                                break;
                                            }
                                            if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                                isUnicode = true;
                                                getNextUnicodeChar();
                                            } else {
                                                isUnicode = false;
                                            }
                                            if (!isUnicode && this.currentCharacter == '\n') {
                                                this.currentPosition--;
                                                break;
                                            }
                                            if (this.currentCharacter == '\"') {
                                                throw new InvalidInputException(INVALID_CHAR_IN_STRING);
                                            }
                                        }
                                    } else {
                                        this.currentPosition--;
                                    }
                                    throw new InvalidInputException(INVALID_CHAR_IN_STRING);
                                }
                                if (this.currentCharacter == '\\') {
                                    if (this.unicodeAsBackSlash) {
                                        this.withoutUnicodePtr--;
                                        this.unicodeAsBackSlash = false;
                                        if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                            getNextUnicodeChar();
                                            isUnicode = true;
                                            this.withoutUnicodePtr--;
                                        } else {
                                            isUnicode = false;
                                        }
                                    } else {
                                        if (this.withoutUnicodePtr == 0) {
                                            unicodeInitializeBuffer(this.currentPosition - this.startPosition);
                                        }
                                        this.withoutUnicodePtr--;
                                        this.currentCharacter = this.source[this.currentPosition++];
                                    }
                                    scanEscapeCharacter();
                                    if (this.withoutUnicodePtr != 0) {
                                        unicodeStore();
                                    }
                                }
                                this.unicodeAsBackSlash = false;
                                if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                    getNextUnicodeChar();
                                    isUnicode = true;
                                } else {
                                    isUnicode = false;
                                    if (this.withoutUnicodePtr != 0) {
                                        unicodeStore();
                                    }
                                }
                            }
                        } catch (IndexOutOfBoundsException e) {
                            this.currentPosition--;
                            throw new InvalidInputException(UNTERMINATED_STRING);
                        } catch (InvalidInputException e) {
                            if (e.getMessage().equals(INVALID_ESCAPE)) {
                                for (int lookAhead = 0; lookAhead < 50; lookAhead++) {
                                    if (this.currentPosition + lookAhead == this.eofPosition)
                                        break;
                                    if (this.source[this.currentPosition + lookAhead] == '\n')
                                        break;
                                    if (this.source[this.currentPosition + lookAhead] == '\"') {
                                        this.currentPosition += lookAhead + 1;
                                        break;
                                    }
                                }
                            }
                            throw e;
                        }
                        return TokenNameStringLiteral;
                    case '/':
                        if (!this.skipComments) {
                            int test = getNextChar('/', '*');
                            if (test == 0) {
                                this.lastCommentLinePosition = this.currentPosition;
                                try {
                                    if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                        getNextUnicodeChar();
                                    }
                                    if (this.currentCharacter == '\\') {
                                        if (this.source[this.currentPosition] == '\\')
                                            this.currentPosition++;
                                    }
                                    boolean isUnicode = false;
                                    while (this.currentCharacter != '\r' && this.currentCharacter != '\n') {
                                        if (this.currentPosition >= this.eofPosition) {
                                            this.lastCommentLinePosition = this.currentPosition;
                                            this.currentPosition++;
                                            throw new IndexOutOfBoundsException();
                                        }
                                        this.lastCommentLinePosition = this.currentPosition;
                                        isUnicode = false;
                                        if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                            getNextUnicodeChar();
                                            isUnicode = true;
                                        }
                                        if (this.currentCharacter == '\\') {
                                            if (this.source[this.currentPosition] == '\\')
                                                this.currentPosition++;
                                        }
                                    }
                                    if (this.currentCharacter == '\r' && this.eofPosition > this.currentPosition) {
                                        if (this.source[this.currentPosition] == '\n') {
                                            this.currentPosition++;
                                            this.currentCharacter = '\n';
                                        } else if ((this.source[this.currentPosition] == '\\') && (this.source[this.currentPosition + 1] == 'u')) {
                                            getNextUnicodeChar();
                                            isUnicode = true;
                                        }
                                    }
                                    recordComment(TokenNameCOMMENT_LINE);
                                    if (this.taskTags != null)
                                        checkTaskTag(this.startPosition, this.currentPosition);
                                    if ((this.currentCharacter == '\r') || (this.currentCharacter == '\n')) {
                                        if (this.checkNonExternalizedStringLiterals && this.lastPosition < this.currentPosition) {
                                            parseTags();
                                        }
                                        if (this.recordLineSeparator) {
                                            if (isUnicode) {
                                                pushUnicodeLineSeparator();
                                            } else {
                                                pushLineSeparator();
                                            }
                                        }
                                    }
                                    if (this.tokenizeComments) {
                                        return TokenNameCOMMENT_LINE;
                                    }
                                } catch (IndexOutOfBoundsException e) {
                                    this.currentPosition--;
                                    recordComment(TokenNameCOMMENT_LINE);
                                    if (this.taskTags != null)
                                        checkTaskTag(this.startPosition, this.currentPosition);
                                    if (this.checkNonExternalizedStringLiterals && this.lastPosition < this.currentPosition) {
                                        parseTags();
                                    }
                                    if (this.tokenizeComments) {
                                        return TokenNameCOMMENT_LINE;
                                    } else {
                                        this.currentPosition++;
                                    }
                                }
                                break;
                            }
                            if (test > 0) {
                                try {
                                    boolean isJavadoc = false, star = false;
                                    boolean isUnicode = false;
                                    int previous;
                                    this.unicodeAsBackSlash = false;
                                    if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                        getNextUnicodeChar();
                                        isUnicode = true;
                                    } else {
                                        isUnicode = false;
                                        if (this.withoutUnicodePtr != 0) {
                                            unicodeStore();
                                        }
                                    }
                                    if (this.currentCharacter == '*') {
                                        isJavadoc = true;
                                        star = true;
                                    }
                                    if ((this.currentCharacter == '\r') || (this.currentCharacter == '\n')) {
                                        if (this.recordLineSeparator) {
                                            if (isUnicode) {
                                                pushUnicodeLineSeparator();
                                            } else {
                                                pushLineSeparator();
                                            }
                                        }
                                    }
                                    isUnicode = false;
                                    previous = this.currentPosition;
                                    if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                        getNextUnicodeChar();
                                        isUnicode = true;
                                    } else {
                                        isUnicode = false;
                                    }
                                    if (this.currentCharacter == '\\') {
                                        if (this.source[this.currentPosition] == '\\')
                                            this.currentPosition++;
                                    }
                                    if (this.currentCharacter == '/') {
                                        isJavadoc = false;
                                    }
                                    int firstTag = 0;
                                    while ((this.currentCharacter != '/') || (!star)) {
                                        if (this.currentPosition >= this.eofPosition) {
                                            throw new InvalidInputException(UNTERMINATED_COMMENT);
                                        }
                                        if ((this.currentCharacter == '\r') || (this.currentCharacter == '\n')) {
                                            if (this.recordLineSeparator) {
                                                if (isUnicode) {
                                                    pushUnicodeLineSeparator();
                                                } else {
                                                    pushLineSeparator();
                                                }
                                            }
                                        }
                                        switch(this.currentCharacter) {
                                            case '*':
                                                star = true;
                                                break;
                                            case '@':
                                                if (firstTag == 0 && this.isFirstTag()) {
                                                    firstTag = previous;
                                                }
                                            default:
                                                star = false;
                                        }
                                        previous = this.currentPosition;
                                        if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                            getNextUnicodeChar();
                                            isUnicode = true;
                                        } else {
                                            isUnicode = false;
                                        }
                                        if (this.currentCharacter == '\\') {
                                            if (this.source[this.currentPosition] == '\\')
                                                this.currentPosition++;
                                        }
                                    }
                                    int token = isJavadoc ? TokenNameCOMMENT_JAVADOC : TokenNameCOMMENT_BLOCK;
                                    recordComment(token);
                                    this.commentTagStarts[this.commentPtr] = firstTag;
                                    if (this.taskTags != null)
                                        checkTaskTag(this.startPosition, this.currentPosition);
                                    if (this.tokenizeComments) {
                                        return token;
                                    }
                                } catch (IndexOutOfBoundsException e) {
                                    this.currentPosition--;
                                    throw new InvalidInputException(UNTERMINATED_COMMENT);
                                }
                                break;
                            }
                        }
                        if (getNextChar('='))
                            return TokenNameDIVIDE_EQUAL;
                        return TokenNameDIVIDE;
                    case '':
                        if (atEnd())
                            return TokenNameEOF;
                        throw new InvalidInputException("Ctrl-Z");
                    default:
                        char c = this.currentCharacter;
                        if (c < ScannerHelper.MAX_OBVIOUS) {
                            if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[c] & ScannerHelper.C_IDENT_START) != 0) {
                                return scanIdentifierOrKeyword();
                            } else if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[c] & ScannerHelper.C_DIGIT) != 0) {
                                return scanNumber(false);
                            } else {
                                return TokenNameERROR;
                            }
                        }
                        boolean isJavaIdStart;
                        if (c >= HIGH_SURROGATE_MIN_VALUE && c <= HIGH_SURROGATE_MAX_VALUE) {
                            if (this.complianceLevel < ClassFileConstants.JDK1_5) {
                                throw new InvalidInputException(INVALID_UNICODE_ESCAPE);
                            }
                            char low = (char) getNextChar();
                            if (low < LOW_SURROGATE_MIN_VALUE || low > LOW_SURROGATE_MAX_VALUE) {
                                throw new InvalidInputException(INVALID_LOW_SURROGATE);
                            }
                            isJavaIdStart = ScannerHelper.isJavaIdentifierStart(this.complianceLevel, c, low);
                        } else if (c >= LOW_SURROGATE_MIN_VALUE && c <= LOW_SURROGATE_MAX_VALUE) {
                            if (this.complianceLevel < ClassFileConstants.JDK1_5) {
                                throw new InvalidInputException(INVALID_UNICODE_ESCAPE);
                            }
                            throw new InvalidInputException(INVALID_HIGH_SURROGATE);
                        } else {
                            isJavaIdStart = ScannerHelper.isJavaIdentifierStart(this.complianceLevel, c);
                        }
                        if (isJavaIdStart)
                            return scanIdentifierOrKeyword();
                        if (ScannerHelper.isDigit(this.currentCharacter)) {
                            return scanNumber(false);
                        }
                        return TokenNameERROR;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            if (this.tokenizeWhiteSpace && (whiteStart != this.currentPosition - 1)) {
                this.currentPosition--;
                this.startPosition = whiteStart;
                return TokenNameWHITESPACE;
            }
        }
        return TokenNameEOF;
    }

    public void getNextUnicodeChar() throws InvalidInputException {
        int c1 = 0, c2 = 0, c3 = 0, c4 = 0, unicodeSize = 6;
        this.currentPosition++;
        if (this.currentPosition < this.eofPosition) {
            while (this.source[this.currentPosition] == 'u') {
                this.currentPosition++;
                if (this.currentPosition >= this.eofPosition) {
                    this.currentPosition--;
                    throw new InvalidInputException(INVALID_UNICODE_ESCAPE);
                }
                unicodeSize++;
            }
        } else {
            this.currentPosition--;
            throw new InvalidInputException(INVALID_UNICODE_ESCAPE);
        }
        if ((this.currentPosition + 4) > this.eofPosition) {
            this.currentPosition += (this.eofPosition - this.currentPosition);
            throw new InvalidInputException(INVALID_UNICODE_ESCAPE);
        }
        if ((c1 = ScannerHelper.getHexadecimalValue(this.source[this.currentPosition++])) > 15 || c1 < 0 || (c2 = ScannerHelper.getHexadecimalValue(this.source[this.currentPosition++])) > 15 || c2 < 0 || (c3 = ScannerHelper.getHexadecimalValue(this.source[this.currentPosition++])) > 15 || c3 < 0 || (c4 = ScannerHelper.getHexadecimalValue(this.source[this.currentPosition++])) > 15 || c4 < 0) {
            throw new InvalidInputException(INVALID_UNICODE_ESCAPE);
        }
        this.currentCharacter = (char) (((c1 * 16 + c2) * 16 + c3) * 16 + c4);
        if (this.withoutUnicodePtr == 0) {
            unicodeInitializeBuffer(this.currentPosition - unicodeSize - this.startPosition);
        }
        unicodeStore();
        this.unicodeAsBackSlash = this.currentCharacter == '\\';
    }

    public NLSTag[] getNLSTags() {
        final int length = this.nlsTagsPtr;
        if (length != 0) {
            NLSTag[] result = new NLSTag[length];
            System.arraycopy(this.nlsTags, 0, result, 0, length);
            this.nlsTagsPtr = 0;
            return result;
        }
        return null;
    }

    public char[] getSource() {
        return this.source;
    }

    protected boolean isFirstTag() {
        return true;
    }

    public final void jumpOverMethodBody() {
        this.wasAcr = false;
        int found = 1;
        try {
            while (true) {
                this.withoutUnicodePtr = 0;
                boolean isWhiteSpace;
                do {
                    this.startPosition = this.currentPosition;
                    if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                        isWhiteSpace = jumpOverUnicodeWhiteSpace();
                    } else {
                        if (this.recordLineSeparator && ((this.currentCharacter == '\r') || (this.currentCharacter == '\n'))) {
                            pushLineSeparator();
                        }
                        isWhiteSpace = CharOperation.isWhitespace(this.currentCharacter);
                    }
                } while (isWhiteSpace);
                NextToken: switch(this.currentCharacter) {
                    case '{':
                        found++;
                        break NextToken;
                    case '}':
                        found--;
                        if (found == 0)
                            return;
                        break NextToken;
                    case '\'':
                        {
                            boolean test;
                            test = getNextChar('\\');
                            if (test) {
                                try {
                                    if (this.unicodeAsBackSlash) {
                                        this.unicodeAsBackSlash = false;
                                        if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                            getNextUnicodeChar();
                                        } else {
                                            if (this.withoutUnicodePtr != 0) {
                                                unicodeStore();
                                            }
                                        }
                                    } else {
                                        this.currentCharacter = this.source[this.currentPosition++];
                                    }
                                    scanEscapeCharacter();
                                } catch (InvalidInputException ex) {
                                }
                            } else {
                                try {
                                    this.unicodeAsBackSlash = false;
                                    if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                        getNextUnicodeChar();
                                    } else {
                                        if (this.withoutUnicodePtr != 0) {
                                            unicodeStore();
                                        }
                                    }
                                } catch (InvalidInputException ex) {
                                }
                            }
                            getNextChar('\'');
                            break NextToken;
                        }
                    case '"':
                        try {
                            try {
                                this.unicodeAsBackSlash = false;
                                if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                    getNextUnicodeChar();
                                } else {
                                    if (this.withoutUnicodePtr != 0) {
                                        unicodeStore();
                                    }
                                }
                            } catch (InvalidInputException ex) {
                            }
                            while (this.currentCharacter != '"') {
                                if (this.currentPosition >= this.eofPosition) {
                                    return;
                                }
                                if (this.currentCharacter == '\r') {
                                    if (this.source[this.currentPosition] == '\n')
                                        this.currentPosition++;
                                    break NextToken;
                                }
                                if (this.currentCharacter == '\n') {
                                    break;
                                }
                                if (this.currentCharacter == '\\') {
                                    try {
                                        if (this.unicodeAsBackSlash) {
                                            this.unicodeAsBackSlash = false;
                                            if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                                getNextUnicodeChar();
                                            } else {
                                                if (this.withoutUnicodePtr != 0) {
                                                    unicodeStore();
                                                }
                                            }
                                        } else {
                                            this.currentCharacter = this.source[this.currentPosition++];
                                        }
                                        scanEscapeCharacter();
                                    } catch (InvalidInputException ex) {
                                    }
                                }
                                try {
                                    this.unicodeAsBackSlash = false;
                                    if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                        getNextUnicodeChar();
                                    } else {
                                        if (this.withoutUnicodePtr != 0) {
                                            unicodeStore();
                                        }
                                    }
                                } catch (InvalidInputException ex) {
                                }
                            }
                        } catch (IndexOutOfBoundsException e) {
                            return;
                        }
                        break NextToken;
                    case '/':
                        {
                            int test;
                            if ((test = getNextChar('/', '*')) == 0) {
                                try {
                                    this.lastCommentLinePosition = this.currentPosition;
                                    if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                        getNextUnicodeChar();
                                    }
                                    if (this.currentCharacter == '\\') {
                                        if (this.source[this.currentPosition] == '\\')
                                            this.currentPosition++;
                                    }
                                    boolean isUnicode = false;
                                    while (this.currentCharacter != '\r' && this.currentCharacter != '\n') {
                                        if (this.currentPosition >= this.eofPosition) {
                                            this.lastCommentLinePosition = this.currentPosition;
                                            this.currentPosition++;
                                            throw new IndexOutOfBoundsException();
                                        }
                                        this.lastCommentLinePosition = this.currentPosition;
                                        isUnicode = false;
                                        if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                            isUnicode = true;
                                            getNextUnicodeChar();
                                        }
                                        if (this.currentCharacter == '\\') {
                                            if (this.source[this.currentPosition] == '\\')
                                                this.currentPosition++;
                                        }
                                    }
                                    if (this.currentCharacter == '\r' && this.eofPosition > this.currentPosition) {
                                        if (this.source[this.currentPosition] == '\n') {
                                            this.currentPosition++;
                                            this.currentCharacter = '\n';
                                        } else if ((this.source[this.currentPosition] == '\\') && (this.source[this.currentPosition + 1] == 'u')) {
                                            isUnicode = true;
                                            getNextUnicodeChar();
                                        }
                                    }
                                    recordComment(TokenNameCOMMENT_LINE);
                                    if (this.recordLineSeparator && ((this.currentCharacter == '\r') || (this.currentCharacter == '\n'))) {
                                        if (this.checkNonExternalizedStringLiterals && this.lastPosition < this.currentPosition) {
                                            parseTags();
                                        }
                                        if (this.recordLineSeparator) {
                                            if (isUnicode) {
                                                pushUnicodeLineSeparator();
                                            } else {
                                                pushLineSeparator();
                                            }
                                        }
                                    }
                                } catch (IndexOutOfBoundsException e) {
                                    this.currentPosition--;
                                    recordComment(TokenNameCOMMENT_LINE);
                                    if (this.checkNonExternalizedStringLiterals && this.lastPosition < this.currentPosition) {
                                        parseTags();
                                    }
                                    if (!this.tokenizeComments) {
                                        this.currentPosition++;
                                    }
                                }
                                break NextToken;
                            }
                            if (test > 0) {
                                boolean isJavadoc = false;
                                try {
                                    boolean star = false;
                                    int previous;
                                    boolean isUnicode = false;
                                    this.unicodeAsBackSlash = false;
                                    if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                        getNextUnicodeChar();
                                        isUnicode = true;
                                    } else {
                                        isUnicode = false;
                                        if (this.withoutUnicodePtr != 0) {
                                            unicodeStore();
                                        }
                                    }
                                    if (this.currentCharacter == '*') {
                                        isJavadoc = true;
                                        star = true;
                                    }
                                    if ((this.currentCharacter == '\r') || (this.currentCharacter == '\n')) {
                                        if (this.recordLineSeparator) {
                                            if (isUnicode) {
                                                pushUnicodeLineSeparator();
                                            } else {
                                                pushLineSeparator();
                                            }
                                        }
                                    }
                                    isUnicode = false;
                                    previous = this.currentPosition;
                                    if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                        getNextUnicodeChar();
                                        isUnicode = true;
                                    } else {
                                        isUnicode = false;
                                    }
                                    if (this.currentCharacter == '\\') {
                                        if (this.source[this.currentPosition] == '\\')
                                            this.currentPosition++;
                                    }
                                    if (this.currentCharacter == '/') {
                                        isJavadoc = false;
                                    }
                                    int firstTag = 0;
                                    while ((this.currentCharacter != '/') || (!star)) {
                                        if (this.currentPosition >= this.eofPosition) {
                                            return;
                                        }
                                        if ((this.currentCharacter == '\r') || (this.currentCharacter == '\n')) {
                                            if (this.recordLineSeparator) {
                                                if (isUnicode) {
                                                    pushUnicodeLineSeparator();
                                                } else {
                                                    pushLineSeparator();
                                                }
                                            }
                                        }
                                        switch(this.currentCharacter) {
                                            case '*':
                                                star = true;
                                                break;
                                            case '@':
                                                if (firstTag == 0 && this.isFirstTag()) {
                                                    firstTag = previous;
                                                }
                                            default:
                                                star = false;
                                        }
                                        previous = this.currentPosition;
                                        if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                            getNextUnicodeChar();
                                            isUnicode = true;
                                        } else {
                                            isUnicode = false;
                                        }
                                        if (this.currentCharacter == '\\') {
                                            if (this.source[this.currentPosition] == '\\')
                                                this.currentPosition++;
                                        }
                                    }
                                    recordComment(isJavadoc ? TokenNameCOMMENT_JAVADOC : TokenNameCOMMENT_BLOCK);
                                    this.commentTagStarts[this.commentPtr] = firstTag;
                                } catch (IndexOutOfBoundsException e) {
                                    return;
                                }
                                break NextToken;
                            }
                            break NextToken;
                        }
                    default:
                        try {
                            char c = this.currentCharacter;
                            if (c < ScannerHelper.MAX_OBVIOUS) {
                                if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[c] & ScannerHelper.C_IDENT_START) != 0) {
                                    scanIdentifierOrKeyword();
                                    break NextToken;
                                } else if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[c] & ScannerHelper.C_DIGIT) != 0) {
                                    scanNumber(false);
                                    break NextToken;
                                } else {
                                    break NextToken;
                                }
                            }
                            boolean isJavaIdStart;
                            if (c >= HIGH_SURROGATE_MIN_VALUE && c <= HIGH_SURROGATE_MAX_VALUE) {
                                if (this.complianceLevel < ClassFileConstants.JDK1_5) {
                                    throw new InvalidInputException(INVALID_UNICODE_ESCAPE);
                                }
                                char low = (char) getNextChar();
                                if (low < LOW_SURROGATE_MIN_VALUE || low > LOW_SURROGATE_MAX_VALUE) {
                                    break NextToken;
                                }
                                isJavaIdStart = ScannerHelper.isJavaIdentifierStart(this.complianceLevel, c, low);
                            } else if (c >= LOW_SURROGATE_MIN_VALUE && c <= LOW_SURROGATE_MAX_VALUE) {
                                break NextToken;
                            } else {
                                isJavaIdStart = ScannerHelper.isJavaIdentifierStart(this.complianceLevel, c);
                            }
                            if (isJavaIdStart) {
                                scanIdentifierOrKeyword();
                                break NextToken;
                            }
                        } catch (InvalidInputException ex) {
                        }
                }
            }
        } catch (IndexOutOfBoundsException e) {
        } catch (InvalidInputException e) {
        }
        return;
    }

    public final boolean jumpOverUnicodeWhiteSpace() throws InvalidInputException {
        this.wasAcr = false;
        getNextUnicodeChar();
        return CharOperation.isWhitespace(this.currentCharacter);
    }

    final char[] optimizedCurrentTokenSource1() {
        char charOne = this.source[this.startPosition];
        switch(charOne) {
            case 'a':
                return charArray_a;
            case 'b':
                return charArray_b;
            case 'c':
                return charArray_c;
            case 'd':
                return charArray_d;
            case 'e':
                return charArray_e;
            case 'f':
                return charArray_f;
            case 'g':
                return charArray_g;
            case 'h':
                return charArray_h;
            case 'i':
                return charArray_i;
            case 'j':
                return charArray_j;
            case 'k':
                return charArray_k;
            case 'l':
                return charArray_l;
            case 'm':
                return charArray_m;
            case 'n':
                return charArray_n;
            case 'o':
                return charArray_o;
            case 'p':
                return charArray_p;
            case 'q':
                return charArray_q;
            case 'r':
                return charArray_r;
            case 's':
                return charArray_s;
            case 't':
                return charArray_t;
            case 'u':
                return charArray_u;
            case 'v':
                return charArray_v;
            case 'w':
                return charArray_w;
            case 'x':
                return charArray_x;
            case 'y':
                return charArray_y;
            case 'z':
                return charArray_z;
            default:
                return new char[] { charOne };
        }
    }

    final char[] optimizedCurrentTokenSource2() {
        char[] src = this.source;
        int start = this.startPosition;
        char c0, c1;
        int hash = (((c0 = src[start]) << 6) + (c1 = src[start + 1])) % TableSize;
        char[][] table = this.charArray_length[0][hash];
        int i = this.newEntry2;
        while (++i < InternalTableSize) {
            char[] charArray = table[i];
            if ((c0 == charArray[0]) && (c1 == charArray[1]))
                return charArray;
        }
        i = -1;
        int max = this.newEntry2;
        while (++i <= max) {
            char[] charArray = table[i];
            if ((c0 == charArray[0]) && (c1 == charArray[1]))
                return charArray;
        }
        if (++max >= InternalTableSize)
            max = 0;
        char[] r;
        System.arraycopy(src, start, r = new char[2], 0, 2);
        return table[this.newEntry2 = max] = r;
    }

    final char[] optimizedCurrentTokenSource3() {
        char[] src = this.source;
        int start = this.startPosition;
        char c0, c1 = src[start + 1], c2;
        int hash = (((c0 = src[start]) << 6) + (c2 = src[start + 2])) % TableSize;
        char[][] table = this.charArray_length[1][hash];
        int i = this.newEntry3;
        while (++i < InternalTableSize) {
            char[] charArray = table[i];
            if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]))
                return charArray;
        }
        i = -1;
        int max = this.newEntry3;
        while (++i <= max) {
            char[] charArray = table[i];
            if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]))
                return charArray;
        }
        if (++max >= InternalTableSize)
            max = 0;
        char[] r;
        System.arraycopy(src, start, r = new char[3], 0, 3);
        return table[this.newEntry3 = max] = r;
    }

    final char[] optimizedCurrentTokenSource4() {
        char[] src = this.source;
        int start = this.startPosition;
        char c0, c1 = src[start + 1], c2, c3 = src[start + 3];
        int hash = (((c0 = src[start]) << 6) + (c2 = src[start + 2])) % TableSize;
        char[][] table = this.charArray_length[2][hash];
        int i = this.newEntry4;
        while (++i < InternalTableSize) {
            char[] charArray = table[i];
            if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]) && (c3 == charArray[3]))
                return charArray;
        }
        i = -1;
        int max = this.newEntry4;
        while (++i <= max) {
            char[] charArray = table[i];
            if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]) && (c3 == charArray[3]))
                return charArray;
        }
        if (++max >= InternalTableSize)
            max = 0;
        char[] r;
        System.arraycopy(src, start, r = new char[4], 0, 4);
        return table[this.newEntry4 = max] = r;
    }

    final char[] optimizedCurrentTokenSource5() {
        char[] src = this.source;
        int start = this.startPosition;
        char c0, c1 = src[start + 1], c2, c3 = src[start + 3], c4;
        int hash = (((c0 = src[start]) << 12) + ((c2 = src[start + 2]) << 6) + (c4 = src[start + 4])) % TableSize;
        char[][] table = this.charArray_length[3][hash];
        int i = this.newEntry5;
        while (++i < InternalTableSize) {
            char[] charArray = table[i];
            if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]) && (c3 == charArray[3]) && (c4 == charArray[4]))
                return charArray;
        }
        i = -1;
        int max = this.newEntry5;
        while (++i <= max) {
            char[] charArray = table[i];
            if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]) && (c3 == charArray[3]) && (c4 == charArray[4]))
                return charArray;
        }
        if (++max >= InternalTableSize)
            max = 0;
        char[] r;
        System.arraycopy(src, start, r = new char[5], 0, 5);
        return table[this.newEntry5 = max] = r;
    }

    final char[] optimizedCurrentTokenSource6() {
        char[] src = this.source;
        int start = this.startPosition;
        char c0, c1 = src[start + 1], c2, c3 = src[start + 3], c4, c5 = src[start + 5];
        int hash = (((c0 = src[start]) << 12) + ((c2 = src[start + 2]) << 6) + (c4 = src[start + 4])) % TableSize;
        char[][] table = this.charArray_length[4][hash];
        int i = this.newEntry6;
        while (++i < InternalTableSize) {
            char[] charArray = table[i];
            if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]) && (c3 == charArray[3]) && (c4 == charArray[4]) && (c5 == charArray[5]))
                return charArray;
        }
        i = -1;
        int max = this.newEntry6;
        while (++i <= max) {
            char[] charArray = table[i];
            if ((c0 == charArray[0]) && (c1 == charArray[1]) && (c2 == charArray[2]) && (c3 == charArray[3]) && (c4 == charArray[4]) && (c5 == charArray[5]))
                return charArray;
        }
        if (++max >= InternalTableSize)
            max = 0;
        char[] r;
        System.arraycopy(src, start, r = new char[6], 0, 6);
        return table[this.newEntry6 = max] = r;
    }

    private void parseTags() {
        int position = 0;
        final int currentStartPosition = this.startPosition;
        final int currentLinePtr = this.linePtr;
        if (currentLinePtr >= 0) {
            position = this.lineEnds[currentLinePtr] + 1;
        }
        while (ScannerHelper.isWhitespace(this.source[position])) {
            position++;
        }
        if (currentStartPosition == position) {
            return;
        }
        char[] s = null;
        int sourceEnd = this.currentPosition;
        int sourceStart = currentStartPosition;
        int sourceDelta = 0;
        if (this.withoutUnicodePtr != 0) {
            System.arraycopy(this.withoutUnicodeBuffer, 1, s = new char[this.withoutUnicodePtr], 0, this.withoutUnicodePtr);
            sourceEnd = this.withoutUnicodePtr;
            sourceStart = 1;
            sourceDelta = currentStartPosition;
        } else {
            s = this.source;
        }
        int pos = CharOperation.indexOf(TAG_PREFIX, s, true, sourceStart, sourceEnd);
        if (pos != -1) {
            if (this.nlsTags == null) {
                this.nlsTags = new NLSTag[10];
                this.nlsTagsPtr = 0;
            }
            while (pos != -1) {
                int start = pos + TAG_PREFIX_LENGTH;
                int end = CharOperation.indexOf(TAG_POSTFIX, s, start, sourceEnd);
                if (end != -1) {
                    NLSTag currentTag = null;
                    final int currentLine = currentLinePtr + 1;
                    try {
                        currentTag = new NLSTag(pos + sourceDelta, end + sourceDelta, currentLine, extractInt(s, start, end));
                    } catch (NumberFormatException e) {
                        currentTag = new NLSTag(pos + sourceDelta, end + sourceDelta, currentLine, -1);
                    }
                    if (this.nlsTagsPtr == this.nlsTags.length) {
                        System.arraycopy(this.nlsTags, 0, (this.nlsTags = new NLSTag[this.nlsTagsPtr + 10]), 0, this.nlsTagsPtr);
                    }
                    this.nlsTags[this.nlsTagsPtr++] = currentTag;
                } else {
                    end = start;
                }
                pos = CharOperation.indexOf(TAG_PREFIX, s, true, end, sourceEnd);
            }
        }
    }

    private int extractInt(char[] array, int start, int end) {
        int value = 0;
        for (int i = start; i < end; i++) {
            final char currentChar = array[i];
            int digit = 0;
            switch(currentChar) {
                case '0':
                    digit = 0;
                    break;
                case '1':
                    digit = 1;
                    break;
                case '2':
                    digit = 2;
                    break;
                case '3':
                    digit = 3;
                    break;
                case '4':
                    digit = 4;
                    break;
                case '5':
                    digit = 5;
                    break;
                case '6':
                    digit = 6;
                    break;
                case '7':
                    digit = 7;
                    break;
                case '8':
                    digit = 8;
                    break;
                case '9':
                    digit = 9;
                    break;
                default:
                    throw new NumberFormatException();
            }
            value *= 10;
            if (digit < 0)
                throw new NumberFormatException();
            value += digit;
        }
        return value;
    }

    public final void pushLineSeparator() {
        final int INCREMENT = 250;
        if (this.currentCharacter == '\r') {
            int separatorPos = this.currentPosition - 1;
            if ((this.linePtr >= 0) && (this.lineEnds[this.linePtr] >= separatorPos))
                return;
            int length = this.lineEnds.length;
            if (++this.linePtr >= length)
                System.arraycopy(this.lineEnds, 0, this.lineEnds = new int[length + INCREMENT], 0, length);
            this.lineEnds[this.linePtr] = separatorPos;
            try {
                if (this.source[this.currentPosition] == '\n') {
                    this.lineEnds[this.linePtr] = this.currentPosition;
                    this.currentPosition++;
                    this.wasAcr = false;
                } else {
                    this.wasAcr = true;
                }
            } catch (IndexOutOfBoundsException e) {
                this.wasAcr = true;
            }
        } else {
            if (this.currentCharacter == '\n') {
                if (this.wasAcr && (this.lineEnds[this.linePtr] == (this.currentPosition - 2))) {
                    this.lineEnds[this.linePtr] = this.currentPosition - 1;
                } else {
                    int separatorPos = this.currentPosition - 1;
                    if ((this.linePtr >= 0) && (this.lineEnds[this.linePtr] >= separatorPos))
                        return;
                    int length = this.lineEnds.length;
                    if (++this.linePtr >= length)
                        System.arraycopy(this.lineEnds, 0, this.lineEnds = new int[length + INCREMENT], 0, length);
                    this.lineEnds[this.linePtr] = separatorPos;
                }
                this.wasAcr = false;
            }
        }
    }

    public final void pushUnicodeLineSeparator() {
        if (this.currentCharacter == '\r') {
            if (this.source[this.currentPosition] == '\n') {
                this.wasAcr = false;
            } else {
                this.wasAcr = true;
            }
        } else {
            if (this.currentCharacter == '\n') {
                this.wasAcr = false;
            }
        }
    }

    public void recordComment(int token) {
        int commentStart = this.startPosition;
        int stopPosition = this.currentPosition;
        switch(token) {
            case TokenNameCOMMENT_LINE:
                commentStart = -this.startPosition;
                stopPosition = -this.lastCommentLinePosition;
                break;
            case TokenNameCOMMENT_BLOCK:
                stopPosition = -this.currentPosition;
                break;
        }
        int length = this.commentStops.length;
        if (++this.commentPtr >= length) {
            int newLength = length + COMMENT_ARRAYS_SIZE * 10;
            System.arraycopy(this.commentStops, 0, this.commentStops = new int[newLength], 0, length);
            System.arraycopy(this.commentStarts, 0, this.commentStarts = new int[newLength], 0, length);
            System.arraycopy(this.commentTagStarts, 0, this.commentTagStarts = new int[newLength], 0, length);
        }
        this.commentStops[this.commentPtr] = stopPosition;
        this.commentStarts[this.commentPtr] = commentStart;
    }

    public void resetTo(int begin, int end) {
        this.diet = false;
        this.initialPosition = this.startPosition = this.currentPosition = begin;
        if (this.source != null && this.source.length < end) {
            this.eofPosition = this.source.length;
        } else {
            this.eofPosition = end < Integer.MAX_VALUE ? end + 1 : end;
        }
        this.commentPtr = -1;
        this.foundTaskCount = 0;
    }

    protected final void scanEscapeCharacter() throws InvalidInputException {
        switch(this.currentCharacter) {
            case 'b':
                this.currentCharacter = '\b';
                break;
            case 't':
                this.currentCharacter = '\t';
                break;
            case 'n':
                this.currentCharacter = '\n';
                break;
            case 'f':
                this.currentCharacter = '\f';
                break;
            case 'r':
                this.currentCharacter = '\r';
                break;
            case '\"':
                this.currentCharacter = '\"';
                break;
            case '\'':
                this.currentCharacter = '\'';
                break;
            case '\\':
                this.currentCharacter = '\\';
                break;
            default:
                int number = ScannerHelper.getHexadecimalValue(this.currentCharacter);
                if (number >= 0 && number <= 7) {
                    boolean zeroToThreeNot = number > 3;
                    if (ScannerHelper.isDigit(this.currentCharacter = this.source[this.currentPosition++])) {
                        int digit = ScannerHelper.getHexadecimalValue(this.currentCharacter);
                        if (digit >= 0 && digit <= 7) {
                            number = (number * 8) + digit;
                            if (ScannerHelper.isDigit(this.currentCharacter = this.source[this.currentPosition++])) {
                                if (zeroToThreeNot) {
                                    this.currentPosition--;
                                } else {
                                    digit = ScannerHelper.getHexadecimalValue(this.currentCharacter);
                                    if (digit >= 0 && digit <= 7) {
                                        number = (number * 8) + digit;
                                    } else {
                                        this.currentPosition--;
                                    }
                                }
                            } else {
                                this.currentPosition--;
                            }
                        } else {
                            this.currentPosition--;
                        }
                    } else {
                        this.currentPosition--;
                    }
                    if (number > 255)
                        throw new InvalidInputException(INVALID_ESCAPE);
                    this.currentCharacter = (char) number;
                } else
                    throw new InvalidInputException(INVALID_ESCAPE);
        }
    }

    public int scanIdentifierOrKeywordWithBoundCheck() {
        this.useAssertAsAnIndentifier = false;
        this.useEnumAsAnIndentifier = false;
        char[] src = this.source;
        identLoop: {
            int pos;
            int srcLength = this.eofPosition;
            while (true) {
                if ((pos = this.currentPosition) >= srcLength)
                    break identLoop;
                char c = src[pos];
                if (c < ScannerHelper.MAX_OBVIOUS) {
                    if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[c] & (ScannerHelper.C_UPPER_LETTER | ScannerHelper.C_LOWER_LETTER | ScannerHelper.C_IDENT_PART | ScannerHelper.C_DIGIT)) != 0) {
                        if (this.withoutUnicodePtr != 0) {
                            this.currentCharacter = c;
                            unicodeStore();
                        }
                        this.currentPosition++;
                    } else if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[c] & (ScannerHelper.C_SEPARATOR | ScannerHelper.C_JLS_SPACE)) != 0) {
                        this.currentCharacter = c;
                        break identLoop;
                    } else {
                        while (getNextCharAsJavaIdentifierPartWithBoundCheck()) {
                        }
                        break identLoop;
                    }
                } else {
                    while (getNextCharAsJavaIdentifierPartWithBoundCheck()) {
                    }
                    break identLoop;
                }
            }
        }
        int index, length;
        char[] data;
        if (this.withoutUnicodePtr == 0) {
            if ((length = this.currentPosition - this.startPosition) == 1) {
                return TokenNameIdentifier;
            }
            data = this.source;
            index = this.startPosition;
        } else {
            if ((length = this.withoutUnicodePtr) == 1)
                return TokenNameIdentifier;
            data = this.withoutUnicodeBuffer;
            index = 1;
        }
        return internalScanIdentifierOrKeyword(index, length, data);
    }

    public int scanIdentifierOrKeyword() {
        this.useAssertAsAnIndentifier = false;
        this.useEnumAsAnIndentifier = false;
        char[] src = this.source;
        identLoop: {
            int pos;
            int srcLength = this.eofPosition;
            while (true) {
                if ((pos = this.currentPosition) >= srcLength)
                    break identLoop;
                char c = src[pos];
                if (c < ScannerHelper.MAX_OBVIOUS) {
                    if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[c] & (ScannerHelper.C_UPPER_LETTER | ScannerHelper.C_LOWER_LETTER | ScannerHelper.C_IDENT_PART | ScannerHelper.C_DIGIT)) != 0) {
                        if (this.withoutUnicodePtr != 0) {
                            this.currentCharacter = c;
                            unicodeStore();
                        }
                        this.currentPosition++;
                    } else if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[c] & (ScannerHelper.C_SEPARATOR | ScannerHelper.C_JLS_SPACE)) != 0) {
                        this.currentCharacter = c;
                        break identLoop;
                    } else {
                        while (getNextCharAsJavaIdentifierPart()) {
                        }
                        break identLoop;
                    }
                } else {
                    while (getNextCharAsJavaIdentifierPart()) {
                    }
                    break identLoop;
                }
            }
        }
        int index, length;
        char[] data;
        if (this.withoutUnicodePtr == 0) {
            if ((length = this.currentPosition - this.startPosition) == 1) {
                return TokenNameIdentifier;
            }
            data = this.source;
            index = this.startPosition;
        } else {
            if ((length = this.withoutUnicodePtr) == 1)
                return TokenNameIdentifier;
            data = this.withoutUnicodeBuffer;
            index = 1;
        }
        return internalScanIdentifierOrKeyword(index, length, data);
    }

    private int internalScanIdentifierOrKeyword(int index, int length, char[] data) {
        switch(data[index]) {
            case 'a':
                switch(length) {
                    case 8:
                        if ((data[++index] == 'b') && (data[++index] == 's') && (data[++index] == 't') && (data[++index] == 'r') && (data[++index] == 'a') && (data[++index] == 'c') && (data[++index] == 't')) {
                            return TokenNameabstract;
                        } else {
                            return TokenNameIdentifier;
                        }
                    case 6:
                        if ((data[++index] == 's') && (data[++index] == 's') && (data[++index] == 'e') && (data[++index] == 'r') && (data[++index] == 't')) {
                            if (this.sourceLevel >= ClassFileConstants.JDK1_4) {
                                this.containsAssertKeyword = true;
                                return TokenNameassert;
                            } else {
                                this.useAssertAsAnIndentifier = true;
                                return TokenNameIdentifier;
                            }
                        } else {
                            return TokenNameIdentifier;
                        }
                    default:
                        return TokenNameIdentifier;
                }
            case 'b':
                switch(length) {
                    case 4:
                        if ((data[++index] == 'y') && (data[++index] == 't') && (data[++index] == 'e'))
                            return TokenNamebyte;
                        else
                            return TokenNameIdentifier;
                    case 5:
                        if ((data[++index] == 'r') && (data[++index] == 'e') && (data[++index] == 'a') && (data[++index] == 'k'))
                            return TokenNamebreak;
                        else
                            return TokenNameIdentifier;
                    case 7:
                        if ((data[++index] == 'o') && (data[++index] == 'o') && (data[++index] == 'l') && (data[++index] == 'e') && (data[++index] == 'a') && (data[++index] == 'n'))
                            return TokenNameboolean;
                        else
                            return TokenNameIdentifier;
                    default:
                        return TokenNameIdentifier;
                }
            case 'c':
                switch(length) {
                    case 4:
                        if (data[++index] == 'a')
                            if ((data[++index] == 's') && (data[++index] == 'e'))
                                return TokenNamecase;
                            else
                                return TokenNameIdentifier;
                        else if ((data[index] == 'h') && (data[++index] == 'a') && (data[++index] == 'r'))
                            return TokenNamechar;
                        else
                            return TokenNameIdentifier;
                    case 5:
                        if (data[++index] == 'a')
                            if ((data[++index] == 't') && (data[++index] == 'c') && (data[++index] == 'h'))
                                return TokenNamecatch;
                            else
                                return TokenNameIdentifier;
                        else if (data[index] == 'l')
                            if ((data[++index] == 'a') && (data[++index] == 's') && (data[++index] == 's'))
                                return TokenNameclass;
                            else
                                return TokenNameIdentifier;
                        else if ((data[index] == 'o') && (data[++index] == 'n') && (data[++index] == 's') && (data[++index] == 't'))
                            return TokenNameconst;
                        else
                            return TokenNameIdentifier;
                    case 8:
                        if ((data[++index] == 'o') && (data[++index] == 'n') && (data[++index] == 't') && (data[++index] == 'i') && (data[++index] == 'n') && (data[++index] == 'u') && (data[++index] == 'e'))
                            return TokenNamecontinue;
                        else
                            return TokenNameIdentifier;
                    default:
                        return TokenNameIdentifier;
                }
            case 'd':
                switch(length) {
                    case 2:
                        if ((data[++index] == 'o'))
                            return TokenNamedo;
                        else
                            return TokenNameIdentifier;
                    case 6:
                        if ((data[++index] == 'o') && (data[++index] == 'u') && (data[++index] == 'b') && (data[++index] == 'l') && (data[++index] == 'e'))
                            return TokenNamedouble;
                        else
                            return TokenNameIdentifier;
                    case 7:
                        if ((data[++index] == 'e') && (data[++index] == 'f') && (data[++index] == 'a') && (data[++index] == 'u') && (data[++index] == 'l') && (data[++index] == 't'))
                            return TokenNamedefault;
                        else
                            return TokenNameIdentifier;
                    default:
                        return TokenNameIdentifier;
                }
            case 'e':
                switch(length) {
                    case 4:
                        if (data[++index] == 'l') {
                            if ((data[++index] == 's') && (data[++index] == 'e')) {
                                return TokenNameelse;
                            } else {
                                return TokenNameIdentifier;
                            }
                        } else if ((data[index] == 'n') && (data[++index] == 'u') && (data[++index] == 'm')) {
                            if (this.sourceLevel >= ClassFileConstants.JDK1_5) {
                                return TokenNameenum;
                            } else {
                                this.useEnumAsAnIndentifier = true;
                                return TokenNameIdentifier;
                            }
                        }
                        return TokenNameIdentifier;
                    case 7:
                        if ((data[++index] == 'x') && (data[++index] == 't') && (data[++index] == 'e') && (data[++index] == 'n') && (data[++index] == 'd') && (data[++index] == 's'))
                            return TokenNameextends;
                        else
                            return TokenNameIdentifier;
                    default:
                        return TokenNameIdentifier;
                }
            case 'f':
                switch(length) {
                    case 3:
                        if ((data[++index] == 'o') && (data[++index] == 'r'))
                            return TokenNamefor;
                        else
                            return TokenNameIdentifier;
                    case 5:
                        if (data[++index] == 'i')
                            if ((data[++index] == 'n') && (data[++index] == 'a') && (data[++index] == 'l')) {
                                return TokenNamefinal;
                            } else
                                return TokenNameIdentifier;
                        else if (data[index] == 'l')
                            if ((data[++index] == 'o') && (data[++index] == 'a') && (data[++index] == 't'))
                                return TokenNamefloat;
                            else
                                return TokenNameIdentifier;
                        else if ((data[index] == 'a') && (data[++index] == 'l') && (data[++index] == 's') && (data[++index] == 'e'))
                            return TokenNamefalse;
                        else
                            return TokenNameIdentifier;
                    case 7:
                        if ((data[++index] == 'i') && (data[++index] == 'n') && (data[++index] == 'a') && (data[++index] == 'l') && (data[++index] == 'l') && (data[++index] == 'y'))
                            return TokenNamefinally;
                        else
                            return TokenNameIdentifier;
                    default:
                        return TokenNameIdentifier;
                }
            case 'g':
                if (length == 4) {
                    if ((data[++index] == 'o') && (data[++index] == 't') && (data[++index] == 'o')) {
                        return TokenNamegoto;
                    }
                }
                return TokenNameIdentifier;
            case 'i':
                switch(length) {
                    case 2:
                        if (data[++index] == 'f')
                            return TokenNameif;
                        else
                            return TokenNameIdentifier;
                    case 3:
                        if ((data[++index] == 'n') && (data[++index] == 't'))
                            return TokenNameint;
                        else
                            return TokenNameIdentifier;
                    case 6:
                        if ((data[++index] == 'm') && (data[++index] == 'p') && (data[++index] == 'o') && (data[++index] == 'r') && (data[++index] == 't'))
                            return TokenNameimport;
                        else
                            return TokenNameIdentifier;
                    case 9:
                        if ((data[++index] == 'n') && (data[++index] == 't') && (data[++index] == 'e') && (data[++index] == 'r') && (data[++index] == 'f') && (data[++index] == 'a') && (data[++index] == 'c') && (data[++index] == 'e'))
                            return TokenNameinterface;
                        else
                            return TokenNameIdentifier;
                    case 10:
                        if (data[++index] == 'm')
                            if ((data[++index] == 'p') && (data[++index] == 'l') && (data[++index] == 'e') && (data[++index] == 'm') && (data[++index] == 'e') && (data[++index] == 'n') && (data[++index] == 't') && (data[++index] == 's'))
                                return TokenNameimplements;
                            else
                                return TokenNameIdentifier;
                        else if ((data[index] == 'n') && (data[++index] == 's') && (data[++index] == 't') && (data[++index] == 'a') && (data[++index] == 'n') && (data[++index] == 'c') && (data[++index] == 'e') && (data[++index] == 'o') && (data[++index] == 'f'))
                            return TokenNameinstanceof;
                        else
                            return TokenNameIdentifier;
                    default:
                        return TokenNameIdentifier;
                }
            case 'l':
                if (length == 4) {
                    if ((data[++index] == 'o') && (data[++index] == 'n') && (data[++index] == 'g')) {
                        return TokenNamelong;
                    }
                }
                return TokenNameIdentifier;
            case 'n':
                switch(length) {
                    case 3:
                        if ((data[++index] == 'e') && (data[++index] == 'w'))
                            return TokenNamenew;
                        else
                            return TokenNameIdentifier;
                    case 4:
                        if ((data[++index] == 'u') && (data[++index] == 'l') && (data[++index] == 'l'))
                            return TokenNamenull;
                        else
                            return TokenNameIdentifier;
                    case 6:
                        if ((data[++index] == 'a') && (data[++index] == 't') && (data[++index] == 'i') && (data[++index] == 'v') && (data[++index] == 'e')) {
                            return TokenNamenative;
                        } else
                            return TokenNameIdentifier;
                    default:
                        return TokenNameIdentifier;
                }
            case 'p':
                switch(length) {
                    case 6:
                        if ((data[++index] == 'u') && (data[++index] == 'b') && (data[++index] == 'l') && (data[++index] == 'i') && (data[++index] == 'c')) {
                            return TokenNamepublic;
                        } else
                            return TokenNameIdentifier;
                    case 7:
                        if (data[++index] == 'a')
                            if ((data[++index] == 'c') && (data[++index] == 'k') && (data[++index] == 'a') && (data[++index] == 'g') && (data[++index] == 'e'))
                                return TokenNamepackage;
                            else
                                return TokenNameIdentifier;
                        else if ((data[index] == 'r') && (data[++index] == 'i') && (data[++index] == 'v') && (data[++index] == 'a') && (data[++index] == 't') && (data[++index] == 'e')) {
                            return TokenNameprivate;
                        } else
                            return TokenNameIdentifier;
                    case 9:
                        if ((data[++index] == 'r') && (data[++index] == 'o') && (data[++index] == 't') && (data[++index] == 'e') && (data[++index] == 'c') && (data[++index] == 't') && (data[++index] == 'e') && (data[++index] == 'd')) {
                            return TokenNameprotected;
                        } else
                            return TokenNameIdentifier;
                    default:
                        return TokenNameIdentifier;
                }
            case 'r':
                if (length == 6) {
                    if ((data[++index] == 'e') && (data[++index] == 't') && (data[++index] == 'u') && (data[++index] == 'r') && (data[++index] == 'n')) {
                        return TokenNamereturn;
                    }
                }
                return TokenNameIdentifier;
            case 's':
                switch(length) {
                    case 5:
                        if (data[++index] == 'h')
                            if ((data[++index] == 'o') && (data[++index] == 'r') && (data[++index] == 't'))
                                return TokenNameshort;
                            else
                                return TokenNameIdentifier;
                        else if ((data[index] == 'u') && (data[++index] == 'p') && (data[++index] == 'e') && (data[++index] == 'r'))
                            return TokenNamesuper;
                        else
                            return TokenNameIdentifier;
                    case 6:
                        if (data[++index] == 't')
                            if ((data[++index] == 'a') && (data[++index] == 't') && (data[++index] == 'i') && (data[++index] == 'c')) {
                                return TokenNamestatic;
                            } else
                                return TokenNameIdentifier;
                        else if ((data[index] == 'w') && (data[++index] == 'i') && (data[++index] == 't') && (data[++index] == 'c') && (data[++index] == 'h'))
                            return TokenNameswitch;
                        else
                            return TokenNameIdentifier;
                    case 8:
                        if ((data[++index] == 't') && (data[++index] == 'r') && (data[++index] == 'i') && (data[++index] == 'c') && (data[++index] == 't') && (data[++index] == 'f') && (data[++index] == 'p'))
                            return TokenNamestrictfp;
                        else
                            return TokenNameIdentifier;
                    case 12:
                        if ((data[++index] == 'y') && (data[++index] == 'n') && (data[++index] == 'c') && (data[++index] == 'h') && (data[++index] == 'r') && (data[++index] == 'o') && (data[++index] == 'n') && (data[++index] == 'i') && (data[++index] == 'z') && (data[++index] == 'e') && (data[++index] == 'd')) {
                            return TokenNamesynchronized;
                        } else
                            return TokenNameIdentifier;
                    default:
                        return TokenNameIdentifier;
                }
            case 't':
                switch(length) {
                    case 3:
                        if ((data[++index] == 'r') && (data[++index] == 'y'))
                            return TokenNametry;
                        else
                            return TokenNameIdentifier;
                    case 4:
                        if (data[++index] == 'h')
                            if ((data[++index] == 'i') && (data[++index] == 's'))
                                return TokenNamethis;
                            else
                                return TokenNameIdentifier;
                        else if ((data[index] == 'r') && (data[++index] == 'u') && (data[++index] == 'e'))
                            return TokenNametrue;
                        else
                            return TokenNameIdentifier;
                    case 5:
                        if ((data[++index] == 'h') && (data[++index] == 'r') && (data[++index] == 'o') && (data[++index] == 'w'))
                            return TokenNamethrow;
                        else
                            return TokenNameIdentifier;
                    case 6:
                        if ((data[++index] == 'h') && (data[++index] == 'r') && (data[++index] == 'o') && (data[++index] == 'w') && (data[++index] == 's'))
                            return TokenNamethrows;
                        else
                            return TokenNameIdentifier;
                    case 9:
                        if ((data[++index] == 'r') && (data[++index] == 'a') && (data[++index] == 'n') && (data[++index] == 's') && (data[++index] == 'i') && (data[++index] == 'e') && (data[++index] == 'n') && (data[++index] == 't')) {
                            return TokenNametransient;
                        } else
                            return TokenNameIdentifier;
                    default:
                        return TokenNameIdentifier;
                }
            case 'v':
                switch(length) {
                    case 4:
                        if ((data[++index] == 'o') && (data[++index] == 'i') && (data[++index] == 'd'))
                            return TokenNamevoid;
                        else
                            return TokenNameIdentifier;
                    case 8:
                        if ((data[++index] == 'o') && (data[++index] == 'l') && (data[++index] == 'a') && (data[++index] == 't') && (data[++index] == 'i') && (data[++index] == 'l') && (data[++index] == 'e')) {
                            return TokenNamevolatile;
                        } else
                            return TokenNameIdentifier;
                    default:
                        return TokenNameIdentifier;
                }
            case 'w':
                switch(length) {
                    case 5:
                        if ((data[++index] == 'h') && (data[++index] == 'i') && (data[++index] == 'l') && (data[++index] == 'e'))
                            return TokenNamewhile;
                        else
                            return TokenNameIdentifier;
                    default:
                        return TokenNameIdentifier;
                }
            default:
                return TokenNameIdentifier;
        }
    }

    public int scanNumber(boolean dotPrefix) throws InvalidInputException {
        boolean floating = dotPrefix;
        if (!dotPrefix && (this.currentCharacter == '0')) {
            if (getNextChar('x', 'X') >= 0) {
                int start = this.currentPosition;
                consumeDigits(16, true);
                int end = this.currentPosition;
                if (getNextChar('l', 'L') >= 0) {
                    if (end == start) {
                        throw new InvalidInputException(INVALID_HEXA);
                    }
                    return TokenNameLongLiteral;
                } else if (getNextChar('.')) {
                    boolean hasNoDigitsBeforeDot = end == start;
                    start = this.currentPosition;
                    consumeDigits(16, true);
                    end = this.currentPosition;
                    if (hasNoDigitsBeforeDot && end == start) {
                        if (this.sourceLevel < ClassFileConstants.JDK1_5) {
                            throw new InvalidInputException(ILLEGAL_HEXA_LITERAL);
                        }
                        throw new InvalidInputException(INVALID_HEXA);
                    }
                    if (getNextChar('p', 'P') >= 0) {
                        this.unicodeAsBackSlash = false;
                        if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                            getNextUnicodeChar();
                        } else {
                            if (this.withoutUnicodePtr != 0) {
                                unicodeStore();
                            }
                        }
                        if ((this.currentCharacter == '-') || (this.currentCharacter == '+')) {
                            this.unicodeAsBackSlash = false;
                            if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                getNextUnicodeChar();
                            } else {
                                if (this.withoutUnicodePtr != 0) {
                                    unicodeStore();
                                }
                            }
                        }
                        if (!ScannerHelper.isDigit(this.currentCharacter)) {
                            if (this.sourceLevel < ClassFileConstants.JDK1_5) {
                                throw new InvalidInputException(ILLEGAL_HEXA_LITERAL);
                            }
                            if (this.currentCharacter == '_') {
                                consumeDigits(10);
                                throw new InvalidInputException(INVALID_UNDERSCORE);
                            }
                            throw new InvalidInputException(INVALID_HEXA);
                        }
                        consumeDigits(10);
                        if (getNextChar('f', 'F') >= 0) {
                            if (this.sourceLevel < ClassFileConstants.JDK1_5) {
                                throw new InvalidInputException(ILLEGAL_HEXA_LITERAL);
                            }
                            return TokenNameFloatingPointLiteral;
                        }
                        if (getNextChar('d', 'D') >= 0) {
                            if (this.sourceLevel < ClassFileConstants.JDK1_5) {
                                throw new InvalidInputException(ILLEGAL_HEXA_LITERAL);
                            }
                            return TokenNameDoubleLiteral;
                        }
                        if (getNextChar('l', 'L') >= 0) {
                            if (this.sourceLevel < ClassFileConstants.JDK1_5) {
                                throw new InvalidInputException(ILLEGAL_HEXA_LITERAL);
                            }
                            throw new InvalidInputException(INVALID_HEXA);
                        }
                        if (this.sourceLevel < ClassFileConstants.JDK1_5) {
                            throw new InvalidInputException(ILLEGAL_HEXA_LITERAL);
                        }
                        return TokenNameDoubleLiteral;
                    } else {
                        if (this.sourceLevel < ClassFileConstants.JDK1_5) {
                            throw new InvalidInputException(ILLEGAL_HEXA_LITERAL);
                        }
                        throw new InvalidInputException(INVALID_HEXA);
                    }
                } else if (getNextChar('p', 'P') >= 0) {
                    this.unicodeAsBackSlash = false;
                    if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                        getNextUnicodeChar();
                    } else {
                        if (this.withoutUnicodePtr != 0) {
                            unicodeStore();
                        }
                    }
                    if ((this.currentCharacter == '-') || (this.currentCharacter == '+')) {
                        this.unicodeAsBackSlash = false;
                        if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                            getNextUnicodeChar();
                        } else {
                            if (this.withoutUnicodePtr != 0) {
                                unicodeStore();
                            }
                        }
                    }
                    if (!ScannerHelper.isDigit(this.currentCharacter)) {
                        if (this.sourceLevel < ClassFileConstants.JDK1_5) {
                            throw new InvalidInputException(ILLEGAL_HEXA_LITERAL);
                        }
                        if (this.currentCharacter == '_') {
                            consumeDigits(10);
                            throw new InvalidInputException(INVALID_UNDERSCORE);
                        }
                        throw new InvalidInputException(INVALID_FLOAT);
                    }
                    consumeDigits(10);
                    if (getNextChar('f', 'F') >= 0) {
                        if (this.sourceLevel < ClassFileConstants.JDK1_5) {
                            throw new InvalidInputException(ILLEGAL_HEXA_LITERAL);
                        }
                        return TokenNameFloatingPointLiteral;
                    }
                    if (getNextChar('d', 'D') >= 0) {
                        if (this.sourceLevel < ClassFileConstants.JDK1_5) {
                            throw new InvalidInputException(ILLEGAL_HEXA_LITERAL);
                        }
                        return TokenNameDoubleLiteral;
                    }
                    if (getNextChar('l', 'L') >= 0) {
                        if (this.sourceLevel < ClassFileConstants.JDK1_5) {
                            throw new InvalidInputException(ILLEGAL_HEXA_LITERAL);
                        }
                        throw new InvalidInputException(INVALID_HEXA);
                    }
                    if (this.sourceLevel < ClassFileConstants.JDK1_5) {
                        throw new InvalidInputException(ILLEGAL_HEXA_LITERAL);
                    }
                    return TokenNameDoubleLiteral;
                } else {
                    if (end == start)
                        throw new InvalidInputException(INVALID_HEXA);
                    return TokenNameIntegerLiteral;
                }
            } else if (getNextChar('b', 'B') >= 0) {
                int start = this.currentPosition;
                consumeDigits(2, true);
                int end = this.currentPosition;
                if (end == start) {
                    if (this.sourceLevel < ClassFileConstants.JDK1_7) {
                        throw new InvalidInputException(BINARY_LITERAL_NOT_BELOW_17);
                    }
                    throw new InvalidInputException(INVALID_BINARY);
                }
                if (getNextChar('l', 'L') >= 0) {
                    if (this.sourceLevel < ClassFileConstants.JDK1_7) {
                        throw new InvalidInputException(BINARY_LITERAL_NOT_BELOW_17);
                    }
                    return TokenNameLongLiteral;
                }
                if (this.sourceLevel < ClassFileConstants.JDK1_7) {
                    throw new InvalidInputException(BINARY_LITERAL_NOT_BELOW_17);
                }
                return TokenNameIntegerLiteral;
            }
            if (getNextCharAsDigit()) {
                consumeDigits(10);
                if (getNextChar('l', 'L') >= 0) {
                    return TokenNameLongLiteral;
                }
                if (getNextChar('f', 'F') >= 0) {
                    return TokenNameFloatingPointLiteral;
                }
                if (getNextChar('d', 'D') >= 0) {
                    return TokenNameDoubleLiteral;
                } else {
                    boolean isInteger = true;
                    if (getNextChar('.')) {
                        isInteger = false;
                        consumeDigits(10);
                    }
                    if (getNextChar('e', 'E') >= 0) {
                        isInteger = false;
                        this.unicodeAsBackSlash = false;
                        if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                            getNextUnicodeChar();
                        } else {
                            if (this.withoutUnicodePtr != 0) {
                                unicodeStore();
                            }
                        }
                        if ((this.currentCharacter == '-') || (this.currentCharacter == '+')) {
                            this.unicodeAsBackSlash = false;
                            if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                                getNextUnicodeChar();
                            } else {
                                if (this.withoutUnicodePtr != 0) {
                                    unicodeStore();
                                }
                            }
                        }
                        if (!ScannerHelper.isDigit(this.currentCharacter)) {
                            if (this.currentCharacter == '_') {
                                consumeDigits(10);
                                throw new InvalidInputException(INVALID_UNDERSCORE);
                            }
                            throw new InvalidInputException(INVALID_FLOAT);
                        }
                        consumeDigits(10);
                    }
                    if (getNextChar('f', 'F') >= 0)
                        return TokenNameFloatingPointLiteral;
                    if (getNextChar('d', 'D') >= 0 || !isInteger)
                        return TokenNameDoubleLiteral;
                    return TokenNameIntegerLiteral;
                }
            } else {
            }
        }
        consumeDigits(10);
        if ((!dotPrefix) && (getNextChar('l', 'L') >= 0))
            return TokenNameLongLiteral;
        if ((!dotPrefix) && (getNextChar('.'))) {
            consumeDigits(10, true);
            floating = true;
        }
        if (getNextChar('e', 'E') >= 0) {
            floating = true;
            this.unicodeAsBackSlash = false;
            if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                getNextUnicodeChar();
            } else {
                if (this.withoutUnicodePtr != 0) {
                    unicodeStore();
                }
            }
            if ((this.currentCharacter == '-') || (this.currentCharacter == '+')) {
                this.unicodeAsBackSlash = false;
                if (((this.currentCharacter = this.source[this.currentPosition++]) == '\\') && (this.source[this.currentPosition] == 'u')) {
                    getNextUnicodeChar();
                } else {
                    if (this.withoutUnicodePtr != 0) {
                        unicodeStore();
                    }
                }
            }
            if (!ScannerHelper.isDigit(this.currentCharacter)) {
                if (this.currentCharacter == '_') {
                    consumeDigits(10);
                    throw new InvalidInputException(INVALID_UNDERSCORE);
                }
                throw new InvalidInputException(INVALID_FLOAT);
            }
            consumeDigits(10);
        }
        if (getNextChar('d', 'D') >= 0)
            return TokenNameDoubleLiteral;
        if (getNextChar('f', 'F') >= 0)
            return TokenNameFloatingPointLiteral;
        return floating ? TokenNameDoubleLiteral : TokenNameIntegerLiteral;
    }

    public final int getLineNumber(int position) {
        return Util.getLineNumber(position, this.lineEnds, 0, this.linePtr);
    }

    public final void setSource(char[] sourceString) {
        int sourceLength;
        if (sourceString == null) {
            this.source = CharOperation.NO_CHAR;
            sourceLength = 0;
        } else {
            this.source = sourceString;
            sourceLength = sourceString.length;
        }
        this.startPosition = -1;
        this.eofPosition = sourceLength;
        this.initialPosition = this.currentPosition = 0;
        this.containsAssertKeyword = false;
        this.linePtr = -1;
    }

    public final void setSource(char[] contents, CompilationResult compilationResult) {
        if (contents == null) {
            char[] cuContents = compilationResult.compilationUnit.getContents();
            setSource(cuContents);
        } else {
            setSource(contents);
        }
        int[] lineSeparatorPositions = compilationResult.lineSeparatorPositions;
        if (lineSeparatorPositions != null) {
            this.lineEnds = lineSeparatorPositions;
            this.linePtr = lineSeparatorPositions.length - 1;
        }
    }

    public final void setSource(CompilationResult compilationResult) {
        setSource(null, compilationResult);
    }

    public String toString() {
        if (this.startPosition == this.eofPosition)
            return "EOF\n\n" + new String(this.source);
        if (this.currentPosition > this.eofPosition)
            return "behind the EOF\n\n" + new String(this.source);
        if (this.currentPosition <= 0)
            return "NOT started!\n\n" + new String(this.source);
        StringBuffer buffer = new StringBuffer();
        if (this.startPosition < 1000) {
            buffer.append(this.source, 0, this.startPosition);
        } else {
            buffer.append("<source beginning>\n...\n");
            int line = Util.getLineNumber(this.startPosition - 1000, this.lineEnds, 0, this.linePtr);
            int lineStart = getLineStart(line);
            buffer.append(this.source, lineStart, this.startPosition - lineStart);
        }
        buffer.append("\n===============================\nStarts here -->");
        int middleLength = (this.currentPosition - 1) - this.startPosition + 1;
        if (middleLength > -1) {
            buffer.append(this.source, this.startPosition, middleLength);
        }
        buffer.append("<-- Ends here\n===============================\n");
        buffer.append(this.source, (this.currentPosition - 1) + 1, this.eofPosition - (this.currentPosition - 1) - 1);
        return buffer.toString();
    }

    public String toStringAction(int act) {
        switch(act) {
            case TokenNameIdentifier:
                return "Identifier(" + new String(getCurrentTokenSource()) + ")";
            case TokenNameabstract:
                return "abstract";
            case TokenNameboolean:
                return "boolean";
            case TokenNamebreak:
                return "break";
            case TokenNamebyte:
                return "byte";
            case TokenNamecase:
                return "case";
            case TokenNamecatch:
                return "catch";
            case TokenNamechar:
                return "char";
            case TokenNameclass:
                return "class";
            case TokenNamecontinue:
                return "continue";
            case TokenNamedefault:
                return "default";
            case TokenNamedo:
                return "do";
            case TokenNamedouble:
                return "double";
            case TokenNameelse:
                return "else";
            case TokenNameextends:
                return "extends";
            case TokenNamefalse:
                return "false";
            case TokenNamefinal:
                return "final";
            case TokenNamefinally:
                return "finally";
            case TokenNamefloat:
                return "float";
            case TokenNamefor:
                return "for";
            case TokenNameif:
                return "if";
            case TokenNameimplements:
                return "implements";
            case TokenNameimport:
                return "import";
            case TokenNameinstanceof:
                return "instanceof";
            case TokenNameint:
                return "int";
            case TokenNameinterface:
                return "interface";
            case TokenNamelong:
                return "long";
            case TokenNamenative:
                return "native";
            case TokenNamenew:
                return "new";
            case TokenNamenull:
                return "null";
            case TokenNamepackage:
                return "package";
            case TokenNameprivate:
                return "private";
            case TokenNameprotected:
                return "protected";
            case TokenNamepublic:
                return "public";
            case TokenNamereturn:
                return "return";
            case TokenNameshort:
                return "short";
            case TokenNamestatic:
                return "static";
            case TokenNamesuper:
                return "super";
            case TokenNameswitch:
                return "switch";
            case TokenNamesynchronized:
                return "synchronized";
            case TokenNamethis:
                return "this";
            case TokenNamethrow:
                return "throw";
            case TokenNamethrows:
                return "throws";
            case TokenNametransient:
                return "transient";
            case TokenNametrue:
                return "true";
            case TokenNametry:
                return "try";
            case TokenNamevoid:
                return "void";
            case TokenNamevolatile:
                return "volatile";
            case TokenNamewhile:
                return "while";
            case TokenNameIntegerLiteral:
                return "Integer(" + new String(getCurrentTokenSource()) + ")";
            case TokenNameLongLiteral:
                return "Long(" + new String(getCurrentTokenSource()) + ")";
            case TokenNameFloatingPointLiteral:
                return "Float(" + new String(getCurrentTokenSource()) + ")";
            case TokenNameDoubleLiteral:
                return "Double(" + new String(getCurrentTokenSource()) + ")";
            case TokenNameCharacterLiteral:
                return "Char(" + new String(getCurrentTokenSource()) + ")";
            case TokenNameStringLiteral:
                return "String(" + new String(getCurrentTokenSource()) + ")";
            case TokenNamePLUS_PLUS:
                return "++";
            case TokenNameMINUS_MINUS:
                return "--";
            case TokenNameEQUAL_EQUAL:
                return "==";
            case TokenNameLESS_EQUAL:
                return "<=";
            case TokenNameGREATER_EQUAL:
                return ">=";
            case TokenNameNOT_EQUAL:
                return "!=";
            case TokenNameLEFT_SHIFT:
                return "<<";
            case TokenNameRIGHT_SHIFT:
                return ">>";
            case TokenNameUNSIGNED_RIGHT_SHIFT:
                return ">>>";
            case TokenNamePLUS_EQUAL:
                return "+=";
            case TokenNameMINUS_EQUAL:
                return "-=";
            case TokenNameARROW:
                return "->";
            case TokenNameMULTIPLY_EQUAL:
                return "*=";
            case TokenNameDIVIDE_EQUAL:
                return "/=";
            case TokenNameAND_EQUAL:
                return "&=";
            case TokenNameOR_EQUAL:
                return "|=";
            case TokenNameXOR_EQUAL:
                return "^=";
            case TokenNameREMAINDER_EQUAL:
                return "%=";
            case TokenNameLEFT_SHIFT_EQUAL:
                return "<<=";
            case TokenNameRIGHT_SHIFT_EQUAL:
                return ">>=";
            case TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL:
                return ">>>=";
            case TokenNameOR_OR:
                return "||";
            case TokenNameAND_AND:
                return "&&";
            case TokenNamePLUS:
                return "+";
            case TokenNameMINUS:
                return "-";
            case TokenNameNOT:
                return "!";
            case TokenNameREMAINDER:
                return "%";
            case TokenNameXOR:
                return "^";
            case TokenNameAND:
                return "&";
            case TokenNameMULTIPLY:
                return "*";
            case TokenNameOR:
                return "|";
            case TokenNameTWIDDLE:
                return "~";
            case TokenNameDIVIDE:
                return "/";
            case TokenNameGREATER:
                return ">";
            case TokenNameLESS:
                return "<";
            case TokenNameLPAREN:
                return "(";
            case TokenNameRPAREN:
                return ")";
            case TokenNameLBRACE:
                return "{";
            case TokenNameRBRACE:
                return "}";
            case TokenNameLBRACKET:
                return "[";
            case TokenNameRBRACKET:
                return "]";
            case TokenNameSEMICOLON:
                return ";";
            case TokenNameQUESTION:
                return "?";
            case TokenNameCOLON:
                return ":";
            case TokenNameCOLON_COLON:
                return "::";
            case TokenNameCOMMA:
                return ",";
            case TokenNameDOT:
                return ".";
            case TokenNameEQUAL:
                return "=";
            case TokenNameEOF:
                return "EOF";
            case TokenNameWHITESPACE:
                return "white_space(" + new String(getCurrentTokenSource()) + ")";
            default:
                return "not-a-token";
        }
    }

    public void unicodeInitializeBuffer(int length) {
        this.withoutUnicodePtr = length;
        if (this.withoutUnicodeBuffer == null)
            this.withoutUnicodeBuffer = new char[length + (1 + 10)];
        int bLength = this.withoutUnicodeBuffer.length;
        if (1 + length >= bLength) {
            System.arraycopy(this.withoutUnicodeBuffer, 0, this.withoutUnicodeBuffer = new char[length + (1 + 10)], 0, bLength);
        }
        System.arraycopy(this.source, this.startPosition, this.withoutUnicodeBuffer, 1, length);
    }

    public void unicodeStore() {
        int pos = ++this.withoutUnicodePtr;
        if (this.withoutUnicodeBuffer == null)
            this.withoutUnicodeBuffer = new char[10];
        int length = this.withoutUnicodeBuffer.length;
        if (pos == length) {
            System.arraycopy(this.withoutUnicodeBuffer, 0, this.withoutUnicodeBuffer = new char[length * 2], 0, length);
        }
        this.withoutUnicodeBuffer[pos] = this.currentCharacter;
    }

    public void unicodeStore(char character) {
        int pos = ++this.withoutUnicodePtr;
        if (this.withoutUnicodeBuffer == null)
            this.withoutUnicodeBuffer = new char[10];
        int length = this.withoutUnicodeBuffer.length;
        if (pos == length) {
            System.arraycopy(this.withoutUnicodeBuffer, 0, this.withoutUnicodeBuffer = new char[length * 2], 0, length);
        }
        this.withoutUnicodeBuffer[pos] = character;
    }

    public static boolean isIdentifier(int token) {
        return token == TerminalTokens.TokenNameIdentifier;
    }

    public static boolean isLiteral(int token) {
        switch(token) {
            case TerminalTokens.TokenNameIntegerLiteral:
            case TerminalTokens.TokenNameLongLiteral:
            case TerminalTokens.TokenNameFloatingPointLiteral:
            case TerminalTokens.TokenNameDoubleLiteral:
            case TerminalTokens.TokenNameStringLiteral:
            case TerminalTokens.TokenNameCharacterLiteral:
                return true;
            default:
                return false;
        }
    }

    public static boolean isKeyword(int token) {
        switch(token) {
            case TerminalTokens.TokenNameabstract:
            case TerminalTokens.TokenNameassert:
            case TerminalTokens.TokenNamebyte:
            case TerminalTokens.TokenNamebreak:
            case TerminalTokens.TokenNameboolean:
            case TerminalTokens.TokenNamecase:
            case TerminalTokens.TokenNamechar:
            case TerminalTokens.TokenNamecatch:
            case TerminalTokens.TokenNameclass:
            case TerminalTokens.TokenNamecontinue:
            case TerminalTokens.TokenNamedo:
            case TerminalTokens.TokenNamedouble:
            case TerminalTokens.TokenNamedefault:
            case TerminalTokens.TokenNameelse:
            case TerminalTokens.TokenNameextends:
            case TerminalTokens.TokenNamefor:
            case TerminalTokens.TokenNamefinal:
            case TerminalTokens.TokenNamefloat:
            case TerminalTokens.TokenNamefalse:
            case TerminalTokens.TokenNamefinally:
            case TerminalTokens.TokenNameif:
            case TerminalTokens.TokenNameint:
            case TerminalTokens.TokenNameimport:
            case TerminalTokens.TokenNameinterface:
            case TerminalTokens.TokenNameimplements:
            case TerminalTokens.TokenNameinstanceof:
            case TerminalTokens.TokenNamelong:
            case TerminalTokens.TokenNamenew:
            case TerminalTokens.TokenNamenull:
            case TerminalTokens.TokenNamenative:
            case TerminalTokens.TokenNamepublic:
            case TerminalTokens.TokenNamepackage:
            case TerminalTokens.TokenNameprivate:
            case TerminalTokens.TokenNameprotected:
            case TerminalTokens.TokenNamereturn:
            case TerminalTokens.TokenNameshort:
            case TerminalTokens.TokenNamesuper:
            case TerminalTokens.TokenNamestatic:
            case TerminalTokens.TokenNameswitch:
            case TerminalTokens.TokenNamestrictfp:
            case TerminalTokens.TokenNamesynchronized:
            case TerminalTokens.TokenNametry:
            case TerminalTokens.TokenNamethis:
            case TerminalTokens.TokenNametrue:
            case TerminalTokens.TokenNamethrow:
            case TerminalTokens.TokenNamethrows:
            case TerminalTokens.TokenNametransient:
            case TerminalTokens.TokenNamevoid:
            case TerminalTokens.TokenNamevolatile:
            case TerminalTokens.TokenNamewhile:
                return true;
            default:
                return false;
        }
    }
}
