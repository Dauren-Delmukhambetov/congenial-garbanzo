package org.serverless.oqu.kerek.repo;

import lombok.RequiredArgsConstructor;
import org.serverless.oqu.kerek.model.BookInfo;
import org.serverless.oqu.kerek.model.BookRequestContext;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.serverless.oqu.kerek.repo.BookMapper.BOOK_ID;
import static org.serverless.oqu.kerek.repo.BookMapper.USER_EMAIL;
import static org.serverless.oqu.kerek.util.EnvironmentUtils.getTableIndexName;
import static org.serverless.oqu.kerek.util.EnvironmentUtils.getTableName;

@RequiredArgsConstructor
public class BookRepository {
    private final DynamoDbClient dynamoDbClient;
    private final BookMapper mapper;

    public void save(BookInfo book, BookRequestContext context) {
        final var putBookRequest = WriteRequest.builder()
                .putRequest(pr -> pr.item(mapper.mapToBookItem(book)))
                .build();
        final var putUserRequest = WriteRequest.builder()
                .putRequest(pr -> pr.item(mapper.mapToUserItem(context)))
                .build();
        final var requestItems = Map.of(getTableName(), List.of(putBookRequest, putUserRequest));

        dynamoDbClient.batchWriteItem(br -> br.requestItems(requestItems));
    }

    private AttributeValue stringAttribute(String value) {
        return AttributeValue.builder().s(value).build();
    }

    public List<BookInfo> findByBookIds(final Collection<String> bookIds) {
        final var keysAndAttributes = KeysAndAttributes.builder()
                .keys(bookIds.stream()
                        .map(id -> Map.of(BOOK_ID, stringAttribute(id), USER_EMAIL, stringAttribute(id)))
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
                .map(mapper::mapToBook)
                .collect(toList());
    }

    public List<String> findBookIdsByUserEmail(final String userEmail) {
        final var queryRequest = QueryRequest.builder()
                .tableName(getTableName())
                .indexName(getTableIndexName())
                .keyConditionExpression("#userEmail = :email")
                .expressionAttributeNames(Map.of("#userEmail", USER_EMAIL))
                .expressionAttributeValues(Map.of(":email", stringAttribute(userEmail)))
                .scanIndexForward(false)
                .limit(5)
                .build();

        return dynamoDbClient.query(queryRequest)
                .items()
                .stream()
                .filter(item -> item.containsKey(BOOK_ID))
                .map(item -> item.get(BOOK_ID))
                .map(AttributeValue::s)
                .distinct()
                .collect(toList());
    }
}
