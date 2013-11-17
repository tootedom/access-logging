package org.greencheek.logging.domin;

import org.greencheek.logging.convertor.StringToResizableCharBuffer;
import org.greencheek.util.UnsafeString;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;


class ResizableCharBufferH1 {
    public int p01, p02, p03, p04, p05, p06, p07, p08;
    public int p11, p12, p13, p14, p15, p16, p17, p18;
}

class ResizableCharBufferData extends ResizableCharBufferH1{
    protected char[] buf;
    protected int currentCapacityLeft;
    protected int position;
}

class ResizableCharBufferTail extends ResizableCharBufferData{
    public int p01, p02, p03, p04, p05, p06, p07, p08;
    public int p11, p12, p13, p14, p15, p16, p17, p18;

}
/**
 * Not thread safe!
 */
public class ResizableCharBuffer extends ResizableCharBufferTail {

//    private static final StringToResizableCharBuffer convertor = StringToResizableCharBuffer.DEFAULT_INSTANCE;

    public ResizableCharBuffer(int capacity) {
        currentCapacityLeft = capacity;
        buf = new char[capacity];
    }

    public int size() {
        return position;
    }

    public void reset() {
        position = 0;
        currentCapacityLeft = buf.length;
    }

    public char[] getBuf() {
        return buf;
    }

    public CharBuffer getCharBuffer() {
        return CharBuffer.wrap(buf,0,position);
    }

    public String toString() {
        return new String(buf,0,position);
    }

    private void checkSizeAndGrow(int extra) {
        if(extra>currentCapacityLeft) {
           grow(extra);
        }
    }

    private void grow(int extra) {
        int currentCapacity = buf.length;

        int newSize = currentCapacity*2;
        if(newSize<0) {
            newSize = currentCapacity+extra;
            if(newSize<0) {
                throw new OutOfMemoryError();
            }
        }
        currentCapacityLeft = newSize - currentCapacity;

        char[] newBuf = new char[newSize];
        System.arraycopy(buf,0,newBuf,0,position);
        buf = newBuf;
    }

    public void append(String s) {
        char[] stringarray = UnsafeString.getChars(s);
        putCharArrayByOffsetWithResize(stringarray,UnsafeString.getOffset(s),s.length());
    }

    public void append(char c1, char c2, char c3) {
        checkSizeAndGrow(3);
        buf[position++]=c1;
        buf[position++]=c2;
        buf[position++]=c3;
        currentCapacityLeft-=3;
    }

    public void append(char c1, char c2) {
        checkSizeAndGrow(2);
        buf[position++]=c1;
        buf[position++]=c2;
        currentCapacityLeft-=2;
    }

    private void appendNoResize(String s) {
        char[] stringarray = UnsafeString.getChars(s);
        putCharArrayByOffset(stringarray,UnsafeString.getOffset(s),s.length());
    }

    public void append(String s, char c, String chars) {
        checkSizeAndGrow(s.length()+1+chars.length());
        appendNoResize(s);
        appendNoResize(c);
        appendNoResize(chars);
    }

    public void append(String s,char c) {
        checkSizeAndGrow(s.length()+1);
        appendNoResize(s);
        appendNoResize(c);
    }

    public void append(char[] s, char c, String chars) {
        checkSizeAndGrow(s.length+1+chars.length());
        appendNoResize(s,s.length);
        appendNoResize(c);
        appendNoResize(chars);
    }

    public void append(char c, String s) {
        checkSizeAndGrow(s.length()+1);
        appendNoResize(c);
        appendNoResize(s);
    }

    public void append(char c) {
        if(currentCapacityLeft==0) grow(1);
        appendNoResize(c);
    }

    private void appendNoResize(char c) {
        buf[position++]=c;
        currentCapacityLeft--;
    }

    private void appendNoResize(char[] chars,int len) {
        System.arraycopy(chars,0,buf,position,len);
        position+=len;
        currentCapacityLeft-=len;
    }



    public void append(char[] chars) {
        int len = chars.length;
        if(len==1) {
            append(chars[0]);
        } else {
            checkSizeAndGrow(len);
            appendNoResize(chars,len);
        }
    }

    public void putCharArrayByOffsetWithResize(char[] b, int off, int len) {
        checkSizeAndGrow(len);
        putCharArrayByOffset(b,off,len);
    }

    private void putCharArrayByOffset(char[] b, int off, int len) {
        System.arraycopy(b,off,buf,position,len);
        position+=len;
        currentCapacityLeft-=len;
    }
}