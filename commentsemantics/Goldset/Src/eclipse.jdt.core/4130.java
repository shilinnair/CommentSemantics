/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.apt.dispatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import javax.annotation.processing.Processor;
import javax.tools.StandardLocation;
import org.eclipse.jdt.internal.compiler.batch.Main;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;

/**
 * Java 6 annotation processor manager used when compiling from the command line
 * or via the javax.tools.JavaCompiler interface.
 * @see org.eclipse.jdt.internal.apt.pluggable.core.dispatch.IdeAnnotationProcessorManager
 */
public class BatchAnnotationProcessorManager extends BaseAnnotationProcessorManager {

    /**
	 * Processors that have been set by calling CompilationTask.setProcessors().
	 */
    private List<Processor> _setProcessors = null;

    private Iterator<Processor> _setProcessorIter = null;

    /**
	 * Processors named with the -processor option on the command line.
	 */
    private List<String> _commandLineProcessors;

    private Iterator<String> _commandLineProcessorIter = null;

    private ServiceLoader<Processor> _serviceLoader = null;

    private Iterator<Processor> _serviceLoaderIter;

    private ClassLoader _procLoader;

    // Set this to true in order to trace processor discovery when -XprintProcessorInfo is specified
    private static final boolean VERBOSE_PROCESSOR_DISCOVERY = true;

    private boolean _printProcessorDiscovery = false;

    /**
	 * Zero-arg constructor so this object can be easily created via reflection.
	 * A BatchAnnotationProcessorManager cannot be used until its
	 * {@link #configure(Object, String[])} method has been called.
	 */
    public  BatchAnnotationProcessorManager() {
    }

    @Override
    public void configure(Object batchCompiler, String[] commandLineArguments) {
        if (null != _processingEnv) {
            throw new IllegalStateException("Calling configure() more than once on an AnnotationProcessorManager is not supported");
        }
        BatchProcessingEnvImpl processingEnv = new BatchProcessingEnvImpl(this, (Main) batchCompiler, commandLineArguments);
        _processingEnv = processingEnv;
        _procLoader = processingEnv.getFileManager().getClassLoader(StandardLocation.ANNOTATION_PROCESSOR_PATH);
        parseCommandLine(commandLineArguments);
        _round = 0;
    }

    /**
	 * If a -processor option was specified in command line arguments,
	 * parse it into a list of qualified classnames.
	 * @param commandLineArguments contains one string for every space-delimited token on the command line
	 */
    private void parseCommandLine(String[] commandLineArguments) {
        List<String> commandLineProcessors = null;
        for (int i = 0; i < commandLineArguments.length; ++i) {
            String option = commandLineArguments[i];
            if (//$NON-NLS-1$
            "-XprintProcessorInfo".equals(option)) {
                _printProcessorInfo = true;
                _printProcessorDiscovery = VERBOSE_PROCESSOR_DISCOVERY;
            } else if (//$NON-NLS-1$
            "-XprintRounds".equals(option)) {
                _printRounds = true;
            } else if (//$NON-NLS-1$
            "-processor".equals(option)) {
                commandLineProcessors = new ArrayList();
                String procs = commandLineArguments[++i];
                for (//$NON-NLS-1$
                String proc : //$NON-NLS-1$
                procs.split(",")) {
                    commandLineProcessors.add(proc);
                }
                break;
            }
        }
        _commandLineProcessors = commandLineProcessors;
        if (null != _commandLineProcessors) {
            _commandLineProcessorIter = _commandLineProcessors.iterator();
        }
    }

    @Override
    public ProcessorInfo discoverNextProcessor() {
        if (null != _setProcessors) {
            // If setProcessors() was called, use that list until it's empty and then stop.
            if (_setProcessorIter.hasNext()) {
                Processor p = _setProcessorIter.next();
                p.init(_processingEnv);
                ProcessorInfo pi = new ProcessorInfo(p);
                _processors.add(pi);
                if (_printProcessorDiscovery && null != _out) {
                    _out.println(//$NON-NLS-1$
                    "API specified processor: " + //$NON-NLS-1$
                    pi);
                }
                return pi;
            }
            return null;
        }
        if (null != _commandLineProcessors) {
            // creating and initializing processors, until no more names are found, then stop.
            if (_commandLineProcessorIter.hasNext()) {
                String proc = _commandLineProcessorIter.next();
                try {
                    Class<?> clazz = _procLoader.loadClass(proc);
                    Object o = clazz.newInstance();
                    Processor p = (Processor) o;
                    p.init(_processingEnv);
                    ProcessorInfo pi = new ProcessorInfo(p);
                    _processors.add(pi);
                    if (_printProcessorDiscovery && null != _out) {
                        _out.println("Command line specified processor: " + pi);
                    }
                    return pi;
                } catch (Exception e) {
                    throw new AbortCompilation(null, e);
                }
            }
            return null;
        }
        // or the command line, search the processor path with ServiceLoader.
        if (null == _serviceLoader) {
            _serviceLoader = ServiceLoader.load(Processor.class, _procLoader);
            _serviceLoaderIter = _serviceLoader.iterator();
        }
        try {
            if (_serviceLoaderIter.hasNext()) {
                Processor p = _serviceLoaderIter.next();
                p.init(_processingEnv);
                ProcessorInfo pi = new ProcessorInfo(p);
                _processors.add(pi);
                if (_printProcessorDiscovery && null != _out) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Discovered processor service ");
                    sb.append(pi);
                    sb.append("\n  supporting ");
                    sb.append(pi.getSupportedAnnotationTypesAsString());
                    //$NON-NLS-1$
                    sb.append(//$NON-NLS-1$
                    "\n  in ");
                    sb.append(getProcessorLocation(p));
                    _out.println(sb.toString());
                }
                return pi;
            }
        } catch (ServiceConfigurationError e) {
            throw new AbortCompilation(null, e);
        }
        return null;
    }

    /**
	 * Used only for debugging purposes.  Generates output like "file:jar:D:/temp/jarfiles/myJar.jar!/".
	 * Surely this code already exists in several hundred other places?  
	 * @return the location whence a processor class was loaded.
	 */
    private String getProcessorLocation(Processor p) {
        // Get the classname in a form that can be passed to ClassLoader.getResource(),
        // e.g., "pa/pb/pc/Outer$Inner.class"
        boolean isMember = false;
        Class<?> outerClass = p.getClass();
        StringBuilder innerName = new StringBuilder();
        while (outerClass.isMemberClass()) {
            innerName.insert(0, outerClass.getSimpleName());
            innerName.insert(0, '$');
            isMember = true;
            outerClass = outerClass.getEnclosingClass();
        }
        String path = outerClass.getName();
        path = path.replace('.', '/');
        if (isMember) {
            path = path + innerName;
        }
        //$NON-NLS-1$
        path = path + ".class";
        // Find the URL for the class resource and strip off the resource name itself
        String location = _procLoader.getResource(path).toString();
        if (location.endsWith(path)) {
            location = location.substring(0, location.length() - path.length());
        }
        return location;
    }

    @Override
    public void reportProcessorException(Processor p, Exception e) {
        // TODO: if (verbose) report the processor
        throw new AbortCompilation(null, e);
    }

    @Override
    public void setProcessors(Object[] processors) {
        if (!_isFirstRound) {
            //$NON-NLS-1$
            throw new IllegalStateException("setProcessors() cannot be called after processing has begun");
        }
        // Cast all the processors here, rather than failing later.
        // But don't call init() until the processor is actually needed.
        _setProcessors = new ArrayList(processors.length);
        for (Object o : processors) {
            Processor p = (Processor) o;
            _setProcessors.add(p);
        }
        _setProcessorIter = _setProcessors.iterator();
        // processors set this way take precedence over anything on the command line 
        _commandLineProcessors = null;
        _commandLineProcessorIter = null;
    }
}
