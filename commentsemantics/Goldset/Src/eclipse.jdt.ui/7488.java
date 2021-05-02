/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import java.io.File;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * <code>CodeTemplates</code> gives access to the available code templates.
 * @since 3.0
 * @deprecated use {@link org.eclipse.jdt.internal.ui.JavaPlugin#getCodeTemplateStore()} instead
 */
@Deprecated
public class CodeTemplates extends org.eclipse.jdt.internal.corext.template.java.TemplateSet {

    //$NON-NLS-1$
    private static final String TEMPLATE_FILE = "codetemplates.xml";

    /** Singleton. */
    private static CodeTemplates fgTemplates;

    public static Template getCodeTemplate(String name) {
        return getInstance().getFirstTemplate(name);
    }

    /**
	 * Returns an instance of templates.
	 * @return an instance of templates
	 */
    public static CodeTemplates getInstance() {
        if (fgTemplates == null)
            fgTemplates = new CodeTemplates();
        return fgTemplates;
    }

    private  CodeTemplates() {
        //$NON-NLS-1$
        super("codetemplate", JavaPlugin.getDefault().getCodeTemplateContextRegistry());
        create();
    }

    private void create() {
        try {
            File templateFile = getTemplateFile();
            if (templateFile.exists()) {
                addFromFile(templateFile, false);
            }
        } catch (CoreException e) {
            JavaPlugin.log(e);
            clear();
        }
    }

    /**
	 * Resets the template set.
	 * @throws CoreException
	 */
    public void reset() throws CoreException {
    }

    /**
	 * Resets the template set with the default templates.
	 * @throws CoreException
	 */
    public void restoreDefaults() throws CoreException {
    }

    /**
	 * Saves the template set.
	 * @throws CoreException
	 */
    public void save() throws CoreException {
    }

    private static File getTemplateFile() {
        IPath path = JavaPlugin.getDefault().getStateLocation();
        path = path.append(TEMPLATE_FILE);
        return path.toFile();
    }
}
