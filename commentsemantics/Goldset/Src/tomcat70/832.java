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
package org.apache.catalina.valves;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public final class RemoteAddrValve extends RequestFilterValve {

    private static final Log log = LogFactory.getLog(RemoteAddrValve.class);

    // ----------------------------------------------------- Instance Variables
    /**
     * The descriptive information related to this implementation.
     */
    private static final String info = "org.apache.catalina.valves.RemoteAddrValve/1.0";

    /**
     * Flag deciding whether we add the server connector port to the property
     * compared in the filtering method. The port will be appended
     * using a ";" as a separator.
     */
    volatile boolean addConnectorPort = false;

    // ------------------------------------------------------------- Properties
    /**
     * Return descriptive information about this Valve implementation.
     */
    @Override
    public String getInfo() {
        return (info);
    }

    /**
     * Get the flag deciding whether we add the server connector port to the
     * property compared in the filtering method. The port will be appended
     * using a ";" as a separator.
     */
    public boolean getAddConnectorPort() {
        return addConnectorPort;
    }

    /**
     * Set the flag deciding whether we add the server connector port to the
     * property compared in the filtering method. The port will be appended
     * using a ";" as a separator.
     *
     * @param addConnectorPort The new flag
     */
    public void setAddConnectorPort(boolean addConnectorPort) {
        this.addConnectorPort = addConnectorPort;
    }

    // --------------------------------------------------------- Public Methods
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        String property;
        if (addConnectorPort) {
            property = request.getRequest().getRemoteAddr() + ";" + request.getConnector().getPort();
        } else {
            property = request.getRequest().getRemoteAddr();
        }
        process(property, request, response);
    }

    @Override
    protected Log getLog() {
        return log;
    }
}
