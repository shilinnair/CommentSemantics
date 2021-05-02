/*******************************************************************************
 *  Copyright (c) 2004, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import org.w3c.dom.*;

public class XMLPrintHandler {

    //	used to print XML file
    //$NON-NLS-1$
    public static final String XML_COMMENT_END_TAG = "-->";

    //$NON-NLS-1$
    public static final String XML_COMMENT_BEGIN_TAG = "<!--";

    //$NON-NLS-1$
    public static final String XML_HEAD = "<?xml version=\"1.0\" encoding=\"";

    //$NON-NLS-1$
    public static final String XML_HEAD_END_TAG = "?>";

    //$NON-NLS-1$
    public static final String XML_DBL_QUOTES = "\"";

    //$NON-NLS-1$
    public static final String XML_SPACE = " ";

    //$NON-NLS-1$
    public static final String XML_BEGIN_TAG = "<";

    //$NON-NLS-1$
    public static final String XML_END_TAG = ">";

    //$NON-NLS-1$
    public static final String XML_EQUAL = "=";

    //$NON-NLS-1$
    public static final String XML_SLASH = "/";

    //$NON-NLS-1$
    public static final String XML_INDENT = "   ";

    public static String generateIndent(int level) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < level; i++) {
            buffer.append(XML_INDENT);
        }
        return buffer.toString();
    }

    public static void printBeginElement(Writer xmlWriter, String elementString, String indent, boolean terminate) throws IOException {
        StringBuffer temp = new StringBuffer(indent);
        temp.append(XML_BEGIN_TAG);
        temp.append(elementString);
        if (terminate)
            temp.append(XML_SLASH);
        temp.append(XML_END_TAG);
        //$NON-NLS-1$
        temp.append("\n");
        xmlWriter.write(temp.toString());
    }

    public static void printEndElement(Writer xmlWriter, String elementString, String indent) throws IOException {
        StringBuffer temp = new StringBuffer(indent);
        temp.append(XML_BEGIN_TAG);
        //$NON-NLS-1$
        temp.append(XML_SLASH).append(elementString).append(XML_END_TAG).append("\n");
        xmlWriter.write(temp.toString());
    }

    public static void printText(Writer xmlWriter, String text, String indent) throws IOException {
        StringBuffer temp = new StringBuffer(indent);
        temp.append(encode(text).toString());
        //$NON-NLS-1$
        temp.append("\n");
        xmlWriter.write(temp.toString());
    }

    public static void printComment(Writer xmlWriter, String comment, String indent) throws IOException {
        //$NON-NLS-1$
        StringBuffer temp = new StringBuffer("\n");
        temp.append(indent);
        temp.append(XML_COMMENT_BEGIN_TAG);
        //$NON-NLS-1$
        temp.append(encode(comment).toString()).append(XML_COMMENT_END_TAG).append("\n\n");
        xmlWriter.write(temp.toString());
    }

    public static void printHead(Writer xmlWriter, String encoding) throws IOException {
        StringBuffer temp = new StringBuffer(XML_HEAD);
        //$NON-NLS-1$
        temp.append(encoding).append(XML_DBL_QUOTES).append(XML_HEAD_END_TAG).append("\n");
        xmlWriter.write(temp.toString());
    }

    public static String wrapAttributeForPrint(String attribute, String value) {
        StringBuffer temp = new StringBuffer(XML_SPACE);
        temp.append(attribute).append(XML_EQUAL).append(XML_DBL_QUOTES).append(encode(value).toString()).append(XML_DBL_QUOTES);
        return temp.toString();
    }

    public static String wrapAttribute(String attribute, String value) {
        StringBuffer buffer = new StringBuffer(XML_SPACE);
        buffer.append(attribute);
        buffer.append(XML_EQUAL);
        buffer.append(XML_DBL_QUOTES);
        buffer.append(value);
        buffer.append(XML_DBL_QUOTES);
        return buffer.toString();
    }

    public static void printNode(Writer xmlWriter, Node node, String encoding, String indent) throws IOException {
        if (node == null) {
            return;
        }
        switch(node.getNodeType()) {
            case Node.DOCUMENT_NODE:
                {
                    printHead(xmlWriter, encoding);
                    printNode(xmlWriter, ((Document) node).getDocumentElement(), encoding, indent);
                    break;
                }
            case Node.ELEMENT_NODE:
                {
                    //get the attribute list for this node.
                    StringBuffer tempElementString = new StringBuffer(node.getNodeName());
                    NamedNodeMap attributeList = node.getAttributes();
                    if (attributeList != null) {
                        for (int i = 0; i < attributeList.getLength(); i++) {
                            Node attribute = attributeList.item(i);
                            tempElementString.append(wrapAttributeForPrint(attribute.getNodeName(), attribute.getNodeValue()));
                        }
                    }
                    // do this recursively for the child nodes.
                    NodeList childNodes = node.getChildNodes();
                    int length = childNodes.getLength();
                    printBeginElement(xmlWriter, tempElementString.toString(), indent, length == 0);
                    for (int i = 0; i < length; i++) printNode(//$NON-NLS-1$
                    xmlWriter, //$NON-NLS-1$
                    childNodes.item(i), //$NON-NLS-1$
                    encoding, //$NON-NLS-1$
                    indent + "\t");
                    if (length > 0)
                        printEndElement(xmlWriter, node.getNodeName(), indent);
                    break;
                }
            case Node.TEXT_NODE:
                {
                    xmlWriter.write(encode(node.getNodeValue()).toString());
                    break;
                }
            default:
                {
                    throw new //$NON-NLS-1$
                    UnsupportedOperationException(//$NON-NLS-1$
                    "Unsupported XML Node Type.");
                }
        }
    }

    public static StringBuffer encode(String value) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch(c) {
                case '&':
                    //$NON-NLS-1$
                    buf.append(//$NON-NLS-1$
                    "&amp;");
                    break;
                case '<':
                    //$NON-NLS-1$
                    buf.append(//$NON-NLS-1$
                    "&lt;");
                    break;
                case '>':
                    //$NON-NLS-1$
                    buf.append(//$NON-NLS-1$
                    "&gt;");
                    break;
                case '\'':
                    //$NON-NLS-1$
                    buf.append(//$NON-NLS-1$
                    "&apos;");
                    break;
                case '\"':
                    //$NON-NLS-1$
                    buf.append(//$NON-NLS-1$
                    "&quot;");
                    break;
                default:
                    buf.append(c);
                    break;
            }
        }
        return buf;
    }

    public static void writeFile(Document doc, File file) throws IOException {
        Writer writer = null;
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            //$NON-NLS-1$
            writer = new OutputStreamWriter(out, "UTF-8");
            //$NON-NLS-1$ //$NON-NLS-2$
            XMLPrintHandler.printNode(writer, doc, "UTF-8", "");
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException e1) {
            }
            try {
                if (out != null)
                    out.close();
            } catch (IOException e1) {
            }
        }
    }
}
