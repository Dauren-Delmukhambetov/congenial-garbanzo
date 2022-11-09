package org.serverless.oqu.kerek.util;

import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;

import java.util.Map;

public class DynamoDbUtils {

    public static AttributeValue stringAttribute(String value) {
        return AttributeValue.builder().s(value).build();
    }

    public static AttributeValueUpdate updateAttribute(String newValue) {
        return AttributeValueUpdate.builder()
                .value(stringAttribute(newValue))
                .action(AttributeAction.PUT)
                .build();
    }

    public static Map<String, AttributeValue> primaryKey(String partitionKey, String sortKey, String pkValue) {
        return Map.of(
                partitionKey, stringAttribute(pkValue),
                sortKey, stringAttribute(pkValue)
        );
    }

}
