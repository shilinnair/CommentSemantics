/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Andreas Schmid, service@aaschmid.de - Locate test method even if it contains parameters - http://bugs.eclipse.org/343935
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.util.JavaConventionsUtil;
import org.eclipse.jdt.internal.junit.BasicElementLabels;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;

/**
 * Open a class on a Test method.
 */
public class OpenTestAction extends OpenEditorAction {

    private String fMethodName;

    private IMethod fMethod;

    private int fLineNumber = -1;

    public  OpenTestAction(TestRunnerViewPart testRunnerPart, TestCaseElement testCase) {
        this(testRunnerPart, testCase.getClassName(), extractRealMethodName(testCase), true);
        String trace = testCase.getTrace();
        if (trace != null) {
            String rawClassName = TestElement.extractRawClassName(testCase.getTestName());
            //$NON-NLS-1$//$NON-NLS-2$
            rawClassName = rawClassName.replaceAll("\\.", "\\\\.");
            //$NON-NLS-1$//$NON-NLS-2$
            rawClassName = rawClassName.replaceAll("\\$", "\\\\\\$");
            Pattern pattern = Pattern.compile(FailureTrace.FRAME_PREFIX + rawClassName + '.' + fMethodName + //$NON-NLS-1$
            "\\(.*:(\\d+)\\)");
            Matcher matcher = pattern.matcher(trace);
            if (matcher.find()) {
                try {
                    fLineNumber = Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                }
            }
        }
    }

    public  OpenTestAction(TestRunnerViewPart testRunner, String className) {
        this(testRunner, className, null, true);
    }

    private  OpenTestAction(TestRunnerViewPart testRunner, String className, String method, boolean activate) {
        super(testRunner, className, activate);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJUnitHelpContextIds.OPENTEST_ACTION);
        fMethodName = method;
    }

    private static String extractRealMethodName(TestCaseElement testCase) {
        //workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=334864 :
        if (testCase.isIgnored() && JavaConventions.validateJavaTypeName(testCase.getTestName(), JavaCore.VERSION_1_5, JavaCore.VERSION_1_5).getSeverity() != IStatus.ERROR) {
            return null;
        }
        //workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=275308 :
        String testMethodName = testCase.getTestMethodName();
        for (int i = 0; i < testMethodName.length(); i++) {
            if (!Character.isJavaIdentifierPart(testMethodName.charAt(i))) {
                return testMethodName.substring(0, i);
            }
        }
        return testMethodName;
    }

    @Override
    protected IJavaElement findElement(IJavaProject project, String className) throws JavaModelException {
        IType type = findType(project, className);
        if (type == null)
            return null;
        if (fMethodName == null)
            return type;
        IMethod method = findMethod(type);
        if (method == null) {
            ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(null);
            IType[] supertypes = typeHierarchy.getAllSuperclasses(type);
            for (IType supertype : supertypes) {
                method = findMethod(supertype);
                if (method != null)
                    break;
            }
        }
        if (method == null) {
            String title = JUnitMessages.OpenTestAction_dialog_title;
            String message = Messages.format(JUnitMessages.OpenTestAction_error_methodNoFound, BasicElementLabels.getJavaElementName(fMethodName));
            MessageDialog.openInformation(getShell(), title, message);
            return type;
        }
        fMethod = method;
        return method;
    }

    private IMethod findMethod(IType type) {
        IStatus status = JavaConventionsUtil.validateMethodName(fMethodName, type);
        if (!status.isOK())
            return null;
        IMethod method = type.getMethod(fMethodName, new String[0]);
        if (method != null && method.exists())
            return method;
        // search just by name, if method not found yet (for custom runner with test methods having parameters)
        try {
            List<IMethod> foundMethods = new ArrayList();
            for (IMethod method2 : type.getMethods()) {
                String methodName = method2.getElementName();
                IAnnotation methodAnnotation = //$NON-NLS-1$
                method2.getAnnotation(//$NON-NLS-1$
                "Test");
                // JUnit3 test method starts with "test" or JUnit4 test method is annotated with "@Test"
                if (!(//$NON-NLS-1$
                methodName.startsWith("test") || (methodAnnotation != null && methodAnnotation.exists())))
                    continue;
                if (fMethodName.equals(methodName))
                    foundMethods.add(method2);
            }
            if (foundMethods.isEmpty())
                return null;
            else if (foundMethods.size() > 1) {
                IMethod[] elements = foundMethods.toArray(new IMethod[foundMethods.size()]);
                String title = JUnitMessages.OpenTestAction_dialog_title;
                String message = JUnitMessages.OpenTestAction_select_element;
                return (IMethod) SelectionConverter.selectJavaElement(elements, getShell(), title, message);
            } else
                return foundMethods.get(0);
        } catch (JavaModelException e) {
        }
        return null;
    }

    @Override
    protected void reveal(ITextEditor textEditor) {
        if (fLineNumber >= 0) {
            try {
                IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
                int lineOffset = document.getLineOffset(fLineNumber - 1);
                int lineLength = document.getLineLength(fLineNumber - 1);
                if (fMethod != null) {
                    try {
                        ISourceRange sr = fMethod.getSourceRange();
                        if (sr == null || sr.getOffset() == -1 || lineOffset < sr.getOffset() || sr.getOffset() + sr.getLength() < lineOffset + lineLength) {
                            throw new BadLocationException();
                        }
                    } catch (JavaModelException e) {
                    }
                }
                textEditor.selectAndReveal(lineOffset, lineLength);
                return;
            } catch (BadLocationException x) {
            }
        }
        if (fMethod != null) {
            try {
                ISourceRange range = fMethod.getNameRange();
                if (range != null && range.getOffset() >= 0)
                    textEditor.selectAndReveal(range.getOffset(), range.getLength());
            } catch (JavaModelException e) {
            }
        }
    }
}
