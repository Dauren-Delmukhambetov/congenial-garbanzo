package org.serverless.oqu.kerek.repo;

import lombok.RequiredArgsConstructor;
import org.serverless.oqu.kerek.model.BookInfo;
import org.serverless.oqu.kerek.model.BookRequestContext;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.stream.Collectors.toList;
import static org.serverless.oqu.kerek.util.EnvironmentUtils.getTableIndexName;
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

    public List<BookInfo> findByBookIds(final Collection<String> bookIds) {
        final var keysAndAttributes = KeysAndAttributes.builder()
                .keys(bookIds.stream()
                        .map(id -> Map.of("BookID", stringAttribute(id), "UserEmail", stringAttribute(id)))
                        .collect(toList())
                )
                .build();
        final var requestItems = Map.of(getTableName(), keysAndAttributes);
        final var request = BatchGetItemRequest.builder()
                .requestItems(requestItems)
                .build();

        return dynamoDbClient.batchGetItem(request)
                .responses()
                .get(getTableName())
                .stream()
                .map(attrs -> BookInfo.builder()
                        .id(attrs.get("BookID").s())
                        .title(attrs.get("Title").s())
                        .author(attrs.get("Author").s())
                        .imageUrl(attrs.get("ImageUrl").s())
                        .build())
                .collect(toList());
    }

    public List<String> findBookIdsByUserEmail(final String userEmail) {
        final var queryRequest = QueryRequest.builder()
                .tableName(getTableName())
                .indexName(getTableIndexName())
                .keyConditionExpression("userEmail = :email")
                .expressionAttributeNames(Map.of("userEmail", "UserEmail"))
                .expressionAttributeValues(Map.of("email", stringAttribute(userEmail)))
                .build();

        return dynamoDbClient.query(queryRequest)
                .items()
                .stream()
                .filter(item -> item.containsKey("BookID"))
                .map(item -> item.get("BookID"))
                .map(AttributeValue::s)
                .distinct()
                .collect(toList());
    }
}
