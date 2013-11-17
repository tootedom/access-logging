package org.greencheek.logging.disruptor.io;

import com.lmax.disruptor.EventHandler;
import org.apache.catalina.Globals;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.buf.B2CConverter;
import org.apache.tomcat.util.res.StringManager;
import org.greencheek.logging.domin.ResizableCharBuffer;
import org.greencheek.util.FastDatePrinter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Event handler that writes in batches, log messages to a file.
 * The log message are written pages at a time to disk.  The
 * file that is written to is rotated, and upon rotation the file
 * is pre allocated to a given size.  This is so that the size of
 * the file does not have to be adjusted during processing
 *
 */
public class LogEventHandler implements EventHandler<ResizableCharBuffer> {

    static final char[] lineEndings =  System.getProperty("line.separator").toCharArray();
    static final byte[] lineEndingsBytes = new byte[lineEndings.length];
    static {
        for(int i=0;i<lineEndings.length;i++) {
            lineEndingsBytes[i] = (byte)lineEndings[i];
        }
    }


    protected String encoding = null;

    private String directory;


    private final StringManager sm;


    private static Log log;

    private String dateStamp;


    /**
     * The prefix that is added to log file filenames.
     */
    protected String prefix = "access_log.";

    /**
     * The suffix that is added to log file filenames.
     */
    protected String suffix = "";

    /**
     * Should we rotate our log file? Default is true (like old behavior)
     */
    protected boolean rotatable = true;


    /**
     * Should we defer inclusion of the date stamp in the file
     * name until rotate time? Default is false.
     */
    protected boolean renameOnRotate = false;






    /**
     * The PrintWriter to which we are currently logging, if any.
     */
    protected RandomAccessFile writer = null;
    protected FileChannel writerChannel = null;


    /**
     * A date formatter to format a Date using the format
     * given by <code>fileDateFormat</code>.
     */
    protected FastDatePrinter fileDateFormatter = null;


    /**
     * The current log file we are writing to. Helpful when checkExists
     * is true.
     */
    protected File currentLogFile = null;

    /**
     * Instant when the log daily rotation was last checked.
     */
    private long rotationLastChecked = 0L;

    /**
     * Do we check for log file existence? Helpful if an external
     * agent renames the log file so we can automagically recreate it.
     */
    private boolean checkExists = false;


    /**
     * Buffered logging.
     */
    private final boolean syncWrite;


    private final byte[] ioBuffer;
    private int currentPosition = 0;

    private final int ioWriteSize;
    private final int ioWriteSizeMinusOne;
    private final int ioPreallocateFileSize;
    public final byte[] BLANK_PAGE;


    private static final Charset UTF8 = Charset.forName( "UTF-8" );
    private final CharsetEncoder encoder;

    /**
     *
     * @param sm
     * @param encoding
     * @param log
     * @param format
     * @param directory
     * @param rotatable
     * @param renameOnRotate
     * @param syncWrite
     */
    public LogEventHandler(StringManager sm,
                           String encoding,
                           Log log, String format,
                           String directory,
                           boolean rotatable, boolean renameOnRotate,
                           boolean syncWrite, int ioWriteSize, int ioPreallocateFileSize,
                           String suffix, String prefix, boolean checkExists) {

        Charset charset;
        try {
            if(encoding!=null) {
                charset = Charset.forName(encoding);
            } else {
                charset = UTF8;
            }
        } catch(UnsupportedCharsetException cex) {
            charset = UTF8;
        }

        this.encoder = charset.newEncoder();
        this.ioWriteSize = ioWriteSize;
        this.ioWriteSizeMinusOne = ioWriteSize-1;
        this.BLANK_PAGE = new byte[ioWriteSize];
        this.ioPreallocateFileSize = ioPreallocateFileSize;
        this.suffix = suffix;
        this.prefix = prefix;
//        Initialize the Date formatters
        this.sm = sm;
        this.encoding = encoding;
        this.directory = directory;
        this.log = log;
        this.rotatable = rotatable;
        this.renameOnRotate = renameOnRotate;
        this.syncWrite = syncWrite;
        this.ioBuffer = new byte[ioWriteSize];
        fileDateFormatter = new FastDatePrinter(format,TimeZone.getDefault(), Locale.US);
        dateStamp = fileDateFormatter.format(System.currentTimeMillis());
        if (rotatable && renameOnRotate) {
            restore();
        }
        open();
    }

    @Override
    public void onEvent(ResizableCharBuffer event, long sequence, boolean endOfBatch) throws Exception {
        event.append(lineEndings);

        CharBuffer flippedBuffer = event.getCharBuffer();
        flippedBuffer.mark();
        ByteBuffer encodedBuffer;
        try {
            encodedBuffer = encoder.encode(flippedBuffer);
        } catch (Exception e) {
            log(event.toString());
            event.reset();
            return;
        }


        int size = encodedBuffer.limit();
        int remainingSize = (ioWriteSizeMinusOne - currentPosition);
        if (remainingSize >= size) {
            System.arraycopy(encodedBuffer.array(),0,ioBuffer,currentPosition,size);
            currentPosition+=size;
        } else {
            byte[] bytes = encodedBuffer.array();
            int offset = 0;
            do {

                System.arraycopy(bytes,offset,ioBuffer,currentPosition,remainingSize);
                currentPosition+=remainingSize;
                offset += remainingSize;

                if (currentPosition==ioWriteSizeMinusOne) {
                    try {
                        log(ioBuffer);
                    } catch (IOException e) {
                        log.info("Unable to log to file:" + currentLogFile);
                    }
                }

                remainingSize = Math.min(ioWriteSize, size - offset);
            } while (offset != size);


        }




        event.reset();
    }

    private void checkFile() {
        rotate();

        /* In case something external rotated the file instead */
        if (checkExists) {
            if (currentLogFile != null && !currentLogFile.exists()) {
                try {
                    close(false);
                } catch (Throwable e) {
                    ExceptionUtils.handleThrowable(e);
                    log.info(sm.getString("accessLogValve.closeFail"), e);
                }

                    /* Make sure date is correct */
                dateStamp = fileDateFormatter.format(
                        new Date(System.currentTimeMillis()));

                open();
            }
        }

    }

    public void log(byte[] message) throws IOException {
//        message.flip();
        checkFile();
        writer.write(message);
        currentPosition=0;
//        message.clear();
    }

    /**
     * Log the specified message to the log file, switching files if the date
     * has changed since the previous log call.
     *
     * @param message Message to be logged
     */
    public void log(String message) {

        checkFile();

        // Log this message
        if (writer != null)
        {
            try {
                try {
                    writer.write(message.getBytes("UTF-8"));
                } catch(UnsupportedEncodingException e) {
                    writer.write(message.getBytes());
                }
            } catch(IOException e) {
                log.error("Unable to log to file:" + currentLogFile);
            }

        }
    }

    /**
     * Rotate the log file if necessary.
     */
    public void rotate() {
        if (rotatable) {
            // Only do a logfile switch check once a second, max.
            long systime = System.currentTimeMillis();
            if ((systime - rotationLastChecked) > 1000) {
                rotationLastChecked = systime;
                String tsDate;
                // Check for a change of date
                tsDate = fileDateFormatter.format(rotationLastChecked);

                // If the date has changed, switch log files
                if (!dateStamp.equals(tsDate)) {
                    close(true);
                    dateStamp = tsDate;
                    open();
                }
            }
        }
    }

    /**
     * Open the new log file for the date specified by <code>dateStamp</code>.
     */
    protected void open() {
        // Open the current log file
        // If no rotate - no need for dateStamp in fileName
        File pathname = getLogFile(rotatable && !renameOnRotate);

        Charset charset = null;
        if (encoding != null) {
            try {
                charset = B2CConverter.getCharset(encoding);
            } catch (UnsupportedEncodingException ex) {
                log.error(sm.getString(
                        "accessLogValve.unsupportedEncoding", encoding), ex);
            }
        }
        if (charset == null) {
            charset = Charset.defaultCharset();
        }

        try {
//            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
//                    new FileOutputStream(pathname, true), charset), 128000),
//                    false);

            // preallocate the file to stop expansion during writes
            boolean preallocated = false;
            if(!pathname.exists() && ioPreallocateFileSize>0) {
                preallocated = true;
                preallocate(pathname);
            }

            if(syncWrite) {
                writer = new RandomAccessFile(pathname, "rwd");
            } else {
                writer = new RandomAccessFile(pathname, "rw");
            }

            if(!preallocated) {
                // set to append to the file
                writer.skipBytes( (int)writer.length() );

                // start on a new line
                writer.write(lineEndingsBytes);
                writer.getFD().sync();
            }

            writerChannel = writer.getChannel();
            currentLogFile = pathname;
        } catch (IOException e) {
            writer = null;
            currentLogFile = null;
            log.error(sm.getString("accessLogValve.openFail", pathname), e);
        }
    }

    private void preallocate(final File pathname)
            throws IOException
    {

        // do not preallocate it already exists, otherwise, the file will be overwritten
        RandomAccessFile file = new RandomAccessFile(pathname, "rw");

        for (long i = 0; i < ioPreallocateFileSize; i+= ioWriteSize)
        {
            file.write(BLANK_PAGE, 0, ioWriteSize);
        }

        file.getFD().sync();
        file.close();
    }


//    /**
//     * Rename the existing log file to something else. Then open the
//     * old log file name up once again. Intended to be called by a JMX
//     * agent.
//     *
//     *
//     * @param newFileName The file name to move the log file entry to
//     * @return true if a file was rotated with no error
//     */
//    public boolean rotate(String newFileName) {
//
//        if (currentLogFile != null) {
//            File holder = currentLogFile;
//            close(false);
//            try {
//                holder.renameTo(new File(newFileName));
//            } catch (Throwable e) {
//                ExceptionUtils.handleThrowable(e);
//                log.error(sm.getString("accessLogValve.rotateFail"), e);
//            }
//
//            /* Make sure date is correct */
//            dateStamp = fileDateFormatter.format(
//                    new Date(System.currentTimeMillis()));
//
//            open();
//            return true;
//        } else {
//            return false;
//        }
//
//    }

    // -------------------------------------------------------- Private Methods


    /**
     * Create a File object based on the current log file name.
     * Directories are created as needed but the underlying file
     * is not created or opened.
     *
     * @param useDateStamp include the timestamp in the file name.
     * @return the log file object
     */
    private File getLogFile(boolean useDateStamp) {

        // Create the directory if necessary
        File dir = new File(directory);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty(Globals.CATALINA_BASE_PROP), directory);
        }
        if (!dir.mkdirs() && !dir.isDirectory()) {
            log.error(sm.getString("accessLogValve.openDirFail", dir));
        }

        // Calculate the current log file name
        File pathname;
        if (useDateStamp) {
            pathname = new File(dir.getAbsoluteFile(), prefix + dateStamp
                    + suffix);
        } else {
            pathname = new File(dir.getAbsoluteFile(), prefix + suffix);
        }
        File parent = pathname.getParentFile();
        if (!parent.mkdirs() && !parent.isDirectory()) {
            log.error(sm.getString("accessLogValve.openDirFail", parent));
        }
        return pathname;
    }

    /**
     * Move a current but rotated log file back to the unrotated
     * one. Needed if date stamp inclusion is deferred to rotation
     * time.
     */
    private void restore() {
        File newLogFile = getLogFile(false);
        File rotatedLogFile = getLogFile(true);
        if (rotatedLogFile.exists() && !newLogFile.exists() &&
                !rotatedLogFile.equals(newLogFile)) {
            try {
                if (!rotatedLogFile.renameTo(newLogFile)) {
                    log.error(sm.getString("accessLogValve.renameFail", rotatedLogFile, newLogFile));
                }
            } catch (Throwable e) {
                ExceptionUtils.handleThrowable(e);
                log.error(sm.getString("accessLogValve.renameFail", rotatedLogFile, newLogFile), e);
            }
        }
    }


    /**
     * Close the currently open log file (if any)
     *
     * @param rename Rename file to final name after closing
     */
    private void close(boolean rename) {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
            writerChannel.close();
        } catch(IOException e) {

        }
        if (rename && renameOnRotate) {
            File newLogFile = getLogFile(true);
            if (!newLogFile.exists()) {
                try {
                    if (!currentLogFile.renameTo(newLogFile)) {
                        log.error(sm.getString("accessLogValve.renameFail", currentLogFile, newLogFile));
                    }
                } catch (Throwable e) {
                    ExceptionUtils.handleThrowable(e);
                    log.error(sm.getString("accessLogValve.renameFail", currentLogFile, newLogFile), e);
                }
            } else {
                log.error(sm.getString("accessLogValve.alreadyExists", currentLogFile, newLogFile));
            }
        }
        writer = null;
        dateStamp = "";
        currentLogFile = null;
    }

    public void shutdown() {
        close(false);
    }

}
