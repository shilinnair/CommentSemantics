/****************************************************************************
 * Copyright (c) 2007 Composent, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *****************************************************************************/
package org.eclipse.ecf.tests.sharedobject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.Assert;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.sharedobject.BaseSharedObject;
import org.eclipse.ecf.core.sharedobject.ReplicaSharedObjectDescription;
import org.eclipse.ecf.core.sharedobject.SharedObjectInitException;
import org.eclipse.ecf.core.sharedobject.SharedObjectMsg;
import org.eclipse.ecf.core.sharedobject.events.ISharedObjectActivatedEvent;
import org.eclipse.ecf.core.util.Event;
import org.eclipse.ecf.core.util.IEventProcessor;

/**
 *
 */
public class TestMessagingSharedObject extends BaseSharedObject implements IMessageSender {

    public static final String NAME_PROPERTY = "name";

    String name;

    IMessageReceiver messageReceiver;

    /**
	 * Primary constructor
	 * @param name the name to say hello to
	 */
    public  TestMessagingSharedObject(String name, IMessageReceiver receiver) {
        this.name = name;
        Assert.isNotNull(name);
        this.messageReceiver = receiver;
    }

    /**
	 * Replica constructor (null constructor)
	 */
    public  TestMessagingSharedObject() {
        super();
    }

    /* (non-Javadoc)
	 * @see org.eclipse.ecf.core.sharedobject.BaseSharedObject#initialize()
	 */
    protected void initialize() throws SharedObjectInitException {
        super.initialize();
        if (isPrimary()) {
            // If primary, then add an event processor that handles activated
            // event by replicating to all current remote containers
            addEventProcessor(new IEventProcessor() {

                public boolean processEvent(Event event) {
                    if (event instanceof ISharedObjectActivatedEvent) {
                        ISharedObjectActivatedEvent ae = (ISharedObjectActivatedEvent) event;
                        if (ae.getActivatedID().equals(getID()) && isConnected()) {
                            TestMessagingSharedObject.this.replicateToRemoteContainers(null);
                        }
                    }
                    return false;
                }
            });
            System.out.println("Primary(" + getContext().getLocalContainerID() + ") says Hello " + name);
        } else {
            // This is a replica, so initialize the name from property
            name = (String) getConfig().getProperties().get(NAME_PROPERTY);
            System.out.println("Replica(" + getContext().getLocalContainerID() + ") says Hello " + name);
        }
    }

    /* (non-Javadoc)
	 * @see org.eclipse.ecf.core.sharedobject.BaseSharedObject#getReplicaDescription(org.eclipse.ecf.core.identity.ID)
	 */
    protected ReplicaSharedObjectDescription getReplicaDescription(ID receiver) {
        // Put primary state into properties and include in replica description
        final Map properties = new HashMap();
        properties.put(NAME_PROPERTY, name);
        return new ReplicaSharedObjectDescription(this.getClass(), getConfig().getSharedObjectID(), getConfig().getHomeContainerID(), properties);
    }

    protected void handleMessage(ID fromID, String message) {
        if (messageReceiver != null)
            messageReceiver.handleMessage(fromID, message);
        else
            System.out.println(name + " says " + message);
    }

    protected boolean handleSharedObjectMsg(SharedObjectMsg msg) {
        try {
            msg.invoke(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public void sendMessage(ID targetId, Object message) throws IOException {
        sendSharedObjectMsgTo(null, SharedObjectMsg.createMsg(this.getClass().getName(), "handleMessage", new Object[] { getLocalContainerID(), message }));
    }
}
