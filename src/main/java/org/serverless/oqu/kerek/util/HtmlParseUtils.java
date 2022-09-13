package org.serverless.oqu.kerek.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.serverless.oqu.kerek.model.BookInfo;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static software.amazon.awssdk.utils.StringUtils.isBlank;


public final class HtmlParseUtils {

    private HtmlParseUtils() {
    }

    private static final String SCRIPT_REGEX = "^pages.push\\(\"(/\\w+){7}\\.png\\?time=\\d+&amp;key=\\w+\"\\);$";
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(SCRIPT_REGEX, Pattern.CASE_INSENSITIVE);
    private static final Pattern PAGE_PATH_PATTERN = Pattern.compile("(/\\w+){7}\\.png\\?time=\\d+&amp;key=\\w+");

    public static List<String> parseBookPagesUrls(String url) {
        Document bookPage;

        try {
            bookPage = Jsoup.parse(new URL(url), 30_000);
        } catch (IOException e) {
            System.err.printf("Error occurred while trying to parse URL %s : %s%n", url, e.getMessage());
            return emptyList();
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

    public static BookInfo parseBookInfo(final String url) {
        return parseBookInfo(url, null);

    }

    public static BookInfo parseBookInfo(final String url, final URL downloadUrl) {
        Document bookPage;
        try {
            bookPage = Jsoup.parse(new URL(url), 30_000);
        } catch (IOException e) {
            System.err.printf("Error occurred while trying to parse URL %s : %s%n", url, e.getMessage());
            return null;
        }

        final var title = requireNonNull(bookPage.select(".arrival-title").first()).text();
        final var author = requireNonNull(bookPage.select(".arrival-info-author").first()).text();
        final var imageUrl = "https://kazneb.kz" + requireNonNull(bookPage.select(".viewing-pic img[src]").first()).attributes().get("src");

        return new BookInfo(
                isBlank(title) ? null : title,
                isBlank(author) ? null : author,
                isBlank(imageUrl) ? null : imageUrl,
                downloadUrl
        );
    }

    public static String parsePagePath(String scriptLine) {

        if (!SCRIPT_PATTERN.matcher(scriptLine).matches()) {
            return null;
        }

        Matcher matcher = PAGE_PATH_PATTERN.matcher(scriptLine);

        return matcher.find() ? matcher.group() : null;
    }
}
