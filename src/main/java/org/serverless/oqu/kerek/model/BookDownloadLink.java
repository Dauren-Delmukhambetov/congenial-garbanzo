package org.serverless.oqu.kerek.model;

import lombok.Getter;

import java.net.URL;

@Getter
public class BookDownloadLink {
    private final String downloadLink;

    private BookDownloadLink(String downloadLink) {
        this.downloadLink = downloadLink;
    }

    public static BookDownloadLink of(URL downloadLinkUrl) {
        return new BookDownloadLink(downloadLinkUrl.toString());
    }
}
