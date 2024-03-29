/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.apt.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.ZipException;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.batch.FileSystem;
import org.eclipse.jdt.internal.compiler.batch.Main;
import org.eclipse.jdt.internal.compiler.batch.Main.ResourceBundleFactory;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.env.AccessRule;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

/**
 * Implementation of the Standard Java File Manager
 */
public class EclipseFileManager implements StandardJavaFileManager {

    //$NON-NLS-1$
    private static final String NO_EXTENSION = "";

    static final int HAS_EXT_DIRS = 1;

    static final int HAS_BOOTCLASSPATH = 2;

    static final int HAS_ENDORSED_DIRS = 4;

    static final int HAS_PROCESSORPATH = 8;

    Map<File, Archive> archivesCache;

    Charset charset;

    Locale locale;

    Map<String, Iterable<? extends File>> locations;

    int flags;

    public ResourceBundle bundle;

    public  EclipseFileManager(Locale locale, Charset charset) {
        this.locale = locale == null ? Locale.getDefault() : locale;
        this.charset = charset == null ? Charset.defaultCharset() : charset;
        this.locations = new HashMap();
        this.archivesCache = new HashMap();
        try {
            this.setLocation(StandardLocation.PLATFORM_CLASS_PATH, getDefaultBootclasspath());
            Iterable<? extends File> defaultClasspath = getDefaultClasspath();
            this.setLocation(StandardLocation.CLASS_PATH, defaultClasspath);
            this.setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, defaultClasspath);
        } catch (IOException e) {
        }
        try {
            this.bundle = ResourceBundleFactory.getBundle(this.locale);
        } catch (MissingResourceException e) {
            System.out.println("Missing resource : " + Main.bundleName.replace('.', '/') + ".properties for locale " + locale);
        }
    }

    /* (non-Javadoc)
	 * @see javax.tools.JavaFileManager#close()
	 */
    @Override
    public void close() throws IOException {
        if (this.locations != null)
            this.locations.clear();
        for (Archive archive : this.archivesCache.values()) {
            archive.close();
        }
        this.archivesCache.clear();
    }

    private void collectAllMatchingFiles(File file, String normalizedPackageName, Set<Kind> kinds, boolean recurse, ArrayList<JavaFileObject> collector) {
        if (!isArchive(file)) {
            // we must have a directory
            File currentFile = new File(file, normalizedPackageName);
            if (!currentFile.exists())
                return;
            String path;
            try {
                path = currentFile.getCanonicalPath();
            } catch (IOException e) {
                return;
            }
            if (File.separatorChar == '/') {
                if (!path.endsWith(normalizedPackageName))
                    return;
            } else if (!path.endsWith(normalizedPackageName.replace('/', File.separatorChar)))
                return;
            File[] files = currentFile.listFiles();
            if (files != null) {
                // this was a directory
                for (File f : files) {
                    if (f.isDirectory() && recurse) {
                        collectAllMatchingFiles(file, normalizedPackageName + '/' + f.getName(), kinds, recurse, collector);
                    } else {
                        final Kind kind = getKind(f);
                        if (kinds.contains(kind)) {
                            collector.add(new EclipseFileObject(normalizedPackageName + f.getName(), f.toURI(), kind, this.charset));
                        }
                    }
                }
            }
        } else {
            Archive archive = this.getArchive(file);
            if (archive == Archive.UNKNOWN_ARCHIVE)
                return;
            String key = normalizedPackageName;
            if (//$NON-NLS-1$
            !normalizedPackageName.endsWith("/")) {
                key += '/';
            }
            // we have an archive file
            if (recurse) {
                for (String packageName : archive.allPackages()) {
                    if (packageName.startsWith(key)) {
                        List<String> types = archive.getTypes(packageName);
                        if (types != null) {
                            for (String typeName : types) {
                                final Kind kind = getKind(getExtension(typeName));
                                if (kinds.contains(kind)) {
                                    collector.add(archive.getArchiveFileObject(packageName + typeName, this.charset));
                                }
                            }
                        }
                    }
                }
            } else {
                List<String> types = archive.getTypes(key);
                if (types != null) {
                    for (String typeName : types) {
                        final Kind kind = getKind(getExtension(typeName));
                        if (kinds.contains(kind)) {
                            collector.add(archive.getArchiveFileObject(key + typeName, this.charset));
                        }
                    }
                }
            }
        }
    }

    private Iterable<? extends File> concatFiles(Iterable<? extends File> iterable, Iterable<? extends File> iterable2) {
        ArrayList<File> list = new ArrayList();
        if (iterable2 == null)
            return iterable;
        for (Iterator<? extends File> iterator = iterable.iterator(); iterator.hasNext(); ) {
            list.add(iterator.next());
        }
        for (Iterator<? extends File> iterator = iterable2.iterator(); iterator.hasNext(); ) {
            list.add(iterator.next());
        }
        return list;
    }

    /* (non-Javadoc)
	 * @see javax.tools.JavaFileManager#flush()
	 */
    @Override
    public void flush() throws IOException {
        for (Archive archive : this.archivesCache.values()) {
            archive.flush();
        }
    }

    private Archive getArchive(File f) {
        // check the archive (jar/zip) cache
        Archive archive = this.archivesCache.get(f);
        if (archive == null) {
            archive = Archive.UNKNOWN_ARCHIVE;
            // create a new archive
            if (f.exists()) {
                try {
                    archive = new Archive(f);
                } catch (ZipException e) {
                } catch (IOException e) {
                }
                if (archive != null) {
                    this.archivesCache.put(f, archive);
                }
            }
            this.archivesCache.put(f, archive);
        }
        return archive;
    }

    /* (non-Javadoc)
	 * @see javax.tools.JavaFileManager#getClassLoader(javax.tools.JavaFileManager.Location)
	 */
    @Override
    public ClassLoader getClassLoader(Location location) {
        Iterable<? extends File> files = getLocation(location);
        if (files == null) {
            // location is unknown
            return null;
        }
        ArrayList<URL> allURLs = new ArrayList();
        for (File f : files) {
            try {
                allURLs.add(f.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        URL[] result = new URL[allURLs.size()];
        return new URLClassLoader(allURLs.toArray(result), getClass().getClassLoader());
    }

    private Iterable<? extends File> getPathsFrom(String path) {
        ArrayList<FileSystem.Classpath> paths = new ArrayList();
        ArrayList<File> files = new ArrayList();
        try {
            this.processPathEntries(Main.DEFAULT_SIZE_CLASSPATH, paths, path, this.charset.name(), false, false);
        } catch (IllegalArgumentException e) {
            return null;
        }
        for (FileSystem.Classpath classpath : paths) {
            files.add(new File(classpath.getPath()));
        }
        return files;
    }

    Iterable<? extends File> getDefaultBootclasspath() {
        List<File> files = new ArrayList();
        //$NON-NLS-1$
        String javaversion = System.getProperty("java.version");
        if (javaversion.length() > 3)
            javaversion = javaversion.substring(0, 3);
        long jdkLevel = CompilerOptions.versionToJdkLevel(javaversion);
        if (jdkLevel < ClassFileConstants.JDK1_6) {
            // wrong jdk - 1.6 or above is required
            return null;
        }
        for (String fileName : org.eclipse.jdt.internal.compiler.util.Util.collectFilesNames()) {
            files.add(new File(fileName));
        }
        return files;
    }

    Iterable<? extends File> getDefaultClasspath() {
        // default classpath
        ArrayList<File> files = new ArrayList();
        //$NON-NLS-1$
        String classProp = System.getProperty("java.class.path");
        if ((classProp == null) || (classProp.length() == 0)) {
            return null;
        } else {
            StringTokenizer tokenizer = new StringTokenizer(classProp, File.pathSeparator);
            String token;
            while (tokenizer.hasMoreTokens()) {
                token = tokenizer.nextToken();
                File file = new File(token);
                if (file.exists()) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    private Iterable<? extends File> getEndorsedDirsFrom(String path) {
        ArrayList<FileSystem.Classpath> paths = new ArrayList();
        ArrayList<File> files = new ArrayList();
        try {
            this.processPathEntries(Main.DEFAULT_SIZE_CLASSPATH, paths, path, this.charset.name(), false, false);
        } catch (IllegalArgumentException e) {
            return null;
        }
        for (FileSystem.Classpath classpath : paths) {
            files.add(new File(classpath.getPath()));
        }
        return files;
    }

    private Iterable<? extends File> getExtdirsFrom(String path) {
        ArrayList<FileSystem.Classpath> paths = new ArrayList();
        ArrayList<File> files = new ArrayList();
        try {
            this.processPathEntries(Main.DEFAULT_SIZE_CLASSPATH, paths, path, this.charset.name(), false, false);
        } catch (IllegalArgumentException e) {
            return null;
        }
        for (FileSystem.Classpath classpath : paths) {
            files.add(new File(classpath.getPath()));
        }
        return files;
    }

    private String getExtension(File file) {
        String name = file.getName();
        return getExtension(name);
    }

    private String getExtension(String name) {
        int index = name.lastIndexOf('.');
        if (index == -1) {
            return EclipseFileManager.NO_EXTENSION;
        }
        return name.substring(index);
    }

    /* (non-Javadoc)
	 * @see javax.tools.JavaFileManager#getFileForInput(javax.tools.JavaFileManager.Location, java.lang.String, java.lang.String)
	 */
    @Override
    public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
        Iterable<? extends File> files = getLocation(location);
        if (files == null) {
            //$NON-NLS-1$
            throw new IllegalArgumentException("Unknown location : " + location);
        }
        String normalizedFileName = normalizedFileName(packageName, relativeName);
        for (File file : files) {
            if (file.isDirectory()) {
                // handle directory
                File f = new File(file, normalizedFileName);
                if (f.exists()) {
                    return new EclipseFileObject(packageName + File.separator + relativeName, f.toURI(), getKind(f), this.charset);
                } else {
                    // go to next entry in the location
                    continue;
                }
            } else if (isArchive(file)) {
                // handle archive file
                Archive archive = getArchive(file);
                if (archive != Archive.UNKNOWN_ARCHIVE) {
                    if (archive.contains(normalizedFileName)) {
                        return archive.getArchiveFileObject(normalizedFileName, this.charset);
                    }
                }
            }
        }
        return null;
    }

    private String normalizedFileName(String packageName, String relativeName) {
        StringBuilder sb = new StringBuilder();
        sb.append(normalized(packageName));
        if (sb.length() > 0) {
            sb.append('/');
        }
        sb.append(relativeName.replace('\\', '/'));
        return sb.toString();
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
        Iterable<? extends File> files = getLocation(location);
        if (files == null) {
            throw new IllegalArgumentException("Unknown location : " + location);
        }
        final Iterator<? extends File> iterator = files.iterator();
        if (iterator.hasNext()) {
            File file = iterator.next();
            String normalizedFileName = normalized(packageName) + '/' + relativeName.replace('\\', '/');
            File f = new File(file, normalizedFileName);
            return new EclipseFileObject(packageName + File.separator + relativeName, f.toURI(), getKind(f), this.charset);
        } else {
            throw new IllegalArgumentException("location is empty : " + location);
        }
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
        if (kind != Kind.CLASS && kind != Kind.SOURCE) {
            throw new IllegalArgumentException("Invalid kind : " + kind);
        }
        Iterable<? extends File> files = getLocation(location);
        if (files == null) {
            throw new IllegalArgumentException("Unknown location : " + location);
        }
        String normalizedFileName = normalized(className);
        normalizedFileName += kind.extension;
        for (File file : files) {
            if (file.isDirectory()) {
                File f = new File(file, normalizedFileName);
                if (f.exists()) {
                    return new EclipseFileObject(className, f.toURI(), kind, this.charset);
                } else {
                    continue;
                }
            } else if (isArchive(file)) {
                Archive archive = getArchive(file);
                if (archive != Archive.UNKNOWN_ARCHIVE) {
                    if (archive.contains(normalizedFileName)) {
                        return archive.getArchiveFileObject(normalizedFileName, this.charset);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
        if (kind != Kind.CLASS && kind != Kind.SOURCE) {
            throw new IllegalArgumentException("Invalid kind : " + kind);
        }
        Iterable<? extends File> files = getLocation(location);
        if (files == null) {
            if (!location.equals(StandardLocation.CLASS_OUTPUT) && !location.equals(StandardLocation.SOURCE_OUTPUT))
                throw new IllegalArgumentException("Unknown location : " + location);
            if (sibling != null) {
                String normalizedFileName = normalized(className);
                int index = normalizedFileName.lastIndexOf('/');
                if (index != -1) {
                    normalizedFileName = normalizedFileName.substring(index + 1);
                }
                normalizedFileName += kind.extension;
                URI uri = sibling.toUri();
                URI uri2 = null;
                try {
                    String path = uri.getPath();
                    index = path.lastIndexOf('/');
                    if (index != -1) {
                        path = path.substring(0, index + 1);
                        path += normalizedFileName;
                    }
                    uri2 = new URI(uri.getScheme(), uri.getHost(), path, uri.getFragment());
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("invalid sibling");
                }
                return new EclipseFileObject(className, uri2, kind, this.charset);
            } else {
                String normalizedFileName = normalized(className);
                normalizedFileName += kind.extension;
                File f = new File(System.getProperty("user.dir"), normalizedFileName);
                return new EclipseFileObject(className, f.toURI(), kind, this.charset);
            }
        }
        final Iterator<? extends File> iterator = files.iterator();
        if (iterator.hasNext()) {
            File file = iterator.next();
            String normalizedFileName = normalized(className);
            normalizedFileName += kind.extension;
            File f = new File(file, normalizedFileName);
            return new EclipseFileObject(className, f.toURI(), kind, this.charset);
        } else {
            throw new IllegalArgumentException("location is empty : " + location);
        }
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
        return getJavaFileObjectsFromFiles(Arrays.asList(files));
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
        return getJavaFileObjectsFromStrings(Arrays.asList(names));
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
        ArrayList<JavaFileObject> javaFileArrayList = new ArrayList();
        for (File f : files) {
            if (f.isDirectory()) {
                throw new IllegalArgumentException("file : " + f.getAbsolutePath() + " is a directory");
            }
            javaFileArrayList.add(new EclipseFileObject(f.getAbsolutePath(), f.toURI(), getKind(f), this.charset));
        }
        return javaFileArrayList;
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
        ArrayList<File> files = new ArrayList();
        for (String name : names) {
            files.add(new File(name));
        }
        return getJavaFileObjectsFromFiles(files);
    }

    public Kind getKind(File f) {
        return getKind(getExtension(f));
    }

    private Kind getKind(String extension) {
        if (Kind.CLASS.extension.equals(extension)) {
            return Kind.CLASS;
        } else if (Kind.SOURCE.extension.equals(extension)) {
            return Kind.SOURCE;
        } else if (Kind.HTML.extension.equals(extension)) {
            return Kind.HTML;
        }
        return Kind.OTHER;
    }

    @Override
    public Iterable<? extends File> getLocation(Location location) {
        if (this.locations == null)
            return null;
        return this.locations.get(location.getName());
    }

    private Iterable<? extends File> getOutputDir(String string) {
        if ("none".equals(string)) {
            return null;
        }
        File file = new File(string);
        if (file.exists() && !file.isDirectory()) {
            throw new IllegalArgumentException("file : " + file.getAbsolutePath() + " is not a directory");
        }
        ArrayList<File> list = new ArrayList(1);
        list.add(file);
        return list;
    }

    @Override
    public boolean handleOption(String current, Iterator<String> remaining) {
        try {
            if ("-bootclasspath".equals(current)) {
                if (remaining.hasNext()) {
                    final Iterable<? extends File> bootclasspaths = getPathsFrom(remaining.next());
                    if (bootclasspaths != null) {
                        Iterable<? extends File> iterable = getLocation(StandardLocation.PLATFORM_CLASS_PATH);
                        if ((this.flags & EclipseFileManager.HAS_ENDORSED_DIRS) == 0 && (this.flags & EclipseFileManager.HAS_EXT_DIRS) == 0) {
                            setLocation(StandardLocation.PLATFORM_CLASS_PATH, bootclasspaths);
                        } else if ((this.flags & EclipseFileManager.HAS_ENDORSED_DIRS) != 0) {
                            setLocation(StandardLocation.PLATFORM_CLASS_PATH, concatFiles(iterable, bootclasspaths));
                        } else {
                            setLocation(StandardLocation.PLATFORM_CLASS_PATH, prependFiles(iterable, bootclasspaths));
                        }
                    }
                    this.flags |= EclipseFileManager.HAS_BOOTCLASSPATH;
                    return true;
                } else {
                    throw new IllegalArgumentException();
                }
            }
            if ("-classpath".equals(current) || "-cp".equals(current)) {
                if (remaining.hasNext()) {
                    final Iterable<? extends File> classpaths = getPathsFrom(remaining.next());
                    if (classpaths != null) {
                        Iterable<? extends File> iterable = getLocation(StandardLocation.CLASS_PATH);
                        if (iterable != null) {
                            setLocation(StandardLocation.CLASS_PATH, concatFiles(iterable, classpaths));
                        } else {
                            setLocation(StandardLocation.CLASS_PATH, classpaths);
                        }
                        if ((this.flags & EclipseFileManager.HAS_PROCESSORPATH) == 0) {
                            setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, classpaths);
                        }
                    }
                    return true;
                } else {
                    throw new IllegalArgumentException();
                }
            }
            if ("-encoding".equals(current)) {
                if (remaining.hasNext()) {
                    this.charset = Charset.forName(remaining.next());
                    return true;
                } else {
                    throw new IllegalArgumentException();
                }
            }
            if ("-sourcepath".equals(current)) {
                if (remaining.hasNext()) {
                    final Iterable<? extends File> sourcepaths = getPathsFrom(remaining.next());
                    if (sourcepaths != null)
                        setLocation(StandardLocation.SOURCE_PATH, sourcepaths);
                    return true;
                } else {
                    throw new IllegalArgumentException();
                }
            }
            if ("-extdirs".equals(current)) {
                if (remaining.hasNext()) {
                    Iterable<? extends File> iterable = getLocation(StandardLocation.PLATFORM_CLASS_PATH);
                    setLocation(StandardLocation.PLATFORM_CLASS_PATH, concatFiles(iterable, getExtdirsFrom(remaining.next())));
                    this.flags |= EclipseFileManager.HAS_EXT_DIRS;
                    return true;
                } else {
                    throw new IllegalArgumentException();
                }
            }
            if ("-endorseddirs".equals(current)) {
                if (remaining.hasNext()) {
                    Iterable<? extends File> iterable = getLocation(StandardLocation.PLATFORM_CLASS_PATH);
                    setLocation(StandardLocation.PLATFORM_CLASS_PATH, prependFiles(iterable, getEndorsedDirsFrom(remaining.next())));
                    this.flags |= EclipseFileManager.HAS_ENDORSED_DIRS;
                    return true;
                } else {
                    throw new IllegalArgumentException();
                }
            }
            if ("-d".equals(current)) {
                if (remaining.hasNext()) {
                    final Iterable<? extends File> outputDir = getOutputDir(remaining.next());
                    if (outputDir != null) {
                        setLocation(StandardLocation.CLASS_OUTPUT, outputDir);
                    }
                    return true;
                } else {
                    throw new IllegalArgumentException();
                }
            }
            if ("-s".equals(current)) {
                if (remaining.hasNext()) {
                    final Iterable<? extends File> outputDir = getOutputDir(remaining.next());
                    if (outputDir != null) {
                        setLocation(StandardLocation.SOURCE_OUTPUT, outputDir);
                    }
                    return true;
                } else {
                    throw new IllegalArgumentException();
                }
            }
            if ("-processorpath".equals(current)) {
                if (remaining.hasNext()) {
                    final Iterable<? extends File> processorpaths = getPathsFrom(remaining.next());
                    if (processorpaths != null) {
                        setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, processorpaths);
                    }
                    this.flags |= EclipseFileManager.HAS_PROCESSORPATH;
                    return true;
                } else {
                    throw new IllegalArgumentException();
                }
            }
        } catch (IOException e) {
        }
        return false;
    }

    @Override
    public boolean hasLocation(Location location) {
        return this.locations != null && this.locations.containsKey(location.getName());
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        String name = file.getName();
        JavaFileObject javaFileObject = null;
        int index = name.lastIndexOf('.');
        if (index != -1) {
            name = name.substring(0, index);
        }
        try {
            javaFileObject = getJavaFileForInput(location, name, file.getKind());
        } catch (IOException e) {
        } catch (IllegalArgumentException iae) {
            return null;
        }
        if (javaFileObject == null) {
            return null;
        }
        return name.replace('/', '.');
    }

    private boolean isArchive(File f) {
        String extension = getExtension(f);
        return extension.equalsIgnoreCase(".jar") || extension.equalsIgnoreCase(".zip");
    }

    @Override
    public boolean isSameFile(FileObject fileObject1, FileObject fileObject2) {
        if (!(fileObject1 instanceof EclipseFileObject))
            throw new IllegalArgumentException("Unsupported file object class : " + fileObject1.getClass());
        if (!(fileObject2 instanceof EclipseFileObject))
            throw new IllegalArgumentException("Unsupported file object class : " + fileObject2.getClass());
        return fileObject1.equals(fileObject2);
    }

    @Override
    public int isSupportedOption(String option) {
        return Options.processOptionsFileManager(option);
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
        Iterable<? extends File> allFilesInLocations = getLocation(location);
        if (allFilesInLocations == null) {
            throw new IllegalArgumentException("Unknown location : " + location);
        }
        ArrayList<JavaFileObject> collector = new ArrayList();
        String normalizedPackageName = normalized(packageName);
        for (File file : allFilesInLocations) {
            collectAllMatchingFiles(file, normalizedPackageName, kinds, recurse, collector);
        }
        return collector;
    }

    private String normalized(String className) {
        char[] classNameChars = className.toCharArray();
        for (int i = 0, max = classNameChars.length; i < max; i++) {
            switch(classNameChars[i]) {
                case '\\':
                    classNameChars[i] = '/';
                    break;
                case '.':
                    classNameChars[i] = '/';
            }
        }
        return new String(classNameChars);
    }

    private Iterable<? extends File> prependFiles(Iterable<? extends File> iterable, Iterable<? extends File> iterable2) {
        if (iterable2 == null)
            return iterable;
        ArrayList<File> list = new ArrayList();
        for (Iterator<? extends File> iterator = iterable2.iterator(); iterator.hasNext(); ) {
            list.add(iterator.next());
        }
        for (Iterator<? extends File> iterator = iterable.iterator(); iterator.hasNext(); ) {
            list.add(iterator.next());
        }
        return list;
    }

    @Override
    public void setLocation(Location location, Iterable<? extends File> path) throws IOException {
        if (path != null) {
            if (location.isOutputLocation()) {
                int count = 0;
                for (Iterator<? extends File> iterator = path.iterator(); iterator.hasNext(); ) {
                    iterator.next();
                    count++;
                }
                if (count != 1) {
                    throw new IllegalArgumentException("output location can only have one path");
                }
            }
            this.locations.put(location.getName(), path);
        }
    }

    public void setLocale(Locale locale) {
        this.locale = locale == null ? Locale.getDefault() : locale;
        try {
            this.bundle = ResourceBundleFactory.getBundle(this.locale);
        } catch (MissingResourceException e) {
            System.out.println("Missing resource : " + Main.bundleName.replace('.', '/') + ".properties for locale " + locale);
            throw e;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void processPathEntries(final int defaultSize, final ArrayList paths, final String currentPath, String customEncoding, boolean isSourceOnly, boolean rejectDestinationPathOnJars) {
        String currentClasspathName = null;
        String currentDestinationPath = null;
        ArrayList currentRuleSpecs = new ArrayList(defaultSize);
        StringTokenizer tokenizer = new StringTokenizer(currentPath, File.pathSeparator + "[]", true);
        ArrayList tokens = new ArrayList();
        while (tokenizer.hasMoreTokens()) {
            tokens.add(tokenizer.nextToken());
        }
        final int start = 0;
        final int readyToClose = 1;
        final int readyToCloseEndingWithRules = 2;
        final int readyToCloseOrOtherEntry = 3;
        final int rulesNeedAnotherRule = 4;
        final int rulesStart = 5;
        final int rulesReadyToClose = 6;
        final int destinationPathReadyToClose = 7;
        final int readyToCloseEndingWithDestinationPath = 8;
        final int destinationPathStart = 9;
        final int bracketOpened = 10;
        final int bracketClosed = 11;
        final int error = 99;
        int state = start;
        String token = null;
        int cursor = 0, tokensNb = tokens.size(), bracket = -1;
        while (cursor < tokensNb && state != error) {
            token = (String) tokens.get(cursor++);
            if (token.equals(File.pathSeparator)) {
                switch(state) {
                    case start:
                    case readyToCloseOrOtherEntry:
                    case bracketOpened:
                        break;
                    case readyToClose:
                    case readyToCloseEndingWithRules:
                    case readyToCloseEndingWithDestinationPath:
                        state = readyToCloseOrOtherEntry;
                        addNewEntry(paths, currentClasspathName, currentRuleSpecs, customEncoding, currentDestinationPath, isSourceOnly, rejectDestinationPathOnJars);
                        currentRuleSpecs.clear();
                        break;
                    case rulesReadyToClose:
                        state = rulesNeedAnotherRule;
                        break;
                    case destinationPathReadyToClose:
                        throw new IllegalArgumentException(this.bind("configure.incorrectDestinationPathEntry", currentPath));
                    case bracketClosed:
                        cursor = bracket + 1;
                        state = rulesStart;
                        break;
                    default:
                        state = error;
                }
            } else if (token.equals("[")) {
                switch(state) {
                    case start:
                        currentClasspathName = "";
                    case readyToClose:
                        bracket = cursor - 1;
                    case bracketClosed:
                        state = bracketOpened;
                        break;
                    case readyToCloseEndingWithRules:
                        state = destinationPathStart;
                        break;
                    case readyToCloseEndingWithDestinationPath:
                        state = rulesStart;
                        break;
                    case bracketOpened:
                    default:
                        state = error;
                }
            } else if (token.equals("]")) {
                switch(state) {
                    case rulesReadyToClose:
                        state = readyToCloseEndingWithRules;
                        break;
                    case destinationPathReadyToClose:
                        state = readyToCloseEndingWithDestinationPath;
                        break;
                    case bracketOpened:
                        state = bracketClosed;
                        break;
                    case bracketClosed:
                    default:
                        state = error;
                }
            } else {
                switch(state) {
                    case start:
                    case readyToCloseOrOtherEntry:
                        state = readyToClose;
                        currentClasspathName = token;
                        break;
                    case rulesStart:
                        if (token.startsWith("-d ")) {
                            if (currentDestinationPath != null) {
                                throw new IllegalArgumentException(this.bind("configure.duplicateDestinationPathEntry", currentPath));
                            }
                            currentDestinationPath = token.substring(3).trim();
                            state = destinationPathReadyToClose;
                            break;
                        }
                    case rulesNeedAnotherRule:
                        if (currentDestinationPath != null) {
                            throw new IllegalArgumentException(this.bind("configure.accessRuleAfterDestinationPath", currentPath));
                        }
                        state = rulesReadyToClose;
                        currentRuleSpecs.add(token);
                        break;
                    case destinationPathStart:
                        if (!token.startsWith("-d ")) {
                            state = error;
                        } else {
                            currentDestinationPath = token.substring(3).trim();
                            state = destinationPathReadyToClose;
                        }
                        break;
                    case bracketClosed:
                        for (int i = bracket; i < cursor; i++) {
                            currentClasspathName += (String) tokens.get(i);
                        }
                        state = readyToClose;
                        break;
                    case bracketOpened:
                        break;
                    default:
                        state = error;
                }
            }
            if (state == bracketClosed && cursor == tokensNb) {
                cursor = bracket + 1;
                state = rulesStart;
            }
        }
        switch(state) {
            case readyToCloseOrOtherEntry:
                break;
            case readyToClose:
            case readyToCloseEndingWithRules:
            case readyToCloseEndingWithDestinationPath:
                addNewEntry(paths, currentClasspathName, currentRuleSpecs, customEncoding, currentDestinationPath, isSourceOnly, rejectDestinationPathOnJars);
                break;
            case bracketOpened:
            case bracketClosed:
            default:
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void addNewEntry(ArrayList paths, String currentClasspathName, ArrayList currentRuleSpecs, String customEncoding, String destPath, boolean isSourceOnly, boolean rejectDestinationPathOnJars) {
        int rulesSpecsSize = currentRuleSpecs.size();
        AccessRuleSet accessRuleSet = null;
        if (rulesSpecsSize != 0) {
            AccessRule[] accessRules = new AccessRule[currentRuleSpecs.size()];
            boolean rulesOK = true;
            Iterator i = currentRuleSpecs.iterator();
            int j = 0;
            while (i.hasNext()) {
                String ruleSpec = (String) i.next();
                char key = ruleSpec.charAt(0);
                String pattern = ruleSpec.substring(1);
                if (pattern.length() > 0) {
                    switch(key) {
                        case '+':
                            accessRules[j++] = new AccessRule(pattern.toCharArray(), 0);
                            break;
                        case '~':
                            accessRules[j++] = new AccessRule(pattern.toCharArray(), IProblem.DiscouragedReference);
                            break;
                        case '-':
                            accessRules[j++] = new AccessRule(pattern.toCharArray(), IProblem.ForbiddenReference);
                            break;
                        case '?':
                            accessRules[j++] = new AccessRule(pattern.toCharArray(), IProblem.ForbiddenReference, true);
                            break;
                        default:
                            rulesOK = false;
                    }
                } else {
                    rulesOK = false;
                }
            }
            if (rulesOK) {
                accessRuleSet = new AccessRuleSet(accessRules, AccessRestriction.COMMAND_LINE, currentClasspathName);
            } else {
                return;
            }
        }
        if (Main.NONE.equals(destPath)) {
            destPath = Main.NONE;
        }
        if (rejectDestinationPathOnJars && destPath != null && (currentClasspathName.endsWith(".jar") || currentClasspathName.endsWith(".zip"))) {
            throw new IllegalArgumentException(this.bind("configure.unexpectedDestinationPathEntryFile", currentClasspathName));
        }
        FileSystem.Classpath currentClasspath = FileSystem.getClasspath(currentClasspathName, customEncoding, isSourceOnly, accessRuleSet, destPath, null);
        if (currentClasspath != null) {
            paths.add(currentClasspath);
        }
    }

    private String bind(String id, String binding) {
        return bind(id, new String[] { binding });
    }

    private String bind(String id, String[] arguments) {
        if (id == null)
            return "No message available";
        String message = null;
        try {
            message = this.bundle.getString(id);
        } catch (MissingResourceException e) {
            return "Missing message: " + id + " in: " + Main.bundleName;
        }
        return MessageFormat.format(message, (Object[]) arguments);
    }
}
