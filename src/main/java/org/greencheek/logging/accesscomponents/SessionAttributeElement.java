package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

import javax.servlet.http.HttpSession;

/**
 * write an attribute in the HttpSession - %{xxx}s
 */
public class SessionAttributeElement implements AccessLogElement {
    private final String header;


    public SessionAttributeElement(String header) {
        this.header = header;
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        Object value = null;
        if (null != request) {
            HttpSession sess = request.getSession(false);
            if (null != sess) {
                value = sess.getAttribute(header);
            }
        } else {
            value = "??";
        }
        if (value != null) {
            if (value instanceof String) {
                buf.append((String)value);
            } else {
                buf.append(value.toString());
            }
        } else {
            buf.append('-');
        }
    }
}