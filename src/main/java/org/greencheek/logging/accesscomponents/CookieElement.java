package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

import javax.servlet.http.Cookie;

/**
 * write a specific cookie - %{xxx}c
 */
public class CookieElement implements AccessLogElement {
    private final String header;


    public CookieElement(String header) {
        this.header = header;
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        String value = null;
        Cookie[] c = request.getCookies();
        if (c != null) {
            for (int i = 0; i < c.length; i++) {
                if (header.equals(c[i].getName())) {
                    value = c[i].getValue();
                    buf.append(value);
                    return;
                }
            }
        }
        buf.append('-');
    }
}