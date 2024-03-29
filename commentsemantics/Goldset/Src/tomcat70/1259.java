/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomcat.util.digester;

import org.apache.tomcat.util.IntrospectionUtils;

public class SetTopRule extends Rule {

    /**
     * Construct a "set parent" rule with the specified method name.  The
     * "set parent" method's argument type is assumed to be the class of the
     * parent object.
     *
     * @param digester The associated Digester
     * @param methodName Method name of the "set parent" method to call
     *
     * @deprecated The digester instance is now set in the {@link Digester#addRule} method. 
     * Use {@link #SetTopRule(String methodName)} instead.
     */
    @Deprecated
    public  SetTopRule(Digester digester, String methodName) {
        this(methodName);
    }

    /**
     * Construct a "set parent" rule with the specified method name.
     *
     * @param digester The associated Digester
     * @param methodName Method name of the "set parent" method to call
     * @param paramType Java class of the "set parent" method's argument
     *  (if you wish to use a primitive type, specify the corresponding
     *  Java wrapper class instead, such as <code>java.lang.Boolean</code>
     *  for a <code>boolean</code> parameter)
     *
     * @deprecated The digester instance is now set in the {@link Digester#addRule} method. 
     * Use {@link #SetTopRule(String methodName, String paramType)} instead.
     */
    @Deprecated
    public  SetTopRule(Digester digester, String methodName, String paramType) {
        this(methodName, paramType);
    }

    /**
     * Construct a "set parent" rule with the specified method name.  The
     * "set parent" method's argument type is assumed to be the class of the
     * parent object.
     *
     * @param methodName Method name of the "set parent" method to call
     */
    public  SetTopRule(String methodName) {
        this(methodName, null);
    }

    /**
     * Construct a "set parent" rule with the specified method name.
     *
     * @param methodName Method name of the "set parent" method to call
     * @param paramType Java class of the "set parent" method's argument
     *  (if you wish to use a primitive type, specify the corresponding
     *  Java wrapper class instead, such as <code>java.lang.Boolean</code>
     *  for a <code>boolean</code> parameter)
     */
    public  SetTopRule(String methodName, String paramType) {
        this.methodName = methodName;
        this.paramType = paramType;
    }

    // ----------------------------------------------------- Instance Variables
    /**
     * The method name to call on the child object.
     */
    protected String methodName = null;

    /**
     * The Java class name of the parameter type expected by the method.
     */
    protected String paramType = null;

    /**
     * Should we use exact matching. Default is no.
     */
    protected boolean useExactMatch = false;

    // --------------------------------------------------------- Public Methods
    /**
     * <p>Is exact matching being used.</p>
     *
     * <p>This rule uses <code>org.apache.commons.beanutils.MethodUtils</code> 
     * to introspect the relevant objects so that the right method can be called.
     * Originally, <code>MethodUtils.invokeExactMethod</code> was used.
     * This matches methods very strictly 
     * and so may not find a matching method when one exists.
     * This is still the behaviour when exact matching is enabled.</p>
     *
     * <p>When exact matching is disabled, <code>MethodUtils.invokeMethod</code> is used.
     * This method finds more methods but is less precise when there are several methods 
     * with correct signatures.
     * So, if you want to choose an exact signature you might need to enable this property.</p>
     *
     * <p>The default setting is to disable exact matches.</p>
     *
     * @return true iff exact matching is enabled
     * @since Digester Release 1.1.1
     */
    public boolean isExactMatch() {
        return useExactMatch;
    }

    /**
     * <p>Set whether exact matching is enabled.</p>
     *
     * <p>See {@link #isExactMatch()}.</p>
     *
     * @param useExactMatch should this rule use exact method matching
     * @since Digester Release 1.1.1
     */
    public void setExactMatch(boolean useExactMatch) {
        this.useExactMatch = useExactMatch;
    }

    /**
     * Process the end of this element.
     * 
     * @param namespace the namespace URI of the matching element, or an 
     *   empty string if the parser is not namespace aware or the element has
     *   no namespace
     * @param name the local name if the parser is namespace aware, or just 
     *   the element name otherwise
     */
    @Override
    public void end(String namespace, String name) throws Exception {
        // Identify the objects to be used
        Object child = digester.peek(0);
        Object parent = digester.peek(1);
        if (digester.log.isDebugEnabled()) {
            if (child == null) {
                digester.log.debug("[SetTopRule]{" + digester.match + "} Call [NULL CHILD]." + methodName + "(" + parent + ")");
            } else {
                digester.log.debug("[SetTopRule]{" + digester.match + "} Call " + child.getClass().getName() + "." + methodName + "(" + parent + ")");
            }
        }
        // Call the specified method
        IntrospectionUtils.callMethod1(child, methodName, parent, paramType, digester.getClassLoader());
    }

    /**
     * Render a printable version of this Rule.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SetTopRule[");
        sb.append("methodName=");
        sb.append(methodName);
        sb.append(", paramType=");
        sb.append(paramType);
        sb.append("]");
        return (sb.toString());
    }
}
