package org.serverless.oqu.kerek;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public final class HtmlParseUtils {

    private HtmlParseUtils() {}

    private static final String SCRIPT_REGEX = "^pages.push\\(\"(/\\w+){7}\\.png\\?time=\\d+&amp;key=\\w+\"\\);$";
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(SCRIPT_REGEX, Pattern.CASE_INSENSITIVE);
    private static final Pattern PAGE_PATH_PATTERN = Pattern.compile("(/\\w+){7}\\.png\\?time=\\d+&amp;key=\\w+");
    private static final Pattern PAGE_NAME_PATTERN = Pattern.compile("\\w+\\.png");

    public static List<String> parseBookPagesPath(String url) {
        Document bookPage;

        try {
            bookPage = Jsoup.parse(new URL(url), 5000);
        } catch (IOException e) {
            return Collections.emptyList();
        }

        Elements elements = bookPage.select("script");
        List<String> pages = new LinkedList<>();

        for (Element element : elements) {
            StringTokenizer tokenizer = new StringTokenizer(element.data());
            while (tokenizer.hasMoreTokens()) {
                pages.add(parsePagePath(tokenizer.nextElement().toString()));
            }
        }
        return pages.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static String parsePagePath(String scriptLine) {

        if (!SCRIPT_PATTERN.matcher(scriptLine).matches()) {
            return null;
        }

        Matcher matcher = PAGE_PATH_PATTERN.matcher(scriptLine);

        return matcher.find() ? matcher.group() : null;
    }

    public static String parsePageImageName(String scriptLine) {

        if (!PAGE_PATH_PATTERN.matcher(scriptLine).matches()) {
            return null;
        }

        Matcher matcher = PAGE_NAME_PATTERN.matcher(scriptLine);

        return matcher.find() ? matcher.group() : null;
    }
}
