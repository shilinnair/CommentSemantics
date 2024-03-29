/*******************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.core.events;

import org.eclipse.ecf.core.identity.ID;

/**
 * Container disconnecting event
 * 
 */
public interface IContainerDisconnectingEvent extends IContainerEvent {

    /**
	 * Get ID of container target (the container we are disconnecting from)
	 * 
	 * @return ID the ID of the container we are disconnecting from. Will not be
	 *         null.
	 */
    public ID getTargetID();
}
