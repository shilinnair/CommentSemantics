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
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.HashMap;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.ClasspathAttributeConfiguration;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

public class ClasspathAttributeConfigurationDescriptors {

    private static class Descriptor {

        private IConfigurationElement fConfigElement;

        private ClasspathAttributeConfiguration fInstance;

        //$NON-NLS-1$
        private static final String ATT_NAME = "attributeName";

        //$NON-NLS-1$
        private static final String ATT_CLASS = "class";

        public  Descriptor(IConfigurationElement configElement) throws CoreException {
            fConfigElement = configElement;
            fInstance = null;
            String name = configElement.getAttribute(ATT_NAME);
            String pageClassName = configElement.getAttribute(ATT_CLASS);
            if (name == null) {
                throw new //$NON-NLS-1$
                CoreException(//$NON-NLS-1$
                new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, 0, "Invalid extension (missing attributeName)", null));
            }
            if (pageClassName == null) {
                throw new //$NON-NLS-1$
                CoreException(//$NON-NLS-1$
                new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, 0, "Invalid extension (missing class name): " + name, null));
            }
        }

        public ClasspathAttributeConfiguration getInstance() throws CoreException {
            if (fInstance == null) {
                Object elem = CoreUtility.createExtension(fConfigElement, ATT_CLASS);
                if (elem instanceof ClasspathAttributeConfiguration) {
                    fInstance = (ClasspathAttributeConfiguration) elem;
                } else {
                    throw new CoreException(new //$NON-NLS-1$
                    Status(//$NON-NLS-1$
                    IStatus.ERROR, //$NON-NLS-1$
                    JavaUI.ID_PLUGIN, //$NON-NLS-1$
                    0, //$NON-NLS-1$
                    "Invalid extension (page not of type IClasspathContainerPage): " + getKey(), //$NON-NLS-1$
                    null));
                }
            }
            return fInstance;
        }

        public String getKey() {
            return fConfigElement.getAttribute(ATT_NAME);
        }
    }

    //$NON-NLS-1$
    private static final String ATT_EXTENSION = "classpathAttributeConfiguration";

    private HashMap<String, Descriptor> fDescriptors;

    public  ClasspathAttributeConfigurationDescriptors() {
        fDescriptors = null;
    }

    private HashMap<String, Descriptor> getDescriptors() {
        if (fDescriptors == null) {
            fDescriptors = readExtensions();
        }
        return fDescriptors;
    }

    public boolean containsKey(String attributeKey) {
        return getDescriptors().containsKey(attributeKey);
    }

    public ClasspathAttributeConfiguration get(final String attributeKey) {
        final Descriptor desc = getDescriptors().get(attributeKey);
        if (desc == null) {
            return null;
        }
        final ClasspathAttributeConfiguration[] res = { null };
        SafeRunner.run(new ISafeRunnable() {

            @Override
            public void handleException(Throwable exception) {
                JavaPlugin.log(exception);
                // remove from list
                getDescriptors().remove(attributeKey);
            }

            @Override
            public void run() throws Exception {
                res[0] = desc.getInstance();
            }
        });
        return res[0];
    }

    private static HashMap<String, Descriptor> readExtensions() {
        IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(JavaUI.ID_PLUGIN, ATT_EXTENSION);
        HashMap<String, Descriptor> descriptors = new HashMap(elements.length * 2);
        for (int i = 0; i < elements.length; i++) {
            try {
                Descriptor curr = new Descriptor(elements[i]);
                descriptors.put(curr.getKey(), curr);
            } catch (CoreException e) {
                JavaPlugin.log(e);
            }
        }
        return descriptors;
    }
}
