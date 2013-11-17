package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;


/**
 * write time until first byte is written (commit time) in millis - %F
 */
public class FirstByteTimeElement implements AccessLogElement {


    public FirstByteTimeElement() {
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request, Response response, long time) {
        long commitTime = response.getCoyoteResponse().getCommitTime();
        if (commitTime == -1) {
            buf.append('-');
        } else {
            long delta = commitTime - request.getCoyoteRequest().getStartTime();
            buf.append(Long.toString(delta));
        }
    }
}