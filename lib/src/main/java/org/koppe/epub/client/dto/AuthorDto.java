package org.koppe.epub.client.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode.Include;

/**
 * Represents a single author
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuthorDto {
    /**
     * Id of the author
     */
    @Include
    @lombok.ToString.Include
    private Long id;
    @Include
    @lombok.ToString.Include
    private String firstName;
    @Include
    @lombok.ToString.Include
    private String surname;
    @Include
    @lombok.ToString.Include
    private LocalDate birthDate;
    @Include
    @lombok.ToString.Include
    private LocalDate deathDate;
    @Include
    @lombok.ToString.Include
    private String description;
    private List<EpubDto> epubs;
    private List<TagDto> tags;
}
