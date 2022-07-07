package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.serverless.template.ApiGatewayEventHandler;
import org.serverless.template.ClientException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static com.amazonaws.util.StringUtils.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Map.Entry.comparingByKey;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.getName;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.serverless.oqu.kerek.HtmlParseUtils.parseBookPagesPath;
import static org.serverless.oqu.kerek.URLUtils.extractQueryParamValue;

public class BookParser extends ApiGatewayEventHandler<BookParser.BookParsingRequest, BookParser.BookParsingResponse> {

    public BookParser() {
        super(BookParser.BookParsingRequest.class);
    }

    @Override
    protected BookParsingResponse doHandleRequest(final BookParsingRequest input, final Context context) {

        final var bookId = extractQueryParamValue(input.url, "brId");
        final var pages = parseBookPagesPath(input.url);

        if (pages.isEmpty()) throw new ClientException(404, "Pages URLs have not been found on the given URL");

        sendMessagesToSqs(pages, bookId);

        return new BookParsingResponse(pages.stream().limit(10).collect(toList()));
    }

    private void sendMessagesToSqs(final List<String> pages, final String bookId) {
        initSqsClient();
        final var queueUrl = System.getenv("QUEUE_NAME");
        final var pagesWithoutLastOne = pages.subList(0, pages.size() - 1);
        final var lastPage = pages.get(pages.size() - 1);
        final var chuckedPages = chuckList(pagesWithoutLastOne, SQS_BATCH_REQUEST_LIMIT);

        for (final var urls : chuckedPages) {
            final var entries = urls.stream()
                    .map(pageUrl -> buildSendMessageRequest(pageUrl, bookId, null))
                    .filter(Objects::nonNull)
                    .collect(toList());
            final var sqsBatchRequest = new SendMessageBatchRequest()
                    .withQueueUrl(queueUrl)
                    .withEntries(entries);
            sqs.sendMessageBatch(sqsBatchRequest);
        }

        sqs.sendMessageBatch(
                new SendMessageBatchRequest()
                        .withQueueUrl(queueUrl)
                        .withEntries(List.of(requireNonNull(buildSendMessageRequest(lastPage, bookId, "last"))))
        );
    }

    private SendMessageBatchRequestEntry buildSendMessageRequest(final String pageUrl, final String bookId, final String filename) {
        try {
            final var url = new URL("https://kazneb.kz" + pageUrl.replace("&amp;", "&"));
            final var filenameWithoutExtension = isNullOrEmpty(filename) ? removeExtension(getName(url.getPath())) : filename;
            final var extension = getExtension(url.getPath());
            final var attributes = Map.of(
                    "filepath", format("%s/%s.%s", bookId, filenameWithoutExtension, extension),
                    "content-type", format("image/%s", extension)
            );
            final var messageAttributes = attributes.entrySet()
                    .stream()
                    .collect(toMap(
                            Map.Entry::getKey,
                            e -> new MessageAttributeValue()
                                    .withDataType(String.class.getSimpleName())
                                    .withStringValue(e.getValue())
                    ));
            return new SendMessageBatchRequestEntry(randomUUID().toString(), url.toString())
                    .withMessageAttributes(messageAttributes);
        } catch (MalformedURLException e) {
            System.out.printf("Error occurred while trying to parse URL %s : %s%n", pageUrl, e.getMessage());
        }
        return null;
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
