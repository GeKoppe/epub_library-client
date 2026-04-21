package org.koppe.epub.client.cache;

import org.koppe.epub.client.dto.AuthorDto;

/**
 * Cache for AuthorDto elements. Does not have a custom refresh algorithm, gets
 * filled manually by the EpubClient.
 */
public class AuthorCache extends AbstractCache<Long, AuthorDto> {

}
