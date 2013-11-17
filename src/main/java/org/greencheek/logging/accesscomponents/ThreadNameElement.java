package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.coyote.RequestInfo;
import org.greencheek.logging.domin.ResizableCharBuffer;

/**
 * write thread name - %I
 */
public class ThreadNameElement implements AccessLogElement {
    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        RequestInfo info = request.getCoyoteRequest().getRequestProcessor();
        if (info != null) {
            buf.append(info.getWorkerThreadName());
        } else {
            buf.append('-');
        }
    }
}