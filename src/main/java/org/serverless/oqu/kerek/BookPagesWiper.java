package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.serverless.template.S3EventHandler;

import static com.amazonaws.util.CollectionUtils.isNullOrEmpty;
import static java.util.stream.Collectors.toList;

public class BookPagesWiper extends S3EventHandler {

    @Override
    protected Void doHandleRequest(S3EventNotification.S3EventNotificationRecord input, Context context) {
        try {
            log(context, "Starting processing S3 Event notification record (Object Key = %s)", input.getS3().getObject().getKey());
            initS3Client();

            final var bucketName = System.getenv("BUCKET_NAME");
            final var directory = input.getS3().getObject().getKey().split("/")[0];

            final var imageKeys = s3Client.listObjectsV2(bucketName, directory)
                    .getObjectSummaries()
                    .stream()
                    .map(S3ObjectSummary::getKey)
                    .filter(key -> key.endsWith(".png"))
                    .map(DeleteObjectsRequest.KeyVersion::new)
                    .collect(toList());

            if (!isNullOrEmpty(imageKeys)) {
                s3Client.deleteObjects(new DeleteObjectsRequest(bucketName).withKeys(imageKeys));
            }

            log(context, "Completed processing S3 Event notification record (Object Key = %s)", input.getS3().getObject().getKey());
        } catch (Exception e) {
            log(context, "Error occurred while processing S3 Event notification record (Object Key = %s): %s", input.getS3().getObject().getKey(), e.getMessage());
        }
        return null;
    }
}
