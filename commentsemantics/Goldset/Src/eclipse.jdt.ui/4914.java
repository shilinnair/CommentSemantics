/*******************************************************************************
 * Copyright (c) 2007, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jerome Cambon <jerome.cambon@oracle.com> - [ltk] Rename refactoring should give more control over new file name - https://bugs.eclipse.org/391389
 *******************************************************************************/
package org.eclipse.ltk.ui.refactoring.resource;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.internal.core.refactoring.resource.RenameResourceProcessor;
import org.eclipse.ltk.internal.ui.refactoring.RefactoringUIMessages;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

/**
 * A wizard for the rename resource refactoring.
 *
 * @since 3.4
 */
public class RenameResourceWizard extends RefactoringWizard {

    /**
	 * Creates a {@link RenameResourceWizard}.
	 *
	 * @param resource
	 *             the resource to rename. The resource must exist.
	 */
    public  RenameResourceWizard(IResource resource) {
        super(new RenameRefactoring(new RenameResourceProcessor(resource)), DIALOG_BASED_USER_INTERFACE);
        setDefaultPageTitle(RefactoringUIMessages.RenameResourceWizard_page_title);
        setWindowTitle(RefactoringUIMessages.RenameResourceWizard_window_title);
    }

    @Override
    protected void addUserInputPages() {
        RenameResourceProcessor processor = getRefactoring().getAdapter(RenameResourceProcessor.class);
        addPage(new RenameResourceRefactoringConfigurationPage(processor));
    }

    private static class RenameResourceRefactoringConfigurationPage extends UserInputWizardPage {

        private final RenameResourceProcessor fRefactoringProcessor;

        private Text fNameField;

        public  RenameResourceRefactoringConfigurationPage(RenameResourceProcessor processor) {
            //$NON-NLS-1$
            super("RenameResourceRefactoringInputPage");
            fRefactoringProcessor = processor;
        }

        @Override
        public void createControl(Composite parent) {
            Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayout(new GridLayout(2, false));
            composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            composite.setFont(parent.getFont());
            Label label = new Label(composite, SWT.NONE);
            label.setText(RefactoringUIMessages.RenameResourceWizard_name_field_label);
            label.setLayoutData(new GridData());
            fNameField = new Text(composite, SWT.BORDER);
            String resourceName = fRefactoringProcessor.getNewResourceName();
            fNameField.setText(resourceName);
            fNameField.setFont(composite.getFont());
            fNameField.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
            fNameField.addModifyListener(new ModifyListener() {

                @Override
                public void modifyText(ModifyEvent e) {
                    validatePage();
                }
            });
            int lastIndexOfDot = resourceName.lastIndexOf('.');
            if ((fRefactoringProcessor.getResource().getType() == IResource.FILE) && (lastIndexOfDot > 0)) {
                fNameField.setSelection(0, lastIndexOfDot);
            } else {
                fNameField.selectAll();
            }
            setPageComplete(false);
            setControl(composite);
        }

        @Override
        public void setVisible(boolean visible) {
            if (visible) {
                fNameField.setFocus();
            }
            super.setVisible(visible);
        }

        protected final void validatePage() {
            String text = fNameField.getText();
            RefactoringStatus status = fRefactoringProcessor.validateNewElementName(text);
            setPageComplete(status);
        }

        @Override
        protected boolean performFinish() {
            initializeRefactoring();
            storeSettings();
            return super.performFinish();
        }

        @Override
        public IWizardPage getNextPage() {
            initializeRefactoring();
            storeSettings();
            return super.getNextPage();
        }

        private void storeSettings() {
        }

        private void initializeRefactoring() {
            fRefactoringProcessor.setNewResourceName(fNameField.getText());
        }
    }
}
