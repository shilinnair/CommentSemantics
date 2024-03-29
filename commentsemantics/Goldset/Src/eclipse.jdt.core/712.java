/*
 * @(#)TypeDeclaration.java	1.4 04/04/30
 *
 * Copyright (c) 2004, Sun Microsystems, Inc.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Sun Microsystems, Inc. nor the names of
 *       its contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sun.mirror.declaration;

import java.util.Collection;
import com.sun.mirror.type.*;

public interface TypeDeclaration extends MemberDeclaration {

    /**
     * Returns the package within which this type is declared.
     *
     * @return the package within which this type is declared
     */
    PackageDeclaration getPackage();

    /**
     * Returns the fully qualified name of this class or interface
     * declaration.  More precisely, it returns the <i>canonical</i>
     * name.
     * The name of a generic type does not include any reference
     * to its formal type parameters.
     * For example, the the fully qualified name of the interface declaration
     * {@code java.util.Set<E>} is <tt>"java.util.Set"</tt>.
     *
     * @return the fully qualified name of this class or interface declaration
     */
    String getQualifiedName();

    /**
     * Returns the formal type parameters of this class or interface.
     *
     * @return the formal type parameters, or an empty collection
     * if there are none
     */
    Collection<TypeParameterDeclaration> getFormalTypeParameters();

    /**
     * Returns the interface types directly implemented by this class
     * or extended by this interface.
     *
     * @return the interface types directly implemented by this class
     * or extended by this interface, or an empty collection if there are none
     *
     * @see com.sun.mirror.util.DeclarationFilter
     */
    Collection<InterfaceType> getSuperinterfaces();

    /**
     * Returns the fields that are directly declared by this class or
     * interface.  Includes enum constants.
     *
     * @return the fields that are directly declared,
     * or an empty collection if there are none
     *
     * @see com.sun.mirror.util.DeclarationFilter
     */
    Collection<FieldDeclaration> getFields();

    /**
     * Returns the methods that are directly declared by this class or
     * interface.  Includes annotation type elements.  Excludes
     * implicitly declared methods of an interface, such as
     * <tt>toString</tt>, that correspond to the methods of
     * <tt>java.lang.Object</tt>.
     *
     * @return the methods that are directly declared,
     * or an empty collection if there are none
     *
     * @see com.sun.mirror.util.DeclarationFilter
     */
    Collection<? extends MethodDeclaration> getMethods();

    /**
     * Returns the declarations of the nested classes and interfaces
     * that are directly declared by this class or interface.
     *
     * @return the declarations of the nested classes and interfaces,
     * or an empty collection if there are none
     *
     * @see com.sun.mirror.util.DeclarationFilter
     */
    Collection<TypeDeclaration> getNestedTypes();
}
