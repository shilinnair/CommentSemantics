/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search.indexing;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipError;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.index.Index;
import org.eclipse.jdt.internal.core.index.IndexLocation;
import org.eclipse.jdt.internal.core.search.JavaSearchDocument;
import org.eclipse.jdt.internal.core.search.processing.JobManager;

@SuppressWarnings("rawtypes")
class AddJarFileToIndex extends IndexRequest {

    private static final char JAR_SEPARATOR = IJavaSearchScope.JAR_FILE_ENTRY_SEPARATOR.charAt(0);

    IFile resource;

    Scanner scanner;

    private IndexLocation indexFileURL;

    private final boolean forceIndexUpdate;

    public  AddJarFileToIndex(IFile resource, IndexLocation indexFile, IndexManager manager) {
        this(resource, indexFile, manager, false);
    }

    public  AddJarFileToIndex(IFile resource, IndexLocation indexFile, IndexManager manager, final boolean updateIndex) {
        super(resource.getFullPath(), manager);
        this.resource = resource;
        this.indexFileURL = indexFile;
        this.forceIndexUpdate = updateIndex;
    }

    public  AddJarFileToIndex(IPath jarPath, IndexLocation indexFile, IndexManager manager) {
        this(jarPath, indexFile, manager, false);
    }

    public  AddJarFileToIndex(IPath jarPath, IndexLocation indexFile, IndexManager manager, final boolean updateIndex) {
        // external JAR scenario - no resource
        super(jarPath, manager);
        this.indexFileURL = indexFile;
        this.forceIndexUpdate = updateIndex;
    }

    public boolean equals(Object o) {
        if (o instanceof AddJarFileToIndex) {
            if (this.resource != null)
                return this.resource.equals(((AddJarFileToIndex) o).resource);
            if (this.containerPath != null)
                return this.containerPath.equals(((AddJarFileToIndex) o).containerPath);
        }
        return false;
    }

    public int hashCode() {
        if (this.resource != null)
            return this.resource.hashCode();
        if (this.containerPath != null)
            return this.containerPath.hashCode();
        return -1;
    }

    public boolean execute(IProgressMonitor progressMonitor) {
        if (this.isCancelled || progressMonitor != null && progressMonitor.isCanceled())
            return true;
        if (hasPreBuiltIndex()) {
            boolean added = this.manager.addIndex(this.containerPath, this.indexFileURL);
            if (added)
                return true;
            this.indexFileURL = null;
        }
        try {
            // if index is already cached, then do not perform any check
            // MUST reset the IndexManager if a jar file is changed
            Index index = this.manager.getIndexForUpdate(this.containerPath, /*do not reuse index file*/
            false, /*do not create if none*/
            false);
            if (index != null) {
                if (JobManager.VERBOSE)
                    org.eclipse.jdt.internal.core.util.Util.verbose("-> no indexing required (index already exists) for " + //$NON-NLS-1$
                    this.containerPath);
                return true;
            }
            index = this.manager.getIndexForUpdate(this.containerPath, /*reuse index file*/
            true, /*create if none*/
            true);
            if (index == null) {
                if (JobManager.VERBOSE)
                    org.eclipse.jdt.internal.core.util.Util.verbose("-> index could not be created for " + //$NON-NLS-1$
                    this.containerPath);
                return true;
            }
            ReadWriteMonitor monitor = index.monitor;
            if (monitor == null) {
                if (JobManager.VERBOSE)
                    //$NON-NLS-1$//$NON-NLS-2$
                    org.eclipse.jdt.internal.core.util.Util.verbose("-> index for " + this.containerPath + " just got deleted");
                // index got deleted since acquired
                return true;
            }
            index.separator = JAR_SEPARATOR;
            ZipFile zip = null;
            try {
                // this path will be a relative path to the workspace in case the zipfile in the workspace otherwise it will be a path in the
                // local file system
                Path zipFilePath = null;
                // ask permission to write
                monitor.enterWrite();
                if (this.resource != null) {
                    URI location = this.resource.getLocationURI();
                    if (location == null)
                        return false;
                    if (JavaModelManager.ZIP_ACCESS_VERBOSE)
                        //$NON-NLS-1$	//$NON-NLS-2$
                        System.out.println("(" + Thread.currentThread() + ") [AddJarFileToIndex.execute()] Creating ZipFile on " + location.getPath());
                    File file = null;
                    try {
                        file = org.eclipse.jdt.internal.core.util.Util.toLocalFile(location, progressMonitor);
                    } catch (CoreException e) {
                        if (JobManager.VERBOSE) {
                            org.eclipse.jdt.internal.core.util.Util.verbose("-> failed to index " + location.getPath() + " because of the following exception:");
                            e.printStackTrace();
                        }
                    }
                    if (file == null) {
                        if (JobManager.VERBOSE)
                            //$NON-NLS-1$ //$NON-NLS-2$
                            org.eclipse.jdt.internal.core.util.Util.verbose("-> failed to index " + location.getPath() + " because the file could not be fetched");
                        return false;
                    }
                    zip = new ZipFile(file);
                    zipFilePath = (Path) this.resource.getFullPath().makeRelative();
                // absolute path relative to the workspace
                } else {
                    if (JavaModelManager.ZIP_ACCESS_VERBOSE)
                        //$NON-NLS-1$	//$NON-NLS-2$
                        System.out.println("(" + Thread.currentThread() + ") [AddJarFileToIndex.execute()] Creating ZipFile on " + this.containerPath);
                    // external file -> it is ok to use toFile()
                    zip = new ZipFile(this.containerPath.toFile());
                    zipFilePath = (Path) this.containerPath;
                // path is already canonical since coming from a library classpath entry
                }
                if (this.isCancelled) {
                    if (JobManager.VERBOSE)
                        //$NON-NLS-1$ //$NON-NLS-2$
                        org.eclipse.jdt.internal.core.util.Util.verbose("-> indexing of " + zip.getName() + " has been cancelled");
                    return false;
                }
                if (JobManager.VERBOSE)
                    org.eclipse.jdt.internal.core.util.Util.verbose(//$NON-NLS-1$
                    "-> indexing " + //$NON-NLS-1$
                    zip.getName());
                long initialTime = System.currentTimeMillis();
                // all file names //$NON-NLS-1$
                String[] paths = index.queryDocumentNames("");
                if (paths != null) {
                    int max = paths.length;
                    /* check integrity of the existing index file
					 * if the length is equal to 0, we want to index the whole jar again
					 * If not, then we want to check that there is no missing entry, if
					 * one entry is missing then we recreate the index
					 */
                    //$NON-NLS-1$
                    String //$NON-NLS-1$
                    EXISTS = "OK";
                    String //$NON-NLS-1$
                    DELETED = //$NON-NLS-1$
                    "DELETED";
                    SimpleLookupTable indexedFileNames = new SimpleLookupTable(max == 0 ? 33 : max + 11);
                    for (int i = 0; i < max; i++) indexedFileNames.put(paths[i], DELETED);
                    for (Enumeration e = zip.entries(); e.hasMoreElements(); ) {
                        // iterate each entry to index it
                        ZipEntry ze = (ZipEntry) e.nextElement();
                        String zipEntryName = ze.getName();
                        if (Util.isClassFileName(zipEntryName) && isValidPackageNameForClass(zipEntryName))
                            // the class file may not be there if the package name is not valid
                            indexedFileNames.put(zipEntryName, EXISTS);
                    }
                    // a new file was added
                    boolean needToReindex = indexedFileNames.elementSize != max;
                    if (!needToReindex) {
                        Object[] valueTable = indexedFileNames.valueTable;
                        for (int i = 0, l = valueTable.length; i < l; i++) {
                            if (valueTable[i] == DELETED) {
                                // a file was deleted so re-index
                                needToReindex = true;
                                break;
                            }
                        }
                        if (!needToReindex) {
                            if (JobManager.VERBOSE)
                                org.eclipse.jdt.internal.core.util.Util.verbose("-> no indexing required (index is consistent with library) for " + //$NON-NLS-1$
                                zip.getName() + " (" + (//$NON-NLS-1$
                                System.currentTimeMillis() - //$NON-NLS-1$
                                initialTime) + "ms)");
                            // to ensure its placed into the saved state
                            this.manager.saveIndex(index);
                            return true;
                        }
                    }
                }
                // Index the jar for the first time or reindex the jar in case the previous index file has been corrupted
                // index already existed: recreate it so that we forget about previous entries
                SearchParticipant participant = SearchEngine.getDefaultSearchParticipant();
                if (!this.manager.resetIndex(this.containerPath)) {
                    // failed to recreate index, see 73330
                    this.manager.removeIndex(this.containerPath);
                    return false;
                }
                index.separator = JAR_SEPARATOR;
                IPath indexPath = null;
                IndexLocation indexLocation;
                if ((indexLocation = index.getIndexLocation()) != null) {
                    indexPath = new Path(indexLocation.getCanonicalFilePath());
                }
                for (Enumeration e = zip.entries(); e.hasMoreElements(); ) {
                    if (this.isCancelled) {
                        if (JobManager.VERBOSE)
                            //$NON-NLS-1$ //$NON-NLS-2$
                            org.eclipse.jdt.internal.core.util.Util.verbose("-> indexing of " + zip.getName() + " has been cancelled");
                        return false;
                    }
                    // iterate each entry to index it
                    ZipEntry ze = (ZipEntry) e.nextElement();
                    String zipEntryName = ze.getName();
                    if (Util.isClassFileName(zipEntryName) && isValidPackageNameForClass(zipEntryName)) {
                        // index only classes coming from valid packages - https://bugs.eclipse.org/bugs/show_bug.cgi?id=293861
                        final byte[] classFileBytes = org.eclipse.jdt.internal.compiler.util.Util.getZipEntryByteContent(ze, zip);
                        JavaSearchDocument entryDocument = new JavaSearchDocument(ze, zipFilePath, classFileBytes, participant);
                        this.manager.indexDocument(entryDocument, participant, index, indexPath);
                    }
                }
                if (this.forceIndexUpdate) {
                    this.manager.savePreBuiltIndex(index);
                } else {
                    this.manager.saveIndex(index);
                }
                if (JobManager.VERBOSE)
                    org.eclipse.jdt.internal.core.util.Util.verbose("-> done indexing of " + //$NON-NLS-1$
                    zip.getName() + " (" + //$NON-NLS-1$
                    (System.currentTimeMillis() - initialTime) + //$NON-NLS-1$
                    "ms)");
            } finally {
                if (zip != null) {
                    if (JavaModelManager.ZIP_ACCESS_VERBOSE)
                        //$NON-NLS-1$	//$NON-NLS-2$
                        System.out.println("(" + Thread.currentThread() + ") [AddJarFileToIndex.execute()] Closing ZipFile " + zip);
                    zip.close();
                }
                // free write lock
                monitor.exitWrite();
            }
        } catch (IOException e) {
            if (JobManager.VERBOSE) {
                org.eclipse.jdt.internal.core.util.Util.verbose("-> failed to index " + this.containerPath + " because of the following exception:");
                e.printStackTrace();
            }
            this.manager.removeIndex(this.containerPath);
            return false;
        } catch (ZipError // merge with the code above using '|' when we move to 1.7
        e) {
            if (JobManager.VERBOSE) {
                org.eclipse.jdt.internal.core.util.Util.verbose("-> failed to index " + this.containerPath + " because of the following exception:");
                e.printStackTrace();
            }
            this.manager.removeIndex(this.containerPath);
            return false;
        }
        return true;
    }

    public String getJobFamily() {
        if (this.resource != null)
            return super.getJobFamily();
        // external jar
        return this.containerPath.toOSString();
    }

    private boolean isIdentifier() throws InvalidInputException {
        switch(this.scanner.scanIdentifier()) {
            // in 1.7 mode, which are in 1.3.
            case TerminalTokens.TokenNameIdentifier:
            case TerminalTokens.TokenNameassert:
            case TerminalTokens.TokenNameenum:
                return true;
            default:
                return false;
        }
    }

    private boolean isValidPackageNameForClass(String className) {
        char[] classNameArray = className.toCharArray();
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=376673
        if (this.scanner == null)
            this.scanner = new Scanner(/* comment */
            false, /* whitespace */
            true, /* nls */
            false, ClassFileConstants.JDK1_7, /* taskTag */
            null, /* taskPriorities */
            null, /* taskCaseSensitive */
            true);
        this.scanner.setSource(classNameArray);
        this.scanner.eofPosition = classNameArray.length - SuffixConstants.SUFFIX_CLASS.length;
        try {
            if (isIdentifier()) {
                while (this.scanner.eofPosition > this.scanner.currentPosition) {
                    if (this.scanner.getNextChar() != '/' || this.scanner.eofPosition <= this.scanner.currentPosition) {
                        return false;
                    }
                    if (!isIdentifier())
                        return false;
                }
                return true;
            }
        } catch (InvalidInputException e) {
        }
        return false;
    }

    protected Integer updatedIndexState() {
        Integer updateState = null;
        if (hasPreBuiltIndex()) {
            updateState = IndexManager.REUSE_STATE;
        } else {
            updateState = IndexManager.REBUILDING_STATE;
        }
        return updateState;
    }

    public String toString() {
        //$NON-NLS-1$
        return "indexing " + this.containerPath.toString();
    }

    protected boolean hasPreBuiltIndex() {
        return !this.forceIndexUpdate && (this.indexFileURL != null && this.indexFileURL.exists());
    }
}
