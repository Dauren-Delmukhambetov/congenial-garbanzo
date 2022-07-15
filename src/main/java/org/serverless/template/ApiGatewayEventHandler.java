package org.serverless.template;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static java.util.Map.of;

@RequiredArgsConstructor
public abstract class ApiGatewayEventHandler<T, R> extends BaseHandler<T, R, APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    protected final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    protected final Map<String, String> headers = of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "*"
    );
    protected final Class<T> inputType;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        try {
            log(context, "Starting processing request %s with input: %s", input.getRequestContext().getRequestId(), input.getBody());
            log(context, "Request processing behalf of %s", input.getRequestContext().getAuthorizer().get("jwt"));
            log(context, "Queue name from env vars is %s", System.getenv("QUEUE_NAME"));

            final var request = gson.fromJson(input.getBody(), inputType);
            final var response = gson.toJson(doHandleRequest(request, context));

            log(context, "Completed processing request %s with output: %s", input.getRequestContext().getRequestId(), response);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(response);
        } catch (ClientException e) {
            log(context, "Client exception occurred while processing request %s: %s", input.getRequestContext().getRequestId(), e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(e.getStatusCode())
                    .withHeaders(headers)
                    .withBody(e.getMessage());
        } catch (Exception e) {
            log(context, "Error occurred while processing request %s: %s", input.getRequestContext().getRequestId(), e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody("Error occurred while processing request: " + e.getMessage());
        }
    }
}
