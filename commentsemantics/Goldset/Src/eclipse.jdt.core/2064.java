/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Terry Parker <tparker@google.com> - Bug 418092
 *******************************************************************************/
package org.eclipse.jdt.core.tests.model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import junit.framework.Test;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.core.BinaryType;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaModelManager.PerProjectInfo;
import org.eclipse.jdt.internal.core.util.Util;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class AttachedJavadocTests extends ModifyingResourceTests {

    private static final String DEFAULT_DOC_FOLDER = "doc";

    static {
    //		TESTS_NAMES = new String[] { "testBug354766" };
    //		TESTS_NUMBERS = new int[] { 24 };
    //		TESTS_RANGE = new int[] { 169, 180 };
    }

    public static Test suite() {
        return buildModelTestSuite(AttachedJavadocTests.class, BYTECODE_DECLARATION_ORDER);
    }

    private IJavaProject project;

    private IPackageFragmentRoot root;

    public  AttachedJavadocTests(String name) {
        super(name);
    }

    private void setJavadocLocationAttribute(String folderName) throws JavaModelException {
        IClasspathEntry[] entries = this.project.getRawClasspath();
        //$NON-NLS-1$
        IResource resource = this.project.getProject().findMember("/" + folderName + "/");
        //$NON-NLS-1$
        assertNotNull("doc folder cannot be null", resource);
        URI locationURI = resource.getLocationURI();
        //$NON-NLS-1$
        assertNotNull("doc folder cannot be null", locationURI);
        URL docUrl = null;
        try {
            docUrl = locationURI.toURL();
        } catch (MalformedURLException e) {
            assertTrue("Should not happen", false);
        } catch (IllegalArgumentException e) {
            assertTrue("Should not happen", false);
        }
        IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, docUrl.toExternalForm());
        for (int i = 0, max = entries.length; i < max; i++) {
            final IClasspathEntry entry = entries[i];
            if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY && entry.getContentKind() == IPackageFragmentRoot.K_BINARY && "/AttachedJavadocProject/lib/test6.jar".equals(//$NON-NLS-1$
            entry.getPath().toString())) {
                entries[i] = JavaCore.newLibraryEntry(entry.getPath(), entry.getSourceAttachmentPath(), entry.getSourceAttachmentRootPath(), entry.getAccessRules(), new IClasspathAttribute[] { attribute }, entry.isExported());
            }
        }
        this.project.setRawClasspath(entries, null);
    }

    /**
	 * Create project and set the jar placeholder.
	 */
    public void setUpSuite() throws Exception {
        super.setUpSuite();
        //$NON-NLS-1$
        this.project = setUpJavaProject("AttachedJavadocProject", "1.5");
        Map options = this.project.getOptions(true);
        //$NON-NLS-1$
        options.put(JavaCore.TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC, "2000");
        this.project.setOptions(options);
        setJavadocLocationAttribute(DEFAULT_DOC_FOLDER);
        IPackageFragmentRoot[] roots = this.project.getAllPackageFragmentRoots();
        int count = 0;
        for (int i = 0, max = roots.length; i < max; i++) {
            final IPackageFragmentRoot packageFragmentRoot = roots[i];
            switch(packageFragmentRoot.getKind()) {
                case IPackageFragmentRoot.K_BINARY:
                    if (!packageFragmentRoot.isExternal()) {
                        count++;
                        if (this.root == null) {
                            this.root = packageFragmentRoot;
                        }
                    }
            }
        }
        //$NON-NLS-1$
        assertEquals("Wrong value", 1, count);
        //$NON-NLS-1$
        assertNotNull("Should not be null", this.root);
    }

    /**
	 * Reset the jar placeholder and delete project.
	 */
    public void tearDownSuite() throws Exception {
        //$NON-NLS-1$
        this.deleteProject("AttachedJavadocProject");
        this.root = null;
        this.project = null;
        super.tearDownSuite();
    }

    // test javadoc for a package fragment
    public void test001() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        String javadoc = packageFragment.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNotNull("Should have a javadoc", javadoc);
    }

    // for a class file
    public void test002() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("X.class");
        assertNotNull(classFile);
        //$NON-NLS-1$
        String javadoc = classFile.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNotNull("Should have a javadoc", javadoc);
    }

    // for a field
    public void test003() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("X.class");
        assertNotNull(classFile);
        IType type = classFile.getType();
        //$NON-NLS-1$
        IField field = type.getField("f");
        assertNotNull(field);
        //$NON-NLS-1$
        String javadoc = field.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNotNull("Should have a javadoc", javadoc);
    }

    // for a method
    public void test004() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("X.class");
        assertNotNull(classFile);
        IType type = classFile.getType();
        //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        IMethod method = type.getMethod("foo", new String[] { "I", "J", "Ljava.lang.String;" });
        assertTrue(method.exists());
        //$NON-NLS-1$
        String javadoc = method.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNotNull("Should have a javadoc", javadoc);
        String[] paramNames = method.getParameterNames();
        assertNotNull(paramNames);
        //$NON-NLS-1$
        assertEquals("Wrong size", 3, paramNames.length);
        //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("Wrong name for first param", "i", paramNames[0]);
        //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("Wrong name for second param", "l", paramNames[1]);
        //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("Wrong name for third param", "s", paramNames[2]);
    }

    // for a constructor
    public void test005() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("X.class");
        assertNotNull(classFile);
        IType type = classFile.getType();
        //$NON-NLS-1$ //$NON-NLS-2$
        IMethod method = type.getMethod("X", new String[] { "I" });
        assertTrue(method.exists());
        //$NON-NLS-1$
        String javadoc = method.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNotNull("Should have a javadoc", javadoc);
        String[] paramNames = method.getParameterNames();
        assertNotNull(paramNames);
        //$NON-NLS-1$
        assertEquals("Wrong size", 1, paramNames.length);
        //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("Wrong name for first param", "i", paramNames[0]);
    }

    // for a member type
    public void test006() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("X$A.class");
        assertNotNull(classFile);
        //$NON-NLS-1$
        String javadoc = classFile.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNotNull("Should have a javadoc", javadoc);
    }

    // for a constructor
    public void test007() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("X$A.class");
        assertNotNull(classFile);
        IType type = classFile.getType();
        //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        IMethod method = type.getMethod("A", new String[] { "Lp1.p2.X;", "F" });
        assertTrue(method.exists());
        //$NON-NLS-1$
        String javadoc = method.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNotNull("Should have a javadoc", javadoc);
        String[] paramNames = method.getParameterNames();
        assertNotNull(paramNames);
        //$NON-NLS-1$
        assertEquals("Wrong size", 1, paramNames.length);
        //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("Wrong name for first param", "f", paramNames[0]);
    }

    // for a method foo2
    public void test008() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("X.class");
        assertNotNull(classFile);
        IType type = classFile.getType();
        //$NON-NLS-1$
        IMethod method = type.getMethod("foo2", new String[0]);
        assertTrue(method.exists());
        //$NON-NLS-1$
        String javadoc = method.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNotNull("Should have a javadoc", javadoc);
        String[] paramNames = method.getParameterNames();
        assertNotNull(paramNames);
        //$NON-NLS-1$
        assertEquals("Wrong size", 0, paramNames.length);
    }

    // for a field f2
    public void test009() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("X.class");
        assertNotNull(classFile);
        IType type = classFile.getType();
        //$NON-NLS-1$
        IField field = type.getField("f2");
        assertNotNull(field);
        //$NON-NLS-1$
        String javadoc = field.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNotNull("Should have a javadoc", javadoc);
    }

    // test archive doc
    public void test010() throws JavaModelException {
        IClasspathEntry[] savedEntries = null;
        try {
            IClasspathEntry[] entries = this.project.getRawClasspath();
            savedEntries = entries.clone();
            final String path = "jar:" + "platform:/resource/AttachedJavadocProject/doc.zip" + "!/doc";
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, path);
            for (int i = 0, max = entries.length; i < max; i++) {
                final IClasspathEntry entry = entries[i];
                if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY && entry.getContentKind() == IPackageFragmentRoot.K_BINARY && "/AttachedJavadocProject/lib/test6.jar".equals(//$NON-NLS-1$
                entry.getPath().toString())) {
                    entries[i] = JavaCore.newLibraryEntry(entry.getPath(), entry.getSourceAttachmentPath(), entry.getSourceAttachmentRootPath(), entry.getAccessRules(), new IClasspathAttribute[] { attribute }, entry.isExported());
                }
            }
            this.project.setRawClasspath(entries, null);
            //$NON-NLS-1$
            IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
            //$NON-NLS-1$
            assertNotNull("Should not be null", packageFragment);
            //$NON-NLS-1$
            IClassFile classFile = packageFragment.getClassFile("X.class");
            assertNotNull(classFile);
            IType type = classFile.getType();
            //$NON-NLS-1$
            IField field = type.getField("f");
            assertNotNull(field);
            //$NON-NLS-1$
            String javadoc = field.getAttachedJavadoc(new NullProgressMonitor());
            //$NON-NLS-1$
            assertNotNull("Should have a javadoc", javadoc);
        } finally {
            // restore classpath
            if (savedEntries != null) {
                this.project.setRawClasspath(savedEntries, null);
            }
        }
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=120597
    public void test011() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("Z.class");
        assertNotNull(classFile);
        IType type = classFile.getType();
        //$NON-NLS-1$
        IField field = type.getField("out");
        assertNotNull(field);
        //$NON-NLS-1$
        String javadoc = field.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNotNull("Should have a javadoc", javadoc);
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=120637
    public void test012() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("Z.class");
        assertNotNull(classFile);
        //$NON-NLS-1$
        String javadoc = classFile.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNotNull("Should have a javadoc", javadoc);
        assertTrue("Should not contain reference to out", javadoc.indexOf("out") == -1);
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=120559
    public void test013() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("W.class");
        assertNotNull(classFile);
        String javadoc = null;
        try {
            //$NON-NLS-1$
            javadoc = classFile.getAttachedJavadoc(new NullProgressMonitor());
        } catch (JavaModelException jme) {
            if (!(jme.getCause() instanceof FileNotFoundException)) {
                fail("Can only throw a FileNotFoundException");
            }
        }
        //$NON-NLS-1$
        assertNull("Should not have a javadoc", javadoc);
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=120637
    public void test014() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("E.class");
        assertNotNull(classFile);
        //$NON-NLS-1$
        String javadoc = classFile.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNotNull("Should have a javadoc", javadoc);
        assertTrue("Should not contain reference to Constant C", javadoc.indexOf("Constant C") == -1);
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=120637
    public void test015() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("Annot.class");
        assertNotNull(classFile);
        //$NON-NLS-1$
        String javadoc = classFile.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNotNull("Should have a javadoc", javadoc);
        assertTrue("Should not contain reference to name", javadoc.indexOf("name") == -1);
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=120847
    public void test016() throws JavaModelException {
        IClasspathEntry[] savedEntries = null;
        try {
            IClasspathEntry[] entries = this.project.getRawClasspath();
            savedEntries = entries.clone();
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, "invalid_path");
            for (int i = 0, max = entries.length; i < max; i++) {
                final IClasspathEntry entry = entries[i];
                if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY && entry.getContentKind() == IPackageFragmentRoot.K_BINARY && "/AttachedJavadocProject/lib/test6.jar".equals(//$NON-NLS-1$
                entry.getPath().toString())) {
                    entries[i] = JavaCore.newLibraryEntry(entry.getPath(), entry.getSourceAttachmentPath(), entry.getSourceAttachmentRootPath(), entry.getAccessRules(), new IClasspathAttribute[] { attribute }, entry.isExported());
                }
            }
            this.project.setRawClasspath(entries, null);
            //$NON-NLS-1$
            IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
            //$NON-NLS-1$
            assertNotNull("Should not be null", packageFragment);
            //$NON-NLS-1$
            IClassFile classFile = packageFragment.getClassFile("X.class");
            assertNotNull(classFile);
            IType type = classFile.getType();
            //$NON-NLS-1$
            IField field = type.getField("f");
            assertNotNull(field);
            //$NON-NLS-1$
            field.getAttachedJavadoc(new NullProgressMonitor());
            assertFalse("Should be unreachable", true);
        } catch (JavaModelException e) {
            assertTrue("Must occur", true);
            assertEquals("Wrong error message", "Cannot retrieve the attached javadoc for invalid_path", e.getMessage());
        } finally {
            // restore classpath
            if (savedEntries != null) {
                this.project.setRawClasspath(savedEntries, null);
            }
        }
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=120875
    public void test017() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("Annot2.class");
        assertNotNull(classFile);
        //$NON-NLS-1$
        String javadoc = classFile.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNotNull("Should have a javadoc", javadoc);
        assertTrue("Should not contain reference to name2", javadoc.indexOf("name2") == -1);
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=138167
    public void test018() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2.p3");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("C.class");
        assertNotNull(classFile);
        IType type = classFile.getType();
        IMethod[] methods = type.getMethods();
        NullProgressMonitor monitor = new NullProgressMonitor();
        for (int i = 0, max = methods.length; i < max; i++) {
            IMethod method = methods[i];
            String javadoc = method.getAttachedJavadoc(monitor);
            assertNotNull("Should have a javadoc", javadoc);
            final String selector = method.getElementName();
            assertTrue("Wrong doc", javadoc.indexOf(selector) != -1);
        }
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=138167
    public void test019() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2.p3");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("C.class");
        assertNotNull(classFile);
        IType type = classFile.getType();
        //$NON-NLS-1$
        IMethod method = type.getMethod("bar5", new String[] { "Ljava.util.Map<TK;TV;>;", "I", "Ljava.util.Map<TK;TV;>;" });
        assertTrue(method.exists());
        //$NON-NLS-1$
        String javadoc = method.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNotNull("Should have a javadoc", javadoc);
        String[] names = method.getParameterNames();
        assertNotNull("No names", names);
        assertEquals("Wrong size", 3, names.length);
        assertEquals("Wrong parameter name", "m", names[0]);
        assertEquals("Wrong parameter name", "j", names[1]);
        assertEquals("Wrong parameter name", "m2", names[2]);
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=139160
    public void test020() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("Z.class");
        assertNotNull(classFile);
        IType type = classFile.getType();
        //$NON-NLS-1$
        IMethod method = type.getMethod("foo", new String[] { "I", "I" });
        assertTrue(method.exists());
        String javadoc = null;
        try {
            //$NON-NLS-1$
            javadoc = method.getAttachedJavadoc(new NullProgressMonitor());
        } catch (JavaModelException e) {
            assertTrue("Should not happen", false);
        }
        //$NON-NLS-1$
        assertNull("Should not have a javadoc", javadoc);
        String[] paramNames = method.getParameterNames();
        assertNotNull(paramNames);
        //$NON-NLS-1$
        assertEquals("Wrong size", 2, paramNames.length);
        //$NON-NLS-1$
        assertEquals("Wrong name", "i", paramNames[0]);
        //$NON-NLS-1$
        assertEquals("Wrong name", "j", paramNames[1]);
    }

    /*
	 * Ensures that calling getAttachedJavadoc(...) on a binary method
	 * has no side-effect on the underlying Java model cache.
	 * (regression test for bug 140879 Spontaneous error "java.util.Set cannot be resolved...")
	 */
    public void test021() throws CoreException, IOException {
        ICompilationUnit workingCopy = null;
        try {
            IPackageFragment p = this.root.getPackageFragment("p2");
            IType type = p.getClassFile("X.class").getType();
            IMethod method = type.getMethod("foo", new String[0]);
            // the following call should have no side-effect
            method.getAttachedJavadoc(null);
            // ensure no side-effect
            ProblemRequestor problemRequestor = new ProblemRequestor();
            workingCopy = getWorkingCopy("/AttachedJavadocProject/src/Test.java", "import p2.Y;\n" + "public class Test extends Y { }", newWorkingCopyOwner(problemRequestor));
            assertProblems("Unexpected problems", "----------\n" + "----------\n", problemRequestor);
        } finally {
            if (workingCopy != null)
                workingCopy.discardWorkingCopy();
            deleteProject("P");
        }
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=149154
    public void test022() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("X.class");
        assertNotNull(classFile);
        IType type = classFile.getType();
        //$NON-NLS-1$
        IMethod method = type.getMethod("access$1", new String[] { "Lp1.p2.X;", "I" });
        assertTrue(method.exists());
        String javadoc = null;
        try {
            //$NON-NLS-1$
            javadoc = method.getAttachedJavadoc(new NullProgressMonitor());
        } catch (JavaModelException e) {
            assertTrue("Should not happen", false);
        }
        //$NON-NLS-1$
        assertNull("Should not have a javadoc", javadoc);
        String[] paramNames = method.getParameterNames();
        assertNotNull(paramNames);
        //$NON-NLS-1$
        assertEquals("Wrong size", 2, paramNames.length);
        //$NON-NLS-1$
        assertEquals("Wrong name", "arg0", paramNames[0]);
        //$NON-NLS-1$
        assertEquals("Wrong name", "arg1", paramNames[1]);
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=221723
    // for a method
    public void test023() throws JavaModelException {
        try {
            setJavadocLocationAttribute("specialDoc");
            //$NON-NLS-1$
            IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
            //$NON-NLS-1$
            assertNotNull("Should not be null", packageFragment);
            //$NON-NLS-1$
            IClassFile classFile = packageFragment.getClassFile("X.class");
            assertNotNull(classFile);
            IType type = classFile.getType();
            //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            IMethod method = type.getMethod("foo", new String[] { "I", "J", "Ljava.lang.String;" });
            assertTrue(method.exists());
            //$NON-NLS-1$
            String javadoc = method.getAttachedJavadoc(new NullProgressMonitor());
            //$NON-NLS-1$
            assertNotNull("Should have a javadoc", javadoc);
            String[] paramNames = method.getParameterNames();
            assertNotNull(paramNames);
            //$NON-NLS-1$
            assertEquals("Wrong size", 3, paramNames.length);
            //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("Wrong name for first param", "i", paramNames[0]);
            //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("Wrong name for second param", "l", paramNames[1]);
            //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("Wrong name for third param", "s", paramNames[2]);
        } finally {
            setJavadocLocationAttribute(DEFAULT_DOC_FOLDER);
        }
    }

    // for a private field
    public void test024() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        //$NON-NLS-1$
        IClassFile classFile = packageFragment.getClassFile("X.class");
        assertNotNull(classFile);
        IType type = classFile.getType();
        //$NON-NLS-1$
        IField field = type.getField("f4");
        assertNotNull(field);
        //$NON-NLS-1$
        String javadoc = field.getAttachedJavadoc(new NullProgressMonitor());
        //$NON-NLS-1$
        assertNull("Should have no javadoc", javadoc);
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=304316
    public void test025() throws JavaModelException {
        IClasspathEntry[] savedEntries = null;
        try {
            IClasspathEntry[] entries = this.project.getRawClasspath();
            savedEntries = entries.clone();
            //$NON-NLS-1$
            final String path = "http:/download.oracle.com/javase/6/docs/api/";
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, path);
            for (int i = 0, max = entries.length; i < max; i++) {
                final IClasspathEntry entry = entries[i];
                if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY && entry.getContentKind() == IPackageFragmentRoot.K_BINARY && "/AttachedJavadocProject/lib/test6.jar".equals(//$NON-NLS-1$
                entry.getPath().toString())) {
                    entries[i] = JavaCore.newLibraryEntry(entry.getPath(), entry.getSourceAttachmentPath(), entry.getSourceAttachmentRootPath(), entry.getAccessRules(), new IClasspathAttribute[] { attribute }, entry.isExported());
                }
            }
            this.project.setRawClasspath(entries, null);
            //$NON-NLS-1$
            IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
            //$NON-NLS-1$
            assertNotNull("Should not be null", packageFragment);
            //$NON-NLS-1$
            IClassFile classFile = packageFragment.getClassFile("X.class");
            assertNotNull(classFile);
            IType type = classFile.getType();
            //$NON-NLS-1$
            IField field = type.getField("f");
            assertNotNull(field);
            //$NON-NLS-1$
            String javadoc = field.getAttachedJavadoc(new NullProgressMonitor());
            //$NON-NLS-1$
            assertNull("Should not have a javadoc", javadoc);
        } finally {
            // restore classpath
            if (savedEntries != null) {
                this.project.setRawClasspath(savedEntries, null);
            }
        }
    }

    /**
	 * @bug 304394: IJavaElement#getAttachedJavadoc(IProgressMonitor) should support referenced entries
	 * Test that javadoc is picked up from the referenced classpath entry when the javadoc location is added
	 * to that entry
	 * 
	 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=304394"
	 * @throws Exception
	 */
    public void testBug304394() throws Exception {
        setJavadocLocationAttribute("specialDoc");
        IClasspathEntry[] savedEntries = null;
        try {
            IClasspathEntry[] entries = this.project.getRawClasspath();
            savedEntries = entries.clone();
            IClasspathEntry chainedJar = null;
            int max = entries.length;
            for (int i = 0; i < max; i++) {
                final IClasspathEntry entry = entries[i];
                if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY && entry.getContentKind() == IPackageFragmentRoot.K_BINARY && "/AttachedJavadocProject/lib/test6.jar".equals(//$NON-NLS-1$
                entry.getPath().toString())) {
                    chainedJar = entries[i];
                    addLibrary(this.project, "/lib/chaining.jar", null, new String[0], new String[] { "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" + "Class-Path: test6.jar\n" }, JavaCore.VERSION_1_4);
                    IPath jarPath = this.project.getPath().append("lib").append("chaining.jar");
                    entries[i] = JavaCore.newLibraryEntry(jarPath, entry.getSourceAttachmentPath(), entry.getSourceAttachmentRootPath());
                    break;
                }
            }
            this.project.setRawClasspath(entries, new IClasspathEntry[] { chainedJar }, this.project.getOutputLocation(), null);
            waitForAutoBuild();
            //$NON-NLS-1$
            IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
            //$NON-NLS-1$
            IClassFile classFile = packageFragment.getClassFile("X.class");
            IType type = classFile.getType();
            //$NON-NLS-1$
            IMethod method = type.getMethod("foo", new String[] { "I", "J", "Ljava.lang.String;" });
            //$NON-NLS-1$
            String javadoc = method.getAttachedJavadoc(new NullProgressMonitor());
            //$NON-NLS-1$
            assertNotNull("Should have a javadoc", javadoc);
        } finally {
            // restore classpath
            if (savedEntries != null) {
                this.project.setRawClasspath(savedEntries, null);
            }
            removeLibrary(this.project, "/lib/chaining.jar", null);
        }
    }

    /**
	 * Additional test for bug 304394.
	 * Test that javadoc is picked up from the raw classpath entry when the referenced entry doesn't 
	 * contain the javadoc location attrribute.
	 * 
	 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=304394"
	 * @throws Exception
	 */
    public void testBug304394a() throws Exception {
        setJavadocLocationAttribute("specialDoc");
        IClasspathEntry[] savedEntries = null;
        try {
            IClasspathEntry[] entries = this.project.getRawClasspath();
            savedEntries = entries.clone();
            IClasspathEntry chainedJar = null;
            int max = entries.length;
            for (int i = 0; i < max; i++) {
                final IClasspathEntry entry = entries[i];
                if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY && entry.getContentKind() == IPackageFragmentRoot.K_BINARY && "/AttachedJavadocProject/lib/test6.jar".equals(//$NON-NLS-1$
                entry.getPath().toString())) {
                    chainedJar = entries[i];
                    addLibrary(this.project, "/lib/chaining.jar", null, new String[0], new String[] { "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" + "Class-Path: test6.jar\n" }, JavaCore.VERSION_1_4);
                    IPath jarPath = this.project.getPath().append("lib").append("chaining.jar");
                    entries[i] = JavaCore.newLibraryEntry(jarPath, entry.getSourceAttachmentPath(), entry.getSourceAttachmentRootPath(), entry.getAccessRules(), entry.getExtraAttributes(), entry.isExported());
                    break;
                }
            }
            chainedJar = JavaCore.newLibraryEntry(chainedJar.getPath(), null, null, null, null, chainedJar.isExported());
            this.project.setRawClasspath(entries, new IClasspathEntry[] { chainedJar }, this.project.getOutputLocation(), null);
            waitForAutoBuild();
            //$NON-NLS-1$
            IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
            //$NON-NLS-1$
            IClassFile classFile = packageFragment.getClassFile("X.class");
            IType type = classFile.getType();
            //$NON-NLS-1$
            IMethod method = type.getMethod("foo", new String[] { "I", "J", "Ljava.lang.String;" });
            //$NON-NLS-1$
            String javadoc = method.getAttachedJavadoc(new NullProgressMonitor());
            //$NON-NLS-1$
            assertNotNull("Should have a javadoc", javadoc);
        } finally {
            // restore classpath
            if (savedEntries != null) {
                this.project.setRawClasspath(savedEntries, null);
            }
            removeLibrary(this.project, "/lib/chaining.jar", null);
        }
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=320167
    // Test to verify that while trying to get javadoc contents from a malformed
    // javadoc, CharOperation doesnt throw an IOOBE
    public void testBug320167() throws JavaModelException {
        try {
            setJavadocLocationAttribute("malformedDoc");
            //$NON-NLS-1$
            IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2");
            //$NON-NLS-1$
            assertNotNull("Should not be null", packageFragment);
            //$NON-NLS-1$
            IClassFile classFile = packageFragment.getClassFile("X.class");
            assertNotNull(classFile);
            IType type = classFile.getType();
            //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            IMethod method = type.getMethod("foo", new String[] { "I", "J", "Ljava.lang.String;" });
            assertTrue(method.exists());
            //$NON-NLS-1$
            String javadoc = method.getAttachedJavadoc(new NullProgressMonitor());
            //$NON-NLS-1$
            assertNotNull("Should have a javadoc", javadoc);
            String[] paramNames = method.getParameterNames();
            assertNotNull(paramNames);
            //$NON-NLS-1$
            assertEquals("Wrong size", 3, paramNames.length);
            //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("Wrong name for first param", "i", paramNames[0]);
            //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("Wrong name for second param", "l", paramNames[1]);
            //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("Wrong name for third param", "s", paramNames[2]);
        } catch (IndexOutOfBoundsException e) {
            assertTrue("Should not happen", false);
        } catch (JavaModelException e) {
            assertTrue("Should happen", true);
        } finally {
            setJavadocLocationAttribute(DEFAULT_DOC_FOLDER);
        }
    }

    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=329671
    public void testBug329671() throws CoreException, IOException {
        Map options = this.project.getOptions(true);
        Object timeout = options.get(JavaCore.TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC);
        //$NON-NLS-1$
        options.put(JavaCore.TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC, "0");
        this.project.setOptions(options);
        IClasspathEntry[] entries = this.project.getRawClasspath();
        try {
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, "jar:platform:/resource/AttachedJavadocProject/bug329671_doc.zip!/");
            IClasspathEntry newEntry = JavaCore.newLibraryEntry(new Path("/AttachedJavadocProject/bug329671.jar"), null, null, null, new IClasspathAttribute[] { attribute }, false);
            this.project.setRawClasspath(new IClasspathEntry[] { newEntry }, null);
            this.project.getResolvedClasspath(false);
            IPackageFragmentRoot jarRoot = this.project.getPackageFragmentRoot(getFile("/AttachedJavadocProject/bug329671.jar"));
            final IType type = jarRoot.getPackageFragment("bug").getClassFile("X.class").getType();
            IMethod method = type.getMethod("foo", new String[] { "Ljava.lang.Object;" });
            assertNotNull(method);
            String[] paramNames = method.getParameterNames();
            assertStringsEqual("Parameter names", new String[] { "arg0" }, paramNames);
            final PerProjectInfo projectInfo = JavaModelManager.getJavaModelManager().getPerProjectInfoCheckExistence(this.project.getProject());
            final Object varThis = this;
            Thread thread = new Thread() {

                public void run() {
                    Object javadocContent = projectInfo.javadocCache.get(type);
                    while (javadocContent == null || javadocContent == BinaryType.EMPTY_JAVADOC) {
                        try {
                            Thread.sleep(50);
                            javadocContent = projectInfo.javadocCache.get(type);
                        } catch (InterruptedException e) {
                        }
                        synchronized (varThis) {
                            varThis.notify();
                        }
                    }
                }
            };
            thread.start();
            synchronized (varThis) {
                try {
                    varThis.wait(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            projectInfo.javadocCache.flush();
            //$NON-NLS-1$
            options.put(JavaCore.TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC, "5000");
            this.project.setOptions(options);
            paramNames = method.getParameterNames();
            assertStringsEqual("Parameter names", new String[] { "param" }, paramNames);
        } finally {
            this.project.setRawClasspath(entries, null);
            if (timeout != null)
                options.put(JavaCore.TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC, timeout);
            this.project.setOptions(options);
        }
    }

    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=334652
    public void testBug334652() throws CoreException, IOException {
        IClasspathEntry[] entries = this.project.getRawClasspath();
        try {
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, "jar:platform:/resource/AttachedJavadocProject/bug334652_doc.zip!/NoJavaDocForInnerClass/doc");
            IClasspathEntry newEntry = JavaCore.newLibraryEntry(new Path("/AttachedJavadocProject/bug334652.jar"), null, null, null, new IClasspathAttribute[] { attribute }, false);
            this.project.setRawClasspath(new IClasspathEntry[] { newEntry }, null);
            this.project.getResolvedClasspath(false);
            IPackageFragmentRoot jarRoot = this.project.getPackageFragmentRoot(getFile("/AttachedJavadocProject/bug334652.jar"));
            final IType type = jarRoot.getPackageFragment("com.test").getClassFile("PublicAbstractClass$InnerFinalException.class").getType();
            IMethod method = type.getMethod("InnerFinalException", new String[] { "Lcom.test.PublicAbstractClass;", "Ljava.lang.String;", "Ljava.lang.String;" });
            assertNotNull(method);
            assertTrue("Does not exist", method.exists());
            String javadoc = method.getAttachedJavadoc(null);
            assertNotNull(javadoc);
            assertEquals("Wrong contents", "<H3>\r\n" + "PublicAbstractClass.InnerFinalException</H3>\r\n" + "<PRE>\r\n" + "public <B>PublicAbstractClass.InnerFinalException</B>(java.lang.String&nbsp;property,\r\n" + "                                               java.lang.String&nbsp;msg)</PRE>\r\n" + "<DL>\r\n" + "<DD>javadoc for InnerFinalException(String property, String msg)\r\n" + "<P>\r\n" + "<DL>\r\n" + "<DT><B>Parameters:</B><DD><CODE>property</CODE> - the property argument<DD><CODE>msg</CODE> - the message argument</DL>\r\n" + "</DL>\r\n", javadoc);
        } finally {
            this.project.setRawClasspath(entries, null);
        }
    }

    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=334652
    public void testBug334652_2() throws CoreException, IOException {
        IClasspathEntry[] entries = this.project.getRawClasspath();
        try {
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, "jar:platform:/resource/AttachedJavadocProject/bug334652(2)_doc.zip!/doc");
            IClasspathEntry newEntry = JavaCore.newLibraryEntry(new Path("/AttachedJavadocProject/bug334652(2).jar"), null, null, null, new IClasspathAttribute[] { attribute }, false);
            this.project.setRawClasspath(new IClasspathEntry[] { newEntry }, null);
            this.project.getResolvedClasspath(false);
            IPackageFragmentRoot jarRoot = this.project.getPackageFragmentRoot(getFile("/AttachedJavadocProject/bug334652(2).jar"));
            final IType type = jarRoot.getPackageFragment("com.test").getClassFile("PublicAbstractClass$InnerFinalException.class").getType();
            IMethod method = type.getMethod("InnerFinalException", new String[] { "Ljava.lang.String;", "Ljava.lang.String;" });
            assertNotNull(method);
            assertTrue("Does not exist", method.exists());
            String javadoc = method.getAttachedJavadoc(null);
            assertNotNull(javadoc);
            assertEquals("Wrong contents", "<H3>\r\n" + "PublicAbstractClass.InnerFinalException</H3>\r\n" + "<PRE>\r\n" + "public <B>PublicAbstractClass.InnerFinalException</B>(java.lang.String&nbsp;property,\r\n" + "                                               java.lang.String&nbsp;msg)</PRE>\r\n" + "<DL>\r\n" + "<DD>javadoc for InnerFinalException(String property, String msg)\r\n" + "<P>\r\n" + "<DL>\r\n" + "<DT><B>Parameters:</B><DD><CODE>property</CODE> - the property argument<DD><CODE>msg</CODE> - the message argument</DL>\r\n" + "</DL>\r\n", javadoc);
        } finally {
            this.project.setRawClasspath(entries, null);
        }
    }

    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=334652
    public void testBug334652_3() throws CoreException, IOException {
        IClasspathEntry[] entries = this.project.getRawClasspath();
        try {
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, "jar:platform:/resource/AttachedJavadocProject/bug334652(3)_doc.zip!/doc");
            IClasspathEntry newEntry = JavaCore.newLibraryEntry(new Path("/AttachedJavadocProject/bug334652(3).jar"), null, null, null, new IClasspathAttribute[] { attribute }, false);
            this.project.setRawClasspath(new IClasspathEntry[] { newEntry }, null);
            this.project.getResolvedClasspath(false);
            IPackageFragmentRoot jarRoot = this.project.getPackageFragmentRoot(getFile("/AttachedJavadocProject/bug334652(3).jar"));
            final IType type = jarRoot.getPackageFragment("com.test").getClassFile("PublicAbstractClass$A$InnerFinalException.class").getType();
            IMethod method = type.getMethod("InnerFinalException", new String[] { "Lcom.test.PublicAbstractClass$A;", "Ljava.lang.String;", "Ljava.lang.String;" });
            assertNotNull(method);
            assertTrue("Does not exist", method.exists());
            String javadoc = method.getAttachedJavadoc(null);
            assertNotNull(javadoc);
            assertEquals("Wrong contents", "<H3>\r\n" + "PublicAbstractClass.A.InnerFinalException</H3>\r\n" + "<PRE>\r\n" + "public <B>PublicAbstractClass.A.InnerFinalException</B>(java.lang.String&nbsp;property,\r\n" + "                                                 java.lang.String&nbsp;msg)</PRE>\r\n" + "<DL>\r\n" + "<DD>javadoc for InnerFinalException(String property, String msg)\r\n" + "<P>\r\n" + "<DL>\r\n" + "<DT><B>Parameters:</B><DD><CODE>property</CODE> - the property argument<DD><CODE>msg</CODE> - the message argument</DL>\r\n" + "</DL>\r\n", javadoc);
        } finally {
            this.project.setRawClasspath(entries, null);
        }
    }

    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=334652
    public void testBug334652_4() throws CoreException, IOException {
        IClasspathEntry[] entries = this.project.getRawClasspath();
        try {
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, "jar:platform:/resource/AttachedJavadocProject/bug334652(4)_doc.zip!/doc");
            IClasspathEntry newEntry = JavaCore.newLibraryEntry(new Path("/AttachedJavadocProject/bug334652(4).jar"), null, null, null, new IClasspathAttribute[] { attribute }, false);
            this.project.setRawClasspath(new IClasspathEntry[] { newEntry }, null);
            this.project.getResolvedClasspath(false);
            IPackageFragmentRoot jarRoot = this.project.getPackageFragmentRoot(getFile("/AttachedJavadocProject/bug334652(4).jar"));
            final IType type = jarRoot.getPackageFragment("com.test").getClassFile("PublicAbstractClass$A$InnerFinalException.class").getType();
            IMethod method = type.getMethod("InnerFinalException", new String[] { "Lcom.test.PublicAbstractClass$A;", "Ljava.lang.String;", "Ljava.lang.String;" });
            assertNotNull(method);
            assertTrue("Does not exist", method.exists());
            String javadoc = method.getAttachedJavadoc(null);
            assertNotNull(javadoc);
            assertEquals("Wrong contents", "<H3>\r\n" + "PublicAbstractClass.A.InnerFinalException</H3>\r\n" + "<PRE>\r\n" + "public <B>PublicAbstractClass.A.InnerFinalException</B>(java.lang.String&nbsp;property,\r\n" + "                                                 java.lang.String&nbsp;msg)</PRE>\r\n" + "<DL>\r\n" + "<DD>javadoc for InnerFinalException(String property, String msg)\r\n" + "<P>\r\n" + "<DL>\r\n" + "<DT><B>Parameters:</B><DD><CODE>property</CODE> - the property argument<DD><CODE>msg</CODE> - the message argument</DL>\r\n" + "</DL>\r\n", javadoc);
        } finally {
            this.project.setRawClasspath(entries, null);
        }
    }

    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=354766
    public void testBug354766() throws CoreException, IOException {
        IClasspathEntry[] entries = this.project.getRawClasspath();
        try {
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, "jar:platform:/resource/AttachedJavadocProject/bug354766_doc.zip!/");
            IClasspathEntry newEntry = JavaCore.newLibraryEntry(new Path("/AttachedJavadocProject/bug354766.jar"), null, null, null, new IClasspathAttribute[] { attribute }, false);
            this.project.setRawClasspath(new IClasspathEntry[] { newEntry }, null);
            this.project.getResolvedClasspath(false);
            IPackageFragmentRoot jarRoot = this.project.getPackageFragmentRoot(getFile("/AttachedJavadocProject/bug354766.jar"));
            final IType type = jarRoot.getPackageFragment("com.test").getClassFile("PublicAbstractClass$InnerFinalException.class").getType();
            IMethod method = type.getMethod("foo", new String[0]);
            assertNotNull(method);
            assertTrue("Does not exist", method.exists());
            String javadoc = method.getAttachedJavadoc(null);
            assertNotNull(javadoc);
            assertEquals("Wrong contents", "<H3>\r\n" + "foo</H3>\r\n" + "<PRE>\r\n" + "public void <B>foo</B>()</PRE>\r\n" + "<DL>\r\n" + "<DD>Test method\r\n" + "<P>\r\n" + "<DD><DL>\r\n" + "</DL>\r\n" + "</DD>\r\n" + "</DL>\r\n", javadoc);
        } finally {
            this.project.setRawClasspath(entries, null);
        }
    }

    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=354766
    public void testBug354766_2() throws CoreException, IOException {
        IClasspathEntry[] entries = this.project.getRawClasspath();
        try {
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, "jar:platform:/resource/AttachedJavadocProject/bug354766_doc.zip!/");
            IClasspathEntry newEntry = JavaCore.newLibraryEntry(new Path("/AttachedJavadocProject/bug354766.jar"), null, null, null, new IClasspathAttribute[] { attribute }, false);
            this.project.setRawClasspath(new IClasspathEntry[] { newEntry }, null);
            this.project.getResolvedClasspath(false);
            IPackageFragmentRoot jarRoot = this.project.getPackageFragmentRoot(getFile("/AttachedJavadocProject/bug354766.jar"));
            final IType type = jarRoot.getPackageFragment("com.test").getClassFile("PublicAbstractClass$InnerFinalException.class").getType();
            IMethod method = type.getMethod("InnerFinalException", new String[] { "Lcom.test.PublicAbstractClass;" });
            assertNotNull(method);
            assertTrue("Does not exist", method.exists());
            String javadoc = method.getAttachedJavadoc(null);
            assertNotNull(javadoc);
            assertEquals("Wrong contents", "<H3>\r\n" + "PublicAbstractClass.InnerFinalException</H3>\r\n" + "<PRE>\r\n" + "public <B>PublicAbstractClass.InnerFinalException</B>()</PRE>\r\n" + "<DL>\r\n" + "<DD>javadoc for InnerFinalException()\r\n" + "<P>\r\n" + "</DL>\r\n" + "\r\n" + "<!-- ============ METHOD DETAIL ========== -->\r\n" + "\r\n", javadoc);
        } finally {
            this.project.setRawClasspath(entries, null);
        }
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=394967
    public void testBug394967() throws JavaModelException {
        IClasspathEntry[] entries = this.project.getRawClasspath();
        try {
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, "jar:platform:/resource/AttachedJavadocProject/bug394967_doc.zip!/");
            IClasspathEntry newEntry = JavaCore.newLibraryEntry(new Path("/AttachedJavadocProject/bug394967.jar"), null, null, null, new IClasspathAttribute[] { attribute }, false);
            this.project.setRawClasspath(new IClasspathEntry[] { newEntry }, null);
            this.project.getResolvedClasspath(false);
            IPackageFragmentRoot jarRoot = this.project.getPackageFragmentRoot(getFile("/AttachedJavadocProject/bug394967.jar"));
            final IPackageFragment packageFragment = jarRoot.getPackageFragment("java.io");
            assertNotNull(packageFragment);
            assertTrue("Does not exist", packageFragment.exists());
            String javadoc = packageFragment.getAttachedJavadoc(null);
            assertNotNull(javadoc);
            assertEquals("Wrong contents", "\r\n<div class=\"block\">Provides for system input and output through data streams, serialization and the file system.\r\n" + "<h2>\r\n" + "Package Specification</h2>\r\n" + "<p><br>Provides for system input and output through data streams, serialization and the file system. Unless otherwise noted, passing a null argument to a constructor or method in any class or interface in this package will cause a NullPointerException to be thrown.\r\n" + "<h2>Related Documentation</h2>\r\n" + "For overviews, tutorials, examples, guides, and tool documentation,\r\n" + "please see:\r\n" + "<ul>\r\n" + "  <li><a href=\"../../../guide/serialization\">Serialization Enhancements</a>\r\n" + "</ul>\r\n" + "<DL>\r\n" + "<DT><B>Since:</B></DT>\r\n" + "<DD>JDK1.0</DD>\r\n" + "</DL></div>\r\n" + "</div>\r\n", javadoc);
        } finally {
            this.project.setRawClasspath(entries, null);
        }
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=394382
    public void testBug394382() throws JavaModelException {
        IClasspathEntry[] oldClasspath = this.project.getRawClasspath();
        try {
            String encoding = "UTF-8";
            //$NON-NLS-1$
            IResource resource = this.project.getProject().findMember("/UTF8doc/");
            //$NON-NLS-1$
            assertNotNull("doc folder cannot be null", resource);
            URI locationURI = resource.getLocationURI();
            //$NON-NLS-1$
            assertNotNull("doc folder cannot be null", locationURI);
            URL docUrl = null;
            try {
                docUrl = locationURI.toURL();
            } catch (MalformedURLException e) {
                assertTrue("Should not happen", false);
            } catch (IllegalArgumentException e) {
                assertTrue("Should not happen", false);
            }
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, docUrl.toExternalForm());
            //$NON-NLS-1$
            IClasspathEntry newEntry = JavaCore.newLibraryEntry(new Path("/AttachedJavadocProject/lib/bug394382.jar"), null, null, new IAccessRule[] {}, new IClasspathAttribute[] { attribute }, false);
            IClasspathEntry[] newClasspath = new IClasspathEntry[oldClasspath.length + 1];
            System.arraycopy(oldClasspath, 0, newClasspath, 0, oldClasspath.length);
            newClasspath[oldClasspath.length] = newEntry;
            this.project.setRawClasspath(newClasspath, null);
            waitForAutoBuild();
            IPackageFragmentRoot[] roots = this.project.getAllPackageFragmentRoots();
            IPackageFragmentRoot packageRoot = null;
            for (int i = 0; i < roots.length; i++) {
                IPath path = roots[i].getPath();
                if (path.segment(path.segmentCount() - 1).equals("bug394382.jar")) {
                    packageRoot = roots[i];
                }
            }
            assertNotNull("Should not be null", packageRoot);
            //$NON-NLS-1$
            IPackageFragment packageFragment = packageRoot.getPackageFragment("p");
            //$NON-NLS-1$
            assertNotNull("Should not be null", packageFragment);
            //$NON-NLS-1$
            IClassFile classFile = packageFragment.getClassFile("TestBug394382.class");
            assertNotNull(classFile);
            IType type = classFile.getType();
            IFile sourceFile = (IFile) this.project.getProject().findMember("UTF8doc/p/TestBug394382.txt");
            String javadoc = null;
            try {
                javadoc = type.getAttachedJavadoc(new NullProgressMonitor());
            } catch (JavaModelException e) {
                assertTrue("Should not happen", false);
            }
            //$NON-NLS-1$
            assertNotNull("Shouldhave a javadoc", javadoc);
            String encodedContents = new String(Util.getResourceContentsAsCharArray(sourceFile, encoding));
            char[] charArray = encodedContents.toCharArray();
            encodedContents = new String(CharOperation.remove(charArray, '\r'));
            charArray = javadoc.toCharArray();
            javadoc = new String(CharOperation.remove(charArray, '\r'));
            assertTrue("Sources should be decoded the same way", encodedContents.equals(javadoc));
        } finally {
            this.project.setRawClasspath(oldClasspath, null);
        }
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=398272
    public void testBug398272() throws JavaModelException {
        //$NON-NLS-1$
        IPackageFragment packageFragment = this.root.getPackageFragment("p1.p2.p3.p4");
        //$NON-NLS-1$
        assertNotNull("Should not be null", packageFragment);
        try {
            //$NON-NLS-1$
            String javadoc = packageFragment.getAttachedJavadoc(new NullProgressMonitor());
            //$NON-NLS-1$
            assertNull("Javadoc should be null", javadoc);
        } catch (JavaModelException jme) {
            if (!(jme.getCause() instanceof FileNotFoundException)) {
                fail("Should not throw Java Model Exception");
            }
        }
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=426058
    public void testBug426058() throws JavaModelException {
        IClasspathEntry[] oldClasspath = this.project.getRawClasspath();
        try {
            String encoding = "UTF-8";
            //$NON-NLS-1$
            IResource resource = this.project.getProject().findMember("/UTF8doc2/");
            //$NON-NLS-1$
            assertNotNull("doc folder cannot be null", resource);
            URI locationURI = resource.getLocationURI();
            //$NON-NLS-1$
            assertNotNull("doc folder cannot be null", locationURI);
            URL docUrl = null;
            try {
                docUrl = locationURI.toURL();
            } catch (MalformedURLException e) {
                assertTrue("Should not happen", false);
            } catch (IllegalArgumentException e) {
                assertTrue("Should not happen", false);
            }
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, docUrl.toExternalForm());
            //$NON-NLS-1$
            IClasspathEntry newEntry = JavaCore.newLibraryEntry(new Path("/AttachedJavadocProject/lib/bug394382.jar"), null, null, new IAccessRule[] {}, new IClasspathAttribute[] { attribute }, false);
            IClasspathEntry[] newClasspath = new IClasspathEntry[oldClasspath.length + 1];
            System.arraycopy(oldClasspath, 0, newClasspath, 0, oldClasspath.length);
            newClasspath[oldClasspath.length] = newEntry;
            this.project.setRawClasspath(newClasspath, null);
            waitForAutoBuild();
            IPackageFragmentRoot[] roots = this.project.getAllPackageFragmentRoots();
            IPackageFragmentRoot packageRoot = null;
            for (int i = 0; i < roots.length; i++) {
                IPath path = roots[i].getPath();
                if (path.segment(path.segmentCount() - 1).equals("bug394382.jar")) {
                    packageRoot = roots[i];
                }
            }
            assertNotNull("Should not be null", packageRoot);
            //$NON-NLS-1$
            IPackageFragment packageFragment = packageRoot.getPackageFragment("p");
            //$NON-NLS-1$
            assertNotNull("Should not be null", packageFragment);
            //$NON-NLS-1$
            IClassFile classFile = packageFragment.getClassFile("TestBug394382.class");
            assertNotNull(classFile);
            IType type = classFile.getType();
            IFile sourceFile = (IFile) this.project.getProject().findMember("UTF8doc2/p/TestBug394382.txt");
            String javadoc = null;
            try {
                javadoc = type.getAttachedJavadoc(new NullProgressMonitor());
            } catch (JavaModelException e) {
                assertTrue("Should not happen", false);
            }
            //$NON-NLS-1$
            assertNotNull("Shouldhave a javadoc", javadoc);
            String encodedContents = new String(Util.getResourceContentsAsCharArray(sourceFile, encoding));
            char[] charArray = encodedContents.toCharArray();
            encodedContents = new String(CharOperation.remove(charArray, '\r'));
            charArray = javadoc.toCharArray();
            javadoc = new String(CharOperation.remove(charArray, '\r'));
            assertTrue("Sources should be decoded the same way", encodedContents.equals(javadoc));
        } finally {
            this.project.setRawClasspath(oldClasspath, null);
        }
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=403154
    public void testBug403154() throws Exception {
        IClasspathEntry[] oldClasspath = this.project.getRawClasspath();
        try {
            IResource invalid = this.project.getProject().getFolder("invalid");
            IResource valid = this.project.getProject().getFolder("valid");
            createFolder("/AttachedJavadocProject/valid");
            URL validUrl = null;
            URL invalidUrl = null;
            try {
                validUrl = valid.getLocationURI().toURL();
                invalidUrl = invalid.getLocationURI().toURL();
            } catch (Exception e) {
                fail("Should not be an exception");
            }
            addLibrary(this.project, "valid.jar", null, new String[] { "p/X.java", "package p;\n" + "/** Javadoc for class X */\n" + "public class X {}" }, JavaCore.VERSION_1_4);
            addLibrary(this.project, "invalid.jar", null, new String[] { "q/Y.java", "package q;\n" + "/** Javadoc for class Y */\n" + "public class Y {}" }, JavaCore.VERSION_1_4);
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, validUrl.toExternalForm());
            IClasspathEntry validEntry = JavaCore.newLibraryEntry(new Path("/AttachedJavadocProject/valid.jar"), null, null, new IAccessRule[] {}, new IClasspathAttribute[] { attribute }, false);
            attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, invalidUrl.toExternalForm());
            IClasspathEntry invalidEntry = JavaCore.newLibraryEntry(new Path("/AttachedJavadocProject/invalid.jar"), null, null, new IAccessRule[] {}, new IClasspathAttribute[] { attribute }, false);
            IClasspathEntry[] newClasspath = new IClasspathEntry[oldClasspath.length + 2];
            System.arraycopy(oldClasspath, 0, newClasspath, 0, oldClasspath.length);
            newClasspath[oldClasspath.length] = validEntry;
            newClasspath[oldClasspath.length + 1] = invalidEntry;
            this.project.setRawClasspath(newClasspath, null);
            waitForAutoBuild();
            IPackageFragmentRoot[] roots = this.project.getAllPackageFragmentRoots();
            IPackageFragmentRoot validRoot = null;
            IPackageFragmentRoot invalidRoot = null;
            for (int i = 0; i < roots.length; i++) {
                IPath path = roots[i].getPath();
                if (path.segment(path.segmentCount() - 1).equals("valid.jar")) {
                    validRoot = roots[i];
                } else if (path.segment(path.segmentCount() - 1).equals("invalid.jar")) {
                    invalidRoot = roots[i];
                }
            }
            IPackageFragment packageFragment = validRoot.getPackageFragment("p");
            IClassFile classFile = packageFragment.getClassFile("X.class");
            IType type = classFile.getType();
            String javadoc = null;
            try {
                javadoc = type.getAttachedJavadoc(new NullProgressMonitor());
            } catch (JavaModelException e) {
                fail("Should not throw JavaModelException");
            }
            assertNull("Should not have a javadoc", javadoc);
            packageFragment = invalidRoot.getPackageFragment("q");
            classFile = packageFragment.getClassFile("Y.class");
            type = classFile.getType();
            try {
                type.getAttachedJavadoc(new NullProgressMonitor());
                fail("Should throw JavaModelException");
            } catch (JavaModelException e) {
            }
        } catch (Exception e) {
        } finally {
            deleteFolder("/AttachedJavadocProject/valid");
            this.project.setRawClasspath(oldClasspath, null);
        }
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=418092
    // Correctly parse Javadoc for methods that have parameterized annotations.
    public void testBug418092() throws JavaModelException {
        try {
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, "jar:platform:/resource/AttachedJavadocProject/bug418092_doc.zip!/");
            IClasspathEntry newEntry = JavaCore.newLibraryEntry(new Path("/AttachedJavadocProject/bug418092.jar"), null, null, null, new IClasspathAttribute[] { attribute }, true);
            this.project.setRawClasspath(new IClasspathEntry[] { newEntry }, null);
            this.project.getResolvedClasspath(false);
            IPackageFragmentRoot jarRoot = this.project.getPackageFragmentRoot(getFile("/AttachedJavadocProject/bug418092.jar"));
            final IType type = jarRoot.getPackageFragment("p1.p2").getClassFile("Annot3.class").getType();
            assertNotNull(type);
            //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            IMethod method = type.getMethod("filter", new String[] { "I", "I" });
            assertTrue(method.exists());
            //$NON-NLS-1$
            String javadoc = method.getAttachedJavadoc(new NullProgressMonitor());
            //$NON-NLS-1$
            assertNotNull("Should have a javadoc", javadoc);
            String[] paramNames = method.getParameterNames();
            assertNotNull(paramNames);
            //$NON-NLS-1$
            assertEquals("Wrong size", 2, paramNames.length);
            //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("Wrong name for first param", "p1", paramNames[0]);
            //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("Wrong name for second param", "p2", paramNames[1]);
        } catch (IndexOutOfBoundsException e) {
            assertTrue("Should not happen", false);
        }
    }

    public void testBug499196() throws JavaModelException {
        try {
            IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, "jar:platform:/resource/AttachedJavadocProject/bug499196_doc.zip!/");
            IClasspathEntry newEntry = JavaCore.newLibraryEntry(new Path("/AttachedJavadocProject/bug499196.jar"), null, null, null, new IClasspathAttribute[] { attribute }, true);
            this.project.setRawClasspath(new IClasspathEntry[] { newEntry }, null);
            this.project.getResolvedClasspath(false);
            IPackageFragmentRoot jarRoot = this.project.getPackageFragmentRoot(getFile("/AttachedJavadocProject/bug499196.jar"));
            final IType type = jarRoot.getPackageFragment("p1.p2").getClassFile("Bug499196.class").getType();
            assertNotNull(type);
            //$NON-NLS-1$
            IMethod method = type.getMethod("Bug499196", new String[] {});
            assertNotNull("Constructor should not be null", method);
            assertTrue(method.exists());
            //$NON-NLS-1$
            String javadoc = method.getAttachedJavadoc(new NullProgressMonitor());
            //$NON-NLS-1$
            assertNotNull("Should have a javadoc", javadoc);
        } catch (IndexOutOfBoundsException e) {
            assertTrue("Should not happen", false);
        }
    }
}
