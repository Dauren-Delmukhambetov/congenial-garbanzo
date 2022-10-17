package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.serverless.oqu.kerek.model.BookInfo;
import org.serverless.template.ApiGatewayEventHandler;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;

import static java.lang.String.format;
import static java.util.Map.of;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.serverless.oqu.kerek.util.HtmlParseUtils.parseBookInfo;
import static org.serverless.oqu.kerek.util.URLUtils.extractQueryParamValue;

public class BookParser extends ApiGatewayEventHandler<BookParser.BookParsingRequest, BookInfo> {

    static {
        initSqsClient();
    }

    public BookParser() {
        super(BookParser.BookParsingRequest.class);
    }

    @Override
    protected BookInfo doHandleRequest(final BookParsingRequest input, final Context context) {
        final var bookId = extractQueryParamValue(input.url, "brId");
        final var queueUrl = System.getenv("QUEUE_NAME");
        final var bookUrl = format("https://kazneb.kz/ru/bookView/view?brId=%s&simple=true", bookId);
        final var metadata = of("initiator.email", email(), "initiator.name", name())
                .entrySet()
                .stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> MessageAttributeValue.builder()
                                .dataType(String.class.getSimpleName())
                                .stringValue(e.getValue())
                                .build()
                ));

        sqs.sendMessage(m -> m.queueUrl(queueUrl).messageBody(bookUrl).messageAttributes(metadata).build());

        final var bookShortInfo = requireNonNull(parseBookInfo(format("https://kazneb.kz/ru/catalogue/view/%s", bookId)));
        return BookInfo.builder()
                .id(bookId)
                .title(bookShortInfo.getTitle())
                .author(bookShortInfo.getAuthor())
                .imageUrl(bookShortInfo.getImageUrl())
                .build();
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
