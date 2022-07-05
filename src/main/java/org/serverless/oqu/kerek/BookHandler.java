package org.serverless.oqu.kerek;

import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.serverless.template.ApiGatewayEventHandler;
import org.serverless.template.ClientException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Map.Entry.comparingByKey;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.serverless.oqu.kerek.HtmlParseUtils.parseBookPagesPath;

public class BookHandler extends ApiGatewayEventHandler<BookHandler.BookParsingRequest, BookHandler.BookParsingResponse> {

    public BookHandler() {
        super(BookHandler.BookParsingRequest.class);
    }

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

        chuckList(pages, SQS_BATCH_REQUEST_LIMIT)
                .forEach(urls -> {
                            final var entries = urls.stream()
                                    .map(pageUrl -> new SendMessageBatchRequestEntry(randomUUID().toString(), pageUrl))
                                    .collect(toList());
                            final var sqsBatchRequest = new SendMessageBatchRequest()
                                    .withQueueUrl(queueUrl)
                                    .withEntries(entries);
                            sqs.sendMessageBatch(sqsBatchRequest);
                        }
                );
    }

    private List<List<String>> chuckList(final List<String> pages, final Integer chunkSize) {
        AtomicInteger index = new AtomicInteger(0);
        return pages.stream().collect(groupingBy(x -> index.getAndIncrement() / chunkSize))
                .entrySet().stream()
                .sorted(comparingByKey())
                .map(Map.Entry::getValue)
                .collect(toList());
    }

    private static final Integer SQS_BATCH_REQUEST_LIMIT = 10;

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
