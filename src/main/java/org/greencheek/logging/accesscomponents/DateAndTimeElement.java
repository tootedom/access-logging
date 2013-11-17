package org.greencheek.logging.accesscomponents;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.greencheek.catalina.valves.FormatType;
import org.greencheek.logging.domin.ResizableCharBuffer;
import org.greencheek.util.FastDatePrinter;
import org.greencheek.util.UnsafeString;

import java.util.Locale;
import java.util.TimeZone;

/**
 * write date and time, in configurable format (default CLF) - %t or %t{format}
 */
public class DateAndTimeElement implements AccessLogElement {

    /* CLF log format */
    private static final String cLFFormat = "[dd/MMM/yyyy:HH:mm:ss Z]";

    private static final Locale locale = Locale.US;


    /**
     * Format prefix specifying request start time
     */
    private static final String requestStartPrefix = "begin";

    /**
     * Format prefix specifying response end time
     */
    private static final String responseEndPrefix = "end";

    /**
     * Separator between optional prefix and rest of format
     */
    private static final String prefixSeparator = ":";

    /**
     * Special format for seconds since epoch
     */
    private static final String secFormat = "sec";

    /**
     * Special format for milliseconds since epoch
     */
    private static final String msecFormat = "msec";

    /**
     * Special format for millisecond part of timestamp
     */
    private static final String msecFractionFormat = "msec_frac";

    /**
     * The patterns we use to replace "S" and "SSS" millisecond
     * formatting of SimpleDateFormat by our own handling
     */
    private static final String msecPattern = "{#}";
    private static final String trippleMsecPattern =
            msecPattern + msecPattern + msecPattern;

    /* Our format description string, null if CLF */
    private final String format;
    /* Whether to use begin of request or end of response as the timestamp */
    private final boolean usesBegin;
    /* The format type */
    private final FormatType type;

    /* Whether we need to postprocess by adding milliseconds */
    private final boolean usesMsecs;

    private final boolean usesTrippleMsecs;

    private final LocalTimeFormatter timeFormatter;

    public DateAndTimeElement() {
        this(null);
    }

    public DateAndTimeElement(String header) {
        String format = header;
        boolean usesBegin = false;
        FormatType type = FormatType.CLF;
        boolean msUsed = false;
        boolean trippleMsUsed = false;

        if (format != null) {
            if (format.equals(requestStartPrefix)) {
                usesBegin = true;
                format = "";
            } else if (format.startsWith(requestStartPrefix + prefixSeparator)) {
                usesBegin = true;
                format = format.substring(6);
            } else if (format.equals(responseEndPrefix)) {
                usesBegin = false;
                format = "";
            } else if (format.startsWith(responseEndPrefix + prefixSeparator)) {
                usesBegin = false;
                format = format.substring(4);
            }
            if (format.length() == 0) {
                type = FormatType.CLF;
                timeFormatter = new CommonTimeFormatter(new ThreadLocalDateCache(cLFFormat,locale));
            } else if (format.equals(secFormat)) {
                type = FormatType.SEC;
                timeFormatter = new SecondsTimeFormatter();
            } else if (format.equals(msecFormat)) {
                type = FormatType.MSEC;
                timeFormatter = new MilliSecondsTimeFormatter();
            } else if (format.equals(msecFractionFormat)) {
                type = FormatType.MSEC_FRAC;
                timeFormatter = new MillisSecondsLeftPaddedTimeFormatter();
            } else {
                type = FormatType.SDF;
                boolean escape = false;
                StringBuilder result = new StringBuilder(format.length()+9);
                int len = format.length();
                char x;
                for (int i = 0; i < len; i++) {
                    x = format.charAt(i);
                    if (escape || x != 'S') {
                        result.append(x);
                    } else {
                        result.append(msecPattern);
                        msUsed = true;
                    }
                    if (x == '\'') {
                        escape = !escape;
                    }
                }

                format = result.toString();
                if(format.contains(trippleMsecPattern)) {
                    trippleMsUsed = true;
                } else {
                    trippleMsUsed = false;
                }
                timeFormatter = new SimpleDateTimeFormatter(new ThreadLocalDateCache(format,Locale.getDefault()));
            }
        } else {
            timeFormatter = new CommonTimeFormatter(new ThreadLocalDateCache(cLFFormat,locale));
        }
        this.usesMsecs = msUsed;
        this.usesTrippleMsecs = trippleMsUsed;
        this.format = format;
        this.usesBegin = usesBegin;
        this.type = type;
    }

    private interface LocalTimeFormatter {
        /**
         * Format the time (given in millis) to a format
         *
         * @param time
         * @return
         */
        void format(ResizableCharBuffer buf, long time);
    }

    private class CommonTimeFormatter implements LocalTimeFormatter {
        private final ThreadLocalDateCache dateFormatter;

        public CommonTimeFormatter(ThreadLocalDateCache dateFormatter) {
            this.dateFormatter = dateFormatter;
        }

        @Override
        public void format(ResizableCharBuffer buf, long time) {
            buf.append(dateFormatter.localDateCache.get().formatToChars(time));
        }
    }

    private class SecondsTimeFormatter implements LocalTimeFormatter {
        @Override
        public void format(ResizableCharBuffer buf, long time) {
            buf.append(Long.toString(time / 1000));
        }
    }

    private class MilliSecondsTimeFormatter implements LocalTimeFormatter {
        @Override
        public void format(ResizableCharBuffer buf, long time) {
            buf.append(Long.toString(time));
        }
    }


    private class MillisSecondsLeftPaddedTimeFormatter implements LocalTimeFormatter {
        /**
         * All possible chars for representing a number as a String
         */
        final char[] digits = {
                '0' , '1' , '2' , '3' , '4' , '5' ,
                '6' , '7' , '8' , '9' , 'a' , 'b' ,
                'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
                'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
                'o' , 'p' , 'q' , 'r' , 's' , 't' ,
                'u' , 'v' , 'w' , 'x' , 'y' , 'z'
        };

        final char [] DigitTens = {
                '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
                '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
                '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
                '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
                '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
                '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
                '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
                '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
                '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
                '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
        } ;

        final char [] DigitOnes = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        } ;


        @Override
        public void format(ResizableCharBuffer buffer, long time) {
            buffer.append(getMillisAs3CharsWithLeftPadding(time%1000));
        }

        public char[] getMillisAs3CharsWithLeftPadding(long i) {
            char[] buf = new char[]{'0','0','0'};
            int charPos = 2;
            int i2 = (int)i;

            // Fall thru to fast mode for smaller numbers
            // assert(i2 <= 65536, i2);
            for (;;) {
                int q2 = (i2 * 52429) >>> (19);
                int r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
                buf[charPos] = digits[r];
                if (q2 == 0) break;
                --charPos;
                i2 = q2;
            }
            return buf;
        }
    }


    private class SimpleDateTimeFormatter implements LocalTimeFormatter {
        private final ThreadLocalDateCache dateFormatter;
        private final MillisSecondsLeftPaddedTimeFormatter millisecondsPadder;

        public SimpleDateTimeFormatter(ThreadLocalDateCache dateFormatter) {
            this.dateFormatter = dateFormatter;
            this.millisecondsPadder = new MillisSecondsLeftPaddedTimeFormatter();
        }

        @Override
        public void format(ResizableCharBuffer buf, long time) {

            String formattedDate = dateFormatter.localDateCache.get().format(time);
            if(usesTrippleMsecs) {
                formattedDate = formattedDate.replace(trippleMsecPattern, new String(millisecondsPadder.getMillisAs3CharsWithLeftPadding(time%1000)));
            } else {
                formattedDate = formattedDate.replace(msecPattern, Long.toString(time/1000));
            }

            buf.append(formattedDate);
        }
    }

    @Override
    public void addElement(ResizableCharBuffer buf, long date, Request request,
                           Response response, long time) {
        long timestamp = date;
        if (usesBegin) {
            timestamp -= time;
        }
        timeFormatter.format(buf,timestamp);
    }


    /**
     * <p>Cache structure for formatted timestamps based on seconds.</p>
     *
     * <p>The cache consists of entries for a consecutive range of
     * seconds. The length of the range is configurable. It is
     * implemented based on a cyclic buffer. New entries shift the range.</p>
     *
     * <p>There is one cache for the CLF format (the access log standard
     * format) and a HashMap of caches for additional formats used by
     * SimpleDateFormat.</p>
     *
     * <p>Although the cache supports specifying a locale when retrieving a
     * formatted timestamp, each format will always use the locale given
     * when the format was first used. New locales can only be used for new formats.
     * The CLF format will always be formatted using the locale
     * <code>en_US</code>.</p>
     *
     * <p>The cache is not threadsafe. It can be used without synchronization
     * via thread local instances, or with synchronization as a global cache.</p>
     *
     * <p>The cache can be created with a parent cache to build a cache hierarchy.
     * Access to the parent cache is threadsafe.</p>
     *
     * <p>This class uses a small thread local first level cache and a bigger
     * synchronized global second level cache.</p>
     */
    public static class ThreadLocalDateCache {

        public final ThreadLocal<CachedDateFormat> localDateCache;

        public ThreadLocalDateCache() {
            this(cLFFormat,locale);
        }

        public ThreadLocalDateCache(final String format,final Locale locale) {
            localDateCache =
                    new ThreadLocal<CachedDateFormat>() {
                        @Override
                        protected CachedDateFormat initialValue() {
                            return new CachedDateFormat(format, locale);
                        }
                    };
        }


        public static class CachedDateFormat {

            /*
             * Thread local date format cache.
             */
            private long previousTime = Long.MIN_VALUE;

            /* Second used to retrieve CLF format in most recent invocation */
            private long previousSeconds = Long.MIN_VALUE;
            /* Value of CLF format retrieved in most recent invocation */
            private String previousFormat = "";
            private char[] previousFormatChars;

            private final FastDatePrinter formatter;

            public CachedDateFormat(final String format,final Locale locale) {
                formatter = new FastDatePrinter(format, TimeZone.getDefault(),locale);
            }

            public String format(long time) {

                if(time == previousTime ) {
                    return previousFormat;
                }

                long seconds = time / 1000;

                    /* First step: if we have seen this timestamp
                       during the previous call, and we need CLF, return the previous value. */
                if (seconds == previousSeconds) {
                    previousTime = time;
                    return previousFormat;
                } else {
                     /* Second step: Try to locate in cache */
                    previousFormat = formatter.format(time);
                    previousFormatChars = UnsafeString.getChars(previousFormat);
                    previousSeconds = seconds;
                    previousTime = time;
                    return previousFormat;
                }
            }

            public char[] formatToChars(long time) {
                format(time);
                return previousFormatChars;
            }
        }
    }
}