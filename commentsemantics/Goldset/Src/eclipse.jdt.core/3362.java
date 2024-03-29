/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.dom.rewrite;

/**
 *
 */
public class NodeRewriteEvent extends RewriteEvent {

    private Object originalValue;

    private Object newValue;

    public  NodeRewriteEvent(Object originalValue, Object newValue) {
        this.originalValue = originalValue;
        this.newValue = newValue;
    }

    /**
	 * @return Returns the new value.
	 */
    public Object getNewValue() {
        return this.newValue;
    }

    /**
	 * @return Returns the original value.
	 */
    public Object getOriginalValue() {
        return this.originalValue;
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.dom.RewriteEvent#getChangeKind()
	 */
    public int getChangeKind() {
        if (this.originalValue == this.newValue) {
            return UNCHANGED;
        }
        if (this.originalValue == null) {
            return INSERTED;
        }
        if (this.newValue == null) {
            return REMOVED;
        }
        if (this.originalValue.equals(this.newValue)) {
            return UNCHANGED;
        }
        return REPLACED;
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.dom.RewriteEvent#isListRewrite()
	 */
    public boolean isListRewrite() {
        return false;
    }

    /*
	 * Sets a new value for the new node. Internal access only.
	 * @param newValue The new value to set.
	 */
    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.dom.RewriteEvent#getChildren()
	 */
    public RewriteEvent[] getChildren() {
        return null;
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        switch(getChangeKind()) {
            case INSERTED:
                //$NON-NLS-1$
                buf.append(" [inserted: ");
                buf.append(getNewValue());
                buf.append(']');
                break;
            case REPLACED:
                //$NON-NLS-1$
                buf.append(" [replaced: ");
                buf.append(getOriginalValue());
                //$NON-NLS-1$
                buf.append(" -> ");
                buf.append(getNewValue());
                buf.append(']');
                break;
            case REMOVED:
                //$NON-NLS-1$
                buf.append(" [removed: ");
                buf.append(getOriginalValue());
                buf.append(']');
                break;
            default:
                //$NON-NLS-1$
                buf.append(" [unchanged]");
        }
        return buf.toString();
    }
}
