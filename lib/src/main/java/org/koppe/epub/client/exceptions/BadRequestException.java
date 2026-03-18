package org.koppe.epub.client.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class BadRequestException extends Exception {
    public BadRequestException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
