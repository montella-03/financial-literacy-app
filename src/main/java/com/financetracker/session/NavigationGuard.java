package com.financetracker.session;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

@Component
public class NavigationGuard implements VaadinServiceInitListener {
    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(ui -> ui.getUI().addBeforeEnterListener(navigation -> {
            if (!"login".equals(navigation.getLocation().getPath()) && !AppSession.isEntered()) {
                navigation.rerouteTo("login");
            }
        }));
    }
}
