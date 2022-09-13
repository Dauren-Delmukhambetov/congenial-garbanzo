package org.serverless.template;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class TemplateHandler extends ApiGatewayEventHandler<TemplateHandler.Request, TemplateHandler.Response>{

    public TemplateHandler() { super(TemplateHandler.Request.class); }

    @Override
    protected Response doHandleRequest(final Request input, final Context context) {
        log(context, "Template handler method has been invoked");
        return new Response(input.data);
    }

    @Override
    protected Request getRequestData(APIGatewayProxyRequestEvent input) {
        return gson.fromJson(input.getBody(), TemplateHandler.Request.class);
    }

    @Getter
    @RequiredArgsConstructor
    static class Response {
        private final String data;
    }

    @Getter
    @RequiredArgsConstructor
    static class Request {
        private final String data;
    }
}
