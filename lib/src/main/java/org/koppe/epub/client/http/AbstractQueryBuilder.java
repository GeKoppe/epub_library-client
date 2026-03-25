package org.koppe.epub.client.http;

import lombok.NoArgsConstructor;

/**
 * Contains query parameters used by different entities
 */
@NoArgsConstructor
public abstract class AbstractQueryBuilder {
    /**
     * Builder
     */
    private final HttpQuery.Builder builder = new HttpQuery.Builder();

    /**
     * Signalises whether authors should be returned by the api when getting an
     * associated entity.
     * 
     * @param w Set to true, if authors should be returned by the api call
     * @return This builder
     */
    public AbstractQueryBuilder withAuthors(boolean w) {
        builder.addParam(Boolean.class, "with_authors", w);
        return this;
    }

    /**
     * Signalises whether metadata of editions should be returned by the api when
     * getting editions.
     * 
     * @param w Set to true, if metadata should be returned as well.
     * @return
     */
    public AbstractQueryBuilder withMetadata(boolean w) {
        builder.addParam(Boolean.class, "with_metadata", w);
        return this;
    }

    /**
     * Signalises whether tags should be returned by the api as well.
     * 
     * @param w True, if tags should be returned
     * @return This builder
     */
    public AbstractQueryBuilder withTags(boolean w) {
        builder.addParam(Boolean.class, "with_tags", w);
        return this;
    }

    /**
     * Signalises whether genres should be returned by the api as well.
     * 
     * @param w True, if genres should be returned
     * @return This builder
     */
    public AbstractQueryBuilder withGenres(boolean w) {
        builder.addParam(Boolean.class, "with_genres", w);
        return this;
    }

    /**
     * Used in paged queries. Sets the page to be returned. 0-Indexed.
     * 
     * @param page Page to be returned in paged queries
     * @return This builder
     */
    public AbstractQueryBuilder page(int page) {
        builder.addParam(Integer.class, "page", page);
        return this;
    }

    /**
     * Sets page size in paged queries. Api defaults to 1000
     * 
     * @param pageSize Size of the pages in paged queries
     * @return this builder
     */
    public AbstractQueryBuilder pageSize(int pageSize) {
        builder.addParam(Integer.class, "page_size", pageSize);
        return this;
    }

    /**
     * Used in update queries. If true, values given as null will overwrite the
     * existing values.
     * 
     * @param overwrite If true, null values will overwrite existing values
     * @return This builder
     */
    public AbstractQueryBuilder overwriteNulls(boolean overwrite) {
        builder.addParam(Boolean.class, "overwrite_nulls", overwrite);
        return this;
    }

    /**
     * Signalises whether book series should be returned by the api as well.
     * 
     * @param w True, if book series should be returned
     * @return This builder
     */
    public AbstractQueryBuilder withSeries(boolean w) {
        builder.addParam(Boolean.class, "with_series", w);
        return this;
    }

    /**
     * Returns the http query builder
     * 
     * @return http query builder
     */
    protected HttpQuery.Builder getBuilder() {
        return builder;
    }

    /**
     * Builds the http query
     * 
     * @return Http query
     */
    public HttpQuery build() {
        return builder.build();
    }
}
