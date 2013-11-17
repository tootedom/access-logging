package org.greencheek.logging.accesscomponents;

import org.apache.catalina.Globals;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

/**
 * write bytes sent, excluding HTTP headers - %b, %B
 */
public class ByteSentElement implements AccessLogElement {
    private final boolean conversion;

    /**
     * if conversion is true, write '-' instead of 0 - %b
     */
    public ByteSentElement(boolean conversion) {
        this.conversion = conversion;
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        // Don't need to flush since trigger for log message is after the
        // response has been committed
        long length = response.getBytesWritten(false);
        if(length>0) {
            if(length<Integer.MAX_VALUE) {
                buf.append(Integer.toString((int)length));
            } else {
                buf.append(Long.toString(length));
            }
            return;
        }

        if (length <= 0) {
            // Protect against nulls and unexpected types as these valves
            // may be set by untrusted applications
            Object start = request.getAttribute(
                    Globals.SENDFILE_FILE_START_ATTR);
            if (start instanceof Long) {
                Object end = request.getAttribute(
                        Globals.SENDFILE_FILE_END_ATTR);
                if (end instanceof Long) {
                    length = ((Long) end).longValue() -
                            ((Long) start).longValue();
                }
            }
        }
        if (length <= 0 && conversion) {
            buf.append('-');
        } else {
            if(length<Integer.MAX_VALUE) {
                buf.append(Integer.toString((int)length));
            } else {
                buf.append(Long.toString(length));
            }
        }
    }
}
