package br.com.microservices.orchestrated.orchestratorservice.config.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ControllerAdvice
public class ExceptionGlobalHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> handleValidationException(ValidationException exception) {
        var details = new ExceptionDetails(BAD_REQUEST.value(), exception.getMessage());
        return ResponseEntity.badRequest().body(details);
    }

}
