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
package org.eclipse.jdt.internal.compiler.apt.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;

/**
 * Implementation of a Java file object that corresponds to an entry in a zip/jar file
 */
public class ArchiveFileObject implements JavaFileObject {

    private String entryName;

    private File file;

    private ZipFile zipFile;

    private Charset charset;

    public  ArchiveFileObject(File file, String entryName, Charset charset) {
        this.entryName = entryName;
        this.file = file;
        this.charset = charset;
    }

    @Override
    protected void finalize() throws Throwable {
        if (this.zipFile != null) {
            try {
                this.zipFile.close();
            } catch (IOException e) {
            }
        }
        super.finalize();
    }

    /* (non-Javadoc)
	 * @see javax.tools.JavaFileObject#getAccessLevel()
	 */
    @Override
    public Modifier getAccessLevel() {
        // cannot express multiple modifier
        if (getKind() != Kind.CLASS) {
            return null;
        }
        ClassFileReader reader = null;
        try {
            try (ZipFile zip = new ZipFile(this.file)) {
                reader = ClassFileReader.read(zip, this.entryName);
            }
        } catch (ClassFormatException e) {
        } catch (IOException e) {
        }
        if (reader == null) {
            return null;
        }
        final int accessFlags = reader.accessFlags();
        if ((accessFlags & ClassFileConstants.AccPublic) != 0) {
            return Modifier.PUBLIC;
        }
        if ((accessFlags & ClassFileConstants.AccAbstract) != 0) {
            return Modifier.ABSTRACT;
        }
        if ((accessFlags & ClassFileConstants.AccFinal) != 0) {
            return Modifier.FINAL;
        }
        return null;
    }

    /* (non-Javadoc)
	 * @see javax.tools.JavaFileObject#getKind()
	 */
    @Override
    public Kind getKind() {
        String name = this.entryName.toLowerCase();
        if (name.endsWith(Kind.CLASS.extension)) {
            return Kind.CLASS;
        } else if (name.endsWith(Kind.SOURCE.extension)) {
            return Kind.SOURCE;
        } else if (name.endsWith(Kind.HTML.extension)) {
            return Kind.HTML;
        }
        return Kind.OTHER;
    }

    /* (non-Javadoc)
	 * @see javax.tools.JavaFileObject#getNestingKind()
	 */
    @Override
    public NestingKind getNestingKind() {
        switch(getKind()) {
            case SOURCE:
                return NestingKind.TOP_LEVEL;
            case CLASS:
                ClassFileReader reader = null;
                try {
                    try (ZipFile zip = new ZipFile(this.file)) {
                        reader = ClassFileReader.read(zip, this.entryName);
                    }
                } catch (ClassFormatException e) {
                } catch (IOException e) {
                }
                if (reader == null) {
                    return null;
                }
                if (reader.isAnonymous()) {
                    return NestingKind.ANONYMOUS;
                }
                if (reader.isLocal()) {
                    return NestingKind.LOCAL;
                }
                if (reader.isMember()) {
                    return NestingKind.MEMBER;
                }
                return NestingKind.TOP_LEVEL;
            default:
                return null;
        }
    }

    /* (non-Javadoc)
	 * @see javax.tools.JavaFileObject#isNameCompatible(java.lang.String, javax.tools.JavaFileObject.Kind)
	 */
    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        return this.entryName.endsWith(simpleName + kind.extension);
    }

    /* (non-Javadoc)
	 * @see javax.tools.FileObject#delete()
	 */
    @Override
    public boolean delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ArchiveFileObject)) {
            return false;
        }
        ArchiveFileObject archiveFileObject = (ArchiveFileObject) o;
        return archiveFileObject.toUri().equals(this.toUri());
    }

    @Override
    public int hashCode() {
        return this.toUri().hashCode();
    }

    /* (non-Javadoc)
	 * @see javax.tools.FileObject#getCharContent(boolean)
	 */
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        if (getKind() == Kind.SOURCE) {
            try (ZipFile zipFile2 = new ZipFile(this.file)) {
                ZipEntry zipEntry = zipFile2.getEntry(this.entryName);
                return Util.getCharContents(this, ignoreEncodingErrors, org.eclipse.jdt.internal.compiler.util.Util.getZipEntryByteContent(zipEntry, zipFile2), this.charset.name());
            }
        }
        return null;
    }

    /* (non-Javadoc)
	 * @see javax.tools.FileObject#getLastModified()
	 */
    @Override
    public long getLastModified() {
        try (ZipFile zip = new ZipFile(this.file)) {
            ZipEntry zipEntry = zip.getEntry(this.entryName);
            // looks the closest from the last modification
            return zipEntry.getTime();
        } catch (IOException e) {
        }
        return 0;
    }

    /* (non-Javadoc)
	 * @see javax.tools.FileObject#getName()
	 */
    @Override
    public String getName() {
        return this.entryName;
    }

    /* (non-Javadoc)
	 * @see javax.tools.FileObject#openInputStream()
	 */
    @Override
    public InputStream openInputStream() throws IOException {
        if (this.zipFile == null) {
            this.zipFile = new ZipFile(this.file);
        }
        ZipEntry zipEntry = this.zipFile.getEntry(this.entryName);
        return this.zipFile.getInputStream(zipEntry);
    }

    /* (non-Javadoc)
	 * @see javax.tools.FileObject#openOutputStream()
	 */
    @Override
    public OutputStream openOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
	 * @see javax.tools.FileObject#openReader(boolean)
	 */
    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
	 * @see javax.tools.FileObject#openWriter()
	 */
    @Override
    public Writer openWriter() throws IOException {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
	 * @see javax.tools.FileObject#toUri()
	 */
    @Override
    public URI toUri() {
        try {
            //$NON-NLS-1$//$NON-NLS-2$
            return new URI("jar:" + this.file.toURI().getPath() + "!" + this.entryName);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        //$NON-NLS-1$//$NON-NLS-2$
        return this.file.getAbsolutePath() + "[" + this.entryName + "]";
    }
}
