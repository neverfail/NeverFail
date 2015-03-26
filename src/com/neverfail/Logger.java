package com.neverfail;

/**
 * Simple class to write message to console
 * Can be extend to write on other output
 */
@SuppressWarnings("UnusedDeclaration")
class Logger {
    private static Logger logger = new Logger();

    /**
     * Used to attach another logger
     * Logger implementation must use super() definition to transmit
     * message through all parents loggers
     * @param logger A Logger implementation
     */
    public static void setLogger(Logger logger) {
        Logger.logger = logger;
    }

    public static void log(final String msg) {
        logger._log(msg);
    }
    
    public static void warn(final String msg) {
        logger._warn(msg);
    }
    
    public static void info(final String msg) {
        logger._info(msg);
    }
    
    public static void error(final String msg) {
        logger._error(msg);
    }

    public void _log(final String msg) {
        System.out.println("[LOG] - " + msg);
    }
    
    public void _warn(final String msg) {
        System.out.println("[WARN] - " + msg);
    }
    
    public void _info(final String msg) {
        System.out.println("[INFO] - " + msg);
    }
    
    public void _error(final String msg) {
        System.err.println("[ERROR] - " + msg);
    }
}