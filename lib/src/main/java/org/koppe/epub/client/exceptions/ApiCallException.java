package org.koppe.epub.client.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ApiCallException extends Exception {
    public ApiCallException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
