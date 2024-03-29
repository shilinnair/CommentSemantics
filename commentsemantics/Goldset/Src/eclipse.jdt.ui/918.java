/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Harry Terkelsen (het@google.com) - Bug 449262 - Allow the use of third-party Java formatters
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatterExtension;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.JavaFormattingContext;

public class CompilationUnitPreview extends JavaPreview {

    private String fPreviewText;

    private String fFormatterId;

    /**
     * @param workingValues
     * @param parent
     */
    public  CompilationUnitPreview(Map<String, String> workingValues, Composite parent) {
        super(workingValues, parent);
    }

    @Override
    protected void doFormatPreview() {
        if (fPreviewText == null) {
            //$NON-NLS-1$
            fPreviewDocument.set("");
            return;
        }
        fPreviewDocument.set(fPreviewText);
        fSourceViewer.setRedraw(false);
        final IFormattingContext context = new JavaFormattingContext();
        try {
            final IContentFormatter formatter = fViewerConfiguration.getContentFormatter(fSourceViewer);
            if (formatter instanceof IContentFormatterExtension) {
                final IContentFormatterExtension extension = (IContentFormatterExtension) formatter;
                Map<String, String> prefs = fWorkingValues;
                if (fFormatterId != null) {
                    prefs = new HashMap(fWorkingValues);
                    prefs.put(JavaCore.JAVA_FORMATTER, fFormatterId);
                }
                context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, prefs);
                context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.valueOf(true));
                extension.format(fPreviewDocument, context);
            } else
                formatter.format(fPreviewDocument, new Region(0, fPreviewDocument.getLength()));
        } catch (Exception e) {
            final IStatus status = new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, FormatterMessages.JavaPreview_formatter_exception, e);
            JavaPlugin.log(status);
        } finally {
            context.dispose();
            fSourceViewer.setRedraw(true);
        }
    }

    public void setPreviewText(String previewText) {
        //        if (previewText == null) throw new IllegalArgumentException();
        fPreviewText = previewText;
        update();
    }

    public void setFormatterId(String formatterId) {
        fFormatterId = formatterId;
    }
}
