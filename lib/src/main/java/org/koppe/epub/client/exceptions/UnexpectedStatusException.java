package org.koppe.epub.client.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UnexpectedStatusException extends Exception {
    public UnexpectedStatusException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
