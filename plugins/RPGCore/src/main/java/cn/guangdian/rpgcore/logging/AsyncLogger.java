package cn.guangdian.rpgcore.logging;

import cn.guangdian.rpgcore.api.GameLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncLogger implements GameLogger {

    private static final String LOGGER_NAME = "cn.guangdian.rpgcore";
    private final Logger logger;

    public AsyncLogger() {
        this.logger = LoggerFactory.getLogger(LOGGER_NAME);
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warning(String message) {
        logger.warn(message);
    }

    @Override
    public void severe(String message) {
        logger.error(message);
    }

    @Override
    public void debug(String message) {
        logger.debug(message);
    }

    @Override
    public int getQueueSize() {
        return 0;
    }

    @Override
    public long getTotalLogged() {
        return 0;
    }

    @Override
    public long getTotalDropped() {
        return 0;
    }

    @Override
    public void shutdown() {
    }
}