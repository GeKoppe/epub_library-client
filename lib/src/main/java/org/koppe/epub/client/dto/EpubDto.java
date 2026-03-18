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
 * Represents a single epub
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EpubDto {
    /**
     * Id of the epub
     */
    @Include
    @lombok.ToString.Include
    private Long id;
    /**
     * Title of the epub
     */
    @Include
    @lombok.ToString.Include
    private String title;
    /**
     * Date the epub was published
     */
    @Include
    @lombok.ToString.Include
    private LocalDate publishDate;
    /**
     * Date the epub has been uploaded
     */
    @Include
    @lombok.ToString.Include
    private LocalDate uploadDate;
    /**
     * List of all authors associated with this epub
     */
    private List<AuthorDto> authors;
    /**
     * List of all genres associated with this epub
     */
    private List<GenreDto> genres;
    /**
     * List of all editions of this epub
     */
    private List<EpubEditionDto> editions;
    /**
     * List of all tags associated with this epub
     */
    private List<TagDto> tags;
    /**
     * List of all book series associated with this epub
     */
    private List<SeriesDto> series;
}
