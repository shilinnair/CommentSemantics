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
package org.eclipse.jdt.core.tests.builder;

import java.util.Vector;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.IDebugRequestor;
import org.eclipse.jdt.internal.core.util.Util;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class EfficiencyCompilerRequestor implements IDebugRequestor {

    private boolean isActive = false;

    private Vector compiledClasses = new Vector(10);

    public void acceptDebugResult(CompilationResult result) {
        ClassFile[] classFiles = result.getClassFiles();
        Util.sort(classFiles, new Util.Comparer() {

            public int compare(Object a, Object b) {
                String aName = new String(((ClassFile) a).fileName());
                String bName = new String(((ClassFile) b).fileName());
                return aName.compareTo(bName);
            }
        });
        for (int i = 0; i < classFiles.length; i++) {
            String className = new String(classFiles[i].fileName());
            this.compiledClasses.addElement(className.replace('/', '.'));
        }
    }

    String[] getCompiledClasses() {
        return (String[]) this.compiledClasses.toArray(new String[0]);
    }

    public void clearResult() {
        this.compiledClasses.clear();
    }

    public void reset() {
    // do nothing by default
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public boolean isActive() {
        return this.isActive;
    }
}
