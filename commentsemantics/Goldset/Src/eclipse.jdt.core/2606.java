/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     alex.panchenko@gmail.com - Bug 470535 - Bug in UserLibrary.hashCode()
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.util.Messages;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Internal model element to represent a user library and code to serialize / deserialize.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class UserLibrary {

    //$NON-NLS-1$
    private static final String VERSION_ONE = "1";

    //$NON-NLS-1$
    private static final String CURRENT_VERSION = "2";

    //$NON-NLS-1$
    private static final String TAG_VERSION = "version";

    //$NON-NLS-1$
    private static final String TAG_USERLIBRARY = "userlibrary";

    //$NON-NLS-1$
    private static final String TAG_SOURCEATTACHMENT = "sourceattachment";

    //$NON-NLS-1$
    private static final String TAG_SOURCEATTACHMENTROOT = "sourceattachmentroot";

    //$NON-NLS-1$
    private static final String TAG_PATH = "path";

    //$NON-NLS-1$
    private static final String TAG_ARCHIVE = "archive";

    //$NON-NLS-1$
    private static final String TAG_SYSTEMLIBRARY = "systemlibrary";

    private boolean isSystemLibrary;

    private IClasspathEntry[] entries;

    public  UserLibrary(IClasspathEntry[] entries, boolean isSystemLibrary) {
        Assert.isNotNull(entries);
        this.entries = entries;
        this.isSystemLibrary = isSystemLibrary;
    }

    public IClasspathEntry[] getEntries() {
        return this.entries;
    }

    public boolean isSystemLibrary() {
        return this.isSystemLibrary;
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass() == getClass()) {
            UserLibrary other = (UserLibrary) obj;
            if (this.entries.length == other.entries.length && this.isSystemLibrary == other.isSystemLibrary) {
                for (int i = 0; i < this.entries.length; i++) {
                    if (!this.entries[i].equals(other.entries[i])) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
    public int hashCode() {
        int hashCode = 0;
        if (this.isSystemLibrary) {
            hashCode++;
        }
        for (int i = 0; i < this.entries.length; i++) {
            hashCode = hashCode * 17 + this.entries[i].hashCode();
        }
        return hashCode;
    }

    public static String serialize(IClasspathEntry[] entries, boolean isSystemLibrary) throws IOException {
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        //$NON-NLS-1$
        OutputStreamWriter writer = new OutputStreamWriter(s, "UTF8");
        XMLWriter xmlWriter = new XMLWriter(writer, /*use the workspace line delimiter*/
        null, /*print XML version*/
        true);
        HashMap library = new HashMap();
        library.put(TAG_VERSION, String.valueOf(CURRENT_VERSION));
        library.put(TAG_SYSTEMLIBRARY, String.valueOf(isSystemLibrary));
        xmlWriter.printTag(TAG_USERLIBRARY, library, true, true, false);
        for (int i = 0, length = entries.length; i < length; ++i) {
            ClasspathEntry cpEntry = (ClasspathEntry) entries[i];
            HashMap archive = new HashMap();
            archive.put(TAG_PATH, cpEntry.getPath().toPortableString());
            IPath sourceAttach = cpEntry.getSourceAttachmentPath();
            if (sourceAttach != null)
                archive.put(TAG_SOURCEATTACHMENT, sourceAttach.toPortableString());
            IPath sourceAttachRoot = cpEntry.getSourceAttachmentRootPath();
            if (sourceAttachRoot != null)
                archive.put(TAG_SOURCEATTACHMENTROOT, sourceAttachRoot.toPortableString());
            boolean hasExtraAttributes = cpEntry.extraAttributes != null && cpEntry.extraAttributes.length != 0;
            // access rule set is null if no access rules
            boolean hasRestrictions = cpEntry.getAccessRuleSet() != null;
            xmlWriter.printTag(TAG_ARCHIVE, archive, true, true, !(hasExtraAttributes || hasRestrictions));
            // write extra attributes if necessary
            if (hasExtraAttributes) {
                cpEntry.encodeExtraAttributes(xmlWriter, true, true);
            }
            // write extra attributes and restriction if necessary
            if (hasRestrictions) {
                cpEntry.encodeAccessRules(xmlWriter, true, true);
            }
            // write archive end tag if necessary
            if (hasExtraAttributes || hasRestrictions) {
                xmlWriter.endTag(TAG_ARCHIVE, /*insert tab*/
                true, /*insert new line*/
                true);
            }
        }
        xmlWriter.endTag(TAG_USERLIBRARY, /*insert tab*/
        true, /*insert new line*/
        true);
        writer.flush();
        writer.close();
        //$NON-NLS-1$
        return s.toString("UTF8");
    }

    public static UserLibrary createFromString(Reader reader) throws IOException {
        Element cpElement;
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            cpElement = parser.parse(new InputSource(reader)).getDocumentElement();
        } catch (SAXException e) {
            throw new IOException(Messages.file_badFormat);
        } catch (ParserConfigurationException e) {
            throw new IOException(Messages.file_badFormat);
        } finally {
            reader.close();
        }
        if (!cpElement.getNodeName().equalsIgnoreCase(TAG_USERLIBRARY)) {
            throw new IOException(Messages.file_badFormat);
        }
        String version = cpElement.getAttribute(TAG_VERSION);
        boolean isSystem = Boolean.valueOf(cpElement.getAttribute(TAG_SYSTEMLIBRARY)).booleanValue();
        NodeList list = cpElement.getChildNodes();
        int length = list.getLength();
        ArrayList res = new ArrayList(length);
        for (int i = 0; i < length; ++i) {
            Node node = list.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (element.getNodeName().equals(TAG_ARCHIVE)) {
                    String pathString = element.getAttribute(TAG_PATH);
                    String sourceAttachString = element.hasAttribute(TAG_SOURCEATTACHMENT) ? element.getAttribute(TAG_SOURCEATTACHMENT) : null;
                    String sourceAttachRootString = element.hasAttribute(TAG_SOURCEATTACHMENTROOT) ? element.getAttribute(TAG_SOURCEATTACHMENTROOT) : null;
                    IPath entryPath = null;
                    IPath sourceAttachPath = null;
                    IPath sourceAttachRootPath = null;
                    if (version.equals(VERSION_ONE)) {
                        entryPath = Path.fromOSString(pathString);
                        if (sourceAttachString != null)
                            sourceAttachPath = Path.fromOSString(sourceAttachString);
                        if (sourceAttachRootString != null)
                            sourceAttachRootPath = Path.fromOSString(sourceAttachRootString);
                    } else {
                        entryPath = Path.fromPortableString(pathString);
                        if (sourceAttachString != null)
                            sourceAttachPath = Path.fromPortableString(sourceAttachString);
                        if (sourceAttachRootString != null)
                            sourceAttachRootPath = Path.fromPortableString(sourceAttachRootString);
                    }
                    NodeList children = //$NON-NLS-1$
                    element.getElementsByTagName(//$NON-NLS-1$
                    "*");
                    boolean[] foundChildren = new boolean[children.getLength()];
                    NodeList attributeList = ClasspathEntry.getChildAttributes(ClasspathEntry.TAG_ATTRIBUTES, children, foundChildren);
                    IClasspathAttribute[] extraAttributes = ClasspathEntry.decodeExtraAttributes(attributeList);
                    attributeList = ClasspathEntry.getChildAttributes(ClasspathEntry.TAG_ACCESS_RULES, children, foundChildren);
                    IAccessRule[] accessRules = ClasspathEntry.decodeAccessRules(attributeList);
                    IClasspathEntry entry = JavaCore.newLibraryEntry(entryPath, sourceAttachPath, sourceAttachRootPath, accessRules, extraAttributes, /*not exported*/
                    false);
                    res.add(entry);
                }
            }
        }
        IClasspathEntry[] entries = (IClasspathEntry[]) res.toArray(new IClasspathEntry[res.size()]);
        return new UserLibrary(entries, isSystem);
    }

    public String toString() {
        if (this.entries == null)
            //$NON-NLS-1$
            return "null";
        StringBuffer buffer = new StringBuffer();
        int length = this.entries.length;
        for (int i = 0; i < length; i++) {
            buffer.append(this.entries[i].toString() + '\n');
        }
        return buffer.toString();
    }
}
