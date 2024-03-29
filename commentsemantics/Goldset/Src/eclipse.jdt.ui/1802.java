/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import java.util.Iterator;
import java.util.List;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

/**
 * Resolver for the <code>import</code> variable. Resolves to a
 * set of import statements.
 *
 * @since 3.4
 */
public class ImportsResolver extends TemplateVariableResolver {

    public  ImportsResolver(String type, String description) {
        super(type, description);
    }

    /**
	 * Default ctor for instantiation by the extension point.
	 */
    public  ImportsResolver() {
    }

    @Override
    public void resolve(TemplateVariable variable, TemplateContext context) {
        variable.setUnambiguous(true);
        //$NON-NLS-1$
        variable.setValue("");
        if (context instanceof JavaContext) {
            JavaContext jc = (JavaContext) context;
            List<String> params = variable.getVariableType().getParams();
            if (params.size() > 0) {
                for (Iterator<String> iterator = params.iterator(); iterator.hasNext(); ) {
                    String typeName = iterator.next();
                    jc.addImport(typeName);
                }
            }
        } else {
            super.resolve(variable, context);
        }
    }

    @Override
    protected String[] resolveAll(TemplateContext context) {
        return new String[0];
    }
}
