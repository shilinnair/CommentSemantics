/**
 * Copyright 2013 Florian Schmaus
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smack.util.dns;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.jivesoftware.smack.util.DNSUtil;

/**
 * A DNS resolver (mostly for SRV records), which makes use of the API provided in the javax.* namespace.
 * 
 * @author Florian Schmaus
 *
 */
public class JavaxResolver implements DNSResolver {

    private static JavaxResolver instance;

    private static DirContext dirContext;

    static {
        try {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            dirContext = new InitialDirContext(env);
        } catch (Exception e) {
        }
        // Try to set this DNS resolver as primary one
        DNSUtil.setDNSResolver(getInstance());
    }

    private  JavaxResolver() {
    }

    public static DNSResolver getInstance() {
        if (instance == null && isSupported()) {
            instance = new JavaxResolver();
        }
        return instance;
    }

    public static boolean isSupported() {
        return dirContext != null;
    }

    public List<SRVRecord> lookupSRVRecords(String name) {
        List<SRVRecord> res = new ArrayList<SRVRecord>();
        try {
            Attributes dnsLookup = dirContext.getAttributes(name, new String[] { "SRV" });
            Attribute srvAttribute = dnsLookup.get("SRV");
            @SuppressWarnings("unchecked") NamingEnumeration<String> srvRecords = (NamingEnumeration<String>) srvAttribute.getAll();
            while (srvRecords.hasMore()) {
                String srvRecordString = srvRecords.next();
                String[] srvRecordEntries = srvRecordString.split(" ");
                int priority = Integer.parseInt(srvRecordEntries[srvRecordEntries.length - 4]);
                int port = Integer.parseInt(srvRecordEntries[srvRecordEntries.length - 2]);
                int weight = Integer.parseInt(srvRecordEntries[srvRecordEntries.length - 3]);
                String host = srvRecordEntries[srvRecordEntries.length - 1];
                SRVRecord srvRecord;
                try {
                    srvRecord = new SRVRecord(host, port, priority, weight);
                } catch (Exception e) {
                    continue;
                }
                res.add(srvRecord);
            }
        } catch (Exception e) {
        }
        return res;
    }
}
