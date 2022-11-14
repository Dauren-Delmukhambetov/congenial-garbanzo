package org.serverless.oqu.kerek.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.serverless.oqu.kerek.model.BookShortInfo;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
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

    public static Optional<BookShortInfo> parseBookInfo(final String url) {
        Document bookPage;
        try {
            bookPage = Jsoup.parse(new URL(url), 30_000);
        } catch (IOException e) {
            System.err.printf("Error occurred while trying to parse URL %s : %s%n", url, e.getMessage());
            return Optional.empty();
        }

        final var title = ofNullable(bookPage.select(".arrival-title").first())
                .map(Element::text)
                .filter(not(String::isBlank))
                .orElse(null);
        final var author = ofNullable(bookPage.select(".arrival-info-author").first())
                .map(Element::text)
                .filter(not(String::isBlank))
                .orElse(null);
        final var imageUrl =  ofNullable(bookPage.select(".viewing-pic img[src]").first())
                .map(Element::attributes)
                .map(a -> a.get("src"))
                .filter(not(String::isBlank))
                .map(path -> format("https://kazneb.kz%s", path))
                .orElse(null);

        return Optional.of(BookShortInfo.builder()
                .title(title)
                .author(author)
                .imageUrl(imageUrl)
                .build()
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
