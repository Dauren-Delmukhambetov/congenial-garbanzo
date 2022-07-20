package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.serverless.template.SqsEventHandler;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.io.IOUtils.toByteArray;
import static org.serverless.oqu.kerek.Constants.S3_OBJECT_INITIATOR_EMAIL_ATTR;
import static org.serverless.oqu.kerek.Constants.S3_OBJECT_INITIATOR_NAME_ATTR;
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
           final var initiatorEmail = getMessageAttributeOrDefault(input, "initiator.email", null);
           final var initiatorName = getMessageAttributeOrDefault(input, "initiator.name", null);

           try (final var bookPageImage = new URL(input.getBody()).openStream()) {
               final var request = PutObjectRequest.builder()
                       .bucket(bucketName)
                       .key(filepath)
                       .metadata(buildObjectMetadata(contentType, initiatorEmail, initiatorName))
                       .build();

               s3Client.putObject(request, fromBytes(toByteArray(bookPageImage)));
           }
           log(context, "Completed processing SQS message (ID = %s)", input.getMessageId());
       } catch (Exception e) {
           log(context, "Error occurred while processing request SQS message (ID = %s): %s", input.getMessageId(), e.getMessage());
       }
        return null;
    }

    private Map<String, String> buildObjectMetadata(final String contentType, final String email, final String name) {
        final var metadata = new HashMap<String, String>();
        if (!isBlank(contentType)) metadata.put("Content-Type", contentType);
        if (!isBlank(email)) metadata.put(S3_OBJECT_INITIATOR_EMAIL_ATTR, email);
        if (!isBlank(name)) metadata.put(S3_OBJECT_INITIATOR_NAME_ATTR, name);
        return metadata;
    }
}
