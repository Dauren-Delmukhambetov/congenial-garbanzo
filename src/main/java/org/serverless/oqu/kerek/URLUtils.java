package org.serverless.oqu.kerek;

import org.apache.http.NameValuePair;

import java.net.MalformedURLException;
import java.net.URL;

import static com.amazonaws.util.StringUtils.UTF8;
import static org.apache.http.client.utils.URLEncodedUtils.parse;

public final class URLUtils {

    private URLUtils() {
    }

    public static String extractQueryParamValue(String url, String queryParam) {
        try {
            return parse(new URL(url).getQuery(), UTF8).stream()
                    .filter(it -> it.getName().equalsIgnoreCase(queryParam))
                    .findFirst()
                    .map(NameValuePair::getValue).orElse(null);
        } catch (MalformedURLException e) {
            System.out.printf("Error occurred while trying to parse URL %s : %s%n", url, e.getMessage());
        }
        return null;
    }

}
