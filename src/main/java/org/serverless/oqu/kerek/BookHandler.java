package org.serverless.oqu.kerek;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.serverless.template.ApiGatewayEventHandler;
import org.serverless.template.ClientException;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.serverless.oqu.kerek.HtmlParseUtils.parseBookPagesPath;

public class BookHandler extends ApiGatewayEventHandler<BookHandler.BookParsingRequest, BookHandler.BookParsingResponse> {

    public BookHandler() { super(BookHandler.BookParsingRequest.class);}

    @Override
    protected BookParsingResponse doHandleRequest(BookParsingRequest input) {

        final var pages = parseBookPagesPath(input.url);

        if (pages.isEmpty()) throw new ClientException(404, "Pages URLs have not been found on the given URL");

        return new BookParsingResponse(pages.stream().limit(10).collect(toList()));
    }

    @Getter
    @RequiredArgsConstructor
    static class BookParsingResponse {
        private final List<String> urls;
    }

    @Getter
    @RequiredArgsConstructor
    static class BookParsingRequest {
        private final String url;
    }
}
