/*******************************************************************************
 *  Copyright (c) 2005, 2007 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.rcp;

import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.pde.ui.templates.NewPluginTemplateWizard;

public class ViewRCPNewWizard extends NewPluginTemplateWizard {

    @Override
    public void init(IFieldData data) {
        super.init(data);
        setWindowTitle(PDETemplateMessages.ViewRCPNewWizard_title);
    }

    @Override
    public ITemplateSection[] createTemplateSections() {
        return new ITemplateSection[] { new ViewRCPTemplate() };
    }
}
