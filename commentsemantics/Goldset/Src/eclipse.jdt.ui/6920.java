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
package org.eclipse.jdt.internal.ui.text;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.jdt.ui.text.IJavaPartitions;

/**
 * This scanner recognizes the JavaDoc comments and Java multi line comments.
 */
public class JavaPartitionScanner extends RuleBasedPartitionScanner implements IJavaPartitions {

    /**
	 * Detector for empty comments.
	 */
    static class EmptyCommentDetector implements IWordDetector {

        /*
		 * @see IWordDetector#isWordStart
		 */
        @Override
        public boolean isWordStart(char c) {
            return (c == '/');
        }

        /*
		 * @see IWordDetector#isWordPart
		 */
        @Override
        public boolean isWordPart(char c) {
            return (c == '*' || c == '/');
        }
    }

    /**
	 * Word rule for empty comments.
	 */
    static class EmptyCommentRule extends WordRule implements IPredicateRule {

        private IToken fSuccessToken;

        /**
		 * Constructor for EmptyCommentRule.
		 * @param successToken
		 */
        public  EmptyCommentRule(IToken successToken) {
            super(new EmptyCommentDetector());
            fSuccessToken = successToken;
            //$NON-NLS-1$
            addWord("/**/", fSuccessToken);
        }

        /*
		 * @see IPredicateRule#evaluate(ICharacterScanner, boolean)
		 */
        @Override
        public IToken evaluate(ICharacterScanner scanner, boolean resume) {
            return evaluate(scanner);
        }

        /*
		 * @see IPredicateRule#getSuccessToken()
		 */
        @Override
        public IToken getSuccessToken() {
            return fSuccessToken;
        }
    }

    /**
	 * Creates the partitioner and sets up the appropriate rules.
	 */
    public  JavaPartitionScanner() {
        super();
        IToken string = new Token(JAVA_STRING);
        IToken character = new Token(JAVA_CHARACTER);
        IToken javaDoc = new Token(JAVA_DOC);
        IToken multiLineComment = new Token(JAVA_MULTI_LINE_COMMENT);
        IToken singleLineComment = new Token(JAVA_SINGLE_LINE_COMMENT);
        List<IPredicateRule> rules = new ArrayList();
        // Add rule for single line comments.
        //$NON-NLS-1$
        rules.add(new EndOfLineRule("//", singleLineComment));
        // Add rule for strings.
        rules.add(new SingleLineRule("\"", "\"", string, '\\'));
        rules.add(new SingleLineRule("'", "'", character, '\\'));
        EmptyCommentRule wordRule = new EmptyCommentRule(multiLineComment);
        rules.add(wordRule);
        rules.add(new MultiLineRule("/**", "*/", javaDoc));
        rules.add(new MultiLineRule("/*", "*/", multiLineComment));
        IPredicateRule[] result = new IPredicateRule[rules.size()];
        rules.toArray(result);
        setPredicateRules(result);
    }
}
