

package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.domin.ResizableCharBuffer;

/**
 * write remote user that was authenticated (if any), else '-' - %u
 */
public class UserElement implements AccessLogElement {
    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        if (request != null) {
            String value = request.getRemoteUser();
            if (value != null) {
                buf.append(value);
            } else {
                buf.append('-');
            }
        } else {
            buf.append('-');
        }
    }
}
