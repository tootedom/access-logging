package org.greencheek.logging.disruptor;

import com.lmax.disruptor.EventTranslator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.logging.accesscomponents.AccessLogElement;
import org.greencheek.logging.domin.ResizableCharBuffer;

import java.util.Date;

/**
 * Not thread safe intended for use in a thread local
 */


class LogMessageEventTranslatorH1 {
    public int p01, p02, p03, p04, p05, p06, p07, p08;
    public int p11, p12, p13, p14, p15, p16, p17, p18;


}

class LogMessageEventTranslatorData extends LogMessageEventTranslatorH1 {

    AccessLogElement[] logElements;
    long logDate;
    Request request;
    Response response;
    long time;

}

class LogMessageEventTranslatorT1 extends LogMessageEventTranslatorData {
    public int e01, e02, e03, e04, e05, e06, e07, e08;
    public int e11, e12, e13, e14, e15, e16, e17, e18;

}

public class LogMessageEventTranslator
        extends LogMessageEventTranslatorT1
        implements EventTranslator<ResizableCharBuffer>
{


    public LogMessageEventTranslator(){
    }

    @Override
    public void translateTo(ResizableCharBuffer event, long sequence) {
        for (int i = 0; i < logElements.length; i++) {
            logElements[i].addElement(event, logDate, request, response, time);
        }
    }


    public void setLogElements(AccessLogElement[] logElements) {
        this.logElements = logElements;
    }


    public void setLogDate(long date) {
        this.logDate = date;
    }


    public void setRequest(Request request) {
        this.request = request;
    }


    public void setResponse(Response response) {
        this.response = response;
    }

    public void setTime(long time) {
        this.time = time;
    }
}