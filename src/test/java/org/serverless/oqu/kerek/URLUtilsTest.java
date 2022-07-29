package org.serverless.oqu.kerek;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.serverless.template.ClientException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class URLUtilsTest {

    @ParameterizedTest
    @CsvSource(
            value = {
                    "http://example.com/bookView/view/?brId=error&simple=true&lang=kk#,brId,error",
                    "https://example.com/bookView/view/?brId=12345&simple=true&lang=kk#,brId,12345",
            },
            nullValues = {"n/a"})
    void shouldExtractQueryParamValue(final String url, final String queryParam, final String expected) {
        assertEquals(expected, URLUtils.extractQueryParamValue(url, queryParam));
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                    "https://example.com/bookView/view/?brId=&simple=true&lang=kk#,brId,12345",
                    "https://example.com/bookView/view?simple=true&lang=kk#,brId,n/a"
            },
            nullValues = {"n/a"})
    void shouldThrowExceptionOnQueryParamValue(final String url, final String queryParam) {
        assertThrows(ClientException.class, () -> URLUtils.extractQueryParamValue(url, queryParam));
    }
}
