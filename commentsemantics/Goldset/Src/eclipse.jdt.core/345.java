/*******************************************************************************
 * Copyright (c) 2005, 2009 BEA Systems, Inc. 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    jgarms@bea.com - initial API and implementation
 *    
 *******************************************************************************/
package org.eclipse.jdt.apt.core.internal.env;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.apt.core.internal.AptPlugin;
import org.eclipse.jdt.apt.core.internal.util.FileSystemUtil;

/**
 * Wrap output operations, caching them in memory,
 * then writing them out at the end if the content
 * is different than what is on disk
 */
public class BinaryFileOutputStream extends ByteArrayOutputStream {

    protected final IFile _file;

    private final BuildEnv _env;

    public  BinaryFileOutputStream(IFile file, BuildEnv env) {
        _file = file;
        _env = env;
    }

    @Override
    public void close() throws IOException {
        super.close();
        InputStream contents = new ByteArrayInputStream(toByteArray());
        try {
            boolean contentsChanged = true;
            if (!_file.exists()) {
                saveToDisk(contents, true);
            } else {
                InputStream in = null;
                InputStream oldData = null;
                try {
                    // Only write the contents if the data is different
                    in = new ByteArrayInputStream(toByteArray());
                    oldData = new BufferedInputStream(_file.getContents());
                    if (FileSystemUtil.compareStreams(in, oldData)) {
                        contentsChanged = false;
                    }
                } catch (CoreException ce) {
                    contentsChanged = true;
                } finally {
                    closeInputStream(in);
                    closeInputStream(oldData);
                }
                if (contentsChanged) {
                    contents.reset();
                    saveToDisk(contents, false);
                }
            }
        } finally {
            closeInputStream(contents);
        }
        IFile parentFile = _env.getFile();
        if (parentFile != null) {
            _env.getAptProject().getGeneratedFileManager().addGeneratedFileDependency(Collections.singleton(parentFile), _file);
            _env.addGeneratedNonSourceFile(_file);
        }
    }

    private void closeInputStream(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ioe) {
            }
        }
    }

    private void saveToDisk(InputStream toSave, boolean create) throws IOException {
        try {
            FileSystemUtil.makeDerivedParentFolders(_file.getParent());
            if (create) {
                _file.create(toSave, IResource.FORCE | IResource.DERIVED, null);
            } else {
                _file.setContents(toSave, true, false, null);
            }
        } catch (CoreException ce) {
            if (_file.exists()) {
            } else {
                AptPlugin.log(ce, "Could not create generated file");
                throw new IOException(ce.getMessage());
            }
        }
    }
}
