/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.sun.jdi.event;

import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;

/**
 * See http://docs.oracle.com/javase/6/docs/jdk/api/jpda/jdi/com/sun/jdi/event/WatchpointEvent.html
 */
public interface WatchpointEvent extends LocatableEvent {

    public Field field();

    public ObjectReference object();

    public Value valueCurrent();
}
