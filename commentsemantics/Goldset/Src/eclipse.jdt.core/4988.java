/*******************************************************************************
 * Copyright (c) 2005, 2008 BEA Systems, Inc. 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    sbandow@bea.com - initial API and implementation
 *    
 *******************************************************************************/
package org.eclipse.jdt.apt.tests.annotations.filegen;

import java.io.IOException;
import java.io.PrintWriter;
import org.eclipse.jdt.apt.tests.annotations.BaseProcessor;
import org.eclipse.jdt.apt.tests.annotations.ProcessorTestStatus;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.Filer;

public class SecondGenAnnotationProcessor extends BaseProcessor {

    public  SecondGenAnnotationProcessor(AnnotationProcessorEnvironment env) {
        super(env);
    }

    public void process() {
        ProcessorTestStatus.setProcessorRan();
        try {
            Filer f = _env.getFiler();
            //$NON-NLS-1$
            PrintWriter pw = f.createSourceFile("duptest.DupFile");
            pw.print(CODE_OVERWRITE);
            pw.close();
            Filer fr = _env.getFiler();
            //$NON-NLS-1$
            PrintWriter pwr = fr.createSourceFile("reftest.RefFile");
            pwr.print(CODE_REF);
            pwr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String CODE_OVERWRITE = "package duptest;" + "\n" + "public class DupFile" + "\n" + "{" + "\n" + "    public class Inner" + "\n" + "    {" + "\n" + "    }" + "\n" + "}";

    protected String CODE_REF = "package reftest;" + "\n" + "public class RefFile" + "\n" + "{" + "\n" + "    duptest.DupFile.Inner i;" + "\n" + "}";
}
