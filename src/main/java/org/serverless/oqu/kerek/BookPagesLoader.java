package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.serverless.template.SqsEventHandler;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.io.IOUtils.toByteArray;
import static software.amazon.awssdk.core.sync.RequestBody.fromBytes;
import static software.amazon.awssdk.utils.StringUtils.isBlank;

public class BookPagesLoader extends SqsEventHandler {

    @Override
    protected Void doHandleRequest(final SQSEvent.SQSMessage input, final Context context) {
       try {
           log(context, "Starting processing SQS message (ID = %s)", input.getMessageId());
           initS3Client();
           final var bucketName = System.getenv("BUCKET_NAME");
           final var filepath = getMessageAttributeOrDefault(input, "filepath", "");
           final var contentType = getMessageAttributeOrDefault(input, "content-type", null);

           try (final var bookPageImage = new URL(input.getBody()).openStream()) {
               final var request = PutObjectRequest.builder()
                       .bucket(bucketName)
                       .key(filepath)
                       .metadata(buildObjectMetadata(contentType))
                       .build();

               s3Client.putObject(request, fromBytes(toByteArray(bookPageImage)));
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

    private Map<String, String> buildObjectMetadata(final String contentType) {
        final var metadata = new HashMap<String, String>();
        if (!isBlank(contentType)) metadata.put("Content-Type", contentType);
        return metadata;
    }
}
