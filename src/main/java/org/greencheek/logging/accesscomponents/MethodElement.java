package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

/**
 * write request method (GET, POST, etc.) - %m
 */
public class MethodElement implements AccessLogElement {


    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        if (request != null) {
            buf.append(request.getMethod());
        }
    }
}
