package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import org.serverless.template.LambdaTestContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookHandlerTest {

    private final BookHandler handler = new BookHandler();
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    @Test
    void shouldReturn404StatusCode() {
        final var request = new BookHandler.BookParsingRequest("http://kazneb.kz/bookView/view/?brId=error&simple=true&lang=kk#");
        final var requestContext = new APIGatewayProxyRequestEvent.ProxyRequestContext()
                .withRequestId(UUID.randomUUID().toString());
        final var apiGatewayProxyRequestEvent = new APIGatewayProxyRequestEvent()
                .withRequestContext(requestContext)
                .withBody(gson.toJson(request));

        final var actual = handler.handleRequest(apiGatewayProxyRequestEvent, new LambdaTestContext());

        assertEquals(404, actual.getStatusCode());
        assertEquals("Pages URLs have not been found on the given URL", actual.getBody());
    }
}
