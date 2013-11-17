package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

import java.util.Enumeration;

/**
 * write incoming headers - %{xxx}i
 */
public class HeaderElement implements AccessLogElement {
    private final String header;

    public HeaderElement(String header) {
        this.header = header;
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        Enumeration<String> iter = request.getHeaders(header);
        if (iter.hasMoreElements()) {
            buf.append(iter.nextElement());
            while (iter.hasMoreElements()) {
                buf.append(',',iter.nextElement());
            }
            return;
        }
        buf.append('-');
    }
}