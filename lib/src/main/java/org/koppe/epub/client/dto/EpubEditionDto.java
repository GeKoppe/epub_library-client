package org.koppe.epub.client.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode.Include;

/**
 * Represents a single epub edition
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EpubEditionDto {
    /**
     * Id of the epub edition
     */
    @Include
    @lombok.ToString.Include
    private Long id;
    @Include
    @lombok.ToString.Include
    private String versionName;
    @Include
    @lombok.ToString.Include
    private long epubId;
    private String downloadGuid;
    private String uploadGuid;
    private EpubEditionMetadata metadata;
}
