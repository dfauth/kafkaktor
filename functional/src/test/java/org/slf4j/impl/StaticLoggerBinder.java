package org.slf4j.impl;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.classic.util.ContextSelectorStaticBinder;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.StatusPrinter;
import com.github.dfauth.trycatch.AssertingLogger;
import org.slf4j.ILoggerFactory;
import org.slf4j.helpers.Util;
import org.slf4j.spi.LoggerFactoryBinder;

public class StaticLoggerBinder implements LoggerFactoryBinder {

    final static String NULL_CS_URL = CoreConstants.CODES_URL + "#null_CS";
    /**
     * Declare the version of the SLF4J API this implementation is compiled
     * against. The value of this field is usually modified with each release.
     */
    // to avoid constant folding by the compiler, this field must *not* be final
    public static String REQUESTED_API_VERSION = "1.7.16"; // !final
    /**
     * The unique instance of this class.
     */
    private static StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

    private static Object KEY = new Object();

    static {
        SINGLETON.init();
    }

    private final ContextSelectorStaticBinder contextSelectorBinder = ContextSelectorStaticBinder.getSingleton();
    private boolean initialized = false;
    private LoggerContext defaultLoggerContext = new LoggerContext();

    private StaticLoggerBinder() {
        defaultLoggerContext.setName(CoreConstants.DEFAULT_CONTEXT_NAME);
    }

    public static StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    /**
     * Package access for testing purposes.
     */
    static void reset() {
        SINGLETON = new StaticLoggerBinder();
        SINGLETON.init();
    }

    /**
     * Package access for testing purposes.
     */
    void init() {
        try {
            try {
                new ContextInitializer(defaultLoggerContext).autoConfig();
            } catch (JoranException je) {
                Util.report("Failed to auto configure default logger context", je);
            }
            // logback-292
            if (!StatusUtil.contextHasStatusListener(defaultLoggerContext)) {
                StatusPrinter.printInCaseOfErrorsOrWarnings(defaultLoggerContext);
            }
            contextSelectorBinder.init(defaultLoggerContext, KEY);
            initialized = true;
        } catch (Exception t) { // see LOGBACK-1159
            Util.report("Failed to instantiate [" + LoggerContext.class.getName() + "]", t);
        }
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return new AssertingLogger();
    }

    @Override
    public String getLoggerFactoryClassStr() {
        return AssertingLogger.class.getName();
    }
}
