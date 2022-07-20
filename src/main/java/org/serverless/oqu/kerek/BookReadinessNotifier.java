package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.serverless.template.S3EventHandler;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.net.URL;
import java.time.Duration;

import static java.lang.String.format;
import static org.serverless.oqu.kerek.Constants.S3_OBJECT_INITIATOR_EMAIL_ATTR;

public class BookReadinessNotifier extends S3EventHandler {

    static {
        initS3Client();
        initS3Presigner();
        initSesClient();
    }

    @Override
    protected Void doHandleRequest(S3EventNotification.S3EventNotificationRecord input, Context context) {
        try {
            log(context, "Starting processing S3 Event notification record (Object Key = %s)", input.getS3().getObject().getKey());

            final var bucketName = input.getS3().getBucket().getName();
            final var directory = input.getS3().getObject().getKey().split("/")[0];
            final var email = acquireInitiatorEmail(bucketName, directory);
            final var url = buildPresignedUrlToPdfFile(bucketName, directory);
            sendEmail(url, email);

            log(context, "Completed processing S3 Event notification record (Object Key = %s)", input.getS3().getObject().getKey());
        } catch (Exception e) {
            log(context, "Error occurred while processing S3 Event notification record (Object Key = %s): %s", input.getS3().getObject().getKey(), e.getMessage());
        }
        return null;
    }

    private void sendEmail(final URL linkToFile, final String email) {

        final var content = Body.builder()
                .html(c -> c.data("<html> <head></head> <body> <a href=\"" + linkToFile.toExternalForm() + "\">Link to file</a> </body></html>"))
                .build();

        SendEmailRequest emailRequest = SendEmailRequest.builder()
                .destination(d -> d.toAddresses(email))
                .message(m -> m.subject(s -> s.data("Your book is ready to download")).body(content))
                .source("dauren_del@mail.ru")
                .build();

        final var response = sesClient.sendEmail(emailRequest);

        System.out.printf("Email with ID (%s) has been send to user", response.messageId());
    }

    private URL buildPresignedUrlToPdfFile(final String bucketName, final String directory) {
        final var getObjectPresignRequest =  GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofDays(3))
                .getObjectRequest(r -> r.bucket(bucketName).key(format("%s/book.pdf", directory)))
                .build();

        final var presignedGetObjectRequest =
                s3Presigner.presignGetObject(getObjectPresignRequest);

        return presignedGetObjectRequest.url();
    }

    private String acquireInitiatorEmail(final String bucketName, final String directory) {
        final var headObject = s3Client.headObject(h -> h.bucket(bucketName).key(format("%s/book.pdf", directory)).build());
        return headObject.metadata().get(S3_OBJECT_INITIATOR_EMAIL_ATTR);
    }
}
