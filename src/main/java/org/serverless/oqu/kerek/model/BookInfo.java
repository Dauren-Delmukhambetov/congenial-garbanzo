package org.serverless.oqu.kerek.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.URL;

@Getter
@RequiredArgsConstructor
public class BookInfo {
    private final String title;
    private final String author;
    private final String imageUrl;
    private final URL linkToDownload;
}
