/*
 * $HeadURL: https://svn.apache.org/repos/asf/jakarta/commons/proper/httpclient/branches/HTTPCLIENT_3_0_BRANCH/src/test/org/apache/commons/httpclient/TestURI.java $
 * $Revision: 1.1 $
 * $Date: 2009/02/13 18:07:47 $
 *
 * ====================================================================
 *
 *  Copyright 2003-2004 The Apache Software Foundation
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
 */
package org.apache.commons.httpclient;

import org.apache.commons.httpclient.methods.GetMethod;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Simple tests for the URI class.
 * 
 * @author Michael Becke
 */
public class TestURI extends TestCase {

    /**
     * Constructor for TestURI.
     * @param testName
     */
    public  TestURI(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestURI.class);
    }

    public void testIPv4Address() throws URIException {
        URI base = new URI("http://10.0.1.10:8830", false);
        URI uri = base;
        assertTrue("Should be an IPv4 address", uri.isIPv4address());
        uri = new URI(base, "/04-1.html", false);
        assertTrue("Should be an IPv4 address", uri.isIPv4address());
        uri = new URI("/04-1.html", false);
        assertFalse("Should NOT be an IPv4 address", uri.isIPv4address());
        uri = new URI(base, "http://10.0.1.10:8830/04-1.html", false);
        assertTrue("Should be an IPv4 address", uri.isIPv4address());
        uri = new URI("http://10.0.1.10:8830/04-1.html", false);
        assertTrue("Should be an IPv4 address", uri.isIPv4address());
        uri = new URI(base, "http://host.org/04-1.html", false);
        assertFalse("Should NOT be an IPv4 address", uri.isIPv4address());
        uri = new URI("http://host.org/04-1.html", false);
        assertFalse("Should NOT be an IPv4 address", uri.isIPv4address());
    }

    public void testUrl() throws URIException {
        URI url = new HttpURL("http://jakarta.apache.org");
        assertEquals(80, url.getPort());
        assertEquals("http", url.getScheme());
        url = new HttpsURL("https://jakarta.apache.org");
        assertEquals(443, url.getPort());
        assertEquals("https", url.getScheme());
    }

    /**
     * Tests the URI(URI, String) constructor.  This tests URIs ability to
     * resolve relative URIs.
     */
    public void testRelativeURIConstructor() {
        URI baseURI = null;
        try {
            baseURI = new URI("http://a/b/c/d;p?q", false);
        } catch (URIException e) {
            fail("unable to create base URI: " + e);
        }
        // the following is an array of arrays in the following order
        // relative URI, scheme, host(authority), path, query, fragment, abs. URI
        //
        // these examples were taken from rfc 2396
        String[][] testRelativeURIs = { { "g:h", "g", null, "h", null, null, "g:h" }, { "g", "http", "a", "/b/c/g", null, null, "http://a/b/c/g" }, { "./g", "http", "a", "/b/c/g", null, null, "http://a/b/c/g" }, { "g/", "http", "a", "/b/c/g/", null, null, "http://a/b/c/g/" }, { "/g", "http", "a", "/g", null, null, "http://a/g" }, { "//g", "http", "g", null, null, null, "http://g" }, { "?y", "http", "a", "/b/c/", "y", null, "http://a/b/c/?y" }, { "g?y", "http", "a", "/b/c/g", "y", null, "http://a/b/c/g?y" }, { "#s", "http", "a", "/b/c/d;p", "q", "s", "http://a/b/c/d;p?q#s" }, { "#", "http", "a", "/b/c/d;p", "q", "", "http://a/b/c/d;p?q#" }, { "", "http", "a", "/b/c/d;p", "q", null, "http://a/b/c/d;p?q" }, { "g#s", "http", "a", "/b/c/g", null, "s", "http://a/b/c/g#s" }, { "g?y#s", "http", "a", "/b/c/g", "y", "s", "http://a/b/c/g?y#s" }, { ";x", "http", "a", "/b/c/;x", null, null, "http://a/b/c/;x" }, { "g;x", "http", "a", "/b/c/g;x", null, null, "http://a/b/c/g;x" }, { "g;x?y#s", "http", "a", "/b/c/g;x", "y", "s", "http://a/b/c/g;x?y#s" }, { ".", "http", "a", "/b/c/", null, null, "http://a/b/c/" }, { "./", "http", "a", "/b/c/", null, null, "http://a/b/c/" }, { "..", "http", "a", "/b/", null, null, "http://a/b/" }, { "../", "http", "a", "/b/", null, null, "http://a/b/" }, { "../g", "http", "a", "/b/g", null, null, "http://a/b/g" }, { "../..", "http", "a", "/", null, null, "http://a/" }, { "../../", "http", "a", "/", null, null, "http://a/" }, { "../../g", "http", "a", "/g", null, null, "http://a/g" }, { "../../../g", "http", "a", "/g", null, null, "http://a/g" }, { "../../../../g", "http", "a", "/g", null, null, "http://a/g" }, { "/./g", "http", "a", "/g", null, null, "http://a/g" }, { "/../g", "http", "a", "/g", null, null, "http://a/g" }, { "g.", "http", "a", "/b/c/g.", null, null, "http://a/b/c/g." }, { ".g", "http", "a", "/b/c/.g", null, null, "http://a/b/c/.g" }, { "g..", "http", "a", "/b/c/g..", null, null, "http://a/b/c/g.." }, { "..g", "http", "a", "/b/c/..g", null, null, "http://a/b/c/..g" }, { "./../g", "http", "a", "/b/g", null, null, "http://a/b/g" }, { "./g/.", "http", "a", "/b/c/g/", null, null, "http://a/b/c/g/" }, { "g/./h", "http", "a", "/b/c/g/h", null, null, "http://a/b/c/g/h" }, { "g/../h", "http", "a", "/b/c/h", null, null, "http://a/b/c/h" }, { "g;x=1/./y", "http", "a", "/b/c/g;x=1/y", null, null, "http://a/b/c/g;x=1/y" }, { "g;x=1/../y", "http", "a", "/b/c/y", null, null, "http://a/b/c/y" }, { "g?y/./x", "http", "a", "/b/c/g", "y/./x", null, "http://a/b/c/g?y/./x" }, { "g?y/../x", "http", "a", "/b/c/g", "y/../x", null, "http://a/b/c/g?y/../x" }, { "g#s/./x", "http", "a", "/b/c/g", null, "s/./x", "http://a/b/c/g#s/./x" }, { "g#s/../x", "http", "a", "/b/c/g", null, "s/../x", "http://a/b/c/g#s/../x" }, // see issue #35148
        { ":g", "http", "a", "/b/c/:g", null, null, "http://a/b/c/:g" } };
        for (int i = 0; i < testRelativeURIs.length; i++) {
            URI testURI = null;
            try {
                testURI = new URI(baseURI, testRelativeURIs[i][0], false);
            } catch (URIException e) {
                e.printStackTrace();
                fail("unable to create URI with relative value(" + testRelativeURIs[i][0] + "): " + e);
            }
            try {
                assertEquals("array index " + i, testRelativeURIs[i][1], testURI.getScheme());
                assertEquals("array index " + i, testRelativeURIs[i][2], testURI.getAuthority());
                assertEquals("array index " + i, testRelativeURIs[i][3], testURI.getPath());
                assertEquals("array index " + i, testRelativeURIs[i][4], testURI.getQuery());
                assertEquals("array index " + i, testRelativeURIs[i][5], testURI.getFragment());
                assertEquals("array index " + i, testRelativeURIs[i][6], testURI.getURIReference());
            } catch (URIException e) {
                fail("error getting URI property: " + e);
            }
        }
    }

    public void testTestURIAuthorityString() throws Exception {
        URI url = new URI("ftp", "user:password", "localhost", -1, "/");
        assertEquals("ftp://user:password@localhost/", url.toString());
        assertEquals("user:password@localhost", url.getAuthority());
    }

    public void testTestHttpUrlAuthorityString() throws Exception {
        HttpURL url = new HttpURL("localhost", -1, "/");
        assertEquals("http://localhost/", url.toString());
        url.setRawUserinfo("user".toCharArray(), "password".toCharArray());
        assertEquals("http://localhost/", url.toString());
        assertEquals("user:password@localhost", url.getAuthority());
        url = new HttpURL("user#@", "pass#@", "localhost", 8080, "/");
        assertEquals("http://localhost:8080/", url.toString());
        assertEquals("user#@:pass#@", url.getUserinfo());
        assertEquals("user%23%40:pass%23%40", url.getEscapedUserinfo());
        url = new HttpURL("user%23%40:pass%23%40", "localhost", 8080, "/");
        assertEquals("http://localhost:8080/", url.toString());
        assertEquals("user#@:pass#@", url.getUserinfo());
        assertEquals("user%23%40:pass%23%40", url.getEscapedUserinfo());
        url = new HttpURL("localhost", 8080, "/");
        assertEquals("http://localhost:8080/", url.toString());
        url.setRawUserinfo("user".toCharArray(), "password".toCharArray());
        assertEquals("http://localhost:8080/", url.toString());
        assertEquals("user:password@localhost:8080", url.getAuthority());
    }

    public void testTestHttpsUrlAuthorityString() throws Exception {
        HttpsURL url = new HttpsURL("localhost", -1, "/");
        assertEquals("https://localhost/", url.toString());
        url.setRawUserinfo("user".toCharArray(), "password".toCharArray());
        assertEquals("https://localhost/", url.toString());
        assertEquals("user:password@localhost", url.getAuthority());
        url = new HttpsURL("user#@", "pass#@", "localhost", 8080, "/");
        assertEquals("https://localhost:8080/", url.toString());
        assertEquals("user#@:pass#@", url.getUserinfo());
        assertEquals("user%23%40:pass%23%40", url.getEscapedUserinfo());
        url = new HttpsURL("user%23%40:pass%23%40", "localhost", 8080, "/");
        assertEquals("https://localhost:8080/", url.toString());
        assertEquals("user#@:pass#@", url.getUserinfo());
        assertEquals("user%23%40:pass%23%40", url.getEscapedUserinfo());
        url = new HttpsURL("localhost", 8080, "/");
        assertEquals("https://localhost:8080/", url.toString());
        url.setRawUserinfo("user".toCharArray(), "password".toCharArray());
        assertEquals("https://localhost:8080/", url.toString());
        assertEquals("user:password@localhost:8080", url.getAuthority());
    }

    public void testURIEscaping() throws Exception {
        String escaped = "http://some.host.com/%41.html";
        String unescaped = "http://some.host.com/A.html";
        URI u1 = new URI(escaped, true);
        GetMethod method = new GetMethod();
        method.setURI(u1);
        URI u2 = method.getURI();
        assertEquals(escaped, u1.toString());
        assertEquals(escaped, new String(u1.getRawURI()));
        assertEquals(unescaped, u1.getURI());
        assertEquals(escaped, u2.toString());
        assertEquals(escaped, new String(u2.getRawURI()));
        assertEquals(unescaped, u2.getURI());
    }
}
