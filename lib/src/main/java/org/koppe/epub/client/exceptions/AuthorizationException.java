package org.koppe.epub.client.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AuthorizationException extends Exception {
    public AuthorizationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
