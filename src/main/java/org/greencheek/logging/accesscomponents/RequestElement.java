package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.greencheek.logging.domin.ResizableCharBuffer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * write first line of the request (method and request URI) - %r
 */
public class RequestElement implements AccessLogElement {
    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        if (request != null) {
            String method = request.getMethod();
            if (method == null) {
                // No method means no request line
                buf.append('-');
            } else {

                buf.append(method,' ',request.getRequestURI());
                String queryString = request.getQueryString();
                if (queryString != null) {
                    buf.append('?',queryString);
                }
                buf.append(' ',request.getProtocol());
            }
        } else {
            buf.append('-');
        }
    }
}