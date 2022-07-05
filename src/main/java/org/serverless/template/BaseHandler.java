package org.serverless.template;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

public abstract class BaseHandler<T, R, I, O> implements RequestHandler<I, O> {

    protected AmazonSQS sqs;

    protected abstract R doHandleRequest(final T input);

    protected void log(Context context, String message, Object... args) {
        context.getLogger().log(String.format(message, args));
    }

    protected void initSqsClient() {
        if (sqs != null) return;
        sqs = AmazonSQSClientBuilder.standard()
                .withRegion(Regions.fromName(System.getenv("AWS_REGION")))
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }
}
