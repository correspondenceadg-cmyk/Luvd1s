package ws.tilda.sentryprotocol.Luvd1s.ui.components;

import com.vaadin.flow.component.Component;

public final class Feedback {

    private Feedback() {}

    public static void pulseSuccess(Component c) {
        c.getElement().executeJs(
                "this.animate([{boxShadow:'0 0 0 0 rgba(76,175,80,.7)'}," +
                "{boxShadow:'0 0 0 12px rgba(76,175,80,0)'}]," +
                "{duration:600,easing:'ease-out'});");
    }

    public static void shake(Component c) {
        c.getElement().executeJs(
                "this.animate([{transform:'translateX(0)'}," +
                "{transform:'translateX(-6px)'},{transform:'translateX(6px)'}," +
                "{transform:'translateX(-4px)'},{transform:'translateX(0)'}]," +
                "{duration:300});");
    }

    public static void fadeRefresh(Component c) {
        c.getElement().executeJs(
                "this.animate([{opacity:0.4},{opacity:1}]," +
                "{duration:400,easing:'ease-out'});");
    }
}
