package org.jboss.pnc.bacon.pnc.client;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BifrostClient {

    public enum LogType {
        COMPLETE, BUILD, ALIGNMENT
    }

    private static final Logger log = LoggerFactory.getLogger(BifrostClient.class);

    private final URI baseUrl;
    private final HttpClient client;

    public BifrostClient(URI baseUrl) {
        this.baseUrl = baseUrl;
        client = HttpClients.createDefault();
    }

    public List<String> getLog(String buildId, LogType logType) throws IOException {
        List<String> logs = new ArrayList<>();
        writeLog(buildId, false, logs::add, logType);
        return logs;
    }

    public void writeLog(String id, boolean follow, Consumer<String> onLine, LogType logType) throws IOException {

        String query;

        switch (logType) {
            case BUILD:
                query = MessageFormat.format(
                        "direction=ASC&matchFilters=mdc.processContext:build-{0},loggerName:org.jboss.pnc._userlog_.build-log&batchSize=5000&batchDelay=500&format=LEVEL",
                        id);
                break;
            case ALIGNMENT:
                query = MessageFormat.format(
                        "direction=ASC&matchFilters=mdc.processContext:build-{0},loggerName:org.jboss.pnc._userlog_.alignment-log&batchSize=5000&batchDelay=500&format=LEVEL",
                        id);
                break;
            default:
                query = MessageFormat.format(
                        "direction=ASC&matchFilters=mdc.processContext:build-{0}&prefixFilters=loggerName:org.jboss.pnc._userlog_&batchSize=500&batchDelay=500&format=TIMESTAMP",
                        id);
        }

        if (follow) {
            query += "&follow=true";
        } else {
            query += "&follow=false";
        }

        URI logsUrl = baseUrl.resolve(URI.create("/text?" + query));
        log.debug("Reading logs from {}", logsUrl);

        HttpUriRequest httpGet = new HttpGet(logsUrl);
        HttpResponse response = client.execute(httpGet);

        try (InputStream is = response.getEntity().getContent();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            br.lines().forEach(onLine);
        }
    }
}
