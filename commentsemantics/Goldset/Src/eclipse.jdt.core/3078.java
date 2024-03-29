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
package org.eclipse.jdt.internal.core.search.matching;

import java.util.Iterator;
import java.util.Map;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.builder.ClasspathLocation;
import org.eclipse.jdt.internal.core.util.ResourceCompilationUnit;
import org.eclipse.jdt.internal.core.util.Util;

@SuppressWarnings("rawtypes")
public class ClasspathSourceDirectory extends ClasspathLocation {

    IContainer sourceFolder;

    SimpleLookupTable directoryCache;

    SimpleLookupTable missingPackageHolder = new SimpleLookupTable();

    char[][] fullExclusionPatternChars;

    char[][] fulInclusionPatternChars;

     ClasspathSourceDirectory(IContainer sourceFolder, char[][] fullExclusionPatternChars, char[][] fulInclusionPatternChars) {
        this.sourceFolder = sourceFolder;
        this.directoryCache = new SimpleLookupTable(5);
        this.fullExclusionPatternChars = fullExclusionPatternChars;
        this.fulInclusionPatternChars = fulInclusionPatternChars;
    }

    public void cleanup() {
        this.directoryCache = null;
    }

    SimpleLookupTable directoryTable(String qualifiedPackageName) {
        SimpleLookupTable dirTable = (SimpleLookupTable) this.directoryCache.get(qualifiedPackageName);
        // package exists in another classpath directory or jar
        if (dirTable == this.missingPackageHolder)
            return null;
        if (dirTable != null)
            return dirTable;
        try {
            // this is a case-sensitive check
            IResource container = this.sourceFolder.findMember(qualifiedPackageName);
            if (container instanceof IContainer) {
                IResource[] members = ((IContainer) container).members();
                dirTable = new SimpleLookupTable();
                for (int i = 0, l = members.length; i < l; i++) {
                    IResource m = members[i];
                    String name;
                    if (m.getType() == IResource.FILE) {
                        int index = Util.indexOfJavaLikeExtension(name = m.getName());
                        if (index >= 0) {
                            String fullPath = m.getFullPath().toString();
                            if (!org.eclipse.jdt.internal.compiler.util.Util.isExcluded(fullPath.toCharArray(), this.fulInclusionPatternChars, this.fullExclusionPatternChars, /*not a folder path*/
                            false)) {
                                dirTable.put(name.substring(0, index), m);
                            }
                        }
                    }
                }
                // look for secondary types, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=382778
                IJavaProject project = JavaCore.create(container.getProject());
                Map secondaryTypePaths = JavaModelManager.getJavaModelManager().secondaryTypes(project, false, null);
                if (secondaryTypePaths.size() > 0) {
                    Map typesInPackage = (Map) secondaryTypePaths.get(qualifiedPackageName.replace('/', '.'));
                    if (typesInPackage != null && typesInPackage.size() > 0) {
                        for (Iterator j = typesInPackage.keySet().iterator(); j.hasNext(); ) {
                            String secondaryTypeName = (String) j.next();
                            IType secondaryType = (IType) typesInPackage.get(secondaryTypeName);
                            IJavaElement parent = secondaryType.getParent();
                            String fullPath = parent.getResource().getFullPath().toString();
                            if (!org.eclipse.jdt.internal.compiler.util.Util.isExcluded(fullPath.toCharArray(), this.fulInclusionPatternChars, this.fullExclusionPatternChars, /*not a folder path*/
                            false)) {
                                dirTable.put(secondaryTypeName, parent.getResource());
                            }
                        }
                    }
                }
                this.directoryCache.put(qualifiedPackageName, dirTable);
                return dirTable;
            }
        } catch (CoreException ignored) {
        }
        this.directoryCache.put(qualifiedPackageName, this.missingPackageHolder);
        return null;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ClasspathSourceDirectory))
            return false;
        return this.sourceFolder.equals(((ClasspathSourceDirectory) o).sourceFolder);
    }

    public NameEnvironmentAnswer findClass(String sourceFileWithoutExtension, String qualifiedPackageName, String qualifiedSourceFileWithoutExtension) {
        SimpleLookupTable dirTable = directoryTable(qualifiedPackageName);
        if (dirTable != null && dirTable.elementSize > 0) {
            IFile file = (IFile) dirTable.get(sourceFileWithoutExtension);
            if (file != null) {
                return new NameEnvironmentAnswer(new ResourceCompilationUnit(file), /* no access restriction */
                null);
            }
        }
        return null;
    }

    public IPath getProjectRelativePath() {
        return this.sourceFolder.getProjectRelativePath();
    }

    public int hashCode() {
        return this.sourceFolder == null ? super.hashCode() : this.sourceFolder.hashCode();
    }

    public boolean isPackage(String qualifiedPackageName) {
        return directoryTable(qualifiedPackageName) != null;
    }

    public void reset() {
        this.directoryCache = new SimpleLookupTable(5);
    }

    public String toString() {
        //$NON-NLS-1$
        return "Source classpath directory " + this.sourceFolder.getFullPath().toString();
    }

    public String debugPathString() {
        return this.sourceFolder.getFullPath().toString();
    }
}
