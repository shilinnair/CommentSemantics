/**
 * Copyright (c) 2006 Ecliptical Software Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 */
package org.eclipse.ecf.pubsub;

public interface IPublishedServiceDirectory {

    // TODO initial state currently delivered as an ADD event during listener registration
    // -- should there be a more explicit initial state delivery?
    void addReplicatedServiceListener(IPublishedServiceDirectoryListener listener);

    void removeReplicatedServiceListener(IPublishedServiceDirectoryListener listener);
}
