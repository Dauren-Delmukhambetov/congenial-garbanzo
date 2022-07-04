package org.serverless.template;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ClientException extends RuntimeException {
    private final Integer statusCode;
    private final String message;
}
