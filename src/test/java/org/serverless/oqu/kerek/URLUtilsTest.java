package org.serverless.oqu.kerek;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class URLUtilsTest {

    @ParameterizedTest
    @CsvSource(
            value = {
                    "http://example.com/bookView/view/?brId=error&simple=true&lang=kk#,brId,error",
                    "https://example.com/bookView/view/?brId=12345&simple=true&lang=kk#,brId,12345",
                    "https://example.com/bookView/view?simple=true&lang=kk#,brId,n/a"
            },
            nullValues = {"n/a"})
    void shouldExtractQueryParamValue(final String url, final String queryParam, final String expected) {
        assertEquals(expected, URLUtils.extractQueryParamValue(url, queryParam));
    }

}