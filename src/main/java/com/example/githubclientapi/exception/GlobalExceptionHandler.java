package com.example.githubclientapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({NotFoundException.class, ForbiddenException.class, NotAcceptableException.class})
    public ResponseEntity<ErrorResponseBody> handleCustomExceptions(RuntimeException ex) {
        int status;
        String errorMessage;

        if (ex instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND.value();
            errorMessage = ex.getMessage();
        } else if (ex instanceof ForbiddenException) {
            status = HttpStatus.FORBIDDEN.value();
            errorMessage = ex.getMessage();
        } else if (ex instanceof NotAcceptableException) {
            status = HttpStatus.NOT_ACCEPTABLE.value();
            errorMessage = ex.getMessage();
        } else if (ex instanceof InternalServerErrorException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR.value();
            errorMessage = ex.getMessage();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR.value();
            errorMessage = "Something went wrong";
        }

        ErrorResponseBody errorResponseBody = new ErrorResponseBody(status, errorMessage);
        return ResponseEntity.status(status).body(errorResponseBody);
    }
}