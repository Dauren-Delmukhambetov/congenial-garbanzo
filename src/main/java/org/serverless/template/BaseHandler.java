package org.serverless.template;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sqs.SqsClient;

public abstract class BaseHandler<T, R, I, O> implements RequestHandler<I, O> {

    protected SqsClient sqs;
    protected S3Client s3Client;
    protected S3Presigner s3Presigner;
    protected SesClient sesClient;

    protected abstract R doHandleRequest(final T input, final Context context);

    protected void log(Context context, String message, Object... args) {
        context.getLogger().log(String.format(message, args));
    }

    protected void initS3Client() {
        if (s3Client != null) return;
        final var start = System.currentTimeMillis();
        s3Client = S3Client.builder()
                .region(getCurrentRegion())
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();
        System.out.printf("initS3Client took %d milliseconds to complete %n", System.currentTimeMillis() - start);
    }

    protected void initS3Presigner() {
        if (s3Presigner != null) return;
        final var start = System.currentTimeMillis();
        s3Presigner = S3Presigner.builder()
                .region(getCurrentRegion())
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();
        System.out.printf("initS3Client took %d milliseconds to complete %n", System.currentTimeMillis() - start);
    }

    protected void initSqsClient() {
        if (sqs != null) return;
        final var start = System.currentTimeMillis();
        sqs = SqsClient.builder()
                .region(getCurrentRegion())
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();
        System.out.printf("initS3Client took %d milliseconds to complete %n", System.currentTimeMillis() - start);
    }

    protected void initSesClient() {
        if (sesClient != null) return;
        final var start = System.currentTimeMillis();
        sesClient = SesClient.builder()
                .region(getCurrentRegion())
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();
        System.out.printf("initS3Client took %d milliseconds to complete %n", System.currentTimeMillis() - start);
    }

    protected Region getCurrentRegion() {
        return Region.of(System.getenv("AWS_REGION"));
    }
}
