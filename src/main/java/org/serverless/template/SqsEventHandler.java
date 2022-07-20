package org.serverless.template;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

public abstract class SqsEventHandler extends BaseHandler<SQSEvent.SQSMessage, Void, SQSEvent, Void>{

    @Override
    public Void handleRequest(final SQSEvent input, final Context context) {
        try {
            log(context, "Starting processing SQS event of size %d", input.getRecords().size());

            for (SQSEvent.SQSMessage message : input.getRecords()) {
                doHandleRequest(message, context);
            }

            log(context, "Completed processing SQS event of size %d", input.getRecords().size());
        } catch (Exception e) {
            log(context, "Error occurred while processing SQS event of size %d: %s", input.getRecords().size(), e.getMessage());
        }
        return null;
    }

    protected String getMessageAttributeOrDefault(final SQSEvent.SQSMessage input, final String attribute, final String defaultValue) {
        return input.getMessageAttributes().containsKey(attribute) ? input.getMessageAttributes().get(attribute).getStringValue() : defaultValue;
    }
}
