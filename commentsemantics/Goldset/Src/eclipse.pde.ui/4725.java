/*******************************************************************************
 *  Copyright (c) 2005, 2014 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.views.log;

import java.io.*;

public class TailInputStream extends InputStream {

    private RandomAccessFile fRaf;

    private long fTail;

    public  TailInputStream(File file, long maxLength) throws IOException {
        super();
        fTail = maxLength;
        //$NON-NLS-1$
        fRaf = new RandomAccessFile(file, "r");
        skipHead(file);
    }

    private void skipHead(File file) throws IOException {
        if (file.length() > fTail) {
            fRaf.seek(file.length() - fTail);
            // skip bytes until a new line to be sure we start from a beginning of valid UTF-8 character
            int c = read();
            while (c != '\n' && c != '\r' && c != -1) {
                c = read();
            }
        }
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        int len = fRaf.read(b, 0, 1);
        if (len < 0) {
            return len;
        }
        return b[0];
    }

    @Override
    public int read(byte[] b) throws IOException {
        return fRaf.read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return fRaf.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        fRaf.close();
    }
}
