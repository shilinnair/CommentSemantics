/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.resources.IResource;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.jdt.core.IJavaElement;

public class TestMoveParticipantSingle extends MoveParticipant {

    static List<TestMoveParticipantSingle> fgInstances = new ArrayList();

    private Object fElement;

    private String fHandle;

    @Override
    public boolean initialize(Object element) {
        fgInstances.add(this);
        fElement = element;
        if (fElement instanceof IJavaElement) {
            fHandle = ((IJavaElement) fElement).getHandleIdentifier();
        } else {
            fHandle = ((IResource) fElement).getFullPath().toString();
        }
        return true;
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) {
        return new RefactoringStatus();
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException {
        return null;
    }

    public static void testNumberOfInstances(int instances) {
        Assert.assertEquals(instances, fgInstances.size());
    }

    public static void testElements(String[] handles) {
        testNumberOfInstances(handles.length);
        List<String> l1 = new ArrayList(Arrays.asList(handles));
        for (int i = 0; i < l1.size(); i++) {
            Assert.assertTrue(l1.contains(getInstance(i).fHandle));
        }
    }

    public static void testArguments(MoveArguments[] args) {
        testNumberOfInstances(args.length);
        for (int i = 0; i < args.length; i++) {
            MoveArguments expected = args[i];
            MoveArguments actual = getInstance(i).getArguments();
            Assert.assertEquals(expected.getDestination(), actual.getDestination());
            Assert.assertEquals(expected.getUpdateReferences(), actual.getUpdateReferences());
        }
    }

    public static void reset() {
        fgInstances = new ArrayList();
    }

    private static TestMoveParticipantSingle getInstance(int i) {
        return fgInstances.get(i);
    }
}
