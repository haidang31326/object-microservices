package com.example.bookingservice.exception;

import com.example.bookingservice.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse>  handleUserNotFoundException(UserNotFoundException e) {
        ErrorResponse err = new ErrorResponse(
                e.getMessage() ,
                HttpStatus.NOT_FOUND.value());
        return new ResponseEntity<>(err, HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(NotEnoughInventoryException.class)
    public ResponseEntity<ErrorResponse> handleNotEnoughInventoryException(NotEnoughInventoryException e) {
        ErrorResponse err = new ErrorResponse(
                e.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );
        return new ResponseEntity<>(err, HttpStatus.BAD_REQUEST);
    }
}
