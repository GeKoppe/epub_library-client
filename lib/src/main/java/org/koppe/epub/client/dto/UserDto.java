package org.koppe.epub.client.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode.Include;

/**
 * Represents a user
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class UserDto {
    /**
     * Id of the user
     */
    @Include
    @lombok.ToString.Include
    private Long id;
    /**
     * Name of the user
     */
    @Include
    @lombok.ToString.Include
    private String name;
    /**
     * Password of the user
     */
    private String password;
}
