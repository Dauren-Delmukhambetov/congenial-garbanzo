package org.serverless.template;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

public abstract class BaseHandler<T, R, I, O> implements RequestHandler<I, O> {

    protected SqsClient sqs;
    protected S3Client s3Client;

    protected abstract R doHandleRequest(final T input, final Context context);

    protected void log(Context context, String message, Object... args) {
        context.getLogger().log(String.format(message, args));
    }

    protected void initS3Client() {
        if (s3Client != null) return;
        s3Client = S3Client.builder()
                .region(Region.of(System.getenv("AWS_REGION")))
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();
    }

    protected void initSqsClient() {
        if (sqs != null) return;
        sqs = SqsClient.builder()
                .region(Region.of(System.getenv("AWS_REGION")))
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();
    }
}
