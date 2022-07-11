package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.serverless.template.ApiGatewayEventHandler;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static java.lang.String.format;
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

        sqs.sendMessage(
                SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(format("https://kazneb.kz/ru/bookView/view?brId=%s&simple=true", bookId))
                        .build()
        );

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
