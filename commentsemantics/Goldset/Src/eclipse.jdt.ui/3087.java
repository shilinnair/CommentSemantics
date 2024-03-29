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
package org.eclipse.jdt.core.refactoring;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.resources.IFile;
import org.eclipse.text.edits.UndoEdit;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.ContentStamp;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;

/**
 * A {@link TextFileChange} that operates on an {@link ICompilationUnit} in the workspace.
 * 
 * @since 1.3
 */
public class CompilationUnitChange extends TextFileChange {

    private final ICompilationUnit fCUnit;

    /** The (optional) refactoring descriptor */
    private ChangeDescriptor fDescriptor;

    /**
	 * Creates a new <code>CompilationUnitChange</code>.
	 *
	 * @param name the change's name, mainly used to render the change in the UI
	 * @param cunit the compilation unit this change works on
	 */
    public  CompilationUnitChange(String name, ICompilationUnit cunit) {
        super(name, getFile(cunit));
        Assert.isNotNull(cunit);
        fCUnit = cunit;
        //$NON-NLS-1$
        setTextType("java");
    }

    private static IFile getFile(ICompilationUnit cunit) {
        return (IFile) cunit.getResource();
    }

    @Override
    public Object getModifiedElement() {
        return fCUnit;
    }

    /**
	 * Returns the compilation unit this change works on.
	 *
	 * @return the compilation unit this change works on
	 */
    public ICompilationUnit getCompilationUnit() {
        return fCUnit;
    }

    @Override
    protected IDocument acquireDocument(IProgressMonitor pm) throws CoreException {
        //$NON-NLS-1$
        pm.beginTask("", 2);
        fCUnit.becomeWorkingCopy(new SubProgressMonitor(pm, 1));
        Assert.isTrue(fCUnit.isWorkingCopy(), fCUnit.toString());
        return super.acquireDocument(new SubProgressMonitor(pm, 1));
    }

    @Override
    protected void releaseDocument(IDocument document, IProgressMonitor pm) throws CoreException {
        boolean isModified = isDocumentModified();
        super.releaseDocument(document, pm);
        try {
            fCUnit.discardWorkingCopy();
        } finally {
            if (isModified && !isDocumentAcquired()) {
                if (fCUnit.isWorkingCopy())
                    fCUnit.reconcile(ICompilationUnit.NO_AST, /* don't force problem detection */
                    false, /* use primary owner */
                    null, /* no progress monitor */
                    null);
                else
                    fCUnit.makeConsistent(pm);
            }
        }
    }

    @Override
    protected Change createUndoChange(UndoEdit edit, ContentStamp stampToRestore) {
        try {
            return new UndoCompilationUnitChange(getName(), fCUnit, edit, stampToRestore, getSaveMode());
        } catch (CoreException e) {
            JavaManipulationPlugin.log(e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (ICompilationUnit.class.equals(adapter))
            return (T) fCUnit;
        return super.getAdapter(adapter);
    }

    /**
	 * Sets the refactoring descriptor for this change.
	 *
	 * @param descriptor the descriptor to set, or <code>null</code> to set no descriptor
	 */
    public void setDescriptor(ChangeDescriptor descriptor) {
        fDescriptor = descriptor;
    }

    @Override
    public ChangeDescriptor getDescriptor() {
        return fDescriptor;
    }
}
