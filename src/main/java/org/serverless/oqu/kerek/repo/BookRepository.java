package org.serverless.oqu.kerek.repo;

import lombok.RequiredArgsConstructor;
import org.serverless.oqu.kerek.model.BookInfo;
import org.serverless.oqu.kerek.model.BookRequestContext;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import java.util.List;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.serverless.oqu.kerek.util.EnvironmentUtils.getTableName;

@RequiredArgsConstructor
public class BookRepository {
    private final DynamoDbClient dynamoDbClient;

    public void save(BookInfo book, BookRequestContext context) {
        final var putBookRequest = WriteRequest.builder()
                .putRequest(pr -> pr.item(buildBookItem(book)))
                .build();
        final var putUserRequest = WriteRequest.builder()
                .putRequest(pr -> pr.item(buildUserItem(book, context)))
                .build();
        final var requestItems = Map.of(getTableName(), List.of(putBookRequest, putUserRequest));

        dynamoDbClient.batchWriteItem(br -> br.requestItems(requestItems));
    }

    private Map<String, AttributeValue> buildBookItem(BookInfo bookInfo) {
        return Map.of(
                "BookID", stringAttribute(bookInfo.getId()),
                "UserEmail", stringAttribute(bookInfo.getId()),
                "Title", stringAttribute(bookInfo.getTitle()),
                "Author", stringAttribute(bookInfo.getAuthor()),
                "ImageUrl", stringAttribute(bookInfo.getImageUrl())
        );
    }

    private Map<String, AttributeValue> buildUserItem(BookInfo bookInfo, BookRequestContext context) {
        return Map.of(
                "BookID", stringAttribute(bookInfo.getId()),
                "UserEmail", stringAttribute(context.getUserEmail()),
                "RequestedAt", stringAttribute(context.getRequestedAt().format(ISO_OFFSET_DATE_TIME))
        );
    }

    private AttributeValue stringAttribute(String value) {
        return AttributeValue.builder().s(value).build();
    }
}
