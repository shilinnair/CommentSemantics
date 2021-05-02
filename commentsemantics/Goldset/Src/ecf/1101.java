/*
 * $Header: /home/data/cvs/rt/org.eclipse.ecf/tests/bundles/org.eclipse.ecf.tests.apache.httpclient.server/src/org/apache/commons/httpclient/TestRequestHeaders.java,v 1.1 2009/02/13 18:07:48 slewis Exp $
 * $Revision: 1.1 $
 * $Date: 2009/02/13 18:07:48 $
 * ====================================================================
 *
 *  Copyright 1999-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */
package org.apache.commons.httpclient;

import org.apache.commons.httpclient.protocol.Protocol;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for reading response headers.
 *
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever</a>
 * @version $Id: TestRequestHeaders.java,v 1.1 2009/02/13 18:07:48 slewis Exp $
 */
public class TestRequestHeaders extends TestCase {

    // ------------------------------------------------------------ Constructor
    public  TestRequestHeaders(String testName) {
        super(testName);
    }

    // ------------------------------------------------------------------- Main
    public static void main(String args[]) {
        String[] testCaseName = { TestRequestHeaders.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    // ------------------------------------------------------- TestCase Methods
    public static Test suite() {
        return new TestSuite(TestRequestHeaders.class);
    }

    public void testNullHeader() throws Exception {
        FakeHttpMethod method = new FakeHttpMethod();
        assertEquals(null, method.getRequestHeader(null));
        assertEquals(null, method.getRequestHeader("bogus"));
    }

    public void testHostHeaderPortHTTP80() throws Exception {
        HttpConnection conn = new HttpConnection("some.host.name", 80);
        HttpState state = new HttpState();
        FakeHttpMethod method = new FakeHttpMethod();
        method.addRequestHeaders(state, conn);
        assertEquals("Host: some.host.name", method.getRequestHeader("Host").toString().trim());
    }

    public void testHostHeaderPortHTTP81() throws Exception {
        HttpConnection conn = new HttpConnection("some.host.name", 81);
        HttpState state = new HttpState();
        FakeHttpMethod method = new FakeHttpMethod();
        method.addRequestHeaders(state, conn);
        assertEquals("Host: some.host.name:81", method.getRequestHeader("Host").toString().trim());
    }

    public void testHostHeaderPortHTTPS443() throws Exception {
        HttpConnection conn = new HttpConnection("some.host.name", 443, Protocol.getProtocol("https"));
        HttpState state = new HttpState();
        FakeHttpMethod method = new FakeHttpMethod();
        method.addRequestHeaders(state, conn);
        assertEquals("Host: some.host.name", method.getRequestHeader("Host").toString().trim());
    }

    public void testHostHeaderPortHTTPS444() throws Exception {
        HttpConnection conn = new HttpConnection("some.host.name", 444, Protocol.getProtocol("https"));
        HttpState state = new HttpState();
        FakeHttpMethod method = new FakeHttpMethod();
        method.addRequestHeaders(state, conn);
        assertEquals("Host: some.host.name:444", method.getRequestHeader("Host").toString().trim());
    }

    public void testHeadersPreserveCaseKeyIgnoresCase() throws Exception {
        FakeHttpMethod method = new FakeHttpMethod();
        method.addRequestHeader(new Header("NAME", "VALUE"));
        Header upHeader = method.getRequestHeader("NAME");
        Header loHeader = method.getRequestHeader("name");
        Header mixHeader = method.getRequestHeader("nAmE");
        assertEquals("NAME", upHeader.getName());
        assertEquals("VALUE", upHeader.getValue());
        assertEquals("NAME", loHeader.getName());
        assertEquals("VALUE", loHeader.getValue());
        assertEquals("NAME", mixHeader.getName());
        assertEquals("VALUE", mixHeader.getValue());
    }
}
