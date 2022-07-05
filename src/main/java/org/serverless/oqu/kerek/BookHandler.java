package org.serverless.oqu.kerek;

import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.serverless.template.ApiGatewayEventHandler;
import org.serverless.template.ClientException;

import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.serverless.oqu.kerek.HtmlParseUtils.parseBookPagesPath;

public class BookHandler extends ApiGatewayEventHandler<BookHandler.BookParsingRequest, BookHandler.BookParsingResponse> {

    public BookHandler() { super(BookHandler.BookParsingRequest.class);}

    @Override
    protected BookParsingResponse doHandleRequest(BookParsingRequest input) {

        final var pages = parseBookPagesPath(input.url);

        if (pages.isEmpty()) throw new ClientException(404, "Pages URLs have not been found on the given URL");

        sendMessagesToSqs(pages);

        return new BookParsingResponse(pages.stream().limit(10).collect(toList()));
    }

    private void sendMessagesToSqs(final List<String> pages) {
        initSqsClient();
        final var queueUrl = System.getenv("QUEUE_NAME");
        final var entries = pages.stream()
                .limit(25)
                .map(pageUrl -> new SendMessageBatchRequestEntry(UUID.randomUUID().toString(), pageUrl))
                .collect(toList());
        SendMessageBatchRequest sqsBatchRequest = new SendMessageBatchRequest()
                .withQueueUrl(queueUrl)
                .withEntries(entries);
        sqs.sendMessageBatch(sqsBatchRequest);
    }

    @Getter
    @RequiredArgsConstructor
    static class BookParsingResponse {
        private final List<String> urls;
    }

    @Getter
    @RequiredArgsConstructor
    static class BookParsingRequest {
        private final String url;
    }
}
