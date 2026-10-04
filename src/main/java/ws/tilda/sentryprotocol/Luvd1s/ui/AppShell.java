package ws.tilda.sentryprotocol.Luvd1s.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@Push(PushMode.AUTOMATIC)
@StyleSheet("context://styles/skeleton.css")
@StyleSheet("context://styles/ai-chat.css")
@StyleSheet("context://styles/micro.css")
@Theme(themeClass = Lumo.class)
public class AppShell implements AppShellConfigurator {

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addInlineWithContents(
                "try{var t=localStorage.getItem('luvd1s-theme');" +
                "if(t==='dark'){document.documentElement.setAttribute('theme','dark');}" +
                "}catch(e){}",
                Inline.Wrapping.JAVASCRIPT
        );
    }
}