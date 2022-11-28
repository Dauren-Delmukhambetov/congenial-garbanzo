package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.serverless.oqu.kerek.model.BookDownloadLink;
import org.serverless.template.ApiGatewayEventHandler;
import org.serverless.template.ClientException;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

import static java.lang.String.format;
import static org.serverless.oqu.kerek.util.EnvironmentUtils.getBooksBucketName;

public class DownloadLinkGenerator extends ApiGatewayEventHandler<String, BookDownloadLink> {

    static {
        initS3Client();
        initS3Presigner();
    }

    public DownloadLinkGenerator() {
        super(String.class);
    }

    protected String getRequestData(final APIGatewayProxyRequestEvent input) {
        return input.getPathParameters().get("bookId");
    }

    @Override
    protected BookDownloadLink doHandleRequest(final String bookId, final Context context) {
        log(context, "Starting generate download link for book (ID = %s)", bookId);
        if (bookExists(getBooksBucketName(), bookId))
            return BookDownloadLink.of(buildPresignedUrlToPdfFile(getBooksBucketName(), bookId));
        else throw new ClientException(404, format("Book with ID %s has not found", bookId));
    }

    private boolean bookExists(final String bucketName, final String directory) {
        return s3Client.listObjectsV2(r -> r.bucket(bucketName).prefix(directory))
                .contents()
                .stream()
                .map(S3Object::key)
                .anyMatch(key -> key.endsWith(".pdf"));
    }

    private URL buildPresignedUrlToPdfFile(final String bucketName, final String directory) {
        final var getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofDays(3))
                .getObjectRequest(r -> r.bucket(bucketName).key(format("%s/book.pdf", directory)))
                .build();

        final var presignedGetObjectRequest =
                s3Presigner.presignGetObject(getObjectPresignRequest);

        return presignedGetObjectRequest.url();
    }
}
