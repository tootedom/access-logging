package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

/**
 * write time taken to process the request - %D, %T
 */
public class ElapsedTimeElement implements AccessLogElement {
    private final boolean millis;

    /**
     * if millis is true, write time in millis - %D
     * if millis is false, write time in seconds - %T
     */
    public ElapsedTimeElement(boolean millis) {
        this.millis = millis;
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        if (millis) {
            if(time<=Integer.MAX_VALUE) {
                buf.append(Integer.toString((int)time));
            } else {
                buf.append(Long.toString(time));
            }
        } else {
            // second
            buf.append(Long.toString(time / 1000));
            buf.append('.');

            // second
            int remains = (int) (time % 1000);
            char[] milliseconds = new char[3];
            milliseconds[0] = ((char) ('0' + (remains / 100)));
            remains = remains % 100;
            milliseconds[1] = ((char) ('0' + (remains / 10)));
            milliseconds[2] = ((char) ('0' + (remains % 10)));

            buf.append(milliseconds);


        }
    }
}