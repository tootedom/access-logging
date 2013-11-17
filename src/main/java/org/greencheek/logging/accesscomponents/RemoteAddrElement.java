package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

import static org.apache.catalina.AccessLog.*;

/**
 * write remote IP address - %a
 */
public class RemoteAddrElement implements AccessLogElement {

    private final boolean requestAttributesEnabled;

    public RemoteAddrElement(boolean requestAttributesEnabled) {
        this.requestAttributesEnabled = requestAttributesEnabled;
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        if (requestAttributesEnabled) {
            Object addr = request.getAttribute(REMOTE_ADDR_ATTRIBUTE);
            if (addr == null) {
                buf.append(request.getRemoteAddr());
            } else {
                buf.append(addr.toString());
            }
        } else {
            buf.append(request.getRemoteAddr());
        }
    }
}