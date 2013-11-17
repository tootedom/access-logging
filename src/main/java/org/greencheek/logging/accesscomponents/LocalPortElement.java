package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

import static org.apache.catalina.AccessLog.*;

/**
 * write local port on which this request was received - %p
 */
public class LocalPortElement implements AccessLogElement {

    private final boolean requestAttributesEnabled;

    public LocalPortElement(boolean requestAttributesEnabled) {
        this.requestAttributesEnabled = requestAttributesEnabled;
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        if (requestAttributesEnabled) {
            Object port = request.getAttribute(SERVER_PORT_ATTRIBUTE);
            if (port == null) {
                buf.append(Integer.toString(request.getServerPort()));
            } else {
                buf.append(port.toString());
            }
        } else {
            buf.append(Integer.toString(request.getServerPort()));
        }
    }
}