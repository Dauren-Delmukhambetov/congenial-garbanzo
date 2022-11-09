package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.serverless.template.S3EventHandler;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;

import static java.util.stream.Collectors.toList;
import static org.serverless.oqu.kerek.util.EnvironmentUtils.getBooksBucketName;
import static software.amazon.awssdk.utils.CollectionUtils.isNullOrEmpty;

public class BookPagesWiper extends S3EventHandler {

    static {
        initS3Client();
        initBookRepository();
    }

    @Override
    protected Void doHandleRequest(S3EventNotification.S3EventNotificationRecord input, Context context) {
        try {
            log(context, "Starting processing S3 Event notification record (Object Key = %s)", input.getS3().getObject().getKey());

            final var bucketName = getBooksBucketName();
            final var bookId = input.getS3().getObject().getKey().split("/")[0];

            bookRepository.updateBookStatus(bookId, "Ready");

            final var imageKeys = s3Client.listObjectsV2(b -> b.bucket(bucketName).prefix(bookId))
                    .contents()
                    .stream()
                    .map(S3Object::key)
                    .filter(key -> key.endsWith(".png"))
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .collect(toList());

            if (!isNullOrEmpty(imageKeys)) {
                s3Client.deleteObjects(b -> b.bucket(bucketName).delete(d -> d.objects(imageKeys)));
            }

            log(context, "Completed processing S3 Event notification record (Object Key = %s)", input.getS3().getObject().getKey());
        } catch (Exception e) {
            log(context, "Error occurred while processing S3 Event notification record (Object Key = %s): %s", input.getS3().getObject().getKey(), e.getMessage());
        }
        return null;
    }
}
