/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net -  83258 [jar exporter] Deploy java application as executable jar
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 262768 [jar exporter] Jardesc for normal Jar contains <fatjar builder="...
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackager;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.history.IRefactoringHistoryService;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.ui.jarpackager.IJarDescriptionReader;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Reads data from an InputStream and returns a JarPackage
 */
public class JarPackageReader extends Object implements IJarDescriptionReader {

    protected InputStream fInputStream;

    private MultiStatus fWarnings;

    /**
	 * Reads a Jar Package from the underlying stream.
	 * It is the client's responsibility to close the stream.
	 * 
	 * @param inputStream the input stream
	 */
    public  JarPackageReader(InputStream inputStream) {
        Assert.isNotNull(inputStream);
        fInputStream = new BufferedInputStream(inputStream);
        fWarnings = new MultiStatus(JavaPlugin.getPluginId(), 0, JarPackagerMessages.JarPackageReader_jarPackageReaderWarnings, null);
    }

    @Override
    public void read(JarPackageData jarPackage) throws CoreException {
        try {
            readXML(jarPackage);
        } catch (IOException ex) {
            String message = (ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "");
            throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, message, ex));
        } catch (SAXException ex) {
            String message = (ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "");
            throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, message, ex));
        }
    }

    /**
     * Closes this stream.
	 * It is the clients responsibility to close the stream.
	 *
	 * @exception CoreException if closing the stream fails
     */
    @Override
    public void close() throws CoreException {
        if (fInputStream != null)
            try {
                fInputStream.close();
            } catch (IOException ex) {
                String message = (ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "");
                throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, message, ex));
            }
    }

    public JarPackageData readXML(JarPackageData jarPackage) throws IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder parser = null;
        try {
            parser = factory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new IOException(ex.getLocalizedMessage());
        } finally {
        // Note: Above code is OK since clients are responsible to close the stream
        }
        parser.setErrorHandler(new DefaultHandler());
        Element xmlJarDesc = parser.parse(new InputSource(fInputStream)).getDocumentElement();
        if (!xmlJarDesc.getNodeName().equals(JarPackagerUtil.DESCRIPTION_EXTENSION)) {
            throw new IOException(JarPackagerMessages.JarPackageReader_error_badFormat);
        }
        NodeList topLevelElements = xmlJarDesc.getChildNodes();
        for (int i = 0; i < topLevelElements.getLength(); i++) {
            Node node = topLevelElements.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;
            Element element = (Element) node;
            xmlReadJarLocation(jarPackage, element);
            xmlReadOptions(jarPackage, element);
            xmlReadRefactoring(jarPackage, element);
            xmlReadSelectedProjects(jarPackage, element);
            if (jarPackage.areGeneratedFilesExported())
                xmlReadManifest(jarPackage, element);
            xmlReadSelectedElements(jarPackage, element);
        }
        return jarPackage;
    }

    private void xmlReadJarLocation(JarPackageData jarPackage, Element element) {
        if (element.getNodeName().equals(JarPackagerUtil.JAR_EXTENSION))
            //$NON-NLS-1$
            jarPackage.setJarLocation(Path.fromPortableString(element.getAttribute("path")));
    }

    private void xmlReadOptions(JarPackageData jarPackage, Element element) throws java.io.IOException {
        if (//$NON-NLS-1$
        element.getNodeName().equals("options")) {
            //$NON-NLS-1$
            jarPackage.setOverwrite(getBooleanAttribute(element, "overwrite"));
            //$NON-NLS-1$
            jarPackage.setCompress(getBooleanAttribute(element, "compress"));
            //$NON-NLS-1$
            jarPackage.setExportErrors(getBooleanAttribute(element, "exportErrors"));
            //$NON-NLS-1$
            jarPackage.setExportWarnings(getBooleanAttribute(element, "exportWarnings"));
            //$NON-NLS-1$
            jarPackage.setSaveDescription(getBooleanAttribute(element, "saveDescription"));
            //$NON-NLS-1$
            jarPackage.setUseSourceFolderHierarchy(getBooleanAttribute(element, "useSourceFolders", false));
            //$NON-NLS-1$
            jarPackage.setDescriptionLocation(Path.fromPortableString(element.getAttribute("descriptionLocation")));
            //$NON-NLS-1$
            jarPackage.setBuildIfNeeded(getBooleanAttribute(element, "buildIfNeeded", jarPackage.isBuildingIfNeeded()));
            //$NON-NLS-1$
            jarPackage.setIncludeDirectoryEntries(getBooleanAttribute(element, "includeDirectoryEntries", false));
            //$NON-NLS-1$
            jarPackage.setRefactoringAware(getBooleanAttribute(element, "storeRefactorings", false));
        }
    }

    private void xmlReadRefactoring(JarPackageData jarPackage, Element element) throws java.io.IOException {
        if (//$NON-NLS-1$
        element.getNodeName().equals("storedRefactorings")) {
            //$NON-NLS-1$
            jarPackage.setExportStructuralOnly(getBooleanAttribute(element, "structuralOnly", jarPackage.isExportStructuralOnly()));
            //$NON-NLS-1$
            jarPackage.setDeprecationAware(getBooleanAttribute(element, "deprecationInfo", jarPackage.isDeprecationAware()));
            List<IAdaptable> elements = new ArrayList();
            int count = 1;
            //$NON-NLS-1$
            String value = element.getAttribute("project" + count);
            while (//$NON-NLS-1$
            value != null && !"".equals(value)) {
                final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(value);
                if (project.exists())
                    elements.add(project);
                count++;
                value = //$NON-NLS-1$
                element.getAttribute(//$NON-NLS-1$
                "project" + count);
            }
            jarPackage.setRefactoringProjects(elements.toArray(new IProject[elements.size()]));
            elements.clear();
            count = 1;
            IRefactoringHistoryService service = RefactoringCore.getHistoryService();
            try {
                service.connect();
                value = //$NON-NLS-1$
                element.getAttribute(//$NON-NLS-1$
                "refactoring" + count);
                while (//$NON-NLS-1$
                value != null && !"".equals(value)) {
                    final ByteArrayInputStream stream = new ByteArrayInputStream(//$NON-NLS-1$
                    value.getBytes(//$NON-NLS-1$
                    "UTF-8"));
                    try {
                        final RefactoringHistory history = service.readRefactoringHistory(stream, RefactoringDescriptor.NONE);
                        if (history != null) {
                            final RefactoringDescriptorProxy[] descriptors = history.getDescriptors();
                            if (descriptors.length > 0) {
                                for (int index = 0; index < descriptors.length; index++) {
                                    elements.add(descriptors[index]);
                                }
                            }
                        }
                    } catch (CoreException exception) {
                        JavaPlugin.log(exception);
                    }
                    count++;
                    value = element.getAttribute(//$NON-NLS-1$
                    "refactoring" + //$NON-NLS-1$
                    count);
                }
            } finally {
                service.disconnect();
            }
            jarPackage.setRefactoringDescriptors(elements.toArray(new RefactoringDescriptorProxy[elements.size()]));
        }
    }

    private void xmlReadManifest(JarPackageData jarPackage, Element element) throws java.io.IOException {
        if (//$NON-NLS-1$
        element.getNodeName().equals("manifest")) {
            //$NON-NLS-1$
            jarPackage.setManifestVersion(element.getAttribute("manifestVersion"));
            //$NON-NLS-1$
            jarPackage.setUsesManifest(getBooleanAttribute(element, "usesManifest"));
            //$NON-NLS-1$
            jarPackage.setReuseManifest(getBooleanAttribute(element, "reuseManifest"));
            //$NON-NLS-1$
            jarPackage.setSaveManifest(getBooleanAttribute(element, "saveManifest"));
            //$NON-NLS-1$
            jarPackage.setGenerateManifest(getBooleanAttribute(element, "generateManifest"));
            //$NON-NLS-1$
            jarPackage.setManifestLocation(Path.fromPortableString(element.getAttribute("manifestLocation")));
            jarPackage.setManifestMainClass(getMainClass(element));
            xmlReadSealingInfo(jarPackage, element);
        }
    }

    private void xmlReadSealingInfo(JarPackageData jarPackage, Element element) throws java.io.IOException {
        /*
		 * Try to find sealing info. Could ask for single child node
		 * but this would stop others from adding more child nodes to
		 * the manifest node.
		 */
        NodeList sealingElementContainer = element.getChildNodes();
        for (int j = 0; j < sealingElementContainer.getLength(); j++) {
            Node sealingNode = sealingElementContainer.item(j);
            if (sealingNode.getNodeType() == Node.ELEMENT_NODE && //$NON-NLS-1$
            sealingNode.getNodeName().equals("sealing")) {
                // Sealing
                Element sealingElement = (Element) sealingNode;
                //$NON-NLS-1$
                jarPackage.setSealJar(//$NON-NLS-1$
                getBooleanAttribute(sealingElement, "sealJar"));
                //$NON-NLS-1$
                jarPackage.setPackagesToSeal(//$NON-NLS-1$
                getPackages(sealingElement.getElementsByTagName("packagesToSeal")));
                //$NON-NLS-1$
                jarPackage.setPackagesToUnseal(//$NON-NLS-1$
                getPackages(sealingElement.getElementsByTagName("packagesToUnSeal")));
            }
        }
    }

    private void xmlReadSelectedElements(JarPackageData jarPackage, Element element) throws java.io.IOException {
        if (//$NON-NLS-1$
        element.getNodeName().equals("selectedElements")) {
            //$NON-NLS-1$
            jarPackage.setExportClassFiles(getBooleanAttribute(element, "exportClassFiles"));
            //$NON-NLS-1$
            jarPackage.setExportOutputFolders(getBooleanAttribute(element, "exportOutputFolder", false));
            //$NON-NLS-1$
            jarPackage.setExportJavaFiles(getBooleanAttribute(element, "exportJavaFiles"));
            NodeList selectedElements = element.getChildNodes();
            Set<IAdaptable> elementsToExport = new HashSet(selectedElements.getLength());
            for (int j = 0; j < selectedElements.getLength(); j++) {
                Node selectedNode = selectedElements.item(j);
                if (selectedNode.getNodeType() != Node.ELEMENT_NODE)
                    continue;
                Element selectedElement = (Element) selectedNode;
                if (//$NON-NLS-1$
                selectedElement.getNodeName().equals(//$NON-NLS-1$
                "file"))
                    addFile(elementsToExport, selectedElement);
                else if (//$NON-NLS-1$
                selectedElement.getNodeName().equals(//$NON-NLS-1$
                "folder"))
                    addFolder(elementsToExport, selectedElement);
                else if (//$NON-NLS-1$
                selectedElement.getNodeName().equals(//$NON-NLS-1$
                "project"))
                    addProject(elementsToExport, selectedElement);
                else if (//$NON-NLS-1$
                selectedElement.getNodeName().equals(//$NON-NLS-1$
                "javaElement"))
                    addJavaElement(elementsToExport, selectedElement);
            // Note: Other file types are not handled by this writer
            }
            jarPackage.setElements(elementsToExport.toArray());
        }
    }

    private void xmlReadSelectedProjects(JarPackageData jarPackage, Element element) throws java.io.IOException {
        if (//$NON-NLS-1$
        element.getNodeName().equals("selectedProjects")) {
            NodeList selectedElements = element.getChildNodes();
            Set<IAdaptable> selectedProjects = new HashSet(selectedElements.getLength());
            for (int index = 0; index < selectedElements.getLength(); index++) {
                Node node = selectedElements.item(index);
                if (node.getNodeType() != Node.ELEMENT_NODE)
                    continue;
                Element selectedElement = (Element) node;
                if (//$NON-NLS-1$
                selectedElement.getNodeName().equals(//$NON-NLS-1$
                "project"))
                    addProject(selectedProjects, selectedElement);
            }
            jarPackage.setRefactoringProjects(selectedProjects.toArray(new IProject[selectedProjects.size()]));
        }
    }

    protected boolean getBooleanAttribute(Element element, String name, boolean defaultValue) throws IOException {
        if (element.hasAttribute(name))
            return getBooleanAttribute(element, name);
        else
            return defaultValue;
    }

    protected boolean getBooleanAttribute(Element element, String name) throws IOException {
        String value = element.getAttribute(name);
        if (//$NON-NLS-1$
        value != null && value.equalsIgnoreCase("true"))
            return true;
        if (//$NON-NLS-1$
        value != null && value.equalsIgnoreCase("false"))
            return false;
        throw new IOException(JarPackagerMessages.JarPackageReader_error_illegalValueForBooleanAttribute);
    }

    private void addFile(Set<IAdaptable> selectedElements, Element element) throws IOException {
        IPath path = getPath(element);
        if (path != null) {
            IFile file = JavaPlugin.getWorkspace().getRoot().getFile(path);
            if (file != null)
                selectedElements.add(file);
        }
    }

    private void addFolder(Set<IAdaptable> selectedElements, Element element) throws IOException {
        IPath path = getPath(element);
        if (path != null) {
            IFolder folder = JavaPlugin.getWorkspace().getRoot().getFolder(path);
            if (folder != null)
                selectedElements.add(folder);
        }
    }

    private void addProject(Set<IAdaptable> selectedElements, Element element) throws IOException {
        //$NON-NLS-1$
        String name = element.getAttribute("name");
        if (name.length() == 0)
            throw new IOException(JarPackagerMessages.JarPackageReader_error_tagNameNotFound);
        IProject project = JavaPlugin.getWorkspace().getRoot().getProject(name);
        if (project != null)
            selectedElements.add(project);
    }

    private IPath getPath(Element element) throws IOException {
        //$NON-NLS-1$
        String pathString = element.getAttribute("path");
        if (pathString.length() == 0)
            throw new IOException(JarPackagerMessages.JarPackageReader_error_tagPathNotFound);
        //$NON-NLS-1$
        return Path.fromPortableString(element.getAttribute("path"));
    }

    private void addJavaElement(Set<IAdaptable> selectedElements, Element element) throws IOException {
        //$NON-NLS-1$
        String handleId = element.getAttribute("handleIdentifier");
        if (handleId.length() == 0)
            throw new IOException(JarPackagerMessages.JarPackageReader_error_tagHandleIdentifierNotFoundOrEmpty);
        IJavaElement je = JavaCore.create(handleId);
        if (je == null)
            addWarning(JarPackagerMessages.JarPackageReader_warning_javaElementDoesNotExist, null);
        else
            selectedElements.add(je);
    }

    private IPackageFragment[] getPackages(NodeList list) throws IOException {
        if (list.getLength() > 1)
            throw new IOException(Messages.format(JarPackagerMessages.JarPackageReader_error_duplicateTag, list.item(0).getNodeName()));
        if (list.getLength() == 0)
            // optional entry is not present
            return null;
        NodeList packageNodes = list.item(0).getChildNodes();
        List<IJavaElement> packages = new ArrayList(packageNodes.getLength());
        for (int i = 0; i < packageNodes.getLength(); i++) {
            Node packageNode = packageNodes.item(i);
            if (//$NON-NLS-1$
            packageNode.getNodeType() == Node.ELEMENT_NODE && packageNode.getNodeName().equals("package")) {
                String handleId = //$NON-NLS-1$
                ((Element) packageNode).getAttribute(//$NON-NLS-1$
                "handleIdentifier");
                if (//$NON-NLS-1$
                handleId.equals(//$NON-NLS-1$
                ""))
                    throw new IOException(JarPackagerMessages.JarPackageReader_error_tagHandleIdentifierNotFoundOrEmpty);
                IJavaElement je = JavaCore.create(handleId);
                if (je != null && je.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
                    packages.add(je);
                else
                    addWarning(JarPackagerMessages.JarPackageReader_warning_javaElementDoesNotExist, null);
            }
        }
        return packages.toArray(new IPackageFragment[packages.size()]);
    }

    private IType getMainClass(Element element) {
        //$NON-NLS-1$
        String handleId = element.getAttribute("mainClassHandleIdentifier");
        if (//$NON-NLS-1$
        handleId.equals(""))
            // Main-Class entry is optional or can be empty
            return null;
        IJavaElement je = JavaCore.create(handleId);
        if (je != null && je.getElementType() == IJavaElement.TYPE)
            return (IType) je;
        addWarning(JarPackagerMessages.JarPackageReader_warning_mainClassDoesNotExist, null);
        return null;
    }

    /**
	 * Returns the status of the reader.
	 * If there were any errors, the result is a status object containing
	 * individual status objects for each error.
	 * If there were no errors, the result is a status object with error code <code>OK</code>.
	 *
	 * @return the status of this operation
	 */
    @Override
    public IStatus getStatus() {
        if (fWarnings.getChildren().length == 0)
            return Status.OK_STATUS;
        else
            return fWarnings;
    }

    /**
	 * Adds a new warning to the list with the passed information.
	 * Normally the export operation continues after a warning.
	 * @param	message		the message
	 * @param	error	      the throwable that caused the warning, or <code>null</code>
	 */
    protected void addWarning(String message, Throwable error) {
        fWarnings.add(new Status(IStatus.WARNING, JavaPlugin.getPluginId(), 0, message, error));
    }
}
