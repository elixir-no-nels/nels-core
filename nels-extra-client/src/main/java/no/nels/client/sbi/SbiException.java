package no.nels.client.sbi;

import javax.ws.rs.core.Response;

public final class SbiException extends Exception {
    private int statusCode;

    public SbiException(String message) {
        this(message, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    public SbiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
