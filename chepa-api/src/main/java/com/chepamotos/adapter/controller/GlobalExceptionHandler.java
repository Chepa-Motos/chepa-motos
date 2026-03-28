package com.chepamotos.adapter.controller;

import com.chepamotos.adapter.dto.ApiErrorResponse;
import com.chepamotos.domain.exception.InvoiceAlreadyCancelledException;
import com.chepamotos.domain.exception.InvoiceNotFoundException;
import com.chepamotos.domain.exception.LiquidationAlreadyExistsException;
import com.chepamotos.domain.exception.MechanicNotFoundException;
import com.chepamotos.domain.exception.VehicleNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MechanicNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMechanicNotFound(MechanicNotFoundException exception) {
        return buildError(HttpStatus.NOT_FOUND, "MECHANIC_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleVehicleNotFound(VehicleNotFoundException exception) {
        return buildError(HttpStatus.NOT_FOUND, "VEHICLE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleInvoiceNotFound(InvoiceNotFoundException exception) {
        return buildError(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvoiceAlreadyCancelledException.class)
    public ResponseEntity<ApiErrorResponse> handleInvoiceAlreadyCancelled(InvoiceAlreadyCancelledException exception) {
        return buildError(HttpStatus.CONFLICT, "INVOICE_ALREADY_CANCELLED", exception.getMessage());
    }

    @ExceptionHandler(LiquidationAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleLiquidationAlreadyExists(LiquidationAlreadyExistsException exception) {
        return buildError(HttpStatus.CONFLICT, "LIQUIDATION_ALREADY_EXISTS", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Validation failed");

        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations()
                .stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .orElse("Validation failed");

        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected internal error");
    }

    private ResponseEntity<ApiErrorResponse> buildError(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(code, message));
    }
}
