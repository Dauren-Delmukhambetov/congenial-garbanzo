package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.serverless.template.ApiGatewayEventHandler;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;

import static java.lang.String.format;
import static java.util.Map.of;
import static java.util.stream.Collectors.toMap;
import static org.serverless.oqu.kerek.HtmlParseUtils.parseBookInfo;
import static org.serverless.oqu.kerek.URLUtils.extractQueryParamValue;

public class BookParser extends ApiGatewayEventHandler<BookParser.BookParsingRequest, BookParser.BookInfo> {

    public BookParser() {
        super(BookParser.BookParsingRequest.class);
        initSqsClient();
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

        return parseBookInfo(format("https://kazneb.kz/ru/catalogue/view/%s", bookId));
    }

    @Getter
    @RequiredArgsConstructor
    static class BookInfo {
        private final String title;
        private final String author;
        private final String imageUrl;
    }

    @Getter
    @RequiredArgsConstructor
    static class BookParsingRequest {
        private final String url;
    }
}
