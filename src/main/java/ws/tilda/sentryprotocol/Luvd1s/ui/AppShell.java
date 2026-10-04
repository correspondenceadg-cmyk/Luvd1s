package ws.tilda.sentryprotocol.Luvd1s.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.shared.communication.PushMode;

@Push(PushMode.AUTOMATIC)
@StyleSheet("context://styles/skeleton.css")
public class AppShell implements AppShellConfigurator {

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addInlineWithContents(
                "try{var t=localStorage.getItem('luvd1s-theme');" +
                "if(t==='dark'){document.documentElement.setAttribute('theme','dark');}" +
                "}catch(e){}",
                "theme-init"
        );
    }
}