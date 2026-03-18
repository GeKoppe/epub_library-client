package org.koppe.epub.client.cache;

import org.koppe.epub.client.EpubClient;
import org.koppe.epub.client.dto.EpubDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@SuppressWarnings("unused")
public class EpubCache extends AbstractCache<Long, EpubDto> {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(EpubCache.class);
}
