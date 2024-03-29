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
package org.eclipse.ecf.internal.provider.jmdns;

import java.util.LinkedList;
import java.util.List;

public class SimpleFIFOQueue {

    private final List list;

    private boolean stopped;

    public  SimpleFIFOQueue() {
        list = new LinkedList();
        stopped = false;
    }

    public synchronized boolean enqueue(final Object obj) {
        if (isStopped() || obj == null) {
            return false;
        }
        // Add item to the list
        list.add(obj);
        // Notify waiting thread. Dequeue should only be read by one thread, so
        // only need
        // notify() rather than notifyAll().
        notify();
        return true;
    }

    public synchronized Object dequeue() {
        final Object val = peekQueue();
        if (val != null) {
            removeHead();
        }
        return val;
    }

    public synchronized Object peekQueue() {
        while (isEmpty()) {
            if (stopped)
                return null;
            try {
                wait();
            } catch (final Exception e) {
                return null;
            }
        }
        return list.get(0);
    }

    public synchronized Object peekQueue(final long waitMS) {
        if (waitMS == 0)
            return peekQueue();
        if (stopped) {
            return null;
        }
        try {
            wait(waitMS);
        } catch (final Exception e) {
            return null;
        }
        if (isEmpty())
            return null;
        return list.get(0);
    }

    public synchronized Object removeHead() {
        if (list.isEmpty())
            return null;
        return list.remove(0);
    }

    public synchronized boolean isEmpty() {
        return list.isEmpty();
    }

    public synchronized void stop() {
        stopped = true;
    }

    public synchronized boolean isStopped() {
        return stopped;
    }

    public synchronized int size() {
        return list.size();
    }

    public synchronized Object[] flush() {
        final Object[] out = list.toArray();
        list.clear();
        close();
        return out;
    }

    public synchronized void close() {
        stop();
        notifyAll();
    }

    public String toString() {
        //$NON-NLS-1$
        final StringBuffer sb = new StringBuffer("SimpleFIFOQueue[");
        //$NON-NLS-1$
        sb.append(list).append("]");
        return sb.toString();
    }
}
