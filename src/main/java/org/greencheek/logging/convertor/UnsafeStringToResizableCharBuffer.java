package org.greencheek.logging.convertor;

import org.greencheek.logging.domin.ResizableCharBuffer;
import org.greencheek.util.UnsafeString;

/**
 * Uses the Unsafe class to copy the contents of the given String to the CharBuffer.
 * This call basically obtains a reference to the char[] array of the string and
 * copies it to the CharBuffer.  This is so that a new char[] isn't created and thrown
 * away.
 */
public class UnsafeStringToResizableCharBuffer implements StringToResizableCharBuffer {

    @Override
    public void copyStringToCharBuffer(String s, ResizableCharBuffer buffer) {
        char[] stringarray = UnsafeString.getChars(s);
        buffer.putCharArrayByOffsetWithResize(stringarray,UnsafeString.getOffset(s),s.length());
    }
}
