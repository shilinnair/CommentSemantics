/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.tool;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Used as a zip file cache.
 */
public class Archive {

    public static final Archive UNKNOWN_ARCHIVE = new Archive();

    ZipFile zipFile;

    File file;

    protected Hashtable<String, ArrayList<String>> packagesCache;

    private  Archive() {
    // used to construct UNKNOWN_ARCHIVE
    }

    public  Archive(File file) throws ZipException, IOException {
        this.file = file;
        this.zipFile = new ZipFile(file);
        initialize();
    }

    private void initialize() {
        // initialize packages
        this.packagesCache = new Hashtable();
        nextEntry: for (Enumeration<? extends ZipEntry> e = this.zipFile.entries(); e.hasMoreElements(); ) {
            String fileName = ((ZipEntry) e.nextElement()).getName();
            // add the package name & all of its parent packages
            int last = fileName.lastIndexOf('/');
            // extract the package name
            String packageName = fileName.substring(0, last + 1);
            String typeName = fileName.substring(last + 1);
            ArrayList<String> types = this.packagesCache.get(packageName);
            if (types == null) {
                // might be empty if this is a directory entry
                if (typeName.length() == 0) {
                    continue nextEntry;
                }
                types = new ArrayList();
                types.add(typeName);
                this.packagesCache.put(packageName, types);
            } else {
                types.add(typeName);
            }
        }
    }

    public ArchiveFileObject getArchiveFileObject(String entryName, Charset charset) {
        return new ArchiveFileObject(this.file, entryName, charset);
    }

    public boolean contains(String entryName) {
        return this.zipFile.getEntry(entryName) != null;
    }

    public Set<String> allPackages() {
        if (this.packagesCache == null) {
            this.initialize();
        }
        return this.packagesCache.keySet();
    }

    public List<String> getTypes(String packageName) {
        // package name is expected to ends with '/'
        if (this.packagesCache == null) {
            try {
                this.zipFile = new ZipFile(this.file);
            } catch (IOException e) {
                return Collections.<String>emptyList();
            }
            this.initialize();
        }
        return this.packagesCache.get(packageName);
    }

    public void flush() {
        this.packagesCache = null;
    }

    public void close() {
        try {
            if (this.zipFile != null) {
                this.zipFile.close();
            }
            this.packagesCache = null;
        } catch (IOException e) {
        }
    }

    @Override
    public String toString() {
        //$NON-NLS-1$ //$NON-NLS-2$
        return "Archive: " + (this.file == null ? "UNKNOWN_ARCHIVE" : this.file.getAbsolutePath());
    }
}
