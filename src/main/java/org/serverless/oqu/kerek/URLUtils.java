package org.serverless.oqu.kerek;

import java.net.MalformedURLException;
import java.net.URL;

import static java.util.Arrays.stream;

public final class URLUtils {

    private URLUtils() {
    }

    public static String extractQueryParamValue(String url, String queryParam) {
        try {
            return stream(new URL(url).getQuery()
                    .split("&"))
                    .filter(pair -> pair.startsWith(queryParam))
                    .map(pair -> pair.split("=")[1])
                    .findFirst().orElse(null);
        } catch (MalformedURLException e) {
            System.out.printf("Error occurred while trying to parse URL %s : %s%n", url, e.getMessage());
        }
        return null;
    }

}
