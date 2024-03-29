/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *         bug 407191 - [1.8] Binary access support for type annotations
 *     Stephan Herrmann - Contribution for
 *								Bug 440474 - [null] textual encoding of external null annotations
 *******************************************************************************/
package org.eclipse.jdt.internal.core.hierarchy;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryField;
import org.eclipse.jdt.internal.compiler.env.IBinaryMethod;
import org.eclipse.jdt.internal.compiler.env.IBinaryNestedType;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.IBinaryTypeAnnotation;
import org.eclipse.jdt.internal.compiler.env.ITypeAnnotationWalker;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding.ExternalAnnotationStatus;
import org.eclipse.jdt.internal.core.search.indexing.IIndexConstants;

public class HierarchyBinaryType implements IBinaryType {

    private int modifiers;

    private char[] sourceName;

    private char[] name;

    private char[] enclosingTypeName;

    private char[] superclass;

    private char[][] superInterfaces = NoInterface;

    private char[][] typeParameterSignatures;

    private char[] genericSignature;

    public  HierarchyBinaryType(int modifiers, char[] qualification, char[] sourceName, char[] enclosingTypeName, char[][] typeParameterSignatures, char typeSuffix) {
        this.modifiers = modifiers;
        this.sourceName = sourceName;
        if (enclosingTypeName == null) {
            this.name = CharOperation.concat(qualification, sourceName, '/');
        } else {
            //rebuild A$B name
            this.name = CharOperation.concat(qualification, '/', enclosingTypeName, '$', sourceName);
            this.enclosingTypeName = CharOperation.concat(qualification, enclosingTypeName, '/');
            CharOperation.replace(this.enclosingTypeName, '.', '/');
        }
        this.typeParameterSignatures = typeParameterSignatures;
        CharOperation.replace(this.name, '.', '/');
    }

    /**
 * @see org.eclipse.jdt.internal.compiler.env.IBinaryType
 */
    public IBinaryAnnotation[] getAnnotations() {
        return null;
    }

    /**
 * @see org.eclipse.jdt.internal.compiler.env.IBinaryType
 */
    public IBinaryTypeAnnotation[] getTypeAnnotations() {
        return null;
    }

    public char[] getEnclosingMethod() {
        return null;
    }

    /**
 * Answer the resolved name of the enclosing type in the
 * class file format as specified in section 4.2 of the Java 2 VM spec
 * or null if the receiver is a top level type.
 *
 * For example, java.lang.String is java/lang/String.
 */
    public char[] getEnclosingTypeName() {
        return this.enclosingTypeName;
    }

    /**
 * Answer the receiver's fields or null if the array is empty.
 */
    public IBinaryField[] getFields() {
        return null;
    }

    /**
 * @see org.eclipse.jdt.internal.compiler.env.IDependent#getFileName()
 */
    public char[] getFileName() {
        return null;
    }

    public char[] getGenericSignature() {
        if (this.typeParameterSignatures != null && this.genericSignature == null) {
            StringBuffer buffer = new StringBuffer();
            buffer.append('<');
            for (int i = 0, length = this.typeParameterSignatures.length; i < length; i++) {
                buffer.append(this.typeParameterSignatures[i]);
            }
            buffer.append('>');
            if (this.superclass == null)
                //$NON-NLS-1$
                buffer.append(Signature.createTypeSignature("java.lang.Object", /*resolved*/
                true));
            else
                buffer.append(Signature.createTypeSignature(this.superclass, /*resolved*/
                true));
            if (this.superInterfaces != null)
                for (int i = 0, length = this.superInterfaces.length; i < length; i++) buffer.append(Signature.createTypeSignature(this.superInterfaces[i], /*resolved*/
                true));
            this.genericSignature = buffer.toString().toCharArray();
            CharOperation.replace(this.genericSignature, '.', '/');
        }
        return this.genericSignature;
    }

    /**
 * Answer the resolved names of the receiver's interfaces in the
 * class file format as specified in section 4.2 of the Java 2 VM spec
 * or null if the array is empty.
 *
 * For example, java.lang.String is java/lang/String.
 */
    public char[][] getInterfaceNames() {
        return this.superInterfaces;
    }

    /**
 * Answer the receiver's nested types or null if the array is empty.
 *
 * This nested type info is extracted from the inner class attributes.
 * Ask the name environment to find a member type using its compound name.
 */
    public IBinaryNestedType[] getMemberTypes() {
        return null;
    }

    /**
 * Answer the receiver's methods or null if the array is empty.
 */
    public IBinaryMethod[] getMethods() {
        return null;
    }

    /**
 * @see org.eclipse.jdt.internal.compiler.env.IBinaryType#getMissingTypeNames()
 */
    public char[][][] getMissingTypeNames() {
        return null;
    }

    /**
 * Answer an int whose bits are set according the access constants
 * defined by the VM spec.
 */
    public int getModifiers() {
        return this.modifiers;
    }

    /**
 * Answer the resolved name of the type in the
 * class file format as specified in section 4.2 of the Java 2 VM spec.
 *
 * For example, java.lang.String is java/lang/String.
 */
    public char[] getName() {
        return this.name;
    }

    public char[] getSourceName() {
        return this.sourceName;
    }

    /**
 * Answer the resolved name of the receiver's superclass in the
 * class file format as specified in section 4.2 of the Java 2 VM spec
 * or null if it does not have one.
 *
 * For example, java.lang.String is java/lang/String.
 */
    public char[] getSuperclassName() {
        return this.superclass;
    }

    // TODO (jerome) please verify that we don't need the tagbits for the receiver
    public long getTagBits() {
        return 0;
    }

    public boolean isAnonymous() {
        // index did not record this information (since unused for hierarchies)
        return false;
    }

    /**
 * Answer whether the receiver contains the resolved binary form
 * or the unresolved source form of the type.
 */
    public boolean isBinaryType() {
        return true;
    }

    public boolean isLocal() {
        // index did not record this information (since unused for hierarchies)
        return false;
    }

    public boolean isMember() {
        // index did not record this information (since unused for hierarchies)
        return false;
    }

    public void recordSuperType(char[] superTypeName, char[] superQualification, char superClassOrInterface) {
        // index encoding of p.A$B was B/p.A$, rebuild the proper name
        if (superQualification != null) {
            int length = superQualification.length;
            if (superQualification[length - 1] == '$') {
                char[] enclosingSuperName = CharOperation.lastSegment(superQualification, '.');
                superTypeName = CharOperation.concat(enclosingSuperName, superTypeName);
                superQualification = CharOperation.subarray(superQualification, 0, length - enclosingSuperName.length - 1);
            }
        }
        if (superClassOrInterface == IIndexConstants.CLASS_SUFFIX) {
            // this is an artifact used for being able to query them only.
            if (TypeDeclaration.kind(this.modifiers) == TypeDeclaration.INTERFACE_DECL)
                return;
            char[] encodedName = CharOperation.concat(superQualification, superTypeName, '/');
            CharOperation.replace(encodedName, '.', '/');
            this.superclass = encodedName;
        } else {
            char[] encodedName = CharOperation.concat(superQualification, superTypeName, '/');
            CharOperation.replace(encodedName, '.', '/');
            if (this.superInterfaces == NoInterface) {
                this.superInterfaces = new char[][] { encodedName };
            } else {
                int length = this.superInterfaces.length;
                System.arraycopy(this.superInterfaces, 0, this.superInterfaces = new char[length + 1][], 0, length);
                this.superInterfaces[length] = encodedName;
            }
        }
    }

    /**
 * @see org.eclipse.jdt.internal.compiler.env.IBinaryType
 */
    public char[] sourceFileName() {
        return null;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        if (this.modifiers == ClassFileConstants.AccPublic) {
            //$NON-NLS-1$
            buffer.append("public ");
        }
        switch(TypeDeclaration.kind(this.modifiers)) {
            case TypeDeclaration.CLASS_DECL:
                //$NON-NLS-1$
                buffer.append("class ");
                break;
            case TypeDeclaration.INTERFACE_DECL:
                //$NON-NLS-1$
                buffer.append("interface ");
                break;
            case TypeDeclaration.ENUM_DECL:
                //$NON-NLS-1$
                buffer.append("enum ");
                break;
        }
        if (this.name != null) {
            buffer.append(this.name);
        }
        if (this.superclass != null) {
            //$NON-NLS-1$
            buffer.append("\n  extends ");
            buffer.append(this.superclass);
        }
        int length;
        if (this.superInterfaces != null && (length = this.superInterfaces.length) != 0) {
            //$NON-NLS-1$
            buffer.append("\n implements ");
            for (int i = 0; i < length; i++) {
                buffer.append(this.superInterfaces[i]);
                if (i != length - 1) {
                    //$NON-NLS-1$
                    buffer.append(//$NON-NLS-1$
                    ", ");
                }
            }
        }
        return buffer.toString();
    }

    public ITypeAnnotationWalker enrichWithExternalAnnotationsFor(ITypeAnnotationWalker walker, Object member, LookupEnvironment environment) {
        return walker;
    }

    @Override
    public ExternalAnnotationStatus getExternalAnnotationStatus() {
        return ExternalAnnotationStatus.NOT_EEA_CONFIGURED;
    }
}
