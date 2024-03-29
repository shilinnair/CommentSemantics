/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Joern Dinkla <devnull@dinkla.com> - bug 210264
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 114080
 *     EclipseSource Corporation - ongoing enhancements
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.plugin.AbstractPluginModelBase;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.util.SourcePluginFilter;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.WorkbenchJob;
import org.osgi.framework.Version;

public class PluginImportWizardDetailedPage extends BaseImportWizardSecondPage {

    class ContentProvider implements IStructuredContentProvider {

        @Override
        public Object[] getElements(Object element) {
            return fModels;
        }
    }

    private Label fCountLabel;

    private int fCountTotal;

    private TableViewer fAvailableListViewer;

    private Text fFilterText;

    private VersionFilter fVersionFilter;

    private AvailableFilter fAvailableFilter;

    private SourcePluginFilter fSourceFilter;

    private ViewerFilter fRepositoryFilter;

    // fSelected is used to track the selection in a hash set so we can efficiently
    // filter selected items out of the available item list
    private Set<Object> fSelected;

    // this job is used to delay the full filter refresh for 200 milliseconds in case the user is still typing
    private WorkbenchJob fFilterJob;

    private Button fAddButton;

    private Button fAddAllButton;

    private Button fRemoveButton;

    private Button fRemoveAllButton;

    private Button fAddRequiredButton;

    private Button fFilterOldVersionButton;

    //$NON-NLS-1$
    private static final String SETTINGS_SHOW_LATEST = "showLatestPluginsOnly";

    private class RepositoryFilter extends ViewerFilter {

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            if (fPage1.getImportType() == PluginImportOperation.IMPORT_FROM_REPOSITORY) {
                return fPage1.repositoryModels.contains(element);
            }
            return true;
        }
    }

    private class AvailableFilter extends ViewerFilter {

        private Pattern fPattern;

        public  AvailableFilter() {
            //$NON-NLS-1$
            setPattern("*");
        }

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            // on a full refresh, these will have been added back to the list
            if (fSelected.contains(element))
                return false;
            if (!(element instanceof AbstractPluginModelBase))
                return false;
            String itemID = ((AbstractPluginModelBase) element).getPluginBase().getId();
            if (fPattern.matcher(itemID).matches())
                return true;
            return false;
        }

        public boolean setPattern(String newPattern) {
            if (//$NON-NLS-1$
            !newPattern.endsWith("*"))
                //$NON-NLS-1$
                newPattern += //$NON-NLS-1$
                "*";
            if (//$NON-NLS-1$
            !newPattern.startsWith("*"))
                newPattern = //$NON-NLS-1$
                "*" + //$NON-NLS-1$
                newPattern;
            if (fPattern != null) {
                String oldPattern = fPattern.pattern();
                if (newPattern.equals(oldPattern))
                    return false;
            }
            fPattern = PatternConstructor.createPattern(newPattern, true);
            return true;
        }
    }

    /**
	 * This filter is used to remove older plug-ins from view
	 *
	 */
    private class VersionFilter extends ViewerFilter {

        private HashMap<String, Version> versions = new HashMap();

        public void setModel(IPluginModelBase[] plugins) {
            if (plugins != null && plugins.length > 0) {
                versions.clear();
            }
            for (int i = 0; i < plugins.length; ++i) {
                String name = plugins[i].getBundleDescription().getSymbolicName();
                Version version = plugins[i].getBundleDescription().getVersion();
                Version oldVersion = versions.get(name);
                if (oldVersion == null || oldVersion.compareTo(version) < 0) {
                    versions.put(name, version);
                }
            }
        }

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            IPluginModelBase plugin = (IPluginModelBase) element;
            Version hVersion = versions.get(plugin.getBundleDescription().getSymbolicName());
            if (hVersion == null)
                return true;
            return hVersion.equals(plugin.getBundleDescription().getVersion());
        }
    }

    public  PluginImportWizardDetailedPage(String pageName, PluginImportWizardFirstPage firstPage) {
        super(pageName, firstPage);
        setTitle(PDEUIMessages.ImportWizard_DetailedPage_title);
        setMessage(PDEUIMessages.ImportWizard_DetailedPage_desc);
        fSelected = new HashSet();
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.makeColumnsEqualWidth = false;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 10;
        container.setLayout(layout);
        createScrollArea(container);
        createAvailableList(container).setLayoutData(new GridData(GridData.FILL_BOTH));
        createButtonArea(container);
        createImportList(container).setLayoutData(new GridData(GridData.FILL_BOTH));
        updateCount();
        // create container for buttons
        Composite optionComp = SWTFactory.createComposite(container, 1, 3, GridData.FILL_HORIZONTAL, 5, 0);
        createComputationsOption(optionComp);
        createFilterOptions(optionComp);
        addViewerListeners();
        initialize();
        addFilters();
        setControl(container);
        Dialog.applyDialogFont(container);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.PLUGIN_IMPORT_SECOND_PAGE);
    }

    private void createFilterOptions(Composite container) {
        fFilterOldVersionButton = SWTFactory.createCheckButton(container, PDEUIMessages.ImportWizard_DetailedPage_filterDesc, null, true, 1);
        if (getDialogSettings().get(SETTINGS_SHOW_LATEST) != null)
            fFilterOldVersionButton.setSelection(getDialogSettings().getBoolean(SETTINGS_SHOW_LATEST));
        else
            fFilterOldVersionButton.setSelection(true);
        fFilterOldVersionButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (fFilterOldVersionButton.getSelection()) {
                    fAvailableListViewer.addFilter(fVersionFilter);
                } else {
                    fAvailableListViewer.removeFilter(fVersionFilter);
                }
                updateCount();
            }
        });
    }

    private void addFilters() {
        fVersionFilter = new VersionFilter();
        fVersionFilter.setModel(fModels);
        fSourceFilter = new SourcePluginFilter(fPage1.getState());
        fAvailableFilter = new AvailableFilter();
        fAvailableListViewer.addFilter(fAvailableFilter);
        if (fFilterOldVersionButton.getSelection()) {
            fAvailableListViewer.addFilter(fVersionFilter);
        }
        fAvailableListViewer.addFilter(fSourceFilter);
        fRepositoryFilter = new RepositoryFilter();
        fAvailableListViewer.addFilter(fRepositoryFilter);
        fFilterJob = new //$NON-NLS-1$
        WorkbenchJob(//$NON-NLS-1$
        "FilterJob") {

            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                handleFilter();
                return Status.OK_STATUS;
            }
        };
        fFilterJob.setSystem(true);
    }

    private void initialize() {
        updateButtonEnablement(true, true);
        setPageComplete(false);
    }

    private void addViewerListeners() {
        fAvailableListViewer.addDoubleClickListener(new IDoubleClickListener() {

            @Override
            public void doubleClick(DoubleClickEvent event) {
                handleAdd();
            }
        });
        fImportListViewer.addDoubleClickListener(new IDoubleClickListener() {

            @Override
            public void doubleClick(DoubleClickEvent event) {
                handleRemove();
            }
        });
        fAvailableListViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                updateSelectionBasedEnablement(event.getSelection(), true);
            }
        });
        fImportListViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                updateSelectionBasedEnablement(event.getSelection(), false);
            }
        });
        fFilterText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                fFilterJob.cancel();
                fFilterJob.schedule(200);
            }
        });
    }

    private Composite createAvailableList(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        container.setLayout(layout);
        container.setLayoutData(new GridData());
        Label label = new Label(container, SWT.NONE);
        label.setText(PDEUIMessages.ImportWizard_DetailedPage_availableList);
        Table table = new Table(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 200;
        gd.widthHint = 225;
        table.setLayoutData(gd);
        fAvailableListViewer = new TableViewer(table);
        fAvailableListViewer.setLabelProvider(new PluginImportLabelProvider());
        fAvailableListViewer.setContentProvider(new ContentProvider());
        fAvailableListViewer.setInput(PDECore.getDefault().getModelManager());
        fAvailableListViewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
        return container;
    }

    private Composite createButtonArea(Composite parent) {
        ScrolledComposite comp = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
        GridLayout layout = new GridLayout();
        layout.marginWidth = layout.marginHeight = 0;
        comp.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        Composite container = new Composite(comp, SWT.NONE);
        layout = new GridLayout();
        layout.marginWidth = 0;
        container.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_VERTICAL);
        gd.verticalIndent = 15;
        container.setLayoutData(gd);
        Button button = new Button(container, SWT.PUSH);
        button.setText(PDEUIMessages.ImportWizard_DetailedPage_existing);
        button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        button.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleExistingProjects();
            }
        });
        SWTUtil.setButtonDimensionHint(button);
        button = new Button(container, SWT.PUSH);
        button.setText(PDEUIMessages.ImportWizard_DetailedPage_existingUnshared);
        button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        button.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleExistingUnshared();
            }
        });
        SWTUtil.setButtonDimensionHint(button);
        fAddButton = new Button(container, SWT.PUSH);
        fAddButton.setText(PDEUIMessages.ImportWizard_DetailedPage_add);
        fAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fAddButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleAdd();
            }
        });
        SWTUtil.setButtonDimensionHint(fAddButton);
        fAddAllButton = new Button(container, SWT.PUSH);
        fAddAllButton.setText(PDEUIMessages.ImportWizard_DetailedPage_addAll);
        fAddAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fAddAllButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleAddAll();
            }
        });
        SWTUtil.setButtonDimensionHint(fAddAllButton);
        fRemoveButton = new Button(container, SWT.PUSH);
        fRemoveButton.setText(PDEUIMessages.ImportWizard_DetailedPage_remove);
        fRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fRemoveButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleRemove();
            }
        });
        SWTUtil.setButtonDimensionHint(fRemoveButton);
        fRemoveAllButton = new Button(container, SWT.PUSH);
        fRemoveAllButton.setText(PDEUIMessages.ImportWizard_DetailedPage_removeAll);
        fRemoveAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fRemoveAllButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleRemoveAll();
            }
        });
        SWTUtil.setButtonDimensionHint(fRemoveAllButton);
        button = new Button(container, SWT.PUSH);
        button.setText(PDEUIMessages.ImportWizard_DetailedPage_swap);
        button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        button.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleSwap();
            }
        });
        SWTUtil.setButtonDimensionHint(button);
        fAddRequiredButton = new Button(container, SWT.PUSH);
        fAddRequiredButton.setText(PDEUIMessages.ImportWizard_DetailedPage_addRequired);
        fAddRequiredButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fAddRequiredButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleAddRequiredPlugins();
            }
        });
        SWTUtil.setButtonDimensionHint(fAddRequiredButton);
        fCountLabel = new Label(container, SWT.NONE);
        fCountLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        comp.setContent(container);
        comp.setMinHeight(250);
        comp.setExpandHorizontal(true);
        comp.setExpandVertical(true);
        return container;
    }

    private Composite createScrollArea(Composite parent) {
        Group container = new Group(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = layout.marginHeight = 6;
        container.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        container.setLayoutData(gd);
        container.setText(PDEUIMessages.ImportWizard_DetailedPage_filter);
        Label filterLabel = new Label(container, SWT.NONE);
        filterLabel.setText(PDEUIMessages.ImportWizard_DetailedPage_search);
        fFilterText = new Text(container, SWT.BORDER);
        //$NON-NLS-1$
        fFilterText.setText("");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        fFilterText.setLayoutData(gd);
        return container;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            fFilterText.setFocus();
            setPageComplete(fImportListViewer.getTable().getItemCount() > 0);
        }
    }

    @Override
    protected void refreshPage() {
        fAvailableListViewer.addFilter(fSourceFilter);
        fImportListViewer.getTable().removeAll();
        fSelected.clear();
        //$NON-NLS-1$
        fAvailableFilter.setPattern("*");
        fSourceFilter.setState(fPage1.getState());
        fVersionFilter.setModel(fModels);
        fAvailableListViewer.refresh();
        pageChanged();
    }

    protected void pageChanged() {
        pageChanged(false, false);
    }

    protected void pageChanged(boolean doAddEnablement, boolean doRemoveEnablement) {
        updateButtonEnablement(doAddEnablement, doRemoveEnablement);
        setPageComplete(fImportListViewer.getTable().getItemCount() > 0);
        setMessage(PDEUIMessages.ImportWizard_DetailedPage_desc);
    }

    private void updateCount() {
        if (fCountTotal <= 0) {
            // Since we filter the list of available plug-ins the total may differ from the model count
            fCountTotal = fAvailableListViewer.getTable().getItemCount();
        }
        fCountLabel.setText(NLS.bind(PDEUIMessages.ImportWizard_DetailedPage_count, (new String[] { Integer.toString(fImportListViewer.getTable().getItemCount()), Integer.toString(fCountTotal) })));
        fCountLabel.getParent().layout();
    }

    private void updateButtonEnablement(boolean doAddEnablement, boolean doRemoveEnablement) {
        updateCount();
        int availableCount = fAvailableListViewer.getTable().getItemCount();
        int importCount = fImportListViewer.getTable().getItemCount();
        if (doAddEnablement)
            updateSelectionBasedEnablement(fAvailableListViewer.getSelection(), true);
        if (doRemoveEnablement)
            updateSelectionBasedEnablement(fImportListViewer.getSelection(), false);
        fAddAllButton.setEnabled(availableCount > 0);
        fRemoveAllButton.setEnabled(importCount > 0);
        fAddRequiredButton.setEnabled(importCount > 0);
    }

    private void updateSelectionBasedEnablement(ISelection theSelection, boolean available) {
        if (available)
            fAddButton.setEnabled(!theSelection.isEmpty());
        else
            fRemoveButton.setEnabled(!theSelection.isEmpty());
    }

    private void handleAdd() {
        IStructuredSelection ssel = (IStructuredSelection) fAvailableListViewer.getSelection();
        if (ssel.size() > 0) {
            Table table = fAvailableListViewer.getTable();
            int index = table.getSelectionIndices()[0];
            doAdd(ssel.toList());
            table.setSelection(index < table.getItemCount() ? index : table.getItemCount() - 1);
            pageChanged(true, false);
        }
    }

    private void handleAddAll() {
        TableItem[] items = fAvailableListViewer.getTable().getItems();
        ArrayList<Object> data = new ArrayList();
        for (int i = 0; i < items.length; i++) {
            data.add(items[i].getData());
        }
        if (data.size() > 0) {
            doAdd(data);
            pageChanged(true, false);
        }
    }

    private void handleFilter() {
        boolean changed = false;
        String newFilter;
        if (fFilterText == null || (newFilter = fFilterText.getText().trim()).length() == 0)
            //$NON-NLS-1$
            newFilter = "*";
        changed = fAvailableFilter.setPattern(newFilter);
        if (changed) {
            fAvailableListViewer.refresh();
            updateButtonEnablement(false, false);
        }
    }

    private void handleRemove() {
        IStructuredSelection ssel = (IStructuredSelection) fImportListViewer.getSelection();
        if (ssel.size() > 0) {
            Table table = fImportListViewer.getTable();
            int index = table.getSelectionIndices()[0];
            doRemove(ssel.toList());
            table.setSelection(index < table.getItemCount() ? index : table.getItemCount() - 1);
            pageChanged(false, true);
        }
    }

    private void doAdd(List<Object> items) {
        fImportListViewer.add(items.toArray());
        fAvailableListViewer.remove(items.toArray());
        fSelected.addAll(items);
    }

    private void doRemove(List<Object> items) {
        fSelected.removeAll(items);
        fImportListViewer.remove(items.toArray());
        fAvailableListViewer.add(items.toArray());
    }

    private void handleRemoveAll() {
        TableItem[] items = fImportListViewer.getTable().getItems();
        ArrayList<Object> data = new ArrayList();
        for (int i = 0; i < items.length; i++) {
            data.add(items[i].getData());
        }
        if (data.size() > 0) {
            doRemove(data);
            pageChanged(false, true);
        }
    }

    private void handleSetImportSelection(ArrayList<Object> newSelectionList) {
        if (newSelectionList.size() == 0) {
            handleRemoveAll();
            pageChanged();
            return;
        }
        TableItem[] items = fImportListViewer.getTable().getItems();
        Object[] oldSelection = new Object[items.length];
        for (int i = 0; i < items.length; i++) {
            oldSelection[i] = items[i].getData();
        }
        // remove items that were in the old selection, but are not in the new one
        List<Object> itemsToRemove = new ArrayList();
        for (int i = 0; i < oldSelection.length; i++) {
            if (newSelectionList.contains(oldSelection[i])) {
                newSelectionList.remove(oldSelection[i]);
            } else {
                itemsToRemove.add(oldSelection[i]);
            }
        }
        doRemove(itemsToRemove);
        // add items that were not in the old selection and are in the new one
        doAdd(newSelectionList);
        pageChanged();
    }

    private void handleSwap() {
        TableItem[] aItems = fAvailableListViewer.getTable().getItems();
        TableItem[] iItems = fImportListViewer.getTable().getItems();
        ArrayList<Object> data = new ArrayList();
        for (int i = 0; i < iItems.length; i++) {
            data.add(iItems[i].getData());
        }
        if (data.size() > 0) {
            doRemove(data);
        }
        data.clear();
        for (int i = 0; i < aItems.length; i++) {
            data.add(aItems[i].getData());
        }
        if (data.size() > 0) {
            doAdd(data);
        }
        pageChanged();
    }

    private void handleExistingProjects() {
        ArrayList<Object> result = new ArrayList();
        for (int i = 0; i < fModels.length; i++) {
            String id = fModels[i].getPluginBase().getId();
            IProject project = (IProject) PDEPlugin.getWorkspace().getRoot().findMember(id);
            if (project != null && project.isOpen() && WorkspaceModelManager.isPluginProject(project)) {
                result.add(fModels[i]);
            }
        }
        handleSetImportSelection(result);
    }

    private void handleExistingUnshared() {
        ArrayList<Object> result = new ArrayList();
        for (int i = 0; i < fModels.length; i++) {
            String id = fModels[i].getPluginBase().getId();
            IProject project = (IProject) PDEPlugin.getWorkspace().getRoot().findMember(id);
            if (project != null && WorkspaceModelManager.isUnsharedProject(project) && WorkspaceModelManager.isPluginProject(project)) {
                result.add(fModels[i]);
            }
        }
        handleSetImportSelection(result);
    }

    private void handleAddRequiredPlugins() {
        TableItem[] items = fImportListViewer.getTable().getItems();
        if (items.length == 0)
            return;
        if (items.length == 1) {
            IPluginModelBase model = (IPluginModelBase) items[0].getData();
            if (//$NON-NLS-1$
            model.getPluginBase().getId().equals("org.eclipse.core.boot")) {
                return;
            }
        }
        ArrayList<IPluginModelBase> result = new ArrayList();
        for (int i = 0; i < items.length; i++) {
            addPluginAndDependencies((IPluginModelBase) items[i].getData(), result, fAddFragmentsButton.getSelection());
        }
        ArrayList<Object> resultObject = new ArrayList(result.size());
        resultObject.addAll(result);
        handleSetImportSelection(resultObject);
    }

    @Override
    public void dispose() {
        fFilterJob.cancel();
    }

    @Override
    public void storeSettings() {
        IDialogSettings settings = getDialogSettings();
        settings.put(SETTINGS_SHOW_LATEST, fFilterOldVersionButton.getSelection());
        super.storeSettings();
    }
}
