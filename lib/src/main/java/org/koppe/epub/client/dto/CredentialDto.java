package org.koppe.epub.client.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a single session.
 */
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class CredentialDto {
    /**
     * Name of the user this session belongs to
     */
    @Include
    @lombok.ToString.Include
    private String username;
    /**
     * Session token (JSON web token)
     */
    @Include
    private String jwt;
    /**
     * Refresh token for the jwt
     */
    private String refresh;
}
