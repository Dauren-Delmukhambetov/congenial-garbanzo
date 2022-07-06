package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.apache.commons.io.FilenameUtils;
import org.serverless.template.SqsEventHandler;

import java.net.URL;

import static java.lang.String.format;

public class BookPageLoader extends SqsEventHandler {

    @Override
    protected Void doHandleRequest(final SQSEvent.SQSMessage input, final Context context) {
       try {
           log(context, "Starting processing SQS message (ID = %s)", input.getMessageId());
           initS3Client();
           final var bucketName = System.getenv("BUCKET_NAME");
           final var pageUrl = new URL("https://kazneb.kz" + input.getBody().replace("&amp;", "&"));
           log(context, "Page URL for downloading is %s", pageUrl.getPath());
           final var contentType = FilenameUtils.getExtension(pageUrl.getPath());
           final var bookId = input.getAttributes().getOrDefault("book-id", "unknown");
           final var filepath = format("%s/%s", bookId, FilenameUtils.getName(pageUrl.getPath()));
           final var metadata = new ObjectMetadata();
           metadata.addUserMetadata("Content-Type", contentType);

           try (final var bookPageImage = pageUrl.openStream()) {
               s3Client.putObject(bucketName, filepath, bookPageImage, metadata);
           }
           log(context, "Completed processing SQS message (ID = %s)", input.getMessageId());
       } catch (Exception e) {
           log(context, "Error occurred while processing request SQS message (ID = %s): %s", input.getMessageId(), e.getMessage());
       }
        return null;
    }
}
