package org.serverless.oqu.kerek.util;

import org.serverless.template.ClientException;
import software.amazon.awssdk.utils.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;

import static java.lang.String.format;
import static java.util.Arrays.stream;

public final class URLUtils {

    private URLUtils() {
    }

    public static String extractQueryParamValue(String url, String queryParam) {
        try {
            return stream(new URL(url).getQuery()
                    .split("&"))
                    .filter(pair -> pair.startsWith(queryParam))
                    .map(pair -> pair.split("="))
                    .filter(array -> array.length == 2)
                    .map(array -> array[1])
                    .filter(StringUtils::isNotBlank)
                    .findFirst()
                    .orElseThrow(() -> new ClientException(400, format("The URL (%s) does not contain the query parameter %s", url, queryParam)));
        } catch (MalformedURLException e) {
            throw new ClientException(400, format("The URL (%s) is malformed", url));
        }
    }
}
