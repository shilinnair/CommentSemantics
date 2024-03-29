/*******************************************************************************
 * Copyright (c) 2004 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.tutorial;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class Activator implements BundleActivator {

    //The shared instance.
    private static Activator plugin;

    private BundleContext context = null;

    /**
	 * The constructor.
	 */
    public  Activator() {
        plugin = this;
    }

    /**
	 * This method is called upon plug-in activation
	 */
    public void start(BundleContext context) throws Exception {
        this.context = context;
    }

    public Bundle getBundle() {
        if (context == null)
            return null;
        else
            return context.getBundle();
    }

    /**
	 * This method is called when the plug-in is stopped
	 */
    public void stop(BundleContext context) throws Exception {
        this.context = null;
        plugin = null;
    }

    /**
	 * Returns the shared instance.
	 *
	 * @return the shared instance.
	 */
    public static Activator getDefault() {
        return plugin;
    }
}
