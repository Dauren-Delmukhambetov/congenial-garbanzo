package org.serverless.template;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sqs.SqsClient;

public abstract class BaseHandler<T, R, I, O> implements RequestHandler<I, O> {

    protected static SqsClient sqs;
    protected static S3Client s3Client;
    protected static S3Presigner s3Presigner;
    protected static SesClient sesClient;

    protected abstract R doHandleRequest(final T input, final Context context);

    protected void log(Context context, String message, Object... args) {
        context.getLogger().log(String.format(message, args));
    }

    protected static void initS3Client() {
        if (s3Client != null) return;
        final var start = System.currentTimeMillis();
        s3Client = S3Client.builder()
                .region(getRegion())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
        System.out.printf("initS3Client took %d milliseconds to complete %n", System.currentTimeMillis() - start);
    }

    protected static void initS3Presigner() {
        if (s3Presigner != null) return;
        final var start = System.currentTimeMillis();
        s3Presigner = S3Presigner.builder()
                .region(getRegion())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
        System.out.printf("initS3Presigner took %d milliseconds to complete %n", System.currentTimeMillis() - start);
    }

    protected static void initSqsClient() {
        if (sqs != null) return;
        final var start = System.currentTimeMillis();
        sqs = SqsClient.builder()
                .region(getRegion())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
        System.out.printf("initSqsClient took %d milliseconds to complete %n", System.currentTimeMillis() - start);
    }

    protected static void initSesClient() {
        if (sesClient != null) return;
        final var start = System.currentTimeMillis();
        sesClient = SesClient.builder()
                .region(getRegion())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
        System.out.printf("initSesClient took %d milliseconds to complete %n", System.currentTimeMillis() - start);
    }

    protected static Region getRegion() {
        return Region.of(System.getenv("AWS_REGION"));
    }
}
