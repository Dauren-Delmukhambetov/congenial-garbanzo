package org.serverless.oqu.kerek.model;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.URL;

@Getter
@RequiredArgsConstructor
@Builder
public class BookInfo {
    private final String id;
    private final String title;
    private final String author;
    private final String imageUrl;
    private final String status;
}
