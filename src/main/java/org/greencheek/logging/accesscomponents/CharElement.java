package org.greencheek.logging.accesscomponents;

/**
 * write any string
 */

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

public class CharElement implements AccessLogElement {
    private final char theChar;

    public CharElement(char str) {
        this.theChar = str;
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        buf.append(theChar);
    }
}