package cn.guangdian.rpgcore.api;

public interface GameLogger {

    void info(String message);

    void warning(String message);

    void severe(String message);

    void debug(String message);

    int getQueueSize();

    long getTotalLogged();

    long getTotalDropped();

    void shutdown();
}