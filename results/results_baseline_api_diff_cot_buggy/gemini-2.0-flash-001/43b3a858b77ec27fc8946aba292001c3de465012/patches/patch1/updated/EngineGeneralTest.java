org.slf4j.Logger rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    if (rootLogger instanceof ch.qos.logback.classic.Logger) {
        ((ch.qos.logback.classic.Logger) rootLogger).setLevel(Level.TRACE);
    }
    