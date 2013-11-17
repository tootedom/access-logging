package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

import static org.apache.catalina.AccessLog.*;

/**
 * write request protocol - %H
 */
public class ProtocolElement implements AccessLogElement {

    private final boolean requestAttributesEnabled;

    public ProtocolElement(boolean requestAttributesEnabled) {
        this.requestAttributesEnabled = requestAttributesEnabled;
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        if (requestAttributesEnabled) {
            Object proto = request.getAttribute(PROTOCOL_ATTRIBUTE);
            if (proto == null) {
                buf.append(request.getProtocol());
            } else {
                buf.append(proto.toString());
            }
        } else {
            buf.append(request.getProtocol());
        }
    }
}