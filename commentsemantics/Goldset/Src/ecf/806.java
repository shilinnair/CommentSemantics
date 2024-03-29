/*******************************************************************************
 * Copyright (c) 2004 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.filetransfer.events;

import org.eclipse.ecf.filetransfer.IIncomingFileTransfer;

/**
 * Super interface for incoming file transfer events
 * 
 */
public interface IIncomingFileTransferEvent extends IFileTransferEvent {

    /**
	 * Get {@link IIncomingFileTransfer} associated with this event
	 * 
	 * @return IIncomingFileTransfer that is source of this event. Will not be
	 *         <code>null</code>.
	 */
    public IIncomingFileTransfer getSource();
}
