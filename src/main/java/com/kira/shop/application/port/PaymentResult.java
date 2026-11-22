package com.kira.shop.application.port;

/** Результат спроби оплати — незмінний value object (Java 16+ record). */
public record PaymentResult(boolean success, String transactionId, String message) {

    public static PaymentResult ok(String transactionId) {
        return new PaymentResult(true, transactionId, "OK");
    }

    public static PaymentResult declined(String message) {
        return new PaymentResult(false, null, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getMessage() {
        return message;
    }
}
