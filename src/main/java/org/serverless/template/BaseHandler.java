package org.serverless.template;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public abstract class BaseHandler<T, R, I, O> implements RequestHandler<I, O> {

    protected abstract R doHandleRequest(final T input);

    protected void log(Context context, String message, Object... args) {
        context.getLogger().log(String.format(message, args));
    }

}
