/****************************************************************************
 * Copyright (c) 2005, 2010 Jan S. Rellermeyer, Systems Group,
 * Department of Computer Science, ETH Zurich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jan S. Rellermeyer - initial API and implementation
 *    Markus Alexander Kuppe - enhancements and bug fixes
 *
*****************************************************************************/
package ch.ethz.iks.slp.impl;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import ch.ethz.iks.slp.ServiceLocationException;

/**
 * A DAAdvertisement is sent by a DA to advertise it's service.
 * 
 * @author Jan S. Rellermeyer, ETH Z�rich
 * @since 0.1
 */
class DAAdvertisement extends ReplyMessage {

    /**
	 * the errorCode of the message.
	 */
    int errorCode;

    /**
	 * the stateless boot timestamp. If 0, the DA will go down. SAs can
	 * determine if the DA has been rebooted since the last registration and if
	 * services have to be reregistered.
	 */
    int statelessBootTimestamp;

    /**
	 * the url of the DA.
	 */
    String url;

    /**
	 * a List of scopes that the DA supports.
	 */
    List scopeList;

    /**
	 * a List of attributes.
	 */
    List attrList;

    /**
	 * the spi string.
	 */
    String spi;

    /**
	 * the original URL.
	 */
    private String origURL;

    /**
	 * the original attributes.
	 */
    private String origAttrs;

    /**
	 * the original scopes.
	 */
    private String origScopes;

    /**
	 * the auth blocks.
	 */
    AuthenticationBlock[] authBlocks;

    /**
	 * create a new DAAdvertisement from a DataInput streaming the bytes of a
	 * DAAdvertisement message body.
	 * 
	 * @param input
	 *            stream of bytes forming the message body.
	 * @throws ServiceLocationException
	 *             in case that the IO caused an exception.
	 * @throws IOException
	 */
     DAAdvertisement(final DataInputStream input) throws ServiceLocationException, IOException {
        errorCode = input.readShort();
        statelessBootTimestamp = input.readInt();
        origURL = input.readUTF().trim();
        if (!origURL.equals("")) {
            url = origURL.substring(origURL.indexOf("//") + 2, origURL.length());
        }
        int pos = url.indexOf(":");
        if (pos > -1) {
            url = url.substring(0, pos);
        }
        origScopes = input.readUTF();
        scopeList = stringToList(origScopes, ",");
        if (scopeList.isEmpty()) {
            throw new ServiceLocationException(ServiceLocationException.PARSE_ERROR, "received DAadvert " + "with empty scope list");
        }
        origAttrs = input.readUTF();
        attrList = attributeStringToList(origAttrs);
        spi = input.readUTF();
        authBlocks = AuthenticationBlock.parse(input);
        if (SLPCore.CONFIG.getSecurityEnabled()) {
            if (!verify()) {
                throw new ServiceLocationException(ServiceLocationException.AUTHENTICATION_FAILED, "could not verify " + toString());
            }
        }
    }

    /**
	 * get the bytes of the message body in the following RFC 2608 compliant
	 * format:
	 * <p>
	 * 
	 * <pre>
	 *  0                   1                   2                   3
	 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * |        Service Location header (function = DAAdvert = 8)      |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * |          Error Code           |  DA Stateless Boot Timestamp  |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * |DA Stateless Boot Time,, contd.|         Length of URL         |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * \                              URL                              \
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * |     Length of &lt;scope-list&gt;    |         &lt;scope-list&gt;          \
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * |     Length of &lt;attr-list&gt;     |          &lt;attr-list&gt;          \
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * |    Length of &lt;SLP SPI List&gt;   |     &lt;SLP SPI List&gt; String     \
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * | # Auth Blocks |         Authentication block (if any)         \
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * </pre>.
	 * </p>
	 * 
	 * @return array of bytes.
	 * @throws ServiceLocationException
	 *             if an IO Exception occurs.
	 */
    protected void writeTo(final DataOutputStream out) throws IOException {
    // this is never sent, since we are not a DA...
    }

    /**
	 * get the length of the message.
	 * 
	 * @return the length of the message.
	 * @see ch.ethz.iks.slp.impl.SLPMessage#getSize()
	 */
    protected int getSize() {
        int len = getHeaderSize() + 8 + origURL.length() + 2 + origScopes.length() + 2 + origAttrs.length() + 2 + spi.length() + 1;
        for (int i = 0; i < authBlocks.length; i++) {
            len += authBlocks[i].getLength();
        }
        return len;
    }

    /**
	 * get a string representation of the AttributeReply message.
	 * 
	 * @return a String displaying the properties of this message instance.
	 */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(super.toString());
        buffer.append(", errorCode " + errorCode);
        buffer.append(", statelessBootTimestamp " + statelessBootTimestamp);
        buffer.append(", url " + url);
        buffer.append(", scopeList " + scopeList);
        buffer.append(", attrList " + attrList);
        buffer.append(", spi " + spi);
        return buffer.toString();
    }

    /**
	 * verify the DAAdvertisement.
	 * 
	 * @return true if verification succeeded.
	 * @throws ServiceLocationException
	 *             in case of IO errors.
	 */
    boolean verify() throws ServiceLocationException {
        for (int i = 0; i < authBlocks.length; i++) {
            if (authBlocks[i].verify(getAuthData(authBlocks[i].getSPI(), authBlocks[i].getTimestamp()))) {
                return true;
            }
        }
        return false;
    }

    /**
	 * get the authentication data.
	 * 
	 * @param spiStr
	 *            the SPI
	 * @param timestamp
	 *            the timestamp
	 * @return the authentication data.
	 * @throws ServiceLocationException
	 *             in case of IO errors.
	 */
    private byte[] getAuthData(final String spiStr, final int timestamp) throws ServiceLocationException {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(spiStr);
            dos.writeInt(statelessBootTimestamp);
            dos.writeUTF(origURL);
            /*
			 * THIS IS WRONG: RFC 2608 wants the attrs first, followed by the
			 * scopes but OpenSLP makes it the other way around !!!
			 * 
			 * see bug #1346056
			 */
            dos.writeUTF(origScopes);
            dos.writeUTF(origAttrs);
            dos.writeUTF(spi);
            dos.writeInt(timestamp);
            return bos.toByteArray();
        } catch (IOException ioe) {
            throw new ServiceLocationException(ServiceLocationException.INTERNAL_SYSTEM_ERROR, ioe.getMessage());
        }
    }

    List getResult() {
        return scopeList;
    }
}
