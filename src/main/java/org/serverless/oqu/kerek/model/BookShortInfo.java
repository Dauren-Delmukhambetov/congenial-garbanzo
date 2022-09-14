package org.serverless.oqu.kerek.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BookShortInfo {
    private final String title;
    private final String author;
    private final String imageUrl;
}
