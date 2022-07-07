package org.serverless.template;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;

public abstract class S3EventHandler extends BaseHandler<S3EventNotification.S3EventNotificationRecord, Void, S3Event, Void>{

    @Override
    public Void handleRequest(S3Event input, Context context) {
        try {
            log(context, "Starting processing S3 event of size %d", input.getRecords().size());

            for (final var record : input.getRecords()) {
                doHandleRequest(record, context);
            }

            log(context, "Completed processing S3 event of size %d", input.getRecords().size());
        } catch (Exception e) {
            log(context, "Error occurred while processing S3 event of size %d: %s", input.getRecords().size(), e.getMessage());
        }
        return null;
    }
}
