package org.serverless.oqu.kerek;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.serverless.oqu.kerek.util.HtmlParseUtils.parseBookInfo;

class HtmlParseUtilsTest {

    @ParameterizedTest
    @CsvSource(
            value = {
                    "1628186|Шәмші Қалдаяқов - ән патшасы|Әкімқұлов, Е.",
                    "82809|Айқап|n/a",
                    "82915|Ақ кеме. Бетпе-бет. Алғашқы ұстаз|Айтматов, Ш.",
                    "1093883|Каталог книгопродавца двора Его Императорского Величества А. Смирдина (сына) и К|n/a"
            },
            delimiter = '|',
            nullValues = {"n/a"}
    )
    void shouldParseBookInfo(final String bookId, final String title, final String author) {
        final var bookInfo = parseBookInfo(format("https://kazneb.kz/ru/catalogue/view/%s", bookId));

        assertTrue(bookInfo.isPresent());
        bookInfo.ifPresent(b -> {
            assertNotNull(b.getImageUrl());
            assertEquals(title, b.getTitle());
            assertEquals(author, b.getAuthor());
        });
    }
}
