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
package org.eclipse.jdt.internal.core.search.indexing;

import java.io.IOException;
import java.net.URI;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.core.index.Index;
import org.eclipse.jdt.internal.core.search.processing.JobManager;
import org.eclipse.jdt.internal.core.util.Util;

public class IndexBinaryFolder extends IndexRequest {

    IContainer folder;

    public  IndexBinaryFolder(IContainer folder, IndexManager manager) {
        super(folder.getFullPath(), manager);
        this.folder = folder;
    }

    public boolean equals(Object o) {
        if (o instanceof IndexBinaryFolder)
            return this.folder.equals(((IndexBinaryFolder) o).folder);
        return false;
    }

    /**
	 * Ensure consistency of a folder index. Need to walk all nested resources,
	 * and discover resources which have either been changed, added or deleted
	 * since the index was produced.
	 */
    public boolean execute(IProgressMonitor progressMonitor) {
        if (this.isCancelled || progressMonitor != null && progressMonitor.isCanceled())
            return true;
        // nothing to do
        if (!this.folder.isAccessible())
            return true;
        Index index = this.manager.getIndexForUpdate(this.containerPath, /*reuse index file*/
        true, /*create if none*/
        true);
        if (index == null)
            return true;
        ReadWriteMonitor monitor = index.monitor;
        // index got deleted since acquired
        if (monitor == null)
            return true;
        try {
            // ask permission to read
            monitor.enterRead();
            // all file names //$NON-NLS-1$
            String[] paths = index.queryDocumentNames("");
            int max = paths == null ? 0 : paths.length;
            final SimpleLookupTable indexedFileNames = new SimpleLookupTable(max == 0 ? 33 : max + 11);
            //$NON-NLS-1$
            final String OK = "OK";
            //$NON-NLS-1$
            final String DELETED = "DELETED";
            if (paths == null) {
                this.folder.accept(new IResourceProxyVisitor() {

                    public boolean visit(IResourceProxy proxy) {
                        if (IndexBinaryFolder.this.isCancelled)
                            return false;
                        if (proxy.getType() == IResource.FILE) {
                            if (org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(proxy.getName())) {
                                IFile file = (IFile) proxy.requestResource();
                                String containerRelativePath = Util.relativePath(file.getFullPath(), IndexBinaryFolder.this.containerPath.segmentCount());
                                indexedFileNames.put(containerRelativePath, file);
                            }
                            return false;
                        }
                        return true;
                    }
                }, IResource.NONE);
            } else {
                for (int i = 0; i < max; i++) {
                    indexedFileNames.put(paths[i], DELETED);
                }
                final long indexLastModified = index.getIndexLastModified();
                this.folder.accept(new IResourceProxyVisitor() {

                    public boolean visit(IResourceProxy proxy) throws CoreException {
                        if (IndexBinaryFolder.this.isCancelled)
                            return false;
                        if (proxy.getType() == IResource.FILE) {
                            if (org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(proxy.getName())) {
                                IFile file = (IFile) proxy.requestResource();
                                URI location = file.getLocationURI();
                                if (location != null) {
                                    String containerRelativePath = Util.relativePath(file.getFullPath(), IndexBinaryFolder.this.containerPath.segmentCount());
                                    indexedFileNames.put(containerRelativePath, indexedFileNames.get(containerRelativePath) == null || indexLastModified < EFS.getStore(location).fetchInfo().getLastModified() ? (Object) file : (Object) OK);
                                }
                            }
                            return false;
                        }
                        return true;
                    }
                }, IResource.NONE);
            }
            Object[] names = indexedFileNames.keyTable;
            Object[] values = indexedFileNames.valueTable;
            for (int i = 0, length = names.length; i < length; i++) {
                String name = (String) names[i];
                if (name != null) {
                    if (this.isCancelled)
                        return false;
                    Object value = values[i];
                    if (value != OK) {
                        if (value == DELETED)
                            this.manager.remove(name, this.containerPath);
                        else {
                            this.manager.addBinary((IFile) value, this.containerPath);
                        }
                    }
                }
            }
            // request to save index when all class files have been indexed... also sets state to SAVED_STATE
            this.manager.request(new SaveIndex(this.containerPath, this.manager));
        } catch (CoreException e) {
            if (JobManager.VERBOSE) {
                Util.verbose("-> failed to index " + this.folder + " because of the following exception:", System.err);
                e.printStackTrace();
            }
            this.manager.removeIndex(this.containerPath);
            return false;
        } catch (IOException e) {
            if (JobManager.VERBOSE) {
                Util.verbose("-> failed to index " + this.folder + " because of the following exception:", System.err);
                e.printStackTrace();
            }
            this.manager.removeIndex(this.containerPath);
            return false;
        } finally {
            // free read lock
            monitor.exitRead();
        }
        return true;
    }

    public int hashCode() {
        return this.folder.hashCode();
    }

    protected Integer updatedIndexState() {
        return IndexManager.REBUILDING_STATE;
    }

    public String toString() {
        //$NON-NLS-1$
        return "indexing binary folder " + this.folder.getFullPath();
    }
}
