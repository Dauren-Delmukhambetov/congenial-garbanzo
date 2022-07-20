package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import org.serverless.template.S3EventHandler;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import static com.itextpdf.io.image.ImageDataFactory.create;
import static java.lang.String.format;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Paths.get;
import static java.util.Map.of;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.serverless.oqu.kerek.Constants.S3_OBJECT_INITIATOR_EMAIL_ATTR;
import static org.serverless.oqu.kerek.Constants.S3_OBJECT_INITIATOR_NAME_ATTR;
import static software.amazon.awssdk.core.sync.RequestBody.fromBytes;

public class BookPagesAssembler extends S3EventHandler {

    @Override
    protected Void doHandleRequest(S3EventNotification.S3EventNotificationRecord input, Context context) {
        try {
            log(context, "Starting processing S3 Event notification record (Object Key = %s)", input.getS3().getObject().getKey());
            initS3Client();

            final var bucketName = System.getenv("BUCKET_NAME");
            final var directory = input.getS3().getObject().getKey().split("/")[0];
            final var tempFile = createTempFile(LAMBDA_TMP_DIR, "book-", ".pdf");

            assembleBookPages(bucketName, directory, tempFile);
            uploadPdfFileToS3(bucketName, directory, tempFile);

            log(context, "Completed processing S3 Event notification record (Object Key = %s)", input.getS3().getObject().getKey());
        } catch (Exception e) {
            log(context, "Error occurred while processing S3 Event notification record (Object Key = %s): %s", input.getS3().getObject().getKey(), e.getMessage());
        }
        return null;
    }

    private void assembleBookPages(final String bucketName, final String directory, final Path tempFile) throws FileNotFoundException {
        final var listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(directory)
                .build();

        final var objectKeys = s3Client.listObjectsV2(listRequest)
                .contents()
                .stream()
                .map(S3Object::key)
                .collect(toList());
        try (
                final var pdfDocument = new PdfDocument(new PdfWriter(tempFile.toFile()));
                final var document = new Document(pdfDocument)
        ) {
            for (int i = 0; i < objectKeys.size(); i++) {
                final var pageImage = new Image(create(readObject(bucketName, objectKeys.get(i))));

                pdfDocument.addNewPage(new PageSize(pageImage.getImageWidth(), pageImage.getImageHeight()));
                pageImage.setFixedPosition(i + 1, 0, 0);
                document.add(pageImage);
            }
        }
    }

    private void uploadPdfFileToS3(final String bucketName, final String directory, final Path tempFile) throws IOException {
        final var headObject = s3Client.headObject(h -> h.bucket(bucketName).key(format("%s/last.png", directory)).build());
        final var metadata = of(
                "Content-Type", "application/pdf",
                S3_OBJECT_INITIATOR_EMAIL_ATTR, headObject.metadata().get(S3_OBJECT_INITIATOR_EMAIL_ATTR),
                S3_OBJECT_INITIATOR_NAME_ATTR, headObject.metadata().get(S3_OBJECT_INITIATOR_NAME_ATTR)
        );
        try (final var pdfFileStream = newInputStream(tempFile)) {
            final var putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(format("%s/%s.pdf", directory, "book"))
                    .metadata(metadata)
                    .build();
            s3Client.putObject(putRequest, fromBytes(toByteArray(pdfFileStream)));
        }
    }

    private byte[] readObject(final String bucketName, final String key) {
        final var getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try (final var stream = s3Client.getObjectAsBytes(getRequest).asInputStream()) {
            return stream.readAllBytes();
        } catch (IOException e) {
            System.err.printf("Error while trying to read object %s from bucket %s%n", key, bucketName);
            throw new RuntimeException(e);
        }
    }

    private static final Path LAMBDA_TMP_DIR = get("/tmp");
}
