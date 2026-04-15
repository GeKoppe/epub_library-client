package org.koppe.epub.client.dto;

import lombok.Data;

/**
 * Dto that just contains a single id. Mostly used to connect entities with each
 * other, for example adding an epub to an author.
 */
@Data
public class IdDto {
    private Long id;
}
