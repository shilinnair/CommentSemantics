/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.text.edits.TextEdit;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IDocumentKey;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.core.util.PropertiesUtil;
import org.eclipse.pde.internal.ui.editor.JarEntryEditorInput;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.ManifestDocumentSetupParticipant;
import org.eclipse.pde.internal.ui.editor.context.UTF8InputContext;
import org.eclipse.text.edits.*;
import org.eclipse.ui.*;

public class BundleInputContext extends UTF8InputContext {

    //$NON-NLS-1$
    public static final String CONTEXT_ID = "bundle-context";

    private HashMap<IDocumentKey, TextEdit> fOperationTable = new HashMap();

    /**
	 * @param editor
	 * @param input
	 */
    public  BundleInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
        super(editor, input, primary);
        create();
    }

    @Override
    protected IBaseModel createModel(IEditorInput input) throws CoreException {
        BundleModel model = null;
        boolean isReconciling = input instanceof IFileEditorInput;
        IDocument document = getDocumentProvider().getDocument(input);
        model = new BundleModel(document, isReconciling);
        if (input instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) input).getFile();
            model.setUnderlyingResource(file);
            model.setCharset(file.getCharset());
        } else if (input instanceof IURIEditorInput) {
            IFileStore store = EFS.getStore(((IURIEditorInput) input).getURI());
            model.setInstallLocation(store.getParent().getParent().toString());
            model.setCharset(getDefaultCharset());
        } else if (input instanceof JarEntryEditorInput) {
            File file = (File) ((JarEntryEditorInput) input).getAdapter(File.class);
            model.setInstallLocation(file.toString());
            model.setCharset(getDefaultCharset());
        } else {
            model.setCharset(getDefaultCharset());
        }
        model.load();
        return model;
    }

    @Override
    public String getId() {
        return CONTEXT_ID;
    }

    @Override
    protected void addTextEditOperation(ArrayList<TextEdit> ops, IModelChangedEvent event) {
        Object[] objects = event.getChangedObjects();
        if (objects != null) {
            for (int i = 0; i < objects.length; i++) {
                Object object = objects[i];
                if (object instanceof PDEManifestElement)
                    object = ((PDEManifestElement) object).getHeader();
                else if (object instanceof PackageFriend)
                    object = ((PackageFriend) object).getHeader();
                if (object instanceof ManifestHeader) {
                    ManifestHeader header = (ManifestHeader) object;
                    TextEdit op = fOperationTable.get(header);
                    if (op != null) {
                        fOperationTable.remove(header);
                        ops.remove(op);
                    }
                    String value = header.getValue();
                    if (value == null || value.trim().length() == 0) {
                        deleteKey(header, ops);
                    } else {
                        modifyKey(header, ops);
                    }
                }
            }
        }
    }

    protected TextEdit[] getMoveOperations() {
        return new TextEdit[0];
    }

    private void insertKey(IDocumentKey key, ArrayList<TextEdit> ops) {
        IDocument doc = getDocumentProvider().getDocument(getInput());
        InsertEdit op = new InsertEdit(PropertiesUtil.getInsertOffset(doc), key.write());
        fOperationTable.put(key, op);
        ops.add(op);
    }

    private void deleteKey(IDocumentKey key, ArrayList<TextEdit> ops) {
        if (key.getOffset() > 0) {
            TextEdit op = new DeleteEdit(key.getOffset(), key.getLength());
            fOperationTable.put(key, op);
            ops.add(op);
        }
    }

    private void modifyKey(IDocumentKey key, ArrayList<TextEdit> ops) {
        if (key.getOffset() == -1) {
            insertKey(key, ops);
        } else {
            TextEdit op = new ReplaceEdit(key.getOffset(), key.getLength(), key.write());
            fOperationTable.put(key, op);
            ops.add(op);
        }
    }

    @Override
    public void doRevert() {
        fEditOperations.clear();
        fOperationTable.clear();
        AbstractEditingModel model = (AbstractEditingModel) getModel();
        model.reconciled(model.getDocument());
    }

    @Override
    protected String getPartitionName() {
        //$NON-NLS-1$
        return "___bundle_partition";
    }

    @Override
    protected IDocumentSetupParticipant getDocumentSetupParticipant() {
        return new ManifestDocumentSetupParticipant();
    }
}
