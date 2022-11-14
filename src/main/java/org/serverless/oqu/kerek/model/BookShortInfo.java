package org.serverless.oqu.kerek.model;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class BookShortInfo {
    private final String title;
    private final String author;
    private final String imageUrl;
}
