package com.kira.shop.infrastructure;

import com.kira.shop.application.port.Notifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/** Відправник повідомлень (лог + журнал для тестів). */
public class ConsoleNotificationSender implements Notifier {
    private static final Logger LOG = Logger.getLogger(ConsoleNotificationSender.class.getName());
    private final List<String> sent = new ArrayList<>();

    @Override
    public void send(String email, String subject, String body) {
        String message = email + " | " + subject + " | " + body;
        sent.add(message);
        LOG.info(message);
    }

    public List<String> getSent() {
        return Collections.unmodifiableList(sent);
    }
}
