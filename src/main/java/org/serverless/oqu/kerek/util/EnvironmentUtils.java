package org.serverless.oqu.kerek.util;

import software.amazon.awssdk.regions.Region;

public class EnvironmentUtils {

    public static Region getRegion() {
        return Region.of(System.getenv("AWS_REGION"));
    }

    public static String getTableName() {
        return System.getenv("TABLE_NAME");
    }
}
