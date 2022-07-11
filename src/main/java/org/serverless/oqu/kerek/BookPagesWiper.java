package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.serverless.template.S3EventHandler;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;

import static java.util.stream.Collectors.toList;
import static software.amazon.awssdk.utils.CollectionUtils.isNullOrEmpty;

public class BookPagesWiper extends S3EventHandler {

    @Override
    protected Void doHandleRequest(S3EventNotification.S3EventNotificationRecord input, Context context) {
        try {
            log(context, "Starting processing S3 Event notification record (Object Key = %s)", input.getS3().getObject().getKey());
            initS3Client();

            final var bucketName = System.getenv("BUCKET_NAME");
            final var directory = input.getS3().getObject().getKey().split("/")[0];
            final var listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(directory)
                    .build();

            final var imageKeys = s3Client.listObjectsV2(listRequest)
                    .contents()
                    .stream()
                    .map(S3Object::key)
                    .filter(key -> key.endsWith(".png"))
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .collect(toList());
            final var deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(d -> d.objects(imageKeys))
                    .build();

            if (!isNullOrEmpty(imageKeys)) {
                s3Client.deleteObjects(deleteRequest);
            }

            log(context, "Completed processing S3 Event notification record (Object Key = %s)", input.getS3().getObject().getKey());
        } catch (Exception e) {
            log(context, "Error occurred while processing S3 Event notification record (Object Key = %s): %s", input.getS3().getObject().getKey(), e.getMessage());
        }
        return null;
    }
}
