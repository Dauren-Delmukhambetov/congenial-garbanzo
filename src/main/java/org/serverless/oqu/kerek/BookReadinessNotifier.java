package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.serverless.template.S3EventHandler;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.net.URL;
import java.time.Duration;

public class BookReadinessNotifier extends S3EventHandler {

    @Override
    protected Void doHandleRequest(S3EventNotification.S3EventNotificationRecord input, Context context) {
        try {
            log(context, "Starting processing S3 Event notification record (Object Key = %s)", input.getS3().getObject().getKey());
            initS3Presigner();
            initSesClient();

            sendEmail(buildPresignedUrlToPdfFile(input.getS3().getBucket().getName(), input.getS3().getObject().getKey()));

            log(context, "Completed processing S3 Event notification record (Object Key = %s)", input.getS3().getObject().getKey());
        } catch (Exception e) {
            log(context, "Error occurred while processing S3 Event notification record (Object Key = %s): %s", input.getS3().getObject().getKey(), e.getMessage());
        }
        return null;
    }

    private void sendEmail(final URL linkToFile) {

        final var content = Body.builder()
                .html(c -> c.data("<html> <head></head> <body> <a href=\"" + linkToFile.toExternalForm() + "\">Link to file</a> </body></html>"))
                .build();

        SendEmailRequest emailRequest = SendEmailRequest.builder()
                .destination(d -> d.toAddresses("dauren_del@mail.ru"))
                .message(m -> m.subject(s -> s.data("Your book is ready to download")).body(content))
                .source("dauren.del@gmail.com")
                .build();

        final var response = sesClient.sendEmail(emailRequest);

        System.out.printf("Email with ID (%s) has been send to user", response.messageId());
    }

    private URL buildPresignedUrlToPdfFile(final String bucketName, final String key) {
        final var getObjectPresignRequest =  GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(r -> r.bucket(bucketName).key(key))
                .build();

        final var presignedGetObjectRequest =
                s3Presigner.presignGetObject(getObjectPresignRequest);

        return presignedGetObjectRequest.url();
    }
}
