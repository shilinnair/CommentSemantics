/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.el.parser;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.MethodInfo;
import javax.el.PropertyNotFoundException;
import javax.el.ValueReference;
import org.apache.el.lang.ELSupport;
import org.apache.el.lang.EvaluationContext;
import org.apache.el.util.MessageFactory;
import org.apache.el.util.ReflectionUtil;

/**
 * @author Jacob Hookom [jacob@hookom.net]
 */
public final class AstValue extends SimpleNode {

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private static final boolean IS_SECURITY_ENABLED = (System.getSecurityManager() != null);

    protected static final boolean COERCE_TO_ZERO;

    static {
        String coerceToZeroStr;
        if (IS_SECURITY_ENABLED) {
            coerceToZeroStr = AccessController.doPrivileged(new PrivilegedAction<String>() {

                @Override
                public String run() {
                    return System.getProperty("org.apache.el.parser.COERCE_TO_ZERO", "true");
                }
            });
        } else {
            coerceToZeroStr = System.getProperty("org.apache.el.parser.COERCE_TO_ZERO", "true");
        }
        COERCE_TO_ZERO = Boolean.parseBoolean(coerceToZeroStr);
    }

    protected static class Target {

        protected Object base;

        protected Object property;
    }

    public  AstValue(int id) {
        super(id);
    }

    @Override
    public Class<?> getType(EvaluationContext ctx) throws ELException {
        Target t = getTarget(ctx);
        ctx.setPropertyResolved(false);
        Class<?> result = ctx.getELResolver().getType(ctx, t.base, t.property);
        if (!ctx.isPropertyResolved()) {
            throw new PropertyNotFoundException(MessageFactory.get("error.resolver.unhandled", t.base, t.property));
        }
        return result;
    }

    private final Target getTarget(EvaluationContext ctx) throws ELException {
        // evaluate expr-a to value-a
        Object base = this.children[0].getValue(ctx);
        // if our base is null (we know there are more properties to evaluate)
        if (base == null) {
            throw new PropertyNotFoundException(MessageFactory.get("error.unreachable.base", this.children[0].getImage()));
        }
        // set up our start/end
        Object property = null;
        int propCount = this.jjtGetNumChildren();
        int i = 1;
        // Evaluate any properties or methods before our target
        ELResolver resolver = ctx.getELResolver();
        while (i < propCount) {
            if (i + 2 < propCount && this.children[i + 1] instanceof AstMethodParameters) {
                // Method call not at end of expression
                base = resolver.invoke(ctx, base, this.children[i].getValue(ctx), null, ((AstMethodParameters) this.children[i + 1]).getParameters(ctx));
                i += 2;
            } else if (i + 2 == propCount && this.children[i + 1] instanceof AstMethodParameters) {
                // Method call at end of expression
                ctx.setPropertyResolved(false);
                property = this.children[i].getValue(ctx);
                i += 2;
                if (property == null) {
                    throw new PropertyNotFoundException(MessageFactory.get("error.unreachable.property", property));
                }
            } else if (i + 1 < propCount) {
                // Object with property not at end of expression
                property = this.children[i].getValue(ctx);
                ctx.setPropertyResolved(false);
                base = resolver.getValue(ctx, base, property);
                i++;
            } else {
                // Object with property at end of expression
                ctx.setPropertyResolved(false);
                property = this.children[i].getValue(ctx);
                i++;
                if (property == null) {
                    throw new PropertyNotFoundException(MessageFactory.get("error.unreachable.property", property));
                }
            }
            if (base == null) {
                throw new PropertyNotFoundException(MessageFactory.get("error.unreachable.property", property));
            }
        }
        Target t = new Target();
        t.base = base;
        t.property = property;
        return t;
    }

    @Override
    public Object getValue(EvaluationContext ctx) throws ELException {
        Object base = this.children[0].getValue(ctx);
        int propCount = this.jjtGetNumChildren();
        int i = 1;
        Object suffix = null;
        ELResolver resolver = ctx.getELResolver();
        while (base != null && i < propCount) {
            suffix = this.children[i].getValue(ctx);
            if (i + 1 < propCount && (this.children[i + 1] instanceof AstMethodParameters)) {
                AstMethodParameters mps = (AstMethodParameters) this.children[i + 1];
                // This is a method
                Object[] paramValues = mps.getParameters(ctx);
                base = resolver.invoke(ctx, base, suffix, getTypesFromValues(paramValues), paramValues);
                i += 2;
            } else {
                // This is a property
                if (suffix == null) {
                    return null;
                }
                ctx.setPropertyResolved(false);
                base = resolver.getValue(ctx, base, suffix);
                i++;
            }
        }
        if (!ctx.isPropertyResolved()) {
            throw new PropertyNotFoundException(MessageFactory.get("error.resolver.unhandled", base, suffix));
        }
        return base;
    }

    @Override
    public boolean isReadOnly(EvaluationContext ctx) throws ELException {
        Target t = getTarget(ctx);
        ctx.setPropertyResolved(false);
        boolean result = ctx.getELResolver().isReadOnly(ctx, t.base, t.property);
        if (!ctx.isPropertyResolved()) {
            throw new PropertyNotFoundException(MessageFactory.get("error.resolver.unhandled", t.base, t.property));
        }
        return result;
    }

    @Override
    public void setValue(EvaluationContext ctx, Object value) throws ELException {
        Target t = getTarget(ctx);
        ctx.setPropertyResolved(false);
        ELResolver resolver = ctx.getELResolver();
        // coerce to the expected type
        Class<?> targetClass = resolver.getType(ctx, t.base, t.property);
        if (COERCE_TO_ZERO == true || !isAssignable(value, targetClass)) {
            resolver.setValue(ctx, t.base, t.property, ELSupport.coerceToType(value, targetClass));
        } else {
            resolver.setValue(ctx, t.base, t.property, value);
        }
        if (!ctx.isPropertyResolved()) {
            throw new PropertyNotFoundException(MessageFactory.get("error.resolver.unhandled", t.base, t.property));
        }
    }

    private boolean isAssignable(Object value, Class<?> targetClass) {
        if (targetClass == null) {
            return false;
        } else if (value != null && targetClass.isPrimitive()) {
            return false;
        } else if (value != null && !targetClass.isInstance(value)) {
            return false;
        }
        return true;
    }

    @Override
    public // Interface el.parser.Node uses raw types (and is auto-generated)
    MethodInfo getMethodInfo(EvaluationContext ctx, @SuppressWarnings("rawtypes") Class[] paramTypes) throws ELException {
        Target t = getTarget(ctx);
        Method m = ReflectionUtil.getMethod(t.base, t.property, paramTypes, null);
        return new MethodInfo(m.getName(), m.getReturnType(), m.getParameterTypes());
    }

    @Override
    public // Interface el.parser.Node uses a raw type (and is auto-generated)
    Object invoke(EvaluationContext ctx, @SuppressWarnings("rawtypes") Class[] paramTypes, Object[] paramValues) throws ELException {
        Target t = getTarget(ctx);
        Method m = null;
        Object[] values = null;
        Class<?>[] types = null;
        if (isParametersProvided()) {
            values = ((AstMethodParameters) this.jjtGetChild(this.jjtGetNumChildren() - 1)).getParameters(ctx);
            types = getTypesFromValues(values);
        } else {
            values = paramValues;
            types = paramTypes;
        }
        m = ReflectionUtil.getMethod(t.base, t.property, types, values);
        // Handle varArgs and any coercion required
        values = convertArgs(values, m);
        Object result = null;
        try {
            result = m.invoke(t.base, values);
        } catch (IllegalAccessException iae) {
            throw new ELException(iae);
        } catch (IllegalArgumentException iae) {
            throw new ELException(iae);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof ThreadDeath) {
                throw (ThreadDeath) cause;
            }
            if (cause instanceof VirtualMachineError) {
                throw (VirtualMachineError) cause;
            }
            throw new ELException(cause);
        }
        return result;
    }

    private Object[] convertArgs(Object[] src, Method m) {
        Class<?>[] types = m.getParameterTypes();
        if (types.length == 0) {
            // Treated as if parameters have been provided so src is ignored
            return EMPTY_ARRAY;
        }
        int paramCount = types.length;
        if (m.isVarArgs() && paramCount > 1 && (src == null || paramCount > src.length) || !m.isVarArgs() && (paramCount > 0 && src == null || src != null && src.length != paramCount)) {
            String srcCount = null;
            if (src != null) {
                srcCount = Integer.toString(src.length);
            }
            String msg;
            if (m.isVarArgs()) {
                msg = MessageFactory.get("error.invoke.tooFewParams", m.getName(), srcCount, Integer.toString(paramCount));
            } else {
                msg = MessageFactory.get("error.invoke.wrongParams", m.getName(), srcCount, Integer.toString(paramCount));
            }
            throw new IllegalArgumentException(msg);
        }
        if (src == null) {
            // contents of the array
            return new Object[1];
        }
        Object[] dest = new Object[paramCount];
        for (int i = 0; i < paramCount - 1; i++) {
            dest[i] = ELSupport.coerceToType(src[i], types[i]);
        }
        if (m.isVarArgs()) {
            Object[] varArgs = (Object[]) Array.newInstance(m.getParameterTypes()[paramCount - 1].getComponentType(), src.length - (paramCount - 1));
            for (int i = 0; i < src.length - (paramCount - 1); i++) {
                varArgs[i] = ELSupport.coerceToType(src[paramCount - 1 + i], types[paramCount - 1].getComponentType());
            }
            dest[paramCount - 1] = varArgs;
        } else {
            dest[paramCount - 1] = ELSupport.coerceToType(src[paramCount - 1], types[paramCount - 1]);
        }
        return dest;
    }

    private Class<?>[] getTypesFromValues(Object[] values) {
        if (values == null) {
            return null;
        }
        Class<?> result[] = new Class<?>[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                result[i] = null;
            } else {
                result[i] = values[i].getClass();
            }
        }
        return result;
    }

    /**
     * @since EL 2.2
     */
    @Override
    public ValueReference getValueReference(EvaluationContext ctx) {
        // Check this is a reference to a base and a property
        if (this.children.length > 2 && this.jjtGetChild(2) instanceof AstMethodParameters) {
            // This is a method call
            return null;
        }
        Target t = getTarget(ctx);
        return new ValueReference(t.base, t.property);
    }

    /**
     * @since EL 2.2
     */
    @Override
    public boolean isParametersProvided() {
        // Assumption is that method parameters, if present, will be the last
        // child
        int len = children.length;
        if (len > 2) {
            if (this.jjtGetChild(len - 1) instanceof AstMethodParameters) {
                return true;
            }
        }
        return false;
    }
}
