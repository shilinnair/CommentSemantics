/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Herrmann - Contributions for
 *								bug 349326 - [1.7] new warning for missing try-with-resources
 *								bug 392384 - [1.8][compiler][null] Restore nullness info from type annotations in class files
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 467032 - TYPE_USE Null Annotations: IllegalStateException with annotated arrays of Enum when accessed via BinaryTypeBinding
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;

public class UnresolvedReferenceBinding extends ReferenceBinding {

    ReferenceBinding resolvedType;

    TypeBinding[] wrappers;

    UnresolvedReferenceBinding prototype;

     UnresolvedReferenceBinding(char[][] compoundName, PackageBinding packageBinding) {
        this.compoundName = compoundName;
        // reasonable guess
        this.sourceName = compoundName[compoundName.length - 1];
        this.fPackage = packageBinding;
        this.wrappers = null;
        this.prototype = this;
        computeId();
    }

    public  UnresolvedReferenceBinding(UnresolvedReferenceBinding prototype) {
        super(prototype);
        this.resolvedType = prototype.resolvedType;
        this.wrappers = null;
        this.prototype = prototype.prototype;
    }

    public TypeBinding clone(TypeBinding outerType) {
        if (this.resolvedType != null)
            return this.resolvedType.clone(outerType);
        UnresolvedReferenceBinding copy = new UnresolvedReferenceBinding(this);
        this.addWrapper(copy, null);
        return copy;
    }

    void addWrapper(TypeBinding wrapper, LookupEnvironment environment) {
        if (this.resolvedType != null) {
            // the type reference B<B<T>.M> means a signature of <T:Ljava/lang/Object;>LB<LB<TT;>.M;>;
            // when the ParameterizedType for Unresolved B is created with args B<T>.M, the Unresolved B is resolved before the wrapper is added
            wrapper.swapUnresolved(this, this.resolvedType, environment);
            return;
        }
        if (this.wrappers == null) {
            this.wrappers = new TypeBinding[] { wrapper };
        } else {
            int length = this.wrappers.length;
            System.arraycopy(this.wrappers, 0, this.wrappers = new TypeBinding[length + 1], 0, length);
            this.wrappers[length] = wrapper;
        }
    }

    public boolean isUnresolvedType() {
        return true;
    }

    public String debugName() {
        return toString();
    }

    public int depth() {
        // we don't yet have our enclosing types wired, but we know the nesting depth from our compoundName:
        // (NOTE: this an upper bound, because class names may contain '$')
        int last = this.compoundName.length - 1;
        // leading '$' must be part of the class name, so start at 1.
        return CharOperation.occurencesOf('$', this.compoundName[last], 1);
    }

    public boolean hasTypeBit(int bit) {
        // shouldn't happen since we are not called before analyseCode(), but play safe:
        return false;
    }

    public TypeBinding prototype() {
        return this.prototype;
    }

    ReferenceBinding resolve(LookupEnvironment environment, boolean convertGenericToRawType) {
        ReferenceBinding targetType;
        if (//$IDENTITY-COMPARISON$
        this != this.prototype) {
            targetType = this.prototype.resolve(environment, convertGenericToRawType);
            if (convertGenericToRawType && targetType != null && targetType.isRawType()) {
                targetType = (ReferenceBinding) environment.createAnnotatedType(targetType, this.typeAnnotations);
            } else {
                targetType = this.resolvedType;
            }
            return targetType;
        }
        targetType = this.resolvedType;
        if (targetType == null) {
            char[] typeName = this.compoundName[this.compoundName.length - 1];
            targetType = this.fPackage.getType0(typeName);
            if (//$IDENTITY-COMPARISON$
            targetType == this) {
                targetType = environment.askForType(this.compoundName);
            }
            if (//$IDENTITY-COMPARISON$
            (targetType == null || targetType == this) && CharOperation.contains('.', typeName)) {
                // bug 491354: this complements the NameLookup#seekTypes(..), which performs the same adaptation
                targetType = environment.askForType(this.fPackage, CharOperation.replaceOnCopy(typeName, '.', '$'));
            }
            if (// could not resolve any better, error was already reported against it //$IDENTITY-COMPARISON$
            targetType == null || targetType == this) {
                // report the missing class file first - only if not resolving a previously missing type
                if ((this.tagBits & TagBits.HasMissingType) == 0 && !environment.mayTolerateMissingType) {
                    environment.problemReporter.isClassPathCorrect(this.compoundName, environment.unitBeingCompleted, environment.missingClassFileLocation);
                }
                // create a proxy for the missing BinaryType
                targetType = environment.createMissingType(null, this.compoundName);
            }
            if (targetType.id != TypeIds.NoId) {
                this.id = targetType.id;
            }
            setResolvedType(targetType, environment);
        }
        if (convertGenericToRawType) {
            targetType = (ReferenceBinding) environment.convertUnresolvedBinaryToRawType(targetType);
        }
        return targetType;
    }

    void setResolvedType(ReferenceBinding targetType, LookupEnvironment environment) {
        // already resolved //$IDENTITY-COMPARISON$
        if (this.resolvedType == targetType)
            return;
        // targetType may be a source or binary type
        this.resolvedType = targetType;
        environment.updateCaches(this, targetType);
        // otherwise we could create 2 : 1 for this unresolved type & 1 for the resolved type
        if (this.wrappers != null)
            for (int i = 0, l = this.wrappers.length; i < l; i++) this.wrappers[i].swapUnresolved(this, targetType, environment);
    }

    public void swapUnresolved(UnresolvedReferenceBinding unresolvedType, ReferenceBinding unannotatedType, LookupEnvironment environment) {
        if (this.resolvedType != null)
            return;
        ReferenceBinding annotatedType = (ReferenceBinding) unannotatedType.clone(null);
        this.resolvedType = annotatedType;
        annotatedType.setTypeAnnotations(getTypeAnnotations(), environment.globalOptions.isAnnotationBasedNullAnalysisEnabled);
        annotatedType.id = unannotatedType.id = this.id;
        environment.updateCaches(this, annotatedType);
        if (this.wrappers != null)
            for (int i = 0, l = this.wrappers.length; i < l; i++) this.wrappers[i].swapUnresolved(this, annotatedType, environment);
    }

    public String toString() {
        if (this.hasTypeAnnotations())
            //$NON-NLS-1$
            return super.annotatedDebugName() + "(unresolved)";
        //$NON-NLS-1$ //$NON-NLS-2$
        return "Unresolved type " + ((this.compoundName != null) ? CharOperation.toString(this.compoundName) : "UNNAMED");
    }
}
