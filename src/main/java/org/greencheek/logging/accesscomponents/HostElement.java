package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

import static org.apache.catalina.AccessLog.*;

/**
 * write remote host name - %h
 */
public class HostElement implements AccessLogElement {

    private final boolean requestAttributesEnabled;

    public HostElement(boolean useRequestAttributes) {
        requestAttributesEnabled = useRequestAttributes;
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        String value = null;
        if (requestAttributesEnabled) {
            Object host = request.getAttribute(REMOTE_HOST_ATTRIBUTE);
            if (host != null) {
                value = host.toString();
            }
        }
        if (value == null || value.length() == 0) {
            value = request.getRemoteHost();
        }
        if (value == null || value.length() == 0) {
            buf.append('-');
        } else {
            buf.append(value);
        }
    }
}
