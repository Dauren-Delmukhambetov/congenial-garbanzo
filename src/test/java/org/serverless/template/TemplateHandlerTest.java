package org.serverless.template;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TemplateHandlerTest {

    private final TemplateHandler handler = new TemplateHandler();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Test
    void shouldHandleRequest() {
        final var request = new TemplateHandler.Request("Hello, world!");
        final var requestContext = new APIGatewayProxyRequestEvent.ProxyRequestContext()
                .withRequestId(UUID.randomUUID().toString());
        final var apiGatewayProxyRequestEvent = new APIGatewayProxyRequestEvent()
                .withRequestContext(requestContext)
                .withBody(gson.toJson(request));

        final var actual = handler.handleRequest(apiGatewayProxyRequestEvent, new LambdaTestContext());
        final var response = gson.fromJson(actual.getBody(), TemplateHandler.Response.class);

        assertEquals(200, actual.getStatusCode());
        assertEquals("Hello, world!", response.getData());
    }
}
