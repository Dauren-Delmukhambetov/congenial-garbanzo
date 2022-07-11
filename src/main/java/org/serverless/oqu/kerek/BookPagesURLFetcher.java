package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.serverless.template.ClientException;
import org.serverless.template.SqsEventHandler;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

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
import static org.serverless.oqu.kerek.HtmlParseUtils.parseBookPagesUrls;
import static org.serverless.oqu.kerek.URLUtils.extractQueryParamValue;
import static software.amazon.awssdk.utils.StringUtils.isBlank;

public class BookPagesURLFetcher extends SqsEventHandler {

    @Override
    protected Void doHandleRequest(SQSEvent.SQSMessage input, Context context) {
        try {
            log(context, "Starting processing SQS message (ID = %s)", input.getMessageId());
            final var bookId = extractQueryParamValue(input.getBody(), "brId");
            final var pages = parseBookPagesUrls(input.getBody());

            if (pages.isEmpty()) throw new ClientException(404, "Pages URLs have not been found on the given URL");

            sendMessagesToSqs(pages, bookId);
            log(context, "Completed processing SQS message (ID = %s)", input.getMessageId());
        } catch (Exception e) {
            log(context, "Error occurred while processing request SQS message (ID = %s): %s", input.getMessageId(), e.getMessage());
        }
        return null;
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
            final var sqsBatchRequest = SendMessageBatchRequest.builder()
                    .queueUrl(queueUrl)
                    .entries(entries)
                    .build();
            sqs.sendMessageBatch(sqsBatchRequest);
        }

        sqs.sendMessageBatch(
                SendMessageBatchRequest.builder()
                        .queueUrl(queueUrl)
                        .entries(List.of(requireNonNull(buildSendMessageRequest(lastPage, bookId, "last"))))
                        .build()
        );
    }

    private SendMessageBatchRequestEntry buildSendMessageRequest(final String pageUrl, final String bookId, final String filename) {
        try {
            final var url = new URL("https://kazneb.kz" + pageUrl.replace("&amp;", "&"));
            final var filenameWithoutExtension = isBlank(filename) ? removeExtension(getName(url.getPath())) : filename;
            final var extension = getExtension(url.getPath());
            final var attributes = Map.of(
                    "filepath", format("%s/%s.%s", bookId, filenameWithoutExtension, extension),
                    "content-type", format("image/%s", extension)
            );
            final var messageAttributes = attributes.entrySet()
                    .stream()
                    .collect(toMap(
                            Map.Entry::getKey,
                            e -> MessageAttributeValue.builder()
                                    .dataType(String.class.getSimpleName())
                                    .stringValue(e.getValue())
                                    .build()
                    ));
            return SendMessageBatchRequestEntry.builder()
                    .id(randomUUID().toString())
                    .messageBody(url.toString())
                    .messageAttributes(messageAttributes)
                    .build();
        } catch (MalformedURLException e) {
            System.err.printf("Error occurred while trying to parse URL %s : %s%n", pageUrl, e.getMessage());
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
}
