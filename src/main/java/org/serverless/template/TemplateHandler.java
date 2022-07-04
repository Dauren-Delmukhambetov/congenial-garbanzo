package org.serverless.template;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class TemplateHandler extends ApiGatewayEventHandler<TemplateHandler.Request, TemplateHandler.Response>{

    public TemplateHandler() { super(TemplateHandler.Request.class); }

    @Override
    protected Response doHandleRequest(Request input) {
        return new Response(input.data);
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
