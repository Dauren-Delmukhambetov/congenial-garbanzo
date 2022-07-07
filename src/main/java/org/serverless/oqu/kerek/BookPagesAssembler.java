package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import org.serverless.template.S3EventHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static com.itextpdf.io.image.ImageDataFactory.create;
import static java.lang.String.format;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Paths.get;
import static java.util.stream.Collectors.toList;

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
        final var objectKeys = s3Client.listObjectsV2(bucketName, directory)
                .getObjectSummaries()
                .stream()
                .map(S3ObjectSummary::getKey)
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
        try (final var pdfFileStream = newInputStream(tempFile)) {
            final var metadata = new ObjectMetadata();
            metadata.setContentType("application/pdf");
            s3Client.putObject(bucketName, format("%s/%s.pdf", directory, UUID.randomUUID()), pdfFileStream, metadata);
        }
    }

    private byte[] readObject(final String bucketName, final String key) {
        try (final var stream = s3Client.getObject(bucketName, key).getObjectContent()) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path LAMBDA_TMP_DIR = get("/tmp");
}
