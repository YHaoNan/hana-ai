package top.yudoge.hanaai.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppLogger {

    public static AppLogger get(Class<?> clazz) {
        return new AppLogger(LoggerFactory.getLogger(clazz));
    }

    public static AppLogger get(String name) {
        return new AppLogger(LoggerFactory.getLogger(name));
    }

    private final org.slf4j.Logger logger;

    private AppLogger(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    public void debug(String msg) {
        logger.debug(msg);
    }

    public void debug(String format, Object... args) {
        logger.debug(format, args);
    }

    public void info(String msg) {
        logger.info(msg);
    }

    public void info(String format, Object... args) {
        logger.info(format, args);
    }

    public void warn(String msg) {
        logger.warn(msg);
    }

    public void warn(String format, Object... args) {
        logger.warn(format, args);
    }

    public void error(String msg) {
        logger.error(msg);
    }

    public void error(String msg, Throwable t) {
        logger.error(msg, t);
    }

    public void error(String format, Object... args) {
        logger.error(format, args);
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }
}
