package ws.tilda.sentryprotocol.Luvd1s.security;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

@Component
public class VaadinErrorConfig implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(sessionInit -> {
            sessionInit.getSession().setErrorHandler(new VaadinAccessDeniedHandler());
        });
    }
}
