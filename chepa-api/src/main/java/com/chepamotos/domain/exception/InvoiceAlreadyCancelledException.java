package com.chepamotos.domain.exception;

public class InvoiceAlreadyCancelledException extends RuntimeException {

    public InvoiceAlreadyCancelledException(Long invoiceId) {
        super("Invoice is already cancelled: " + invoiceId);
    }
}