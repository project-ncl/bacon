package org.jboss.pnc.bacon.pnc.client;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BifrostClient {

    private Logger logger = LoggerFactory.getLogger(BifrostClient.class);

    private final URI baseUrl;
    private final HttpClient client;

    public BifrostClient(URI baseUrl) {
        this.baseUrl = baseUrl;
        client = HttpClients.createDefault();
    }

    public void writeLog(String id, boolean follow, Consumer<String> onLine) throws IOException {
        String query = "direction=ASC"
                + "&matchFilters=mdc.processContext.keyword:build-" + id
                + "&prefixFilters=loggerName.keyword:org.jboss.pnc._userlog_";

        if (follow) {
            query += "&follow=true";
        } else {
            query += "&follow=false";
        }

        URI logsUrl = baseUrl.resolve(URI.create("/text?" + query));
        logger.debug("Reading logs from {}.", logsUrl.toString());

        HttpGet httpGet = new HttpGet(logsUrl);

        HttpResponse response = client.execute(httpGet);
        try (
                InputStream inputStream = response.getEntity().getContent();
                BufferedReader is = new BufferedReader(new InputStreamReader(inputStream));
        ) {
            is.lines().forEach(line -> onLine.accept(line));
        }
    }
}
