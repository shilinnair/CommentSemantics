/*******************************************************************************
 * Copyright (c) 2007, 2008 BEA Systems, Inc. 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    wharley@bea.com - initial API and implementation
 *    
 *******************************************************************************/
package org.eclipse.jdt.apt.tests;

import java.io.File;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.tests.util.Util;

/**
 * 
 * @since 3.3
 */
public class ScalingTests extends APTTestBase {

    private final boolean VERBOSE = true;

    public  ScalingTests(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ScalingTests.class);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    /**
	 * A customer reports that projects with ~2000 files abort generation.
	 * Note, this test will take quite a long time to run.
	 */
    public void testGeneratingLotsOfFiles() throws Exception {
        // total number of files to create
        final int FILES_TO_GENERATE = 4000;
        // wait for indexer to catch up after creating this many files
        final int PAUSE_EVERY = 200;
        // milliseconds to wait for indexer
        final int PAUSE_TIME = 2000;
        // set up project with unique name
        //$NON-NLS-1$
        final String projName = ScalingTests.class.getName() + "LotsOfFilesProject";
        //$NON-NLS-1$
        IPath projectPath = env.addProject(projName, "1.5");
        env.addExternalJars(projectPath, Util.getJavaClassLibs());
        //$NON-NLS-1$
        env.removePackageFragmentRoot(projectPath, "");
        //$NON-NLS-1$
        env.addPackageFragmentRoot(projectPath, "src");
        //$NON-NLS-1$
        env.setOutputFolder(projectPath, "bin");
        TestUtil.createAndAddAnnotationJar(env.getJavaProject(projectPath));
        IProject project = env.getProject(projName);
        IFolder srcFolder = project.getFolder("src");
        IPath srcRoot = srcFolder.getFullPath();
        String template = "package p;\n" + "import org.eclipse.jdt.apt.tests.annotations.helloworld.HelloWorldAnnotation;\n" + "@HelloWorldAnnotation(\"Generated%05d\")\n" + "public class Test%05d { generatedfilepackage.Generated%05d _t; }";
        for (int i = 1; i <= FILES_TO_GENERATE; ++i) {
            String name = String.format("Test%05d", i);
            String contents = String.format(template, i, i, FILES_TO_GENERATE - i + 1);
            //$NON-NLS-1$ //$NON-NLS-2$
            env.addClass(srcRoot, "p", name, contents);
            // pause to let indexer catch up
            if (i % PAUSE_EVERY == 0) {
                if (VERBOSE)
                    System.out.println("Created " + i + " files; pausing for indexer");
                Thread.sleep(PAUSE_TIME);
            }
        }
        if (VERBOSE)
            System.out.println("Done creating source files");
        // Set some per-project preferences
        IJavaProject jproj = env.getJavaProject(projName);
        AptConfig.setEnabled(jproj, true);
        long start = System.currentTimeMillis();
        fullBuild(project.getFullPath());
        if (VERBOSE)
            System.out.println("Done with build after " + ((System.currentTimeMillis() - start) / 1000L) + " sec");
        expectingNoProblems();
        IPath projPath = jproj.getProject().getLocation();
        for (int i = 1; i <= FILES_TO_GENERATE; ++i) {
            // check that file was generated
            String genFileName = String.format(".apt_generated/generatedfilepackage/Generated%05d.java", i);
            File genFile = new File(projPath.append(genFileName).toOSString());
            assertTrue("Expected generated source file " + genFileName + " was not found", genFile != null && genFile.exists());
            // check that generated file was compiled
            String genClassName = String.format("bin/generatedfilepackage/Generated%05d.class", i);
            File genClass = new File(projPath.append(genClassName).toOSString());
            assertTrue("Compiled file " + genClassName + " was not found", genClass != null && genClass.exists());
        }
        if (VERBOSE)
            System.out.println("Done checking output");
        Util.delete(project);
    }
}
