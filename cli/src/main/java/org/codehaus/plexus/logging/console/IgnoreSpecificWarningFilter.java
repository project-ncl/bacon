package org.codehaus.plexus.logging.console;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class IgnoreSpecificWarningFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {

        String msg = event.getFormattedMessage();

        if (msg != null &&
                msg.contains("CDI container is not available")) {
            return FilterReply.DENY;
        }
        if (msg != null &&
                msg.contains("Invalid cookie header")) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }
}
