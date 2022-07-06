package org.serverless.template;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

public abstract class BaseHandler<T, R, I, O> implements RequestHandler<I, O> {

    protected AmazonSQS sqs;
    protected AmazonS3 s3Client;

    protected abstract R doHandleRequest(final T input, final Context context);

    protected void log(Context context, String message, Object... args) {
        context.getLogger().log(String.format(message, args));
    }

    protected void initS3Client() {
        if (s3Client != null) return;
        s3Client = AmazonS3ClientBuilder.defaultClient();
    }

    protected void initSqsClient() {
        if (sqs != null) return;
        sqs = AmazonSQSClientBuilder.standard()
                .withRegion(Regions.fromName(System.getenv("AWS_REGION")))
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }
}
