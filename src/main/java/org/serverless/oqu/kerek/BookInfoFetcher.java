package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.serverless.oqu.kerek.model.BookInfo;
import org.serverless.template.ApiGatewayEventHandler;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.serverless.oqu.kerek.util.HtmlParseUtils.parseBookInfo;

public class BookInfoFetcher extends ApiGatewayEventHandler<String, BookInfo> {

    static {
        initS3Client();
        initS3Presigner();
    }

    public BookInfoFetcher() {
        super(String.class);
    }

    @Override
    protected String getRequestData(final APIGatewayProxyRequestEvent input) {
        return input.getPathParameters().get("bookId");
    }

    @Override
    protected BookInfo doHandleRequest(final String input, final Context context) {
        try {
            log(context, "Starting fetch info for book (ID = %s)", input);
            // final var bucketName = getBooksBucketName();
            // final var url = Optional.ofNullable(bookExists(bucketName, input) ? buildPresignedUrlToPdfFile(bucketName, input) : null);
            final var bookShortInfo = requireNonNull(parseBookInfo(format("https://kazneb.kz/ru/catalogue/view/%s", input)));

            return BookInfo.builder()
                    .id(input)
                    .title(bookShortInfo.getTitle())
                    .author(bookShortInfo.getAuthor())
                    .build();
        } catch (Exception e) {
            log(context, "Error occurred while fetching info for book (Object Key = %s): %s", input, e.getMessage());
        }
        return null;
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
