/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *     Heiko Seeberger - changes for bug 237764
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.sections;

import java.lang.reflect.Field;
import java.util.*;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.pde.internal.runtime.spy.SpyFormToolkit;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.PartSite;
import org.eclipse.ui.internal.PopupMenuExtender;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.PageBookView;
import org.osgi.framework.Bundle;

/**
 * @since 3.4
 */
public class ActivePartSection implements ISpySection {

    @Override
    public void build(ScrolledForm form, SpyFormToolkit toolkit, ExecutionEvent event) {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        if (// if we don't have an active workbench, we don't have a valid selection to analyze
        window == null)
            return;
        final IWorkbenchPart part = HandlerUtil.getActivePart(event);
        if (part == null)
            // (Bug 237764) if no active part let's do nothing ...
            return;
        //$NON-NLS-1$ //$NON-NLS-2$
        String partType = part instanceof IEditorPart ? "editor" : "view";
        Section section = toolkit.createSection(form.getBody(), ExpandableComposite.TITLE_BAR);
        section.setText(NLS.bind(PDERuntimeMessages.SpyDialog_activePart_title, part.getSite().getRegisteredName()));
        FormText text = toolkit.createFormText(section, true);
        section.setClient(text);
        TableWrapData td = new TableWrapData();
        td.align = TableWrapData.FILL;
        td.grabHorizontal = true;
        section.setLayoutData(td);
        //toolkit.createImageAction(section, part.getTitleImage());
        StringBuffer buffer = new StringBuffer();
        //$NON-NLS-1$
        buffer.append("<form>");
        // time to analyze the active part
        buffer.append(toolkit.createClassSection(text, NLS.bind(PDERuntimeMessages.SpyDialog_activePart_desc, partType), new Class[] { part.getClass() }));
        if (part instanceof PageBookView) {
            PageBookView outline = (PageBookView) part;
            IPage currentPage = outline.getCurrentPage();
            if (currentPage != null) {
                buffer.append(toolkit.createClassSection(text, PDERuntimeMessages.SpyDialog_activePageBook_title, new Class[] { currentPage.getClass() }));
            }
        }
        // time to analyze the contributing plug-in
        final Bundle bundle = Platform.getBundle(part.getSite().getPluginId());
        toolkit.generatePluginDetailsText(bundle, part.getSite().getId(), partType, buffer, text);
        // get menu information using reflection
        try {
            PartSite site = (PartSite) part.getSite();
            Class clazz = site.getClass().getSuperclass();
            //$NON-NLS-1$
            Field field = clazz.getDeclaredField("menuExtenders");
            field.setAccessible(true);
            List list = (List) field.get(site);
            if (list != null && list.size() > 0) {
                Set menuIds = new LinkedHashSet();
                for (int i = 0; i < list.size(); i++) {
                    PopupMenuExtender extender = (PopupMenuExtender) list.get(i);
                    menuIds.addAll(extender.getMenuIds());
                }
                //$NON-NLS-1$
                buffer.append(//$NON-NLS-1$
                "<p>");
                buffer.append(PDERuntimeMessages.SpyDialog_activeMenuIds);
                //$NON-NLS-1$
                buffer.append(//$NON-NLS-1$
                "</p>");
                for (Iterator it = menuIds.iterator(); it.hasNext(); ) {
                    buffer.append("<li bindent=\"20\" style=\"image\" value=\"menu\">");
                    buffer.append(it.next().toString());
                    //$NON-NLS-1$
                    buffer.append(//$NON-NLS-1$
                    "</li>");
                }
                Image menuImage = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_MENU_OBJ);
                //$NON-NLS-1$
                text.setImage(//$NON-NLS-1$
                "menu", //$NON-NLS-1$
                menuImage);
            }
        } catch (SecurityException e) {
            PDERuntimePlugin.log(e);
        } catch (NoSuchFieldException e) {
            PDERuntimePlugin.log(e);
        } catch (IllegalArgumentException e) {
            PDERuntimePlugin.log(e);
        } catch (IllegalAccessException e) {
            PDERuntimePlugin.log(e);
        }
        //$NON-NLS-1$
        buffer.append("</form>");
        Image idImage = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_ID_OBJ);
        //$NON-NLS-1$
        text.setImage("id", idImage);
        text.setText(buffer.toString(), true, false);
        text.layout();
    }
}
