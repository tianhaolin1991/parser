package com.huawei.fossbot.dependency.log;

/**
 * 统一打印输出日志
 * 适配Jenkins的日志打印方法
 */
public class Logger {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Logger.class);
    private static final Logger instance = new Logger();
    public static void info(String msg) {
        log.info(msg);
    }

    public static void info(String format, Object arg) {
        log.info(format, arg);
    }

    public static void info(String format, Object arg1, Object arg2) {
        log.info(format, arg1, arg2);
    }

    public static void info(String format, Object... arguments) {
        log.info(format, arguments);
    }

    public static void info(String msg, Throwable t) {
        log.info(msg, t);
    }

    public static Logger getInstance(){
        return instance;
    }

}
