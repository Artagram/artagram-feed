package com.artagram.feed.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ApiException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() { return status; }
    public String getCodigo() { return codigo; }

    public static ApiException notFound(String codigo, String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, codigo, mensaje);
    }

    public static ApiException badRequest(String codigo, String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, codigo, mensaje);
    }

    public static ApiException unauthorized(String codigo, String mensaje) {
        return new ApiException(HttpStatus.UNAUTHORIZED, codigo, mensaje);
    }

    public static ApiException forbidden(String codigo, String mensaje) {
        return new ApiException(HttpStatus.FORBIDDEN, codigo, mensaje);
    }
}
