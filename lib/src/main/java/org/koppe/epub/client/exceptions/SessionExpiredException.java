package org.koppe.epub.client.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SessionExpiredException extends Exception {
    public SessionExpiredException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
