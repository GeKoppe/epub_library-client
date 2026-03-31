package org.koppe.epub.client.http;

import lombok.NoArgsConstructor;

/**
 * Builder for queries associated with authors
 */
@NoArgsConstructor
public class AuthorQueryBuilder extends AbstractQueryBuilder {

    /**
     * If set to true and given to a delete query, all epubs the given author is
     * associated with are deleted as well. Use with caution!
     * 
     * @param delete Set to true to delete all epubs associated with the given
     *               author as well.
     * @return This builder
     */
    public AuthorQueryBuilder deleteEpubsAsWell(boolean delete) {
        getBuilder().addParam(Boolean.class, "with_epubs", delete);
        return this;
    }

    public AuthorQueryBuilder withEpubs(boolean w) {
        getBuilder().addParam(Boolean.class, "with_epubs", w);
        return this;
    }
}
