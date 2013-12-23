package com.dslplatform.compiler.client.cmdline.logger;

import org.apache.logging.log4j.LogManager;

import com.dslplatform.compiler.client.io.Logger;

public class LoggerLog4J implements Logger {
    private final Logger logger;

    public LoggerLog4J() {
        Logger logger;

        try {
            Class.forName("org.apache.logging.log4j.LogManager");
            logger = new LoggerLog4JBridge();
        } catch (final ClassNotFoundException e) {
            logger = null;
        }

        this.logger = logger;
    }

    @Override
    public boolean isAvailable() {
        return logger != null;
    }

    @Override
    public void setLevel(final Level level) {
        if (isAvailable()) {
            logger.setLevel(level);
        }
    }

    @Override
    public void trace(String message) {
        if (isAvailable()) {
            logger.trace(message);
        }
    }

    @Override
    public void debug(String message) {
        if (isAvailable()) {
            logger.debug(message);
        }
    }

    @Override
    public void info(String message) {
        if (isAvailable()) {
            logger.info(message);
        }
    }

    @Override
    public void warn(String message) {
        if (isAvailable()) {
            logger.warn(message);
        }
    }

    @Override
    public void error(String message) {
        if (isAvailable()) {
            logger.error(message);
        }
    }

    private static class LoggerLog4JBridge implements Logger {
        private final org.apache.logging.log4j.Logger logger;

        public LoggerLog4JBridge() {
            logger = LogManager.getLogger(LoggerLog4J.class);
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void setLevel(final Level level) {
            // NOOP: Cannot set level of Log4J logger without knowing the underlying implementation
        }

        @Override
        public void trace(String message) {
            logger.trace(message);
        }

        @Override
        public void debug(String message) {
            logger.debug(message);
        }

        @Override
        public void info(String message) {
            logger.info(message);
        }

        @Override
        public void warn(String message) {
            logger.warn(message);
        }

        @Override
        public void error(String message) {
            logger.error(message);
        }
    }
}