/*******************************************************************************
 * Copyright (c) 2005, 2015 BEA Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    mkaufman@bea.com - initial API and implementation
 *    
 *******************************************************************************/
package org.eclipse.jdt.apt.core.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.apt.core.internal.AptPlugin;
import org.eclipse.jdt.apt.core.internal.env.BuildEnv;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

public class ScannerUtil {

    /**
	 * scan the source code to see if there are any annotation tokens
	 */
    public static boolean hasAnnotationInstance(IFile f) {
        InputStreamReader reader = null;
        InputStream input = null;
        try {
            AnnotationScanner scanner;
            // with the bytes, which is *much* faster
            if (SINGLE_BYTE_ENCODINGS.contains(f.getCharset())) {
                input = BuildEnv.getInputStream(f);
                scanner = new InputStreamAnnotationScanner(input);
            } else {
                reader = BuildEnv.getFileReader(f);
                scanner = new ReaderAnnotationScanner(reader);
            }
            return scanner.containsAnnotations();
        } catch (Exception ioe) {
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    public static boolean hasAnnotationInstance(ICompilationUnit cu) {
        try {
            IBuffer b = cu.getBuffer();
            if (b == null)
                return false;
            char[] source = b.getCharacters();
            return hasAnnotationInstance(source);
        } catch (JavaModelException jme) {
            return false;
        }
    }

    public static boolean hasAnnotationInstance(char[] source) {
        try {
            if (source == null)
                return false;
            IScanner scanner = ToolFactory.createScanner(false, false, false, JavaCore.VERSION_1_5);
            scanner.setSource(source);
            int token = scanner.getNextToken();
            while (token != ITerminalSymbols.TokenNameEOF) {
                token = scanner.getNextToken();
                if (token == ITerminalSymbols.TokenNameAT) {
                    //
                    // found an @ sign, see if next token is "interface"
                    // @interface is an annotation decl and not an annotation
                    // instance.  
                    //
                    token = scanner.getNextToken();
                    if (token != ITerminalSymbols.TokenNameinterface)
                        return true;
                }
            }
            return false;
        } catch (InvalidInputException iie) {
            return false;
        } catch (Exception e) {
            AptPlugin.log(e, "Failure scanning source: \n" + new String(source));
            return false;
        }
    }

    private static final String[] SINGLE_BYTE_ENCODING_ARRAY = { //$NON-NLS-1$
    "ASCII", //$NON-NLS-1$
    "Cp1250", //$NON-NLS-1$
    "Cp1251", //$NON-NLS-1$
    "Cp1252", //$NON-NLS-1$
    "Cp1253", //$NON-NLS-1$
    "Cp1254", //$NON-NLS-1$
    "Cp1257", //$NON-NLS-1$
    "ISO8859_1", //$NON-NLS-1$
    "ISO8859_2", //$NON-NLS-1$
    "ISO8859_4", //$NON-NLS-1$
    "ISO8859_5", //$NON-NLS-1$
    "ISO8859_7", //$NON-NLS-1$
    "ISO8859_9", //$NON-NLS-1$
    "ISO8859_13", //$NON-NLS-1$
    "ISO8859_15", //$NON-NLS-1$
    "UTF8" };

    private static final Set<String> SINGLE_BYTE_ENCODINGS = new HashSet(SINGLE_BYTE_ENCODING_ARRAY.length);

    static {
        for (String encoding : SINGLE_BYTE_ENCODING_ARRAY) {
            SINGLE_BYTE_ENCODINGS.add(encoding);
        }
    }
}
