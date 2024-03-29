/*******************************************************************************
 * Copyright (c) 2000-2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.Map;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;

/**
 * A parameterized type encapsulates a type with type arguments,
 */
public class ParameterizedTypeBinding extends ReferenceBinding implements Substitution {

    public ReferenceBinding type;

    public TypeBinding[] arguments;

    public LookupEnvironment environment;

    public char[] genericTypeSignature;

    public ReferenceBinding superclass;

    public ReferenceBinding[] superInterfaces;

    public FieldBinding[] fields;

    public ReferenceBinding[] memberTypes;

    public MethodBinding[] methods;

    private ReferenceBinding enclosingType;

    public  ParameterizedTypeBinding(ReferenceBinding type, TypeBinding[] arguments, ReferenceBinding enclosingType, LookupEnvironment environment) {
        this.environment = environment;
        if (// fixup instance of parameterized member type, e.g. Map<K,V>.Entry + <A,B>
        type.isParameterizedType() && type.isMemberType()) {
            // use enclosing from previously parameterized
            enclosingType = type.enclosingType();
            // connect to erasure of member type
            type = (ReferenceBinding) type.erasure();
        }
        initialize(type, arguments);
        // never unresolved, never lazy per construction
        this.enclosingType = enclosingType;
        if (type instanceof UnresolvedReferenceBinding)
            ((UnresolvedReferenceBinding) type).addWrapper(this);
        if (arguments != null) {
            for (int i = 0, l = arguments.length; i < l; i++) if (arguments[i] instanceof UnresolvedReferenceBinding)
                ((UnresolvedReferenceBinding) arguments[i]).addWrapper(this);
        }
    }

    /**
	 * Collect the substitutes into a map for certain type variables inside the receiver type
	 * e.g.   Collection<T>.findSubstitute(T, Collection<List<X>>):   T --> List<X>
	 */
    public void collectSubstitutes(TypeBinding otherType, Map substitutes) {
        if (this.arguments == null)
            return;
        if (otherType instanceof ReferenceBinding) {
            // allow List<T> to match with LinkedList<String>
            ReferenceBinding otherEquivalent = ((ReferenceBinding) otherType).findSuperTypeErasingTo((ReferenceBinding) this.type.erasure());
            if (otherEquivalent != null && otherEquivalent.isParameterizedType()) {
                ParameterizedTypeBinding otherParameterizedType = (ParameterizedTypeBinding) otherEquivalent;
                for (int i = 0, length = this.arguments.length; i < length; i++) {
                    this.arguments[i].collectSubstitutes(otherParameterizedType.arguments[i], substitutes);
                }
            }
        }
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#computeId()
	 */
    public void computeId() {
        this.id = NoId;
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#constantPoolName()
	 */
    public char[] constantPoolName() {
        // erasure
        return this.type.constantPoolName();
    }

    public ParameterizedMethodBinding createParameterizedMethod(MethodBinding originalMethod) {
        return new ParameterizedMethodBinding(this, originalMethod, originalMethod.isStatic());
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#debugName()
	 */
    public String debugName() {
        StringBuffer nameBuffer = new StringBuffer(10);
        nameBuffer.append(this.type.sourceName());
        if (this.arguments != null) {
            nameBuffer.append('<');
            for (int i = 0, length = this.arguments.length; i < length; i++) {
                if (i > 0)
                    nameBuffer.append(',');
                nameBuffer.append(this.arguments[i].debugName());
            }
            nameBuffer.append('>');
        }
        return nameBuffer.toString();
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#enclosingType()
	 */
    public ReferenceBinding enclosingType() {
        if (this.isMemberType() && this.enclosingType == null) {
            ReferenceBinding originalEnclosing = this.type.enclosingType();
            this.enclosingType = originalEnclosing.isGenericType() ? // TODO (need to propagate in depth on enclosing type)
            this.environment.createRawType(originalEnclosing, null) : originalEnclosing;
        }
        return this.enclosingType;
    }

    /**
     * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#erasure()
     */
    public TypeBinding erasure() {
        // erasure
        return this.type.erasure();
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#fieldCount()
	 */
    public int fieldCount() {
        // same as erasure (lazy)
        return this.type.fieldCount();
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#fields()
	 */
    public FieldBinding[] fields() {
        if (this.fields == null) {
            try {
                FieldBinding[] originalFields = this.type.fields();
                int length = originalFields.length;
                FieldBinding[] parameterizedFields = new FieldBinding[length];
                for (int i = 0; i < length; i++) // substitute all fields, so as to get updated declaring class at least
                parameterizedFields[i] = new ParameterizedFieldBinding(this, originalFields[i]);
                this.fields = parameterizedFields;
            } finally {
                // if the original fields cannot be retrieved (ex. AbortCompilation), then assume we do not have any fields
                if (this.fields == null)
                    this.fields = NoFields;
            }
        }
        return this.fields;
    }

    /**
	 * Ltype<param1 ... paremN>;
	 * LY<TT;>;
	 */
    public char[] genericTypeSignature() {
        if (this.genericTypeSignature == null) {
            StringBuffer sig = new StringBuffer(10);
            if (this.isMemberType() && this.enclosingType().isParameterizedType()) {
                char[] typeSig = this.enclosingType().genericTypeSignature();
                // copy all but trailing semicolon
                for (int i = 0; i < typeSig.length - 1; i++) sig.append(typeSig[i]);
                sig.append('.').append(this.sourceName());
            } else {
                char[] typeSig = this.type.signature();
                // copy all but trailing semicolon
                for (int i = 0; i < typeSig.length - 1; i++) sig.append(typeSig[i]);
            }
            if (this.arguments != null) {
                sig.append('<');
                for (int i = 0, length = this.arguments.length; i < length; i++) {
                    sig.append(this.arguments[i].genericTypeSignature());
                }
                //$NON-NLS-1$
                sig.append('>');
            }
            sig.append(';');
            int sigLength = sig.length();
            this.genericTypeSignature = new char[sigLength];
            sig.getChars(0, sigLength, this.genericTypeSignature, 0);
        }
        return this.genericTypeSignature;
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getExactConstructor(TypeBinding[])
	 */
    public MethodBinding getExactConstructor(TypeBinding[] argumentTypes) {
        int argCount = argumentTypes.length;
        if (// have resolved all arg types & return type of the methods
        (modifiers & AccUnresolved) == 0) {
            nextMethod: for (int m = methods.length; --m >= 0; ) {
                MethodBinding method = methods[m];
                if (method.selector == ConstructorDeclaration.ConstantPoolName && method.parameters.length == argCount) {
                    TypeBinding[] toMatch = method.parameters;
                    for (int p = 0; p < argCount; p++) if (toMatch[p] != argumentTypes[p])
                        continue nextMethod;
                    return method;
                }
            }
        } else {
            // takes care of duplicates & default abstract methods
            MethodBinding[] constructors = getMethods(ConstructorDeclaration.ConstantPoolName);
            nextConstructor: for (int c = constructors.length; --c >= 0; ) {
                MethodBinding constructor = constructors[c];
                TypeBinding[] toMatch = constructor.parameters;
                if (toMatch.length == argCount) {
                    for (int p = 0; p < argCount; p++) if (toMatch[p] != argumentTypes[p])
                        continue nextConstructor;
                    return constructor;
                }
            }
        }
        return null;
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getExactMethod(char[], TypeBinding[])
	 */
    public MethodBinding getExactMethod(char[] selector, TypeBinding[] argumentTypes, CompilationUnitScope refScope) {
        if (refScope != null)
            refScope.recordTypeReference(this);
        int argCount = argumentTypes.length;
        int selectorLength = selector.length;
        boolean foundNothing = true;
        MethodBinding match = null;
        if (// have resolved all arg types & return type of the methods
        (modifiers & AccUnresolved) == 0) {
            nextMethod: for (int m = methods.length; --m >= 0; ) {
                MethodBinding method = methods[m];
                if (method.selector.length == selectorLength && CharOperation.equals(method.selector, selector)) {
                    // inner type lookups must know that a method with this name exists
                    foundNothing = false;
                    if (method.parameters.length == argCount) {
                        TypeBinding[] toMatch = method.parameters;
                        for (int p = 0; p < argCount; p++) if (toMatch[p] != argumentTypes[p])
                            continue nextMethod;
                        if (// collision case
                        match != null)
                            // collision case
                            return null;
                        match = method;
                    }
                }
            }
        } else {
            // takes care of duplicates & default abstract methods
            MethodBinding[] matchingMethods = getMethods(selector);
            foundNothing = matchingMethods == NoMethods;
            nextMethod: for (int m = matchingMethods.length; --m >= 0; ) {
                MethodBinding method = matchingMethods[m];
                TypeBinding[] toMatch = method.parameters;
                if (toMatch.length == argCount) {
                    for (int p = 0; p < argCount; p++) if (toMatch[p] != argumentTypes[p])
                        continue nextMethod;
                    if (// collision case
                    match != null)
                        // collision case
                        return null;
                    match = method;
                }
            }
        }
        if (match != null)
            return match;
        if (foundNothing) {
            if (isInterface()) {
                if (superInterfaces().length == 1)
                    return superInterfaces[0].getExactMethod(selector, argumentTypes, refScope);
            } else if (superclass() != null) {
                return superclass.getExactMethod(selector, argumentTypes, refScope);
            }
        }
        return null;
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getField(char[], boolean)
	 */
    public FieldBinding getField(char[] fieldName, boolean needResolve) {
        // ensure fields have been initialized... must create all at once unlike methods
        fields();
        int fieldLength = fieldName.length;
        for (int i = fields.length; --i >= 0; ) {
            FieldBinding field = fields[i];
            if (field.name.length == fieldLength && CharOperation.equals(field.name, fieldName))
                return field;
        }
        return null;
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getMemberType(char[])
	 */
    public ReferenceBinding getMemberType(char[] typeName) {
        // ensure memberTypes have been initialized... must create all at once unlike methods
        memberTypes();
        int typeLength = typeName.length;
        for (int i = this.memberTypes.length; --i >= 0; ) {
            ReferenceBinding memberType = this.memberTypes[i];
            if (memberType.sourceName.length == typeLength && CharOperation.equals(memberType.sourceName, typeName))
                return memberType;
        }
        return null;
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getMethods(char[])
	 */
    public MethodBinding[] getMethods(char[] selector) {
        java.util.ArrayList matchingMethods = null;
        if (this.methods != null) {
            int selectorLength = selector.length;
            for (int i = 0, length = this.methods.length; i < length; i++) {
                MethodBinding method = methods[i];
                if (method.selector.length == selectorLength && CharOperation.equals(method.selector, selector)) {
                    if (matchingMethods == null)
                        matchingMethods = new java.util.ArrayList(2);
                    matchingMethods.add(method);
                }
            }
            if (matchingMethods != null) {
                MethodBinding[] result = new MethodBinding[matchingMethods.size()];
                matchingMethods.toArray(result);
                return result;
            }
        }
        // have created all the methods and there are no matches
        if ((modifiers & AccUnresolved) == 0)
            return NoMethods;
        MethodBinding[] parameterizedMethods = null;
        try {
            MethodBinding[] originalMethods = this.type.getMethods(selector);
            int length = originalMethods.length;
            if (length == 0)
                return NoMethods;
            parameterizedMethods = new MethodBinding[length];
            for (int i = 0; i < length; i++) // substitute methods, so as to get updated declaring class at least
            parameterizedMethods[i] = createParameterizedMethod(originalMethods[i]);
            if (this.methods == null) {
                this.methods = parameterizedMethods;
            } else {
                MethodBinding[] temp = new MethodBinding[length + this.methods.length];
                System.arraycopy(parameterizedMethods, 0, temp, 0, length);
                System.arraycopy(this.methods, 0, temp, length, this.methods.length);
                this.methods = temp;
            }
            return parameterizedMethods;
        } finally {
            // if the original methods cannot be retrieved (ex. AbortCompilation), then assume we do not have any methods
            if (parameterizedMethods == null)
                this.methods = parameterizedMethods = NoMethods;
        }
    }

    public boolean hasMemberTypes() {
        return this.type.hasMemberTypes();
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#implementsMethod(MethodBinding)
	 */
    public boolean implementsMethod(MethodBinding method) {
        // erasure
        return this.type.implementsMethod(method);
    }

    void initialize(ReferenceBinding someType, TypeBinding[] someArguments) {
        this.type = someType;
        this.sourceName = someType.sourceName;
        this.compoundName = someType.compoundName;
        this.fPackage = someType.fPackage;
        this.fileName = someType.fileName;
        // should not be set yet
        // this.superclass = null;
        // this.superInterfaces = null;
        // this.fields = null;
        // this.methods = null;		
        // until methods() is sent
        this.modifiers = someType.modifiers | AccGenericSignature | AccUnresolved;
        if (someArguments != null) {
            this.arguments = someArguments;
            for (int i = 0, length = someArguments.length; i < length; i++) {
                TypeBinding someArgument = someArguments[i];
                if (!someArgument.isWildcard() || ((WildcardBinding) someArgument).kind != Wildcard.UNBOUND) {
                    this.tagBits |= IsBoundParameterizedType;
                }
                this.tagBits |= someArgument.tagBits & (HasTypeVariable | HasWildcard);
            }
        }
        this.tagBits |= someType.tagBits & (IsLocalType | IsMemberType | IsNestedType);
    }

    protected void initializeArguments() {
    // do nothing for true parameterized types (only for raw types)
    }

    public boolean isEquivalentTo(TypeBinding otherType) {
        if (this == otherType)
            return true;
        if (otherType == null)
            return false;
        if (otherType.isRawType())
            return erasure() == otherType.erasure();
        if (otherType.isParameterizedType()) {
            if ((otherType.tagBits & HasWildcard) == 0 && (!this.isMemberType() || !otherType.isMemberType()))
                // should have been identical
                return false;
            ParameterizedTypeBinding otherParamType = (ParameterizedTypeBinding) otherType;
            if (this.type != otherParamType.type)
                return false;
            ReferenceBinding enclosing = enclosingType();
            if (enclosing != null && !enclosing.isEquivalentTo(otherParamType.enclosingType()))
                return false;
            int length = this.arguments == null ? 0 : this.arguments.length;
            TypeBinding[] otherArguments = otherParamType.arguments;
            int otherLength = otherArguments == null ? 0 : otherArguments.length;
            if (otherLength != length)
                return false;
            // argument must be identical, only equivalence is allowed if wildcard other type
            for (int i = 0; i < length; i++) {
                TypeBinding argument = this.arguments[i];
                TypeBinding otherArgument = otherArguments[i];
                if (!(argument == otherArgument || (otherArgument.isWildcard()) && argument.isEquivalentTo(otherArgument))) {
                    return false;
                }
            }
            return true;
        }
        if (otherType.isWildcard())
            return ((WildcardBinding) otherType).boundCheck(this);
        return false;
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#isParameterizedType()
	 */
    public boolean isParameterizedType() {
        return true;
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#memberTypes()
	 */
    public ReferenceBinding[] memberTypes() {
        if (this.memberTypes == null) {
            try {
                ReferenceBinding[] originalMemberTypes = this.type.memberTypes();
                int length = originalMemberTypes.length;
                ReferenceBinding[] parameterizedMemberTypes = new ReferenceBinding[length];
                for (int i = 0; i < length; i++) // substitute all member types, so as to get updated enclosing types
                parameterizedMemberTypes[i] = this.environment.createParameterizedType(originalMemberTypes[i], null, this);
                this.memberTypes = parameterizedMemberTypes;
            } finally {
                // if the original fields cannot be retrieved (ex. AbortCompilation), then assume we do not have any fields
                if (this.memberTypes == null)
                    this.memberTypes = NoMemberTypes;
            }
        }
        return this.memberTypes;
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#methods()
	 */
    public MethodBinding[] methods() {
        if ((modifiers & AccUnresolved) == 0)
            return this.methods;
        try {
            MethodBinding[] originalMethods = this.type.methods();
            int length = originalMethods.length;
            MethodBinding[] parameterizedMethods = new MethodBinding[length];
            for (int i = 0; i < length; i++) // substitute all methods, so as to get updated declaring class at least
            parameterizedMethods[i] = createParameterizedMethod(originalMethods[i]);
            this.methods = parameterizedMethods;
        } finally {
            // if the original methods cannot be retrieved (ex. AbortCompilation), then assume we do not have any methods
            if (this.methods == null)
                this.methods = NoMethods;
            modifiers ^= AccUnresolved;
        }
        return this.methods;
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#qualifiedSourceName()
	 */
    public char[] qualifiedSourceName() {
        return this.type.qualifiedSourceName();
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.Binding#readableName()
	 */
    public char[] readableName() {
        StringBuffer nameBuffer = new StringBuffer(10);
        if (this.isMemberType()) {
            nameBuffer.append(CharOperation.concat(this.enclosingType().readableName(), sourceName, '.'));
        } else {
            nameBuffer.append(CharOperation.concatWith(this.type.compoundName, '.'));
        }
        if (this.arguments != null) {
            nameBuffer.append('<');
            for (int i = 0, length = this.arguments.length; i < length; i++) {
                if (i > 0)
                    nameBuffer.append(',');
                nameBuffer.append(this.arguments[i].readableName());
            }
            nameBuffer.append('>');
        }
        int nameLength = nameBuffer.length();
        char[] readableName = new char[nameLength];
        nameBuffer.getChars(0, nameLength, readableName, 0);
        return readableName;
    }

    ReferenceBinding resolve() {
        // TODO need flag to know that this has already been done... should it be on ReferenceBinding?
        // still part of parameterized type ref
        ReferenceBinding resolvedType = BinaryTypeBinding.resolveType(this.type, this.environment, false);
        if (this.arguments != null) {
            int argLength = this.arguments.length;
            for (int i = 0; i < argLength; i++) BinaryTypeBinding.resolveType(this.arguments[i], this.environment, this, i);
            // arity check
            TypeVariableBinding[] refTypeVariables = resolvedType.typeVariables();
            if (// check generic
            refTypeVariables == NoTypeVariables) {
                this.environment.problemReporter.nonGenericTypeCannotBeParameterized(null, resolvedType, this.arguments);
                // cannot reach here as AbortCompilation is thrown
                return this;
            } else if (// check arity
            argLength != refTypeVariables.length) {
                this.environment.problemReporter.incorrectArityForParameterizedType(null, resolvedType, this.arguments);
                // cannot reach here as AbortCompilation is thrown
                return this;
            }
            // check argument type compatibility
            for (int i = 0; i < argLength; i++) {
                TypeBinding resolvedArgument = this.arguments[i];
                if (!refTypeVariables[i].boundCheck(this, resolvedArgument)) {
                    this.environment.problemReporter.typeMismatchError(resolvedArgument, refTypeVariables[i], resolvedType, null);
                }
            }
        }
        return this;
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.Binding#shortReadableName()
	 */
    public char[] shortReadableName() {
        StringBuffer nameBuffer = new StringBuffer(10);
        if (this.isMemberType()) {
            nameBuffer.append(CharOperation.concat(this.enclosingType().shortReadableName(), sourceName, '.'));
        } else {
            nameBuffer.append(this.type.sourceName);
        }
        if (this.arguments != null) {
            nameBuffer.append('<');
            for (int i = 0, length = this.arguments.length; i < length; i++) {
                if (i > 0)
                    nameBuffer.append(',');
                nameBuffer.append(this.arguments[i].shortReadableName());
            }
            nameBuffer.append('>');
        }
        int nameLength = nameBuffer.length();
        char[] shortReadableName = new char[nameLength];
        nameBuffer.getChars(0, nameLength, shortReadableName, 0);
        return shortReadableName;
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#signature()
	 */
    public char[] signature() {
        if (this.signature == null) {
            // erasure
            this.signature = this.type.signature();
        }
        return this.signature;
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#sourceName()
	 */
    public char[] sourceName() {
        return this.type.sourceName();
    }

    /**
	 * Returns a type, where original type was substituted using the receiver
	 * parameterized type.
	 */
    public TypeBinding substitute(TypeBinding originalType) {
        if ((originalType.tagBits & TagBits.HasTypeVariable) != 0) {
            if (originalType.isTypeVariable()) {
                TypeVariableBinding originalVariable = (TypeVariableBinding) originalType;
                ParameterizedTypeBinding currentType = this;
                while (true) {
                    if (currentType.arguments != null) {
                        TypeVariableBinding[] typeVariables = currentType.type.typeVariables();
                        int length = typeVariables.length;
                        // check this variable can be substituted given parameterized type
                        if (originalVariable.rank < length && typeVariables[originalVariable.rank] == originalVariable) {
                            return currentType.arguments[originalVariable.rank];
                        }
                    }
                    // recurse on enclosing type, as it may hold more substitutions to perform
                    ReferenceBinding enclosing = currentType.enclosingType();
                    if (!(enclosing instanceof ParameterizedTypeBinding))
                        break;
                    currentType = (ParameterizedTypeBinding) enclosing;
                }
            } else if (originalType.isParameterizedType()) {
                ParameterizedTypeBinding originalParameterizedType = (ParameterizedTypeBinding) originalType;
                TypeBinding[] originalArguments = originalParameterizedType.arguments;
                TypeBinding[] substitutedArguments = Scope.substitute(this, originalArguments);
                if (substitutedArguments != originalArguments) {
                    identicalVariables: // if substituted with original variables, then answer the generic type itself
                    {
                        TypeVariableBinding[] originalVariables = originalParameterizedType.type.typeVariables();
                        for (int i = 0, length = originalVariables.length; i < length; i++) {
                            if (substitutedArguments[i] != originalVariables[i])
                                break identicalVariables;
                        }
                        return originalParameterizedType.type;
                    }
                    return this.environment.createParameterizedType(originalParameterizedType.type, substitutedArguments, originalParameterizedType.enclosingType);
                }
            } else if (originalType.isArrayType()) {
                TypeBinding originalLeafComponentType = originalType.leafComponentType();
                // substitute could itself be array type
                TypeBinding substitute = substitute(originalLeafComponentType);
                if (substitute != originalLeafComponentType) {
                    return this.environment.createArrayType(substitute.leafComponentType(), substitute.dimensions() + originalType.dimensions());
                }
            } else if (originalType.isWildcard()) {
                WildcardBinding wildcard = (WildcardBinding) originalType;
                if (wildcard.kind != Wildcard.UNBOUND) {
                    TypeBinding originalBound = wildcard.bound;
                    TypeBinding substitutedBound = substitute(originalBound);
                    if (substitutedBound != originalBound) {
                        return this.environment.createWildcard(wildcard.genericType, wildcard.rank, substitutedBound, wildcard.kind);
                    }
                }
            }
        } else if (originalType.isGenericType()) {
            // treat as if parameterized with its type variables
            ReferenceBinding originalGenericType = (ReferenceBinding) originalType;
            TypeVariableBinding[] originalVariables = originalGenericType.typeVariables();
            int length = originalVariables.length;
            TypeBinding[] originalArguments;
            System.arraycopy(originalVariables, 0, originalArguments = new TypeBinding[length], 0, length);
            TypeBinding[] substitutedArguments = Scope.substitute(this, originalArguments);
            if (substitutedArguments != originalArguments) {
                return this.environment.createParameterizedType(originalGenericType, substitutedArguments, null);
            }
        }
        return originalType;
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#superclass()
	 */
    public ReferenceBinding superclass() {
        if (this.superclass == null) {
            // note: Object cannot be generic
            ReferenceBinding genericSuperclass = this.type.superclass();
            // e.g. interfaces
            if (genericSuperclass == null)
                return null;
            this.superclass = (ReferenceBinding) substitute(genericSuperclass);
        }
        return this.superclass;
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#superInterfaces()
	 */
    public ReferenceBinding[] superInterfaces() {
        if (this.superInterfaces == null) {
            this.superInterfaces = Scope.substitute(this, this.type.superInterfaces());
        }
        return this.superInterfaces;
    }

    public void swapUnresolved(UnresolvedReferenceBinding unresolvedType, ReferenceBinding resolvedType, LookupEnvironment env) {
        boolean update = false;
        if (this.type == unresolvedType) {
            // cannot be raw since being parameterized below
            this.type = resolvedType;
            update = true;
        }
        if (this.arguments != null) {
            for (int i = 0, l = this.arguments.length; i < l; i++) {
                if (this.arguments[i] == unresolvedType) {
                    this.arguments[i] = resolvedType.isGenericType() ? env.createRawType(resolvedType, null) : resolvedType;
                    update = true;
                }
            }
        }
        if (update)
            initialize(this.type, this.arguments);
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#syntheticEnclosingInstanceTypes()
	 */
    public ReferenceBinding[] syntheticEnclosingInstanceTypes() {
        return this.type.syntheticEnclosingInstanceTypes();
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#syntheticOuterLocalVariables()
	 */
    public SyntheticArgumentBinding[] syntheticOuterLocalVariables() {
        return this.type.syntheticOuterLocalVariables();
    }

    /**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#qualifiedPackageName()
	 */
    public char[] qualifiedPackageName() {
        return this.type.qualifiedPackageName();
    }

    /**
	 * @see java.lang.Object#toString()
	 */
    public String toString() {
        StringBuffer buffer = new StringBuffer(30);
        //$NON-NLS-1$
        if (isDeprecated())
            buffer.append("deprecated ");
        //$NON-NLS-1$
        if (isPublic())
            buffer.append("public ");
        //$NON-NLS-1$
        if (isProtected())
            buffer.append("protected ");
        //$NON-NLS-1$
        if (isPrivate())
            buffer.append("private ");
        //$NON-NLS-1$
        if (isAbstract() && isClass())
            buffer.append("abstract ");
        //$NON-NLS-1$
        if (isStatic() && isNestedType())
            buffer.append("static ");
        //$NON-NLS-1$
        if (isFinal())
            buffer.append("final ");
        //$NON-NLS-1$ //$NON-NLS-2$
        buffer.append(isInterface() ? "interface " : "class ");
        buffer.append(this.debugName());
        //$NON-NLS-1$
        buffer.append("\n\textends ");
        //$NON-NLS-1$
        buffer.append((superclass != null) ? superclass.debugName() : "NULL TYPE");
        if (superInterfaces != null) {
            if (superInterfaces != NoSuperInterfaces) {
                //$NON-NLS-1$
                buffer.append(//$NON-NLS-1$
                "\n\timplements : ");
                for (int i = 0, length = superInterfaces.length; i < length; i++) {
                    if (i > 0)
                        //$NON-NLS-1$
                        buffer.append(", ");
                    buffer.append(//$NON-NLS-1$
                    (superInterfaces[i] != null) ? //$NON-NLS-1$
                    superInterfaces[i].debugName() : //$NON-NLS-1$
                    "NULL TYPE");
                }
            }
        } else {
            //$NON-NLS-1$
            buffer.append("NULL SUPERINTERFACES");
        }
        if (enclosingType() != null) {
            //$NON-NLS-1$
            buffer.append("\n\tenclosing type : ");
            buffer.append(enclosingType().debugName());
        }
        if (fields != null) {
            if (fields != NoFields) {
                //$NON-NLS-1$
                buffer.append(//$NON-NLS-1$
                "\n/*   fields   */");
                for (int i = 0, length = fields.length; i < length; i++) //$NON-NLS-1$ 
                buffer.append('\n').append((fields[i] != null) ? fields[i].toString() : "NULL FIELD");
            }
        } else {
            //$NON-NLS-1$
            buffer.append("NULL FIELDS");
        }
        if (methods != null) {
            if (methods != NoMethods) {
                //$NON-NLS-1$
                buffer.append(//$NON-NLS-1$
                "\n/*   methods   */");
                for (int i = 0, length = methods.length; i < length; i++) //$NON-NLS-1$ //$NON-NLS-2$
                buffer.append('\n').append((methods[i] != null) ? methods[i].toString() : "NULL METHOD");
            }
        } else {
            //$NON-NLS-1$
            buffer.append("NULL METHODS");
        }
        //		if (memberTypes != null) {
        //			if (memberTypes != NoMemberTypes) {
        //				buffer.append("\n/*   members   */"); //$NON-NLS-1$
        //				for (int i = 0, length = memberTypes.length; i < length; i++)
        //					buffer.append('\n').append((memberTypes[i] != null) ? memberTypes[i].toString() : "NULL TYPE"); //$NON-NLS-1$ //$NON-NLS-2$
        //			}
        //		} else {
        //			buffer.append("NULL MEMBER TYPES"); //$NON-NLS-1$
        //		}
        //$NON-NLS-1$
        buffer.append("\n\n");
        return buffer.toString();
    }

    public TypeVariableBinding[] typeVariables() {
        if (this.arguments == null) {
            // retain original type variables if not substituted (member type of parameterized type)
            return this.type.typeVariables();
        }
        return NoTypeVariables;
    }
}
