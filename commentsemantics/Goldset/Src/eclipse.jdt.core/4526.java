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
package org.eclipse.jdt.internal.core.search;

import java.io.IOException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.index.FileIndexLocation;
import org.eclipse.jdt.internal.core.index.Index;
import org.eclipse.jdt.internal.core.index.IndexLocation;
import org.eclipse.jdt.internal.core.search.indexing.ReadWriteMonitor;
import org.eclipse.jdt.internal.core.search.matching.MatchLocator;
import org.eclipse.jdt.internal.core.search.processing.IJob;
import org.eclipse.jdt.internal.core.search.processing.JobManager;
import org.eclipse.jdt.internal.core.util.Util;

public class PatternSearchJob implements IJob {

    protected SearchPattern pattern;

    protected IJavaSearchScope scope;

    protected SearchParticipant participant;

    protected IndexQueryRequestor requestor;

    protected boolean areIndexesReady;

    protected long executionTime = 0;

    public  PatternSearchJob(SearchPattern pattern, SearchParticipant participant, IJavaSearchScope scope, IndexQueryRequestor requestor) {
        this.pattern = pattern;
        this.participant = participant;
        this.scope = scope;
        this.requestor = requestor;
    }

    public boolean belongsTo(String jobFamily) {
        return true;
    }

    public void cancel() {
    // search job is cancelled through progress
    }

    public void ensureReadyToRun() {
        if (!this.areIndexesReady)
            // may trigger some index recreation
            getIndexes(/*progress*/
            null);
    }

    public boolean execute(IProgressMonitor progressMonitor) {
        SubMonitor subMonitor = SubMonitor.convert(progressMonitor, 3);
        boolean isComplete = COMPLETE;
        this.executionTime = 0;
        Index[] indexes = getIndexes(subMonitor.split(1));
        try {
            int max = indexes.length;
            SubMonitor loopMonitor = subMonitor.split(2).setWorkRemaining(max);
            for (int i = 0; i < max; i++) {
                isComplete &= search(indexes[i], loopMonitor.split(1));
            }
            if (JobManager.VERBOSE)
                //$NON-NLS-1$//$NON-NLS-2$
                Util.verbose("-> execution time: " + this.executionTime + "ms - " + this);
            return isComplete;
        } finally {
            SubMonitor.done(progressMonitor);
        }
    }

    public Index[] getIndexes(IProgressMonitor progressMonitor) {
        // acquire the in-memory indexes on the fly
        IndexLocation[] indexLocations;
        int length;
        if (this.participant instanceof JavaSearchParticipant) {
            indexLocations = ((JavaSearchParticipant) this.participant).selectIndexURLs(this.pattern, this.scope);
            length = indexLocations.length;
        } else {
            IPath[] paths = this.participant.selectIndexes(this.pattern, this.scope);
            length = paths.length;
            indexLocations = new IndexLocation[paths.length];
            for (int i = 0, len = paths.length; i < len; i++) {
                indexLocations[i] = new FileIndexLocation(paths[i].toFile(), true);
            }
        }
        Index[] indexes = JavaModelManager.getIndexManager().getIndexes(indexLocations, progressMonitor);
        this.areIndexesReady = indexes.length == length;
        return indexes;
    }

    public String getJobFamily() {
        //$NON-NLS-1$
        return "";
    }

    public boolean search(Index index, IProgressMonitor progressMonitor) {
        if (index == null)
            return COMPLETE;
        if (progressMonitor != null && progressMonitor.isCanceled())
            throw new OperationCanceledException();
        ReadWriteMonitor monitor = index.monitor;
        // index got deleted since acquired
        if (monitor == null)
            return COMPLETE;
        try {
            // ask permission to read
            monitor.enterRead();
            long start = System.currentTimeMillis();
            MatchLocator.findIndexMatches(this.pattern, index, this.requestor, this.participant, this.scope, progressMonitor);
            this.executionTime += System.currentTimeMillis() - start;
            return COMPLETE;
        } catch (IOException e) {
            if (e instanceof java.io.EOFException)
                e.printStackTrace();
            return FAILED;
        } finally {
            // finished reading
            monitor.exitRead();
        }
    }

    public String toString() {
        //$NON-NLS-1$
        return "searching " + this.pattern.toString();
    }
}
