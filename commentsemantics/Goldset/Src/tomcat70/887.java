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

import java.util.ArrayList;
import org.apache.el.lang.EvaluationContext;

public final class AstMethodParameters extends SimpleNode {

    public  AstMethodParameters(int id) {
        super(id);
    }

    public Object[] getParameters(EvaluationContext ctx) {
        ArrayList<Object> params = new ArrayList<Object>();
        for (int i = 0; i < this.jjtGetNumChildren(); i++) {
            params.add(this.jjtGetChild(i).getValue(ctx));
        }
        return params.toArray(new Object[params.size()]);
    }

    public Class<?>[] getParameterTypes(EvaluationContext ctx) {
        ArrayList<Class<?>> paramTypes = new ArrayList<Class<?>>();
        for (int i = 0; i < this.jjtGetNumChildren(); i++) {
            paramTypes.add(this.jjtGetChild(i).getType(ctx));
        }
        return paramTypes.toArray(new Class<?>[paramTypes.size()]);
    }
}
