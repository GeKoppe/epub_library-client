package org.koppe.epub.client.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ForbiddenException extends Exception {
    public ForbiddenException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
