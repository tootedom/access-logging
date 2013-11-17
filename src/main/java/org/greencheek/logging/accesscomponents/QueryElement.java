package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

/**
 * write Query string (prepended with a '?' if it exists) - %q
 */
public class QueryElement implements AccessLogElement {
    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        String query = null;
        if (request != null) {
            query = request.getQueryString();
        }
        if (query != null) {
            buf.append('?');
            buf.append(query);
        }
    }
}