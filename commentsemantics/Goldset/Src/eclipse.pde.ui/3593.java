/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 *
 */
public class ArchiveSection extends PDESection {

    private Table fTable;

    private TableViewer fViewer;

    private ISiteModel fModel;

    private Button fAddButton;

    private Button fEditButton;

    private Button fRemoveButton;

    class FolderProvider extends WorkbenchContentProvider {

        @Override
        public boolean hasChildren(Object element) {
            Object[] children = getChildren(element);
            for (int i = 0; i < children.length; i++) {
                if (children[i] instanceof IFolder) {
                    return true;
                }
            }
            return false;
        }
    }

    class ContentProvider implements IStructuredContentProvider {

        @Override
        public Object[] getElements(Object parent) {
            ISiteModel model = (ISiteModel) parent;
            return model.getSite().getArchives();
        }
    }

    class ArchiveLabelProvider extends LabelProvider implements ITableLabelProvider {

        @Override
        public String getColumnText(Object obj, int index) {
            ISiteArchive archive = (ISiteArchive) obj;
            switch(index) {
                case 0:
                    return archive.getPath();
                case 1:
                    return archive.getURL();
            }
            //$NON-NLS-1$
            return "";
        }

        @Override
        public Image getColumnImage(Object obj, int index) {
            return null;
        }
    }

    /**
	 * @param formPage
	 */
    public  ArchiveSection(PDEFormPage formPage, Composite parent) {
        super(formPage, parent, Section.DESCRIPTION);
        getSection().setText(PDEUIMessages.SiteEditor_ArchiveSection_header);
        getSection().setDescription(PDEUIMessages.SiteEditor_ArchiveSection_instruction);
        createClient(getSection(), formPage.getManagedForm().getToolkit());
    }

    @Override
    public void createClient(Section section, FormToolkit toolkit) {
        fModel = (ISiteModel) getPage().getModel();
        fModel.addModelChangedListener(this);
        section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
        Composite container = toolkit.createComposite(section);
        container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
        container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridData data = new GridData(GridData.FILL_BOTH);
        section.setLayoutData(data);
        createTable(container, toolkit);
        createTableViewer();
        createButtons(container, toolkit);
        toolkit.paintBordersFor(container);
        section.setClient(container);
        initialize();
    }

    @Override
    public void dispose() {
        fModel.removeModelChangedListener(this);
        super.dispose();
    }

    private void createButtons(Composite parent, FormToolkit toolkit) {
        Composite container = toolkit.createComposite(parent);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 10;
        container.setLayout(layout);
        container.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        fAddButton = toolkit.createButton(container, PDEUIMessages.SiteEditor_add, SWT.PUSH);
        fAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fAddButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                showDialog(null);
            }
        });
        fAddButton.setEnabled(isEditable());
        fEditButton = toolkit.createButton(container, PDEUIMessages.SiteEditor_edit, SWT.PUSH);
        fEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fEditButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection ssel = (IStructuredSelection) fViewer.getSelection();
                if (ssel != null && ssel.size() == 1)
                    showDialog((ISiteArchive) ssel.getFirstElement());
            }
        });
        fRemoveButton = toolkit.createButton(container, PDEUIMessages.SiteEditor_remove, SWT.PUSH);
        fRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fRemoveButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDelete();
            }
        });
        fRemoveButton.setEnabled(false);
        fEditButton.setEnabled(false);
        toolkit.paintBordersFor(container);
    }

    private void createTable(Composite container, FormToolkit toolkit) {
        fTable = toolkit.createTable(container, SWT.FULL_SELECTION);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 100;
        fTable.setLayoutData(gd);
        TableColumn col1 = new TableColumn(fTable, SWT.NULL);
        col1.setText(PDEUIMessages.SiteEditor_ArchiveSection_col1);
        TableColumn col2 = new TableColumn(fTable, SWT.NULL);
        col2.setText(PDEUIMessages.SiteEditor_ArchiveSection_col2);
        TableLayout tlayout = new TableLayout();
        tlayout.addColumnData(new ColumnWeightData(50, 200));
        tlayout.addColumnData(new ColumnWeightData(50, 200));
        fTable.setLayout(tlayout);
        fTable.setHeaderVisible(true);
        createContextMenu(fTable);
    }

    private void createTableViewer() {
        fViewer = new TableViewer(fTable);
        fViewer.setContentProvider(new ContentProvider());
        fViewer.setLabelProvider(new ArchiveLabelProvider());
        fViewer.setInput(getPage().getModel());
        fViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                handleSelectionChanged();
            }
        });
    }

    private void handleSelectionChanged() {
        ISelection selection = fViewer.getSelection();
        getManagedForm().fireSelectionChanged(this, selection);
        getPage().getPDEEditor().setSelection(selection);
        if (!isEditable()) {
            return;
        }
        if (selection != null && selection instanceof IStructuredSelection) {
            IStructuredSelection ssel = (IStructuredSelection) selection;
            fRemoveButton.setEnabled(ssel.size() > 0);
            fEditButton.setEnabled(ssel.size() == 1);
        } else {
            fRemoveButton.setEnabled(false);
            fEditButton.setEnabled(false);
        }
    }

    private void showDialog(final ISiteArchive archive) {
        final ISiteModel model = (ISiteModel) getPage().getModel();
        BusyIndicator.showWhile(fTable.getDisplay(), new Runnable() {

            @Override
            public void run() {
                NewArchiveDialog dialog = new NewArchiveDialog(fTable.getShell(), model, archive);
                dialog.create();
                SWTUtil.setDialogSize(dialog, 400, -1);
                dialog.open();
            }
        });
    }

    private void handleDelete() {
        try {
            ISelection selection = fViewer.getSelection();
            if (selection != null && selection instanceof IStructuredSelection) {
                IStructuredSelection ssel = (IStructuredSelection) selection;
                if (ssel.size() > 0) {
                    ISiteArchive[] array = (ISiteArchive[]) ssel.toList().toArray(new ISiteArchive[ssel.size()]);
                    ISite site = ((ISiteModel) getPage().getModel()).getSite();
                    site.removeArchives(array);
                }
            }
        } catch (CoreException e) {
        }
    }

    @Override
    public boolean doGlobalAction(String actionId) {
        if (actionId.equals(ActionFactory.DELETE.getId())) {
            BusyIndicator.showWhile(fTable.getDisplay(), new Runnable() {

                @Override
                public void run() {
                    handleDelete();
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public void refresh() {
        fViewer.refresh();
        super.refresh();
    }

    public void initialize() {
        refresh();
    }

    @Override
    public void modelChanged(IModelChangedEvent e) {
        markStale();
    }

    private void createContextMenu(Control control) {
        MenuManager popupMenuManager = new MenuManager();
        IMenuListener listener = new IMenuListener() {

            @Override
            public void menuAboutToShow(IMenuManager mng) {
                Action removeAction = new Action(PDEUIMessages.SiteEditor_remove) {

                    @Override
                    public void run() {
                        doGlobalAction(ActionFactory.DELETE.getId());
                    }
                };
                removeAction.setEnabled(isEditable());
                mng.add(removeAction);
                mng.add(new Separator());
                PDEFormEditorContributor contributor = getPage().getPDEEditor().getContributor();
                contributor.contextMenuAboutToShow(mng);
            }
        };
        popupMenuManager.addMenuListener(listener);
        popupMenuManager.setRemoveAllWhenShown(true);
        control.setMenu(popupMenuManager.createContextMenu(control));
    }

    @Override
    public boolean setFormInput(Object input) {
        if (input instanceof ISiteArchive) {
            fViewer.setSelection(new StructuredSelection(input), true);
            return true;
        }
        return super.setFormInput(input);
    }
}
