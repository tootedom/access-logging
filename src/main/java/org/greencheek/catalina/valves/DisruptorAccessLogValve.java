package org.greencheek.catalina.valves;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;

import javax.servlet.ServletException;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.IgnoreExceptionHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.catalina.AccessLog;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.Constants;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.greencheek.util.Util;
import org.greencheek.logging.accesscomponents.*;
import org.greencheek.logging.disruptor.DefaultWaitStrategyFactory;
import org.greencheek.logging.disruptor.LogMessageEventTranslator;
import org.greencheek.logging.disruptor.WaitStrategyFactory;
import org.greencheek.logging.domin.ResizableCharBuffer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import org.greencheek.logging.disruptor.io.*;

/**
 *
 * Modification of the original AccessLogValue from tomcat 7, but changed to use
 * a distruptor for writing to the access log.  This removes the synchronization blocks
 * the original access log implementation has around the write to disk
 *
 *
 * <p>Implementation of the <b>Valve</b> interface that generates a web server
 * access log with the detailed line contents matching a configurable pattern.
 * The syntax of the available patterns is similar to that supported by the
 * <a href="http://httpd.apache.org/">Apache HTTP Server</a>
 * <code>mod_log_config</code> module.  As an additional feature,
 * automatic rollover of log files when the date changes is also supported.</p>
 *
 * <p>Patterns for the logged message may include constant text or any of the
 * following replacement strings, for which the corresponding information
 * from the specified Response is substituted:</p>
 * <ul>
 * <li><b>%a</b> - Remote IP address
 * <li><b>%A</b> - Local IP address
 * <li><b>%b</b> - Bytes sent, excluding HTTP headers, or '-' if no bytes
 *     were sent
 * <li><b>%B</b> - Bytes sent, excluding HTTP headers
 * <li><b>%h</b> - Remote host name (or IP address if
 * <code>enableLookups</code> for the connector is false)
 * <li><b>%H</b> - Request protocol
 * <li><b>%l</b> - Remote logical username from identd (always returns '-')
 * <li><b>%m</b> - Request method
 * <li><b>%p</b> - Local port
 * <li><b>%q</b> - Query string (prepended with a '?' if it exists, otherwise
 *     an empty string
 * <li><b>%r</b> - First line of the request
 * <li><b>%s</b> - HTTP status code of the response
 * <li><b>%S</b> - User session ID
 * <li><b>%t</b> - Date and time, in Common Log Format format
 * <li><b>%t{format}</b> - Date and time, in any format supported by SimpleDateFormat
 * <li><b>%u</b> - Remote user that was authenticated
 * <li><b>%U</b> - Requested URL path
 * <li><b>%v</b> - Local server name
 * <li><b>%D</b> - Time taken to process the request, in millis
 * <li><b>%T</b> - Time taken to process the request, in seconds
 * <li><b>%I</b> - current Request thread name (can compare later with stacktraces)
 * </ul>
 * <p>In addition, the caller can specify one of the following aliases for
 * commonly utilized patterns:</p>
 * <ul>
 * <li><b>common</b> - <code>%h %l %u %t "%r" %s %b</code>
 * <li><b>combined</b> -
 *   <code>%h %l %u %t "%r" %s %b "%{Referer}i" "%{User-Agent}i"</code>
 * </ul>
 *
 * <p>
 * There is also support to write information from the cookie, incoming
 * header, the Session or something else in the ServletRequest.<br>
 * It is modeled after the
 * <a href="http://httpd.apache.org/">Apache HTTP Server</a> log configuration
 * syntax:</p>
 * <ul>
 * <li><code>%{xxx}i</code> for incoming headers
 * <li><code>%{xxx}o</code> for outgoing response headers
 * <li><code>%{xxx}c</code> for a specific cookie
 * <li><code>%{xxx}r</code> xxx is an attribute in the ServletRequest
 * <li><code>%{xxx}s</code> xxx is an attribute in the HttpSession
 * <li><code>%{xxx}t</code> xxx is an enhanced SimpleDateFormat pattern
 * (see Configuration Reference document for details on supported time patterns)
 * </ul>
 *
 * <p>
 * Log rotation can be on or off. This is dictated by the
 * <code>rotatable</code> property.
 * </p>
 *
 * <p>
 * For UNIX users, another field called <code>checkExists</code> is also
 * available. If set to true, the log file's existence will be checked before
 * each logging. This way an external log rotator can move the file
 * somewhere and Tomcat will start with a new file.
 * </p>
 *
 * <p>
 * For JMX junkies, a public method called <code>rotate</code> has
 * been made available to allow you to tell this instance to move
 * the existing log file to somewhere else and start writing a new log file.
 * </p>
 *
 * <p>
 * Conditional logging is also supported. This can be done with the
 * <code>conditionUnless</code> and <code>conditionIf</code> properties.
 * If the value returned from ServletRequest.getAttribute(conditionUnless)
 * yields a non-null value, the logging will be skipped.
 * If the value returned from ServletRequest.getAttribute(conditionIf)
 * yields the null value, the logging will be skipped.
 * The <code>condition</code> attribute is synonym for
 * <code>conditionUnless</code> and is provided for backwards compatibility.
 * </p>
 *
 * <p>
 * For extended attributes coming from a getAttribute() call,
 * it is you responsibility to ensure there are no newline or
 * control characters.
 * </p>
 *
 * @author Craig R. McClanahan
 * @author Jason Brittain
 * @author Remy Maucherat
 * @author Takayuki Kaneko
 * @author Peter Rossbach
 *
 * @version $Id$
 */

public class DisruptorAccessLogValve extends ValveBase implements AccessLog {

    private static final Log log = LogFactory.getLog(DisruptorAccessLogValve.class);

    private final ExecutorService executorService = newSingleThreadExecutor();
    private Disruptor<ResizableCharBuffer> disruptor;

    // the size of the ring buffer for storing log messages
    // This is about 22mb (less than.  A ResizeableCharBuffer, that is 256 chars in size
    // is 680 bytes in size.
    private int bufferSize = 32768;

    private WaitStrategyFactory waitStrategyFactory = new DefaultWaitStrategyFactory(DefaultWaitStrategyFactory.WAIT_STRATEGY_TYPE.SLEEPING);
    private String waitStrategy;
    LogEventHandler eventHandler;

    // The size of a message.
    private int messageSize = 256;

    // The page size of the os.  The size of the buffer
    // before an io write is committed
    private int ioPageSize = 4096;

    // PreAllocate 100 mb access log file
    private int ioPreallocatedFileSize = 1024*1024*100;


    //------------------------------------------------------ Constructor
    public DisruptorAccessLogValve() {
        super(true);
    }

    // ----------------------------------------------------- Instance Variables


    /**
     * The as-of date for the currently open log file, or a zero-length
     * string if there is no open log file.
     */
    private volatile String dateStamp = "";


    /**
     * The directory in which log files are created.
     */
    private String directory = "logs";


    /**
     * The descriptive information about this implementation.
     */
    protected static final String info =
            "org.greencheek.catalina.valves.DisruptorAccessLogValve/1.0";


    /**
     * enabled this component
     */
    protected boolean enabled = true;

    /**
     * The pattern used to format our access log lines.
     */
    protected String pattern = null;


    /**
     * The prefix that is added to log file filenames.
     */
    protected String prefix = "access_log.";


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
     * Buffered logging.
     */
    private boolean buffered = true;


    /**
     * The suffix that is added to log file filenames.
     */
    protected String suffix = "";


    /**
     * The PrintWriter to which we are currently logging, if any.
     */
    protected PrintWriter writer = null;


    /**
     * A date formatter to format a Date using the format
     * given by <code>fileDateFormat</code>.
     */
    protected SimpleDateFormat fileDateFormatter = null;


    /**
     * The current log file we are writing to. Helpful when checkExists
     * is true.
     */
    protected File currentLogFile = null;


    /**
     * The system time when we last updated the Date that this valve
     * uses for log lines.
     */
    private static final ThreadLocal<Date> localDate = new ThreadLocal<Date>() {
        @Override
        protected Date initialValue() {
            return new Date();
        }
    };


    /**
     * The disruptor translators used for copying into the ResizableCharBuffer
     * from the ring buffer.
     */
    private static final ThreadLocal<LogMessageEventTranslator> logEventTranslator = new ThreadLocal<LogMessageEventTranslator>() {
        @Override
        protected LogMessageEventTranslator initialValue() {
            return new LogMessageEventTranslator();
        }
    };


    /**
     * Resolve hosts.
     */
    private boolean resolveHosts = false;


    /**
     * Instant when the log daily rotation was last checked.
     */
    private volatile long rotationLastChecked = 0L;

    /**
     * Do we check for log file existence? Helpful if an external
     * agent renames the log file so we can automagically recreate it.
     */
    private boolean checkExists = false;


    /**
     * Are we doing conditional logging. default null.
     * It is the value of <code>conditionUnless</code> property.
     */
    protected String condition = null;

    /**
     * Are we doing conditional logging. default null.
     * It is the value of <code>conditionIf</code> property.
     */
    protected String conditionIf = null;

    /**
     * Date format to place in log file name.
     */
    protected String fileDateFormat = "yyyy-MM-dd";


    /**
     * Name of locale used to format timestamps in log entries and in
     * log file name suffix.
     */
    protected String localeName = Locale.getDefault().toString();


    /**
     * Locale used to format timestamps in log entries and in
     * log file name suffix.
     */
    protected Locale locale = Locale.getDefault();

    /**
     * Character set used by the log file. If it is <code>null</code>, the
     * system default character set will be used. An empty string will be
     * treated as <code>null</code> when this property is assigned.
     */
    protected String encoding = null;

    /**
     * Array of AccessLogElement, they will be used to make log message.
     */
    protected AccessLogElement[] logElements = null;

    /**
     * @see #setRequestAttributesEnabled(boolean)
     */
    protected boolean requestAttributesEnabled = false;


    // ------------------------------------------------------------- Properties

    public int getIoPreallocatedFileSize() {
        return ioPreallocatedFileSize;
    }

    public void setIoPreallocatedFileSize(int size) {
        this.ioPreallocatedFileSize = size;
    }

    public int getIoPageSize() {
        return ioPageSize;
    }

    public void setIoPageSize(int ioPageSize) {
        this.ioPageSize = ioPageSize;
    }

    public String getWaitStrategy() {
        return waitStrategy;
    }

    public void setWaitStrategy(String waitStrategy) {
        this.waitStrategy = waitStrategy;
        if(waitStrategy.contains("yield")) {
            waitStrategyFactory = new DefaultWaitStrategyFactory(DefaultWaitStrategyFactory.WAIT_STRATEGY_TYPE.YIELDING);
        }
        else if(waitStrategy.contains("block")) {
            waitStrategyFactory = new DefaultWaitStrategyFactory(DefaultWaitStrategyFactory.WAIT_STRATEGY_TYPE.BLOCKING);
        }
        else if(waitStrategy.contains("sleep")) {
            waitStrategyFactory = new DefaultWaitStrategyFactory(DefaultWaitStrategyFactory.WAIT_STRATEGY_TYPE.SLEEPING);
        }
        else if (waitStrategy.contains("busy")) {
            waitStrategyFactory = new DefaultWaitStrategyFactory(DefaultWaitStrategyFactory.WAIT_STRATEGY_TYPE.BUSY);
        }
        else {
            waitStrategyFactory = new DefaultWaitStrategyFactory(DefaultWaitStrategyFactory.WAIT_STRATEGY_TYPE.YIELDING);
        }
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public int getMessageSize() {
        return messageSize;
    }

    public void setMessageSize(int size) {
        this.messageSize = size;
    }


    /**
     * @return Returns the enabled.
     */
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRequestAttributesEnabled(boolean requestAttributesEnabled) {
        this.requestAttributesEnabled = requestAttributesEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getRequestAttributesEnabled() {
        return requestAttributesEnabled;
    }

    /**
     * @param enabled
     *            The enabled to set.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return the directory in which we create log files.
     */
    public String getDirectory() {
        return (directory);
    }


    /**
     * Set the directory in which we create log files.
     *
     * @param directory The new log file directory
     */
    public void setDirectory(String directory) {
        this.directory = directory;
    }


    /**
     * Return descriptive information about this implementation.
     */
    @Override
    public String getInfo() {
        return (info);
    }


    /**
     * Return the format pattern.
     */
    public String getPattern() {
        return (this.pattern);
    }


    /**
     * Set the format pattern, first translating any recognized alias.
     *
     * @param pattern The new pattern
     */
    public void setPattern(String pattern) {
        if (pattern == null) {
            this.pattern = "";
        } else if (pattern.equals(Constants.AccessLog.COMMON_ALIAS)) {
            this.pattern = Constants.AccessLog.COMMON_PATTERN;
        } else if (pattern.equals(Constants.AccessLog.COMBINED_ALIAS)) {
            this.pattern = Constants.AccessLog.COMBINED_PATTERN;
        } else {
            this.pattern = pattern;
        }
        logElements = createLogElements();
    }


    /**
     * Check for file existence before logging.
     */
    public boolean isCheckExists() {

        return checkExists;

    }


    /**
     * Set whether to check for log file existence before logging.
     *
     * @param checkExists true meaning to check for file existence.
     */
    public void setCheckExists(boolean checkExists) {

        this.checkExists = checkExists;

    }


    /**
     * Return the log file prefix.
     */
    public String getPrefix() {
        return (prefix);
    }


    /**
     * Set the log file prefix.
     *
     * @param prefix The new log file prefix
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }


    /**
     * Should we rotate the logs
     */
    public boolean isRotatable() {
        return rotatable;
    }


    /**
     * Set the value is we should we rotate the logs
     *
     * @param rotatable true is we should rotate.
     */
    public void setRotatable(boolean rotatable) {
        this.rotatable = rotatable;
    }


    /**
     * Should we defer inclusion of the date stamp in the file
     * name until rotate time
     */
    public boolean isRenameOnRotate() {
        return renameOnRotate;
    }


    /**
     * Set the value if we should defer inclusion of the date
     * stamp in the file name until rotate time
     *
     * @param renameOnRotate true if defer inclusion of date stamp
     */
    public void setRenameOnRotate(boolean renameOnRotate) {
        this.renameOnRotate = renameOnRotate;
    }


    /**
     * Is the logging buffered
     */
    public boolean isBuffered() {
        return buffered;
    }


    /**
     * Set the value if the logging should be buffered
     *
     * @param buffered true if buffered.
     */
    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }


    /**
     * Return the log file suffix.
     */
    public String getSuffix() {
        return (suffix);
    }


    /**
     * Set the log file suffix.
     *
     * @param suffix The new log file suffix
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }


    /**
     * Set the resolve hosts flag.
     *
     * @param resolveHosts The new resolve hosts value
     * @deprecated Unused, removed in Tomcat 8.
     * See org.apache.catalina.connector.Connector.setEnableLookups(boolean).
     */
    @Deprecated
    public void setResolveHosts(boolean resolveHosts) {
        this.resolveHosts = resolveHosts;
    }


    /**
     * Get the value of the resolve hosts flag.
     * @deprecated Unused, removed in Tomcat 8.
     * See org.apache.catalina.connector.Connector.setEnableLookups(boolean).
     */
    @Deprecated
    public boolean isResolveHosts() {
        return resolveHosts;
    }


    /**
     * Return whether the attribute name to look for when
     * performing conditional logging. If null, every
     * request is logged.
     */
    public String getCondition() {
        return condition;
    }


    /**
     * Set the ServletRequest.attribute to look for to perform
     * conditional logging. Set to null to log everything.
     *
     * @param condition Set to null to log everything
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }


    /**
     * Return whether the attribute name to look for when
     * performing conditional logging. If null, every
     * request is logged.
     */
    public String getConditionUnless() {
        return getCondition();
    }


    /**
     * Set the ServletRequest.attribute to look for to perform
     * conditional logging. Set to null to log everything.
     *
     * @param condition Set to null to log everything
     */
    public void setConditionUnless(String condition) {
        setCondition(condition);
    }

    /**
     * Return whether the attribute name to look for when
     * performing conditional logging. If null, every
     * request is logged.
     */
    public String getConditionIf() {
        return conditionIf;
    }


    /**
     * Set the ServletRequest.attribute to look for to perform
     * conditional logging. Set to null to log everything.
     *
     * @param condition Set to null to log everything
     */
    public void setConditionIf(String condition) {
        this.conditionIf = condition;
    }

    /**
     *  Return the date format date based log rotation.
     */
    public String getFileDateFormat() {
        return fileDateFormat;
    }


    /**
     *  Set the date format date based log rotation.
     */
    public void setFileDateFormat(String fileDateFormat) {
        String newFormat;
        if (fileDateFormat == null) {
            newFormat = "";
        } else {
            newFormat = fileDateFormat;
        }
        this.fileDateFormat = newFormat;

        synchronized (this) {
            fileDateFormatter = new SimpleDateFormat(newFormat, Locale.US);
            fileDateFormatter.setTimeZone(TimeZone.getDefault());
        }
    }


    /**
     * Return the locale used to format timestamps in log entries and in
     * log file name suffix.
     */
    public String getLocale() {
        return localeName;
    }


    /**
     * Set the locale used to format timestamps in log entries and in
     * log file name suffix. Changing the locale is only supported
     * as long as the AccessLogValve has not logged anything. Changing
     * the locale later can lead to inconsistent formatting.
     *
     * @param localeName The locale to use.
     */
    public void setLocale(String localeName) {
        this.localeName = localeName;
        locale = findLocale(localeName, locale);
    }

    /**
     * Return the character set name that is used to write the log file.
     *
     * @return Character set name, or <code>null</code> if the system default
     *  character set is used.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Set the character set that is used to write the log file.
     *
     * @param encoding The name of the character set.
     */
    public void setEncoding(String encoding) {
        if (encoding != null && encoding.length() > 0) {
            this.encoding = encoding;
        } else {
            this.encoding = null;
        }
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Execute a periodic task, such as reloading, etc. This method will be
     * invoked inside the classloading context of this container. Unexpected
     * throwables will be caught and logged.
     */
    @Override
    public synchronized void backgroundProcess() {
        if (getState().isAvailable() && getEnabled() && writer != null &&
                buffered) {
            writer.flush();
        }
    }

    /**
     * Log a message summarizing the specified request and response, according
     * to the format specified by the <code>pattern</code> property.
     *
     * @param request Request being processed
     * @param response Response being processed
     *
     * @exception IOException if an input/output error has occurred
     * @exception ServletException if a servlet error has occurred
     */
    @Override
    public void invoke(Request request, Response response) throws IOException,
            ServletException {
        getNext().invoke(request, response);
    }


    @Override
    public void log(Request request, Response response, long time) {
        if (!getState().isAvailable() || !getEnabled() || logElements == null
                || condition != null
                && null != request.getRequest().getAttribute(condition)
                || conditionIf != null
                && null == request.getRequest().getAttribute(conditionIf)) {
            return;
        }

        /**
         * XXX This is a bit silly, but we want to have start and stop time and
         * duration consistent. It would be better to keep start and stop
         * simply in the request and/or response object and remove time
         * (duration) from the interface.
         */
        long start = request.getCoyoteRequest().getStartTime();

        LogMessageEventTranslator translator = logEventTranslator.get();
        translator.setLogElements(logElements);
        translator.setLogDate(start + time);
        translator.setRequest(request);
        translator.setResponse(response);
        translator.setTime(time);
        disruptor.publishEvent(translator);
    }



    // -------------------------------------------------------- Private Methods




    /**
     * This method returns a ThreadLocal Date object that is set to the
     * specified time. This saves creating a new Date object for every request.
     *
     * @return Date
     */
    private static Date getDate(long systime) {
        Date date = localDate.get();
        date.setTime(systime);
        return date;
    }


    /**
     * Find a locale by name
     */
    protected static Locale findLocale(String name, Locale fallback) {
        if (name == null || name.isEmpty()) {
            return Locale.getDefault();
        } else {
            for (Locale l: Locale.getAvailableLocales()) {
                if (name.equals(l.toString())) {
                    return(l);
                }
            }
        }
        log.error(sm.getString("accessLogValve.invalidLocale", name));
        return fallback;
    }


    /**
     * Start this component and implement the requirements
     * of {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    @Override
    protected synchronized void startInternal() throws LifecycleException {

        disruptor = new Disruptor<ResizableCharBuffer>(
                new EventFactory<ResizableCharBuffer>() {
                    @Override
                    public ResizableCharBuffer newInstance() {
                        return new ResizableCharBuffer(messageSize);
                    }
                },
                Util.ceilingNextPowerOfTwo(bufferSize), executorService,
                ProducerType.MULTI, waitStrategyFactory.createWaitStrategy());


        eventHandler = new LogEventHandler(
                sm,encoding,log,getFileDateFormat(),
                directory,rotatable,renameOnRotate,
                !buffered,ioPageSize,ioPreallocatedFileSize,suffix,prefix,checkExists);
        disruptor.handleExceptionsWith(new IgnoreExceptionHandler());
        disruptor.handleEventsWith(new EventHandler[] {eventHandler});
        disruptor.start();

        setState(LifecycleState.STARTING);
    }


    /**
     * Stop this component and implement the requirements
     * of {@link org.apache.catalina.util.LifecycleBase#stopInternal()}.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    @Override
    protected synchronized void stopInternal() throws LifecycleException {
        eventHandler.shutdown();
        executorService.shutdownNow();
        disruptor.shutdown();
        setState(LifecycleState.STOPPING);
    }


    /**
     * parse pattern string and create the array of AccessLogElement
     */
    protected AccessLogElement[] createLogElements() {
        List<AccessLogElement> list = new ArrayList<AccessLogElement>();
        boolean replace = false;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (replace) {
                /*
                 * For code that processes {, the behavior will be ... if I do
                 * not encounter a closing } - then I ignore the {
                 */
                if ('{' == ch) {
                    StringBuilder name = new StringBuilder();
                    int j = i + 1;
                    for (; j < pattern.length() && '}' != pattern.charAt(j); j++) {
                        name.append(pattern.charAt(j));
                    }
                    if (j + 1 < pattern.length()) {
                        /* the +1 was to account for } which we increment now */
                        j++;
                        list.add(createAccessLogElement(name.toString(),
                                pattern.charAt(j)));
                        i = j; /* Since we walked more than one character */
                    } else {
                        // D'oh - end of string - pretend we never did this
                        // and do processing the "old way"
                        list.add(createAccessLogElement(ch));
                    }
                } else {
                    list.add(createAccessLogElement(ch));
                }
                replace = false;
            } else if (ch == '%') {
                replace = true;
                String characters = buf.toString();
                if(characters.length()>1) {
                    list.add(new StringElement(characters));
                } else {
                    if(characters.length()==1) {
                        list.add(new CharElement(characters.charAt(0)));
                    }
                }
                buf = new StringBuilder();
            } else {
                buf.append(ch);
            }
        }
        if (buf.length() > 1) {
            list.add(new StringElement(buf.toString()));
        } else {
            if(buf.length()==1) {
                list.add(new CharElement(buf.charAt(0)));
            }
        }
        return list.toArray(new AccessLogElement[0]);
    }

    /**
     * create an AccessLogElement implementation which needs header string
     */
    protected AccessLogElement createAccessLogElement(String header, char pattern) {
        switch (pattern) {
            case 'i':
                return new HeaderElement(header);
            case 'c':
                return new CookieElement(header);
            case 'o':
                return new ResponseHeaderElement(header);
            case 'r':
                return new RequestAttributeElement(header);
            case 's':
                return new SessionAttributeElement(header);
            case 't':
                return new DateAndTimeElement(header);
            default:
                return new StringElement("???");
        }
    }

    /**
     * create an AccessLogElement implementation
     */
    protected AccessLogElement createAccessLogElement(char pattern) {
        switch (pattern) {
            case 'a':
                return new RemoteAddrElement(requestAttributesEnabled);
            case 'A':
                return new LocalAddrElement();
            case 'b':
                return new ByteSentElement(true);
            case 'B':
                return new ByteSentElement(false);
            case 'D':
                return new ElapsedTimeElement(true);
            case 'F':
                return new FirstByteTimeElement();
            case 'h':
                return new HostElement(requestAttributesEnabled);
            case 'H':
                return new ProtocolElement(requestAttributesEnabled);
            case 'l':
                return new LogicalUserNameElement();
            case 'm':
                return new MethodElement();
            case 'p':
                return new LocalPortElement(requestAttributesEnabled);
            case 'q':
                return new QueryElement();
            case 'r':
                return new RequestElement();
            case 's':
                return new HttpStatusCodeElement();
            case 'S':
                return new SessionIdElement();
            case 't':
                return new DateAndTimeElement();
            case 'T':
                return new ElapsedTimeElement(false);
            case 'u':
                return new UserElement();
            case 'U':
                return new RequestURIElement();
            case 'v':
                return new LocalServerNameElement();
            case 'I':
                return new ThreadNameElement();
            default:
                return new StringElement("???" + pattern + "???");
        }
    }
}