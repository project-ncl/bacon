package org.jboss.pnc.bacon.pig.impl.repo;

import org.slf4j.Logger;

import io.quarkus.devtools.messagewriter.MessageWriter;

class Sl4jMessageWriter implements MessageWriter {

    private final Logger log;

    Sl4jMessageWriter(Logger log) {
        this.log = log;
    }

    @Override
    public void info(String s) {
        log.info(s);
    }

    @Override
    public void error(String s) {
        log.error(s);
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        log.debug(s);
    }

    @Override
    public void warn(String s) {
        log.warn(s);
    }
}
