package org.greencheek.util;

import java.lang.reflect.Field;
import java.nio.CharBuffer;

import sun.misc.Unsafe;

/**
 * All credit for this goes to:
 * https://github.com/nitsanw/javanetperf/blob/psylobsaw/src/psy/lob/saw/UnsafeString.java
 */
public class UnsafeString {
    private static final Unsafe unsafe;
    private static final long valueOffset;
    private static final long offsetOffset;

    static {
        try {
            // This is a bit of voodoo to force the unsafe object into
            // visibility and acquire it.
            // This is not playing nice, but as an established back door it is
            // not likely to be
            // taken away.
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            valueOffset = unsafe.objectFieldOffset(String.class
                    .getDeclaredField("value"));
            Field declaredField;

            try {
                declaredField = String.class.getDeclaredField("offset");
            }
            // this will happen for jdk7 as these fields have been removed
            catch (NoSuchFieldException e) {
                declaredField = null;
            }
            if (declaredField != null) {
                offsetOffset = unsafe.objectFieldOffset(declaredField);
            } else {
                offsetOffset = -1L;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public final static char[] getChars(String s) {
        return (char[]) unsafe.getObject(s, valueOffset);
    }

    public final static int getOffset(String s) {
        if (offsetOffset == -1L)
            return 0;
        else
            return unsafe.getInt(s, offsetOffset);
    }


}