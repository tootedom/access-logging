package org.greencheek.logging.accesscomponents;

/**
 * write any string
 */

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

public class StringElement implements AccessLogElement {
    private final char[] str;

    public StringElement(String str) {
        this.str = str.toCharArray();
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        buf.append(str);
    }
}