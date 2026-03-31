package org.koppe.epub.client.http;

import java.time.LocalDate;

import lombok.NoArgsConstructor;

/**
 * Convencience class for building the http query when getting EpubDto
 * entities from the api.
 * 
 * Example usage:
 * 
 * <pre>
 * {@code
 * HttpQuery query = new EpubQueryBuilder()
 *         .withEditions(true)
 *         .withAuthors(true)
 *         .withMetadata(true)
 *         .withTableOfContents(true)
 *         .build();
 * 
 * EpubClient client = EpubClientFactory.newDefaultClient("http://localhost:9093");
 * // Must be wrapped in try catch of course
 * EpubDto epub = client.getEpub("user", "password", 1, query);
 * }
 * </pre>
 */
@NoArgsConstructor
public class EpubQueryBuilder extends AbstractQueryBuilder {

    public static EpubQueryBuilder newInstance() {
        return new EpubQueryBuilder();
    }

    /**
     * Signalises whether editions should be returned by the api when getting epubs.
     * 
     * @param w Set to true, if editions should be returned by the api call
     * @return This builder
     */
    public EpubQueryBuilder withEditions(boolean w) {
        getBuilder().addParam(Boolean.class, "with_editions", w);
        return this;
    }

    /**
     * Find all epubs which titles contain the given string.
     * 
     * @param title Substring of the title to find
     * @return This builder
     */
    public EpubQueryBuilder titleContains(String title) {
        getBuilder().addParam(String.class, "title_contains", title);
        return this;
    }

    public EpubQueryBuilder publishedBefore(LocalDate date) {
        getBuilder().addParam(LocalDate.class, "published_before", date);
        return this;
    }

    public EpubQueryBuilder publishedAfter(LocalDate date) {
        getBuilder().addParam(LocalDate.class, "published_after", date);
        return this;
    }

    public EpubQueryBuilder uploadedBefore(LocalDate date) {
        getBuilder().addParam(LocalDate.class, "uploaded_before", date);
        return this;
    }

    public EpubQueryBuilder uploadedAfter(LocalDate date) {
        getBuilder().addParam(LocalDate.class, "uploaded_after", date);
        return this;
    }

    public EpubQueryBuilder withTableOfContents(boolean w) {
        getBuilder().addParam(Boolean.class, "with_toc", w);
        return this;
    }

    public EpubQueryBuilder uploadGuid(String uploadGuid) {
        getBuilder().addParam(String.class, "upload-guid", uploadGuid);
        return this;
    }

    // #region
    /**
     * Sets the download guid for downloading an epub or a cover.
     * 
     * @param downloadGuid Download guid of the epub edition, the epub or cover
     *                     should be downloaded of.
     * @return This builder
     */
    public EpubQueryBuilder downloadGuid(String downloadGuid) {
        getBuilder().addParam(String.class, "download-guid", downloadGuid);
        return this;
    }

    // #region download cover
    /**
     * Used only when using one of the download methods, otherwise ignored. If set
     * to true, cover picture instead of epub is downloaded.
     * 
     * @param downloadCover If true, cover instead of epub is downloaded
     * @return This builder
     */
    public EpubQueryBuilder downloadCover(boolean downloadCover) {
        getBuilder().addParam(Boolean.class, "cover", downloadCover);
        return this;
    }
}
