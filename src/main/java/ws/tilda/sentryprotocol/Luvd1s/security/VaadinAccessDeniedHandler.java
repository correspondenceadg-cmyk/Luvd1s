package ws.tilda.sentryprotocol.Luvd1s.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

public class VaadinAccessDeniedHandler implements ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(VaadinAccessDeniedHandler.class);

    @Override
    public void error(ErrorEvent event) {
        Throwable cause = event.getThrowable();
        while (cause != null && !(cause instanceof AccessDeniedException)) {
            cause = cause.getCause();
        }
        if (cause != null) {
            UI ui = UI.getCurrent();
            if (ui != null) {
                ui.access(() -> ui.getPage().setLocation("/login?denied"));
            }
            return;
        }
        log.error("Unhandled Vaadin error", event.getThrowable());
    }
}