package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

/**
 * write an attribute in the ServletRequest - %{xxx}r
 */
public class RequestAttributeElement implements AccessLogElement {
    private final String header;

    public RequestAttributeElement(String header) {
        this.header = header;
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        Object value = null;
        if (request != null) {
            value = request.getAttribute(header);
        } else {
            value = "??";
        }
        if (value != null) {
            if (value instanceof String) {
                buf.append(((String) value));
            } else {
                buf.append(value.toString());
            }
        } else {
            buf.append('-');
        }
    }
}