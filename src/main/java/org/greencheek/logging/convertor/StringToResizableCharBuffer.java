package org.greencheek.logging.convertor;

import org.greencheek.logging.domin.ResizableCharBuffer;

/**
 * copys the underlying char[] array in the given string,
 * to the given CharBuffer.
 */
public interface StringToResizableCharBuffer {

    public static final StringToResizableCharBuffer DEFAULT_INSTANCE = new UnsafeStringToResizableCharBuffer();

    /**
     * take the given string object, and copy the contents of the
     * char[] array it wraps, into the given ResizableCharBuffer.
     *
     * @param s The string to copy the contents
     * @param buffer the buffer to copy the characters to.
     */
    public void copyStringToCharBuffer(String s, ResizableCharBuffer buffer);
}
