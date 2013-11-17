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
 * write requested URL path - %U
 */
public class RequestURIElement implements AccessLogElement {
    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        if (request != null) {
            MessageBytes b = request.getCoyoteRequest().requestURI();
            switch (b.getType()) {
                case MessageBytes.T_BYTES:
                    ByteChunk by = b.getByteChunk();
                    CharBuffer cb;
                    Charset charset = by.getCharset();
                    cb = charset.decode(ByteBuffer.wrap(by.getBuffer(), by.getOffset(), by.getLength()));
                    buf.putCharArrayByOffsetWithResize(cb.array(), cb.arrayOffset(), cb.length());
                case MessageBytes.T_CHARS:
                    CharChunk c = b.getCharChunk();
                    buf.putCharArrayByOffsetWithResize(c.getChars(),c.getOffset(),c.getLength());
                    break;
            }
//            buf.append(request.getRequestURI());
        } else {
            buf.append('-');
        }
    }
}