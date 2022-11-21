package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.serverless.template.ApiGatewayEventHandler;
import org.serverless.template.ClientException;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;

import static java.lang.String.format;
import static org.serverless.oqu.kerek.util.EnvironmentUtils.getBooksBucketName;

public class DownloadLinkGenerator extends ApiGatewayEventHandler<String, String> {

    static {
        initS3Client();
        initS3Presigner();
    }

    public DownloadLinkGenerator() {
        super(String.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            log(context, "Starting processing request %s with input: %s", input.getRequestContext().getRequestId(), input.getBody());

            final var request = getRequestData(input);
            final var response = doHandleRequest(request, context);

            log(context, "Completed processing request %s with output: %s", input.getRequestContext().getRequestId(), response);

            final var responseHeaders = new HashMap<>(headers);
            responseHeaders.putIfAbsent("Location", response);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(302)
                    .withHeaders(responseHeaders);
        } catch (ClientException e) {
            log(context, "Client exception occurred while processing request %s: %s", input.getRequestContext().getRequestId(), e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(e.getStatusCode())
                    .withHeaders(headers)
                    .withBody(e.getMessage());
        } catch (Exception e) {
            log(context, "Error occurred while processing request %s: %s", input.getRequestContext().getRequestId(), e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody("Error occurred while processing request: " + e.getMessage());
        }
    }

    protected String getRequestData(final APIGatewayProxyRequestEvent input) {
        return input.getPathParameters().get("bookId");
    }

    @Override
    protected String doHandleRequest(final String bookId, final Context context) {
        log(context, "Starting generate download link for book (ID = %s)", bookId);
        if (bookExists(getBooksBucketName(), bookId))
            return buildPresignedUrlToPdfFile(getBooksBucketName(), bookId).toString();
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
