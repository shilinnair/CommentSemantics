/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text;

public abstract class DocumentXMLNode implements IDocumentXMLNode {

    public  DocumentXMLNode() {
    // NO-OP
    }

    @Override
    public String toString() {
        return write();
    }

    public abstract String write();
}
