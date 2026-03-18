package org.koppe.epub.client.configuration;

import lombok.Getter;

public enum ApiEndpoints {
    /**
     * Endpoint for logging into the application
     */
    login("/auth/login"),
    /**
     * Endpoint for refreshing the jwt token
     */
    refresh("/auth/refresh"),
    /**
     * Endpoint for finding all epubs
     */
    epubs_getAll("/epubs");

    @Getter
    private String url;

    private ApiEndpoints(String url) {
        this.url = url;
    }
}
