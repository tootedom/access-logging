package org.greencheek.logging.accesscomponents;


import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

import java.util.Date;

/*
* AccessLogElement writes the partial message into the buffer.
     */
public interface AccessLogElement {
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time);

}





