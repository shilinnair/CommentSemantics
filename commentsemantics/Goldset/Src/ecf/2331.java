//Copyright 2003-2005 Arthur van Hoff, Rick Blair
package javax.jmdns.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * HostInfo information on the local host to be able to cope with change of addresses.
 *
 * @version %I%, %G%
 * @author	Pierre Frisch, Werner Randelshofer
 */
public class HostInfo {

    //    private static Logger logger = Logger.getLogger(HostInfo.class.getName());
    protected String name;

    protected InetAddress address;

    protected NetworkInterface interfaze;

    /**
     * This is used to create a unique name for the host name.
     */
    private int hostNameCount;

    public  HostInfo(InetAddress address, String name) {
        super();
        this.address = address;
        this.name = name;
        if (address != null) {
            try {
                interfaze = NetworkInterface.getByInetAddress(address);
            } catch (Exception exception) {
            }
        }
    }

    public String getName() {
        return name;
    }

    public InetAddress getAddress() {
        return address;
    }

    public NetworkInterface getInterface() {
        return interfaze;
    }

    synchronized String incrementHostName() {
        hostNameCount++;
        int plocal = name.indexOf(".local.");
        int punder = name.lastIndexOf("-");
        name = name.substring(0, (punder == -1 ? plocal : punder)) + "-" + hostNameCount + ".local.";
        return name;
    }

    boolean shouldIgnorePacket(DatagramPacket packet) {
        boolean result = false;
        if (getAddress() != null) {
            InetAddress from = packet.getAddress();
            if (from != null) {
                if (from.isLinkLocalAddress() && (!getAddress().isLinkLocalAddress())) {
                    // Ignore linklocal packets on regular interfaces, unless this is
                    // also a linklocal interface. This is to avoid duplicates. This is
                    // a terrible hack caused by the lack of an API to get the address
                    // of the interface on which the packet was received.
                    result = true;
                }
                if (from.isLoopbackAddress() && (!getAddress().isLoopbackAddress())) {
                    // Ignore loopback packets on a regular interface unless this is
                    // also a loopback interface.
                    result = true;
                }
            }
        }
        return result;
    }

    DNSRecord.Address getDNSAddressRecord(DNSRecord.Address address) {
        return (DNSConstants.TYPE_AAAA == address.type ? getDNS6AddressRecord() : getDNS4AddressRecord());
    }

    public DNSRecord.Address getDNS4AddressRecord() {
        if ((getAddress() != null) && ((getAddress() instanceof Inet4Address) || ((getAddress() instanceof Inet6Address) && (((Inet6Address) getAddress()).isIPv4CompatibleAddress())))) {
            return new DNSRecord.Address(getName(), DNSConstants.TYPE_A, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, getAddress());
        }
        return null;
    }

    public DNSRecord.Address getDNS6AddressRecord() {
        if ((getAddress() != null) && (getAddress() instanceof Inet6Address)) {
            return new DNSRecord.Address(getName(), DNSConstants.TYPE_AAAA, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, getAddress());
        }
        return null;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("local host info[");
        buf.append(getName() != null ? getName() : "no name");
        buf.append(", ");
        buf.append(getInterface() != null ? getInterface().getDisplayName() : "???");
        buf.append(":");
        buf.append(getAddress() != null ? getAddress().getHostAddress() : "no address");
        buf.append("]");
        return buf.toString();
    }

    public void addAddressRecords(DNSOutgoing out, boolean authoritative) throws IOException {
        DNSRecord answer = getDNS4AddressRecord();
        if (answer != null) {
            if (authoritative) {
                out.addAuthorativeAnswer(answer);
            } else {
                out.addAnswer(answer, 0);
            }
        }
        answer = getDNS6AddressRecord();
        if (answer != null) {
            if (authoritative) {
                out.addAuthorativeAnswer(answer);
            } else {
                out.addAnswer(answer, 0);
            }
        }
    }
}
