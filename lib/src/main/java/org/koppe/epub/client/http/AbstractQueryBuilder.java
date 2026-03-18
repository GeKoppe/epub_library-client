package org.koppe.epub.client.http;

import lombok.NoArgsConstructor;

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

    public AbstractQueryBuilder withTags(boolean w) {
        builder.addParam(Boolean.class, "with_tags", w);
        return this;
    }

    public AbstractQueryBuilder page(int page) {
        builder.addParam(Integer.class, "page", page);
        return this;
    }

    public AbstractQueryBuilder pageSize(int pageSize) {
        builder.addParam(Integer.class, "page_size", pageSize);
        return this;
    }

    protected HttpQuery.Builder getBuilder() {
        return builder;
    }

    public HttpQuery build() {
        return builder.build();
    }
}
