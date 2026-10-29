package com.financetracker.views;

import com.financetracker.session.AppSession;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.streams.DownloadHandler;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

final class FeatureSupport {
    private FeatureSupport() {
    }

    static void attempt(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE);
        } catch (org.springframework.dao.OptimisticLockingFailureException |
                 org.springframework.dao.DataIntegrityViolationException ex) {
            Notification.show("This record changed in another tab or already exists. Refresh and try again.", 5000, Notification.Position.MIDDLE);
        }
    }

    static Anchor download(String label, String filename, Supplier<String> content) {
        String owner = AppSession.ownerId();
        Anchor anchor = new Anchor((DownloadHandler) event -> {
            byte[][] bytes = new byte[1][];
            event.getUI().accessSynchronously(() -> {
                if (!AppSession.isEntered() || !owner.equals(AppSession.ownerId()))
                    throw new SecurityException("Please sign in again");
                bytes[0] = content.get().getBytes(StandardCharsets.UTF_8);
            });
            event.setFileName(filename);
            event.setContentType("text/csv; charset=UTF-8");
            event.setContentLength(bytes[0].length);
            event.getResponse().setHeader("Cache-Control", "no-store");
            event.getOutputStream().write(bytes[0]);
        }, label);
        anchor.addClassName("editorial-link");
        return anchor;
    }
}
