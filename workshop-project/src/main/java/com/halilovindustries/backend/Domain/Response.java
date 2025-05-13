package com.halilovindustries.backend.Domain;

public class Response<T> {
    private final boolean ok;
    private final String error;
    private final T data;

    private Response(boolean ok, String error, T data) {
        this.ok = ok;
        this.error = error;
        this.data = data;
    }

    /** Success without payload **/
    public static <T> Response<T> ok() {
        return new Response<>(true, null, null);
    }

    /** Success with payload **/
    public static <T> Response<T> ok(T data) {
        return new Response<>(true, null, data);
    }

    /** Failure with error message **/
    public static <T> Response<T> error(String errorMessage) {
        return new Response<>(false, errorMessage, null);
    }

    public boolean isOk() {
        return ok;
    }

    public String getError() {
        return error;
    }

    public T getData() {
        return data;
    }
}
