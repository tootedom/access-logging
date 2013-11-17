package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

import java.net.InetAddress;

/**
 * write local IP address - %A
 */
public class LocalAddrElement implements AccessLogElement {

    private static final char[] LOCAL_ADDR_VALUE;

    static {
        String init;
        try {
            init = InetAddress.getLocalHost().getHostAddress();
        } catch (Throwable e) {
            ExceptionUtils.handleThrowable(e);
            init = "127.0.0.1";
        }

        LOCAL_ADDR_VALUE = init.toCharArray();
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        buf.append(LOCAL_ADDR_VALUE);
    }
}