package org.koppe.epub.client.cache;

import org.koppe.epub.client.dto.AuthorDto;
import org.koppe.epub.client.dto.CredentialDto;
import org.koppe.epub.client.dto.EpubDto;
import org.koppe.epub.client.dto.EpubEditionDto;
import org.koppe.epub.client.dto.GenreDto;
import org.koppe.epub.client.dto.TagDto;

import lombok.Getter;

/**
 * All types of caches.
 */
public enum CacheType {
    /**
     * Cache for credentials
     */
    CREDENTIALS(String.class, CredentialDto.class),
    /**
     * Cache for epubs
     */
    EPUBS(Long.class, EpubDto.class),
    /**
     * Cache for epub editions
     */
    EDITIONS(Long.class, EpubEditionDto.class),
    /**
     * Cache for authors
     */
    AUTHORS(Long.class, AuthorDto.class),
    /**
     * Cache for genres
     */
    GENRES(Long.class, GenreDto.class),
    /**
     * Cache for requests
     */
    REQUEST(String.class, RequestCacheEntity.class),
    /**
     * Cache for tags
     */
    TAGS(Long.class, TagDto.class);

    /**
     * Type of elements the cache holds
     */
    @Getter
    private Class<?> type;
    @Getter
    private Class<?> keys;

    /**
     * Default constructor
     * 
     * @param type Type of values the cache holds
     */
    private CacheType(Class<?> keys, Class<?> type) {
        this.keys = keys;
        this.type = type;
    }
}
