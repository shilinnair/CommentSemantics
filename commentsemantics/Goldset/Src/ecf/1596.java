/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ecf.internal.ui.wizards;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter2;

/**
 * Provides basic labels for adaptable objects that have the
 * <code>IWorkbenchAdapter</code> adapter associated with them. All dispensed
 * images are cached until the label provider is explicitly rootDisposed. This
 * class provides a facility for subclasses to define annotations on the labels
 * and icons of adaptable objects.
 */
public class WorkbenchLabelProvider extends LabelProvider implements IColorProvider, IFontProvider {

    /**
	 * Returns a workbench label provider that is hooked up to the decorator
	 * mechanism.
	 * 
	 * @return a new <code>DecoratingLabelProvider</code> which wraps a <code>
	 *   new <code>WorkbenchLabelProvider</code>
	 */
    public static ILabelProvider getDecoratingWorkbenchLabelProvider() {
        return new DecoratingLabelProvider(new WorkbenchLabelProvider(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());
    }

    /**
	 * Listener that tracks changes to the editor registry and does a full
	 * update when it changes, since many workbench adapters derive their icon
	 * from the file associations in the registry.
	 */
    private IPropertyListener editorRegistryListener = new IPropertyListener() {

        public void propertyChanged(Object source, int propId) {
            if (propId == IEditorRegistry.PROP_CONTENTS) {
                fireLabelProviderChanged(new LabelProviderChangedEvent(WorkbenchLabelProvider.this));
            }
        }
    };

    /**
	 * Creates a new workbench label provider.
	 */
    public  WorkbenchLabelProvider() {
        PlatformUI.getWorkbench().getEditorRegistry().addPropertyListener(editorRegistryListener);
    }

    /**
	 * Returns an image descriptor that is based on the given descriptor, but
	 * decorated with additional information relating to the state of the
	 * provided object.
	 * 
	 * Subclasses may reimplement this method to decorate an object's image.
	 * 
	 * @param input
	 *            The base image to decorate.
	 * @param element
	 *            The element used to look up decorations.
	 * @return the resuling ImageDescriptor.
	 * @see org.eclipse.jface.resource.CompositeImageDescriptor
	 */
    protected ImageDescriptor decorateImage(ImageDescriptor input, Object element) {
        return input;
    }

    /**
	 * Returns a label that is based on the given label, but decorated with
	 * additional information relating to the state of the provided object.
	 * 
	 * Subclasses may implement this method to decorate an object's label.
	 * 
	 * @param input
	 *            The base text to decorate.
	 * @param element
	 *            The element used to look up decorations.
	 * @return the resulting text
	 */
    protected String decorateText(String input, Object element) {
        return input;
    }

    /*
	 * (non-Javadoc) Method declared on ILabelProvider
	 */
    public void dispose() {
        PlatformUI.getWorkbench().getEditorRegistry().removePropertyListener(editorRegistryListener);
        super.dispose();
    }

    protected static final Object getAdapterUtil(Object sourceObject, Class adapterType) {
        Assert.isNotNull(adapterType);
        if (sourceObject == null)
            return null;
        if (adapterType.isInstance(sourceObject))
            return sourceObject;
        if (sourceObject instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) sourceObject;
            Object result = adaptable.getAdapter(adapterType);
            if (result != null) {
                // Sanity-check
                Assert.isTrue(adapterType.isInstance(result));
                return result;
            }
        }
        if (!(sourceObject instanceof PlatformObject)) {
            Object result = Platform.getAdapterManager().getAdapter(sourceObject, adapterType);
            if (result != null)
                return result;
        }
        return null;
    }

    /**
	 * Returns the implementation of IWorkbenchAdapter for the given object.
	 * 
	 * @param o
	 *            the object to look up.
	 * @return IWorkbenchAdapter or<code>null</code> if the adapter is not
	 *         defined or the object is not adaptable.
	 */
    protected final IWorkbenchAdapter getAdapter(Object o) {
        return (IWorkbenchAdapter) getAdapterUtil(o, IWorkbenchAdapter.class);
    }

    /**
	 * Returns the implementation of IWorkbenchAdapter2 for the given object.
	 * 
	 * @param o
	 *            the object to look up.
	 * @return IWorkbenchAdapter2 or<code>null</code> if the adapter is not
	 *         defined or the object is not adaptable.
	 */
    protected final IWorkbenchAdapter2 getAdapter2(Object o) {
        return (IWorkbenchAdapter2) getAdapterUtil(o, IWorkbenchAdapter2.class);
    }

    /*
	 * (non-Javadoc) Method declared on ILabelProvider
	 */
    public final Image getImage(Object element) {
        // obtain the base image by querying the element
        IWorkbenchAdapter adapter = getAdapter(element);
        if (adapter == null)
            return null;
        ImageDescriptor descriptor = adapter.getImageDescriptor(element);
        if (descriptor == null)
            return null;
        // add any annotations to the image descriptor
        descriptor = decorateImage(descriptor, element);
        /*
		 * Image image = (Image)
		 * SWTResourceUtil.getImageTable().get(descriptor); if (image == null) {
		 * image = descriptor.createImage();
		 * SWTResourceUtil.getImageTable().put(descriptor, image); }
		 */
        Image image = descriptor.createImage();
        return image;
    }

    /*
	 * (non-Javadoc) Method declared on ILabelProvider
	 */
    public final String getText(Object element) {
        // query the element for its label
        IWorkbenchAdapter adapter = getAdapter(element);
        if (adapter == null)
            //$NON-NLS-1$
            return "";
        String label = adapter.getLabel(element);
        // return the decorated label
        return decorateText(label, element);
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
    public Color getForeground(Object element) {
        return getColor(element, true);
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
	 */
    public Color getBackground(Object element) {
        return getColor(element, false);
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
	 */
    public Font getFont(Object element) {
        IWorkbenchAdapter2 adapter = getAdapter2(element);
        if (adapter == null)
            return null;
        FontData descriptor = adapter.getFont(element);
        if (descriptor == null)
            return null;
        /*
		 * Font font = (Font) SWTResourceUtil.getFontTable().get(descriptor); if
		 * (font == null) { font = new Font(Display.getCurrent(), descriptor);
		 * SWTResourceUtil.getFontTable().put(descriptor, font); }
		 */
        Font font = new Font(Display.getCurrent(), descriptor);
        return font;
    }

    private Color getColor(Object element, boolean forground) {
        IWorkbenchAdapter2 adapter = getAdapter2(element);
        if (adapter == null)
            return null;
        RGB descriptor = forground ? adapter.getForeground(element) : adapter.getBackground(element);
        if (descriptor == null)
            return null;
        /*
		 * Color color = (Color)
		 * SWTResourceUtil.getColorTable().get(descriptor); if (color == null) {
		 * color = new Color(Display.getCurrent(), descriptor);
		 * SWTResourceUtil.getColorTable().put(descriptor, color); }
		 */
        Color color = new Color(Display.getCurrent(), descriptor);
        return color;
    }
}
