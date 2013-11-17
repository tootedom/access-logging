package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

import java.util.Iterator;

/**
 * write a specific response header - %{xxx}o
 */
public class ResponseHeaderElement implements AccessLogElement {
    private final String header;

    public ResponseHeaderElement(String header) {
        this.header = header;
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        if (null != response) {
            Iterator<String> iter = response.getHeaders(header).iterator();
            if (iter.hasNext()) {
                buf.append(iter.next());
                while (iter.hasNext()) {
                    buf.append(',');
                    buf.append(iter.next());
                }
                return;
            }
        }
        buf.append('-');
    }
}
