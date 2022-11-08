package org.serverless.oqu.kerek.model;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.time.OffsetDateTime;

@Builder
@Value
@RequiredArgsConstructor
public class BookRequestContext {
    String bookId;
    String userEmail;
    OffsetDateTime requestedAt;
}
