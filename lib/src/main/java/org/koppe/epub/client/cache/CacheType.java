package org.koppe.epub.client.cache;

import org.koppe.epub.client.dto.CredentialDto;

import lombok.Getter;

public enum CacheType {
    CREDENTIALS(CredentialDto.class),
    EPUBS(CredentialDto.class),
    AUTHORS(CredentialDto.class),
    GENRES(CredentialDto.class),
    TAGS(CredentialDto.class);

    @Getter
    private Class<?> type;

    private CacheType(Class<?> type) {
        this.type = type;
    }
}
