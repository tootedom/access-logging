package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

/**
 * write HTTP status code of the response - %s
 */
public class HttpStatusCodeElement implements AccessLogElement {

    private static final char[][] codes = new char[1001][];
    static {
        for(int i=0;i<1001;i++) {
            codes[i] = Integer.toString(i).toCharArray();
        }
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        if (response != null) {
            // This approach is used to reduce GC from toString conversion
            int status = response.getStatus();
            if (-1 < status && status < 1001) {
                buf.append(codes[status]);
            } else {
                buf.append(Integer.toString(status));
            }
        } else {
            buf.append('-');
        }
    }
}