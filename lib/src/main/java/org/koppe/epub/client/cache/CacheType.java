package org.koppe.epub.client.cache;

import org.koppe.epub.client.dto.CredentialDto;
import org.koppe.epub.client.dto.EpubEditionDto;

import lombok.Getter;

public enum CacheType {
    CREDENTIALS(CredentialDto.class),
    EPUBS(CredentialDto.class),
    EDITIONS(EpubEditionDto.class),
    AUTHORS(CredentialDto.class),
    GENRES(CredentialDto.class),
    REQUEST(RequestCacheEntity.class),
    TAGS(CredentialDto.class);

    @Getter
    private Class<?> type;

    private CacheType(Class<?> type) {
        this.type = type;
    }
}
