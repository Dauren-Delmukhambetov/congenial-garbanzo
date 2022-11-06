package org.serverless.oqu.kerek.util;

import software.amazon.awssdk.regions.Region;

public class EnvironmentUtils {

    public static Region getRegion() {
        return Region.of(System.getenv("AWS_REGION"));
    }

    public static String getTableName() {
        return System.getenv("TABLE_NAME");
    }

    public static String getBooksBucketName() {
        return System.getenv("BOOKS_BUCKET_NAME");
    }

    public static String getQueueName() {
        return System.getenv("QUEUE_NAME");
    }
}
