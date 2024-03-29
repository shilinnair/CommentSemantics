/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.Map;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public abstract class FormatterTabPage extends ModifyDialogTabPage {

    //$NON-NLS-1$
    private static final String SHOW_INVISIBLE_PREFERENCE_KEY = JavaUI.ID_PLUGIN + ".formatter_page.show_invisible_characters";

    /**
	 * Constant array for boolean false/true selection.
	 */
    protected static String[] FALSE_TRUE = { DefaultCodeFormatterConstants.FALSE, DefaultCodeFormatterConstants.TRUE };

    /**
	 * Constant array for boolean true/false selection.
	 * 
	 * @since 3.5
	 */
    protected static String[] TRUE_FALSE = { DefaultCodeFormatterConstants.TRUE, DefaultCodeFormatterConstants.FALSE };

    /**
	 * Constant array for insert / not_insert.
	 */
    protected static String[] DO_NOT_INSERT_INSERT = { JavaCore.DO_NOT_INSERT, JavaCore.INSERT };

    private JavaPreview fPreview;

    private final IDialogSettings fDialogSettings;

    private Button fShowInvisibleButton;

    public  FormatterTabPage(IModifyDialogTabPage.IModificationListener modifyListener, Map<String, String> workingValues) {
        super(modifyListener, workingValues);
        fDialogSettings = JavaPlugin.getDefault().getDialogSettings();
    }

    @Override
    protected Composite doCreatePreviewPane(Composite composite, int numColumns) {
        createLabel(numColumns - 1, composite, FormatterMessages.ModifyDialogTabPage_preview_label_text);
        fShowInvisibleButton = new Button(composite, SWT.CHECK);
        fShowInvisibleButton.setText(FormatterMessages.FormatterTabPage_ShowInvisibleCharacters_label);
        fShowInvisibleButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));
        fShowInvisibleButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                fPreview.showInvisibleCharacters(fShowInvisibleButton.getSelection());
                fDialogSettings.put(SHOW_INVISIBLE_PREFERENCE_KEY, fShowInvisibleButton.getSelection());
                doUpdatePreview();
            }
        });
        fShowInvisibleButton.setSelection(isShowInvisible());
        fPreview = doCreateJavaPreview(composite);
        fDefaultFocusManager.add(fPreview.getControl());
        fPreview.showInvisibleCharacters(fShowInvisibleButton.getSelection());
        final GridData gd = createGridData(numColumns, GridData.FILL_BOTH, 0);
        gd.widthHint = 0;
        gd.heightHint = 0;
        fPreview.getControl().setLayoutData(gd);
        return composite;
    }

    private boolean isShowInvisible() {
        return fDialogSettings.getBoolean(SHOW_INVISIBLE_PREFERENCE_KEY);
    }

    @Override
    protected void doUpdatePreview() {
        boolean showInvisible = isShowInvisible();
        fPreview.showInvisibleCharacters(showInvisible);
        fShowInvisibleButton.setSelection(showInvisible);
    }
}
