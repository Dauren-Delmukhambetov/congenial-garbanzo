package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.serverless.oqu.kerek.model.BookInfo;
import org.serverless.oqu.kerek.model.BookRequestContext;
import org.serverless.template.ApiGatewayEventHandler;

import java.time.OffsetDateTime;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.serverless.oqu.kerek.util.EnvironmentUtils.getQueueName;
import static org.serverless.oqu.kerek.util.HtmlParseUtils.parseBookInfo;
import static org.serverless.oqu.kerek.util.URLUtils.extractQueryParamValue;

public class BookParser extends ApiGatewayEventHandler<BookParser.BookParsingRequest, BookInfo> {

    static {
        initSqsClient();
        initBookRepository();
    }

    public BookParser() {
        super(BookParser.BookParsingRequest.class);
    }

    @Override
    protected BookInfo doHandleRequest(final BookParsingRequest request, final Context context) {
        final var bookId = extractBookIdFromUrl(request);
        final var book = fetchBookInfo(bookId);
        saveBookInfoInDb(book);
        sendMessageToQueue(bookId);
        return book;
    }

    private String extractBookIdFromUrl(final BookParsingRequest request) {
        return extractQueryParamValue(request.url, "brId");
    }

    private void saveBookInfoInDb(final BookInfo bookInfo) {
        final var requestContext = BookRequestContext.builder()
                .userEmail(email())
                .requestedAt(OffsetDateTime.now())
                .build();
        bookRepository.save(bookInfo, requestContext);
    }

    private BookInfo fetchBookInfo(final String bookId) {
        final var bookShortInfo = requireNonNull(parseBookInfo(format("https://kazneb.kz/ru/catalogue/view/%s", bookId)));
        return BookInfo.builder()
                .id(bookId)
                .title(bookShortInfo.getTitle())
                .author(bookShortInfo.getAuthor())
                .imageUrl(bookShortInfo.getImageUrl())
                .build();
    }

    private void sendMessageToQueue(final String bookId) {
        final var bookUrl = format("https://kazneb.kz/ru/bookView/view?brId=%s&simple=true", bookId);
        sqs.sendMessage(m -> m.queueUrl(getQueueName()).messageBody(bookUrl).build());
    }

    @Override
    protected BookParsingRequest getRequestData(final APIGatewayProxyRequestEvent input) {
        return gson.fromJson(input.getBody(), inputType);
    }

    @Getter
    @RequiredArgsConstructor
    static class BookParsingRequest {
        private final String url;
    }
}
