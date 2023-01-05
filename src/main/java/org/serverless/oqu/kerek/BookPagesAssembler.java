package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Link;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.extern.slf4j.Slf4j;
import org.serverless.template.S3EventHandler;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.nio.file.Path;

import static com.itextpdf.io.image.ImageDataFactory.create;
import static java.lang.String.format;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Paths.get;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.serverless.oqu.kerek.util.EnvironmentUtils.getBooksBucketName;
import static software.amazon.awssdk.core.sync.RequestBody.fromBytes;

@Slf4j
public class BookPagesAssembler extends S3EventHandler {

    static {
        initS3Client();
    }

    @Override
    protected Void doHandleRequest(S3EventNotification.S3EventNotificationRecord input, Context context) {
        try {
            log.info("Starting processing S3 Event notification record (Object Key = {})", input.getS3().getObject().getKey());

            final var bucketName = getBooksBucketName();
            final var directory = input.getS3().getObject().getKey().split("/")[0];
            final var tempFile = createTempFile(LAMBDA_TMP_DIR, "book-", ".pdf");

            assembleBookPages(bucketName, directory, tempFile);
            uploadPdfFileToS3(bucketName, directory, tempFile);

            log.info("Completed processing S3 Event notification record (Object Key = {})", input.getS3().getObject().getKey());
        } catch (Exception e) {
            log.error("Error occurred while processing S3 Event notification record (Object Key = {}): {}", input.getS3().getObject().getKey(), e.getMessage());
        }
        return null;
    }

    private void assembleBookPages(final String bucketName, final String directory, final Path tempFile) throws IOException {
        final var objectKeys = s3Client.listObjectsV2(b -> b.bucket(bucketName).prefix(directory))
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
            addLastPage(pdfDocument, document);
        }
    }

    private void addLastPage(final PdfDocument pdfDocument, final Document document) {
        final var lastPageSize = pdfDocument.getLastPage().getPageSize();
        pdfDocument.addNewPage(new PageSize(lastPageSize.getWidth(), lastPageSize.getHeight()));

        final var paragraph = new Paragraph()
                .setFixedPosition(pdfDocument.getNumberOfPages(), 0, lastPageSize.getHeight() / 2, UnitValue.createPercentValue(100))
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(24)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        paragraph.add(new Text("This book downloaded from "))
                .add(new Link("oqukerek.de", PdfAction.createURI("https://oqukerek.de/")).setBold());

        document.add(paragraph);
    }

    private void uploadPdfFileToS3(final String bucketName, final String directory, final Path tempFile) throws IOException {
        try (final var pdfFileStream = newInputStream(tempFile)) {
            final var putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(format("%s/%s.pdf", directory, "book"))
                    .contentType("application/pdf")
                    .build();
            s3Client.putObject(putRequest, fromBytes(toByteArray(pdfFileStream)));
        }
    }

    private byte[] readObject(final String bucketName, final String key) throws IOException {
        final var getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try (final var stream = s3Client.getObjectAsBytes(getRequest).asInputStream()) {
            return stream.readAllBytes();
        } catch (IOException e) {
            log.error("Error while trying to read object {} from bucket {}", key, bucketName);
            throw e;
        }
    }

    private static final Path LAMBDA_TMP_DIR = get("/tmp");
}
