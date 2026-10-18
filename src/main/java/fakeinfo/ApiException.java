package fakeinfo;

import org.springframework.http.HttpStatus;

/**
 * An API error reported to the client as {"error": message}
 */
public class ApiException extends RuntimeException {

    public static final String INCORRECT_METHOD = "Incorrect HTTP method";
    public static final String INCORRECT_ENDPOINT = "Incorrect API endpoint";
    public static final String INCORRECT_PARAMS = "Incorrect GET parameter value";
    public static final String UNKNOWN_ERROR = "Unknown error";

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
