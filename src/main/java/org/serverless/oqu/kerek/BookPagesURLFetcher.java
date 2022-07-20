package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.serverless.template.ClientException;
import org.serverless.template.SqsEventHandler;
import software.amazon.awssdk.services.s3.model.S3Object;
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
import static java.util.Objects.nonNull;
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
            final var bucketName = System.getenv("BOOKS_BUCKET_NAME");
            final var initiatorEmail = getMessageAttributeOrDefault(input, "initiator.email", null);
            final var initiatorName = getMessageAttributeOrDefault(input, "initiator.name", null);

            if (bookExists(bucketName, bookId)) {
                log(context, "Book with ID %s has already been loaded", bookId);
            }

            final var pages = parseBookPagesUrls(input.getBody());

            if (pages.isEmpty()) throw new ClientException(404, "Pages URLs have not been found on the given URL");

            sendMessagesToSqs(pages, bookId, initiatorEmail, initiatorName);
            log(context, "Completed processing SQS message (ID = %s)", input.getMessageId());
        } catch (Exception e) {
            log(context, "Error occurred while processing request SQS message (ID = %s): %s", input.getMessageId(), e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private void sendMessagesToSqs(final List<String> pages, final String bookId, final String email, final String name) {
        initSqsClient();
        final var queueUrl = System.getenv("QUEUE_NAME");
        final var pagesWithoutLastOne = pages.subList(0, pages.size() - 1);
        final var lastPage = pages.get(pages.size() - 1);
        final var chuckedPages = chuckList(pagesWithoutLastOne, SQS_BATCH_REQUEST_LIMIT);

        for (final var urls : chuckedPages) {
            final var entries = urls.stream()
                    .map(pageUrl -> buildSendMessageRequest(pageUrl, bookId))
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
                        .entries(List.of(requireNonNull(buildSendMessageRequest(lastPage, bookId, "last", email, name))))
                        .build()
        );
    }

    private SendMessageBatchRequestEntry buildSendMessageRequest(final String pageUrl, final String bookId) {
        return buildSendMessageRequest(pageUrl, bookId, null, null, null);
    }

    private SendMessageBatchRequestEntry buildSendMessageRequest(final String pageUrl, final String bookId, final String filename, final String email, final String name) {
        try {
            final var url = new URL("https://kazneb.kz" + pageUrl.replace("&amp;", "&"));
            final var filenameWithoutExtension = isBlank(filename) ? removeExtension(getName(url.getPath())) : filename;
            final var extension = getExtension(url.getPath());
            final var attributes = Map.of(
                    "filepath", format("%s/%s.%s", bookId, filenameWithoutExtension, extension),
                    "content-type", format("image/%s", extension),
                    "initiator.email", email,
                    "initiator.name", name
            );
            final var messageAttributes = attributes.entrySet()
                    .stream()
                    .filter(e -> nonNull(e.getValue()))
                    .collect(toMap(
                            Map.Entry::getKey,
                            e -> MessageAttributeValue.builder()
                                    .dataType(String.class.getSimpleName())
                                    .stringValue(e.getValue())
                                    .build()
                    ));
            return SendMessageBatchRequestEntry.builder()
                    .id(randomUUID().toString())
                    .messageGroupId(bookId)
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

    private boolean bookExists(final String bucketName, final String directory) {
        initS3Client();
        return s3Client.listObjectsV2(r -> r.bucket(bucketName).prefix(directory))
                .contents()
                .stream()
                .map(S3Object::key)
                .anyMatch(key -> key.endsWith(".pdf"));
    }

    private static final Integer SQS_BATCH_REQUEST_LIMIT = 10;
}
