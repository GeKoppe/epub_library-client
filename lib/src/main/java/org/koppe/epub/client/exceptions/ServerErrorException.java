package org.koppe.epub.client.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ServerErrorException extends Exception {
    public ServerErrorException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
