package org.koppe.epub.client.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CacheMissException extends Exception {
    public CacheMissException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
