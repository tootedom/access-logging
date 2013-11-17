package org.greencheek.logging.accesscomponents;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: dominictootell
 * Date: 05/11/2013
 * Time: 21:56
 * To change this template use File | Settings | File Templates.
 */
public class ThreadLocalDateCache {
    /* CLF log format */
    private static final String DEFAULT_FORMAT = "[dd/MMM/yyyy:HH:mm:ss Z]";

    private static final Locale DEFAULT_LOCAL = Locale.US;

    /*
    * Thread local date format cache.
    */
    public static final ThreadLocal<CachedDateFormat> localDateCache =
            new ThreadLocal<CachedDateFormat>() {
                @Override
                protected CachedDateFormat initialValue() {
                    return new CachedDateFormat(DEFAULT_FORMAT, DEFAULT_LOCAL);
                }
            };


    public static class CachedDateFormat {
        /* Second used to retrieve CLF format in most recent invocation */
        private long previousSeconds = Long.MIN_VALUE;
        /* Value of CLF format retrieved in most recent invocation */
        private String previousFormat = "";

        private final SimpleDateFormat formatter;

        private final Date currentDate = new Date();

        public CachedDateFormat(String format, Locale locale) {
            formatter = new SimpleDateFormat(format,locale);
        }

        public String format(long time) {
            long seconds = time / 1000;

                /* First step: if we have seen this timestamp
                   during the previous call, and we need CLF, return the previous value. */
            if (seconds == previousSeconds) {
                return previousFormat;
            } else {
                currentDate.setTime(time);
                 /* Second step: Try to locate in cache */
                previousFormat = formatter.format(currentDate);
                previousSeconds = seconds;
                return previousFormat;
            }
        }
    }
}
