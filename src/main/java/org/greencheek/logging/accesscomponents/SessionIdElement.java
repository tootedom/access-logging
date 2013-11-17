package org.greencheek.logging.accesscomponents;

import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

/**
 * write user session ID - %S
 */
public class SessionIdElement implements AccessLogElement {
    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        if (request == null) {
            buf.append('-');
        } else {
            Session session = request.getSessionInternal(false);
            if (session == null) {
                buf.append('-');
            } else {
                buf.append(session.getIdInternal());
            }
        }
    }
}
