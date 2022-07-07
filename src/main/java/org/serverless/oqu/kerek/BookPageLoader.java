package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.serverless.template.SqsEventHandler;

import java.net.URL;

import static com.amazonaws.util.StringUtils.isNullOrEmpty;

public class BookPageLoader extends SqsEventHandler {

    @Override
    protected Void doHandleRequest(final SQSEvent.SQSMessage input, final Context context) {
       try {
           log(context, "Starting processing SQS message (ID = %s)", input.getMessageId());
           initS3Client();
           final var bucketName = System.getenv("BUCKET_NAME");
           final var filepath = getMessageAttributeOrDefault(input, "filepath", "");
           final var contentType = getMessageAttributeOrDefault(input, "content-type", null);

           try (final var bookPageImage = new URL(input.getBody()).openStream()) {
               s3Client.putObject(bucketName, filepath, bookPageImage, buildObjectMetadata(contentType));
           }
           log(context, "Completed processing SQS message (ID = %s)", input.getMessageId());
       } catch (Exception e) {
           log(context, "Error occurred while processing request SQS message (ID = %s): %s", input.getMessageId(), e.getMessage());
       }
        return null;
    }

    private String getMessageAttributeOrDefault(final SQSEvent.SQSMessage input, final String attribute, final String defaultValue) {
        return input.getMessageAttributes().containsKey(attribute) ? input.getMessageAttributes().get(attribute).getStringValue() : defaultValue;
    }

    private ObjectMetadata buildObjectMetadata(final String contentType) {
        final var metadata = new ObjectMetadata();
        if (!isNullOrEmpty(contentType)) metadata.setContentType(contentType);
        return metadata;
    }
}
