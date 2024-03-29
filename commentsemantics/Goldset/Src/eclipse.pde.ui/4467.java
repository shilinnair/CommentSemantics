/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.plugin;

import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;

public class FragmentModel extends PluginModelBase implements IFragmentModel {

    public  FragmentModel(IDocument document, boolean isReconciling) {
        super(document, isReconciling);
    }

    @Override
    public IFragment getFragment() {
        return (IFragment) getPluginBase();
    }

    @Override
    public boolean isFragmentModel() {
        return true;
    }

    @Override
    public BundleDescription getBundleDescription() {
        return null;
    }

    @Override
    public void setBundleDescription(BundleDescription description) {
    }
}
