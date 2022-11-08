package org.serverless.oqu.kerek.repo;

import org.serverless.oqu.kerek.model.BookInfo;
import org.serverless.oqu.kerek.model.BookRequestContext;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.ImmutableMap;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;
import static software.amazon.awssdk.utils.StringUtils.isNotBlank;

public class BookMapper {

    public static final String BOOK_ID = "BookID";
    public static final String USER_EMAIL = "UserEmail";
    public static final String TITLE = "Title";
    public static final String AUTHOR = "Author";
    public static final String IMAGE_URL = "ImageUrl";
    public static final String REQUESTED_AT = "RequestedAt";

    public Map<String, AttributeValue> mapToBookItem(final BookInfo bookInfo) {
        return bookGetters.entrySet()
                .stream()
                .filter(e -> isNotBlank(e.getValue().apply(bookInfo)))
                .collect(
                        toMap(
                                Map.Entry::getKey,
                                e -> stringAttribute(e.getValue().apply(bookInfo))
                        )
                );
    }

    public BookInfo mapToBook(final Map<String, AttributeValue> item) {
        final var builder = BookInfo.builder();
        bookSetters.entrySet()
                .stream()
                .filter(e -> item.containsKey(e.getKey()))
                .filter(e -> nonNull(item.get(e.getKey())))
                .forEach(e -> e.getValue().accept(builder, item.get(e.getKey()).s()));
        return builder.build();
    }

    public Map<String, AttributeValue> mapToUserItem(BookRequestContext request) {
        return userGetters.entrySet()
                .stream()
                .filter(e -> isNotBlank(e.getValue().apply(request)))
                .collect(
                        toMap(
                                Map.Entry::getKey,
                                e -> stringAttribute(e.getValue().apply(request))
                        )
                );
    }

    private AttributeValue stringAttribute(String value) {
        return AttributeValue.builder().s(value).build();
    }

    private final Map<String, Function<BookInfo, String>> bookGetters = ImmutableMap.of(
            BOOK_ID, BookInfo::getId,
            USER_EMAIL, BookInfo::getId,
            TITLE, BookInfo::getTitle,
            AUTHOR, BookInfo::getAuthor,
            IMAGE_URL, BookInfo::getImageUrl
    );

    private final Map<String, BiConsumer<BookInfo.BookInfoBuilder, String>> bookSetters = ImmutableMap.of(
            BOOK_ID, BookInfo.BookInfoBuilder::id,
            TITLE, BookInfo.BookInfoBuilder::title,
            AUTHOR, BookInfo.BookInfoBuilder::author,
            IMAGE_URL, BookInfo.BookInfoBuilder::imageUrl
    );

    private final Map<String, Function<BookRequestContext, String>> userGetters = ImmutableMap.of(
            BOOK_ID, BookRequestContext::getBookId,
            USER_EMAIL, BookRequestContext::getUserEmail,
            REQUESTED_AT, request -> request.getRequestedAt().format(ISO_OFFSET_DATE_TIME)
    );
}
