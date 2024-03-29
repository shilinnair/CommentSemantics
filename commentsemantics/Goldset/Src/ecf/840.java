/*******************************************************************************
 * Copyright (c) 2008 EclipseSource and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.core.status;

import org.eclipse.core.runtime.*;

/**
 * @since 3.2
 */
public class SerializableMultiStatus extends SerializableStatus {

    private static final long serialVersionUID = 2971900808938367039L;

    /**
	 * List of child statuses.
	 */
    private IStatus[] children = new IStatus[0];

    public  SerializableMultiStatus(IStatus status) {
        this(status.getPlugin(), status.getCode(), status.getMessage(), status.getException());
        IStatus[] childs = status.getChildren();
        if (childs != null) {
            for (int i = 0; i < childs.length; i++) {
                if (childs[i].isMultiStatus()) {
                    add(new SerializableMultiStatus(childs[i]));
                } else {
                    add(new SerializableStatus(childs[i]));
                }
            }
        }
    }

    public  SerializableMultiStatus(MultiStatus multiStatus) {
        this(multiStatus.getPlugin(), multiStatus.getCode(), multiStatus.getMessage(), multiStatus.getException());
        IStatus[] childs = multiStatus.getChildren();
        if (childs != null) {
            for (int i = 0; i < childs.length; i++) {
                if (childs[i].isMultiStatus()) {
                    add(new SerializableMultiStatus(childs[i]));
                } else {
                    add(new SerializableStatus(childs[i]));
                }
            }
        }
    }

    public  SerializableMultiStatus(String pluginId, int code, IStatus[] newChildren, String message, Throwable exception) {
        this(pluginId, code, message, exception);
        Assert.isLegal(newChildren != null);
        int maxSeverity = getSeverity();
        if (newChildren != null) {
            for (int i = 0; i < newChildren.length; i++) {
                Assert.isLegal(newChildren[i] != null);
                int severity = newChildren[i].getSeverity();
                if (severity > maxSeverity)
                    maxSeverity = severity;
            }
            this.children = new IStatus[newChildren.length];
            System.arraycopy(newChildren, 0, this.children, 0, newChildren.length);
        }
        setSeverity(maxSeverity);
    }

    public  SerializableMultiStatus(String pluginId, int code, String message, Throwable exception) {
        super(OK, pluginId, code, message, exception);
    }

    /**
	 * Adds the given status to this multi-status.
	 * 
	 * @param status
	 *            the new child status
	 */
    public void add(SerializableStatus status) {
        Assert.isLegal(status != null);
        IStatus[] result = new IStatus[children.length + 1];
        System.arraycopy(children, 0, result, 0, children.length);
        result[result.length - 1] = status;
        children = result;
        int newSev = status.getSeverity();
        if (newSev > getSeverity()) {
            setSeverity(newSev);
        }
    }

    /**
	 * Adds all of the children of the given status to this multi-status. Does
	 * nothing if the given status has no children (which includes the case
	 * where it is not a multi-status).
	 * 
	 * @param status
	 *            the status whose children are to be added to this one
	 */
    public void addAll(SerializableStatus status) {
        Assert.isLegal(status != null);
        SerializableStatus[] statuses = (SerializableStatus[]) status.getChildren();
        for (int i = 0; i < statuses.length; i++) {
            add(statuses[i]);
        }
    }

    /*
	 * (Intentionally not javadoc'd) Implements the corresponding method on
	 * <code>IStatus</code>.
	 */
    public IStatus[] getChildren() {
        return children;
    }

    /*
	 * (Intentionally not javadoc'd) Implements the corresponding method on
	 * <code>IStatus</code>.
	 */
    public boolean isMultiStatus() {
        return true;
    }

    /**
	 * Merges the given status into this multi-status. Equivalent to
	 * <code>add(status)</code> if the given status is not a multi-status.
	 * Equivalent to <code>addAll(status)</code> if the given status is a
	 * multi-status.
	 * 
	 * @param status
	 *            the status to merge into this one
	 * @see #add(SerializableStatus)
	 * @see #addAll(SerializableStatus)
	 */
    public void merge(SerializableStatus status) {
        Assert.isLegal(status != null);
        if (!status.isMultiStatus()) {
            add(status);
        } else {
            addAll(status);
        }
    }

    /**
	 * Returns a string representation of the status, suitable for debugging
	 * purposes only.
	 * @return String
	 */
    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());
        //$NON-NLS-1$
        buf.append(" children=[");
        for (int i = 0; i < children.length; i++) {
            if (i != 0) {
                //$NON-NLS-1$
                buf.append(//$NON-NLS-1$
                " ");
            }
            buf.append(children[i].toString());
        }
        //$NON-NLS-1$
        buf.append("]");
        return buf.toString();
    }
}
