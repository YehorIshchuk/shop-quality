package com.kira.shop.application.port;

/** Контракт відправлення повідомлень клієнтам. */
public interface Notifier {
    void send(String email, String subject, String body);
}
