package org.koppe.epub.client.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class IllegalFileTypeException extends Exception {
    public IllegalFileTypeException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
